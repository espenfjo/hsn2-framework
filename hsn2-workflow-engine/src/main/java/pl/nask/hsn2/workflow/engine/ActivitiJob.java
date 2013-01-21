/*
 * Copyright (c) NASK, NCSC
 * 
 * This file is part of HoneySpider Network 2.0.
 * 
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.workflow.engine;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.activiti.engine.impl.pvm.PvmExecution;
import org.activiti.engine.impl.pvm.PvmProcessDefinition;
import org.activiti.engine.impl.pvm.PvmProcessInstance;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.activiti.ExtendedExecutionImpl;
import pl.nask.hsn2.activiti.behavior.FatalTaskErrorException;
import pl.nask.hsn2.activiti.behavior.HSNBehavior;
import pl.nask.hsn2.bus.api.BusManager;
import pl.nask.hsn2.bus.connector.objectstore.ObjectStoreConnectorException;
import pl.nask.hsn2.bus.operations.JobStatus;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.TaskErrorReasonType;
import pl.nask.hsn2.framework.bus.FrameworkBus;
import pl.nask.hsn2.framework.workflow.engine.ProcessBasedWorkflowDescriptor;
import pl.nask.hsn2.framework.workflow.job.DefaultTasksStatistics;
import pl.nask.hsn2.framework.workflow.job.WorkflowJob;
import pl.nask.hsn2.framework.workflow.job.WorkflowJobInfo;

public class ActivitiJob implements WorkflowJob, WorkflowJobInfo {

    private final static Logger LOGGER = LoggerFactory.getLogger(ActivitiJob.class);

    private final ProcessBasedWorkflowDescriptor<PvmProcessDefinition> workflowDefinitionDescriptor;

    private final PvmProcessDefinition processDefinition;
    private PvmProcessInstance processInstance;
    private long jobId;

    private TaskErrorReasonType failureReason;
    private boolean running = false;

    private String failureDescription;

    private Map<String, Properties> userConfig;

    private long startTime = 0;
    private long endTime = 0;

    private String lastActiveStepName;

    private long objectDataId;

    private DefaultTasksStatistics stats = new DefaultTasksStatistics();

	public ActivitiJob(PvmProcessDefinition processDefinition,
			ProcessBasedWorkflowDescriptor<PvmProcessDefinition> workflowDefinitionDescriptorImpl,
			Map<String, Properties> workflowConfig) {
        this.processDefinition = processDefinition;
        this.workflowDefinitionDescriptor = workflowDefinitionDescriptorImpl;
        this.userConfig = workflowConfig;
    }

    @Override
    public synchronized void start(long jobId) {
        if (processInstance != null) {
            throw new IllegalStateException("Job already started");
        } else {
            processInstance = processDefinition.createProcessInstance();
            this.objectDataId = createInitialObject(jobId);
            ExecutionWrapper utils = new ExecutionWrapper(processInstance);
            utils.initProcessState(jobId, objectDataId, userConfig, workflowDefinitionDescriptor, stats);
            this.startTime  = System.currentTimeMillis();
            processInstance.start();
            this.jobId = jobId;
            this.running = true;
            ((FrameworkBus)BusManager.getBus()).jobStarted(jobId);
            LOGGER.info("Job started (jobId={}, userConfig={}, workflowDefinition={}, initialObjectId={}", new Object[] {jobId, userConfig, workflowDefinitionDescriptor, objectDataId});
        }
    }

    private long createInitialObject(long jobId) {
    	try {
	        return ((FrameworkBus)BusManager.getBus()).getObjectStoreConnector().sendObjectStoreData(jobId, new ObjectData());
    	} catch (ObjectStoreConnectorException e) {
    		throw new IllegalStateException(e);
    	}
    }

    @Override
    public synchronized boolean isEnded() {
        return processInstance.isEnded();
    }

    @Override
    public synchronized JobStatus getStatus() {
        if (isFailed()) {
            return JobStatus.FAILED;
        } else if (isAborted()) {
            return JobStatus.CANCELLED;
        } else if (isEnded()) {
            return JobStatus.COMPLETED;
        } else {
            return JobStatus.PROCESSING;
        }
    }

    private boolean isAborted() {
        return false;
    }

    private boolean isFailed() {
        return failureReason != null;
    }

    @Override
    public synchronized void markTaskAsAccepted(int requestId) {
        if (running) {
            ExecutionWrapper execution = findExecutionForTaskId(requestId);
            if (execution == null) {
                LOGGER.debug("Cannot find execution for taskId={}. The task may be already completed", requestId);
            } else {
                //  don't care, if the task was accepted previously.
                execution.markTaskAsAccepted();
            }
        } else {
            LOGGER.debug("Job (id={}) is not running. Can not mark task (id={}) as accepted", jobId, requestId);
        }
    }

    private ExecutionWrapper findExecutionForTaskId(int requestId) {
        long searchStartTime = System.currentTimeMillis();
        ExtendedExecutionImpl extendedProcessInstance = (ExtendedExecutionImpl) processInstance;
        PvmExecution execution = extendedProcessInstance.findExecuionWithTaskId(requestId);
        if (execution != null) {
            ExecutionWrapper wrapper = new ExecutionWrapper(execution);
            // verify taskId
            Integer taskId = wrapper.getTaskId();
            if (taskId != null && taskId.equals(requestId)) {
                LOGGER.debug("Execution (jobId={}, taskId={}) found in {} ms", new Object[] {jobId, requestId, System.currentTimeMillis() - searchStartTime});
                return wrapper;
            }
        }
        LOGGER.debug("Execution (jobId={}, taskId={}) not found after {} ms", new Object[] {jobId, requestId, System.currentTimeMillis() - searchStartTime});
        return null;
    }

    @Override
    public synchronized void markTaskAsCompleted(int requestId, Set<Long> newObjects) {
        if (running) {
            ExecutionWrapper execution = findExecutionForTaskId(requestId);
            if (execution == null) {
            	LOGGER.warn("Execution for taskId={} cannot be found. The task may be already completed.", requestId);
            } else {
                if (newObjects != null) {
                    for (Long objectId: newObjects) {
                        execution.subprocess(workflowDefinitionDescriptor, objectId);
                    }
                    resume();
                }
                try {
                	execution.signal("completeTask", requestId);
                } catch (Exception e) {
                	 LOGGER.error("Error processing job", e);
                     markTaskAsFailed(requestId, TaskErrorReasonType.DEFUNCT, e.getMessage());
                }
            }

            if (processInstance.isEnded()) {
            	finishJob();
            }
        } else {
            LOGGER.debug("Job (id={}) is not running. Can not mark task (id={}) as completed", jobId, requestId);
        }
    }

    public synchronized long getId() {
        return jobId;
    }

    @Override
    public synchronized void markTaskAsFailed(int requestId, TaskErrorReasonType reason, String description) {
    	if (running) {
    		ExecutionWrapper exec = findExecutionForTaskId(requestId);
    		try {
    			this.failureDescription = description;
    			exec.signal("taskFailed", new Object[] {requestId, reason, description});
    		} catch(Exception e) {
    			this.failureReason = reason;
    			this.failureDescription = description;
    			this.lastActiveStepName = getActiveStepName();
    			LOGGER.info("Job failed (jobId={}, taskId={}, stepName={}, reason={}, errorMsg={})", new Object[] {jobId, requestId, this.lastActiveStepName, reason, description});
    			processInstance.deleteCascade(description);
    			finishJob();
    		}
    	} else {
    		LOGGER.debug("Job (id={}) is not running (already failed). Can not mark task (id={}) as failed. Ignoring new failure reason {} ({})", new Object[] {jobId, requestId, reason, description});
    	}
    }
    
    private void finishJob(){
    	this.endTime  = System.currentTimeMillis();
		this.running = false;
		try{
			((FrameworkBus)BusManager.getBus()).getObjectStoreConnector().sendJobFinished(jobId, getStatus());
		}
		catch (ObjectStoreConnectorException e) {
			LOGGER.error("Error when sending JobFinished to store!",e);
		}
    }
    
    @Override
    public synchronized void resume() {
        if (running) {
            ExecutionWrapper wrapper = new ExecutionWrapper(processInstance);
            try {
                wrapper.signal("resume");
            } catch (Exception e) {
                LOGGER.error("Error processing job", e);
                markTaskAsFailed(wrapper.getTaskId() == null ? -1 : wrapper.getTaskId(), TaskErrorReasonType.DEFUNCT, e.getMessage());
            }
        } else {
            LOGGER.debug("Job (id={}) is not running. Can not resume it's processes", jobId);
        }
    }

    @Override
    public synchronized String getActiveStepName() {
        ActivityImpl activity = (ActivityImpl) processInstance.getActivity();
        if (activity != null) {
            HSNBehavior behavior = (HSNBehavior) activity.getActivityBehavior();
            return behavior.getStepName();
        } else {
            return lastActiveStepName;
        }
    }

    @Override
    public synchronized String getErrorMessage() {
        return failureDescription;
    }

    @Override
    public synchronized Map<String, Properties> getUserConfig() {
        return userConfig;
    }

    @Override
    public synchronized long getStartTime() {
        return startTime;
    }

    @Override
    public synchronized long getEndTime() {
        return endTime;
    }

    @Override
    public synchronized int getActiveSubtasksCount() {
        ExecutionWrapper utils = new ExecutionWrapper(processInstance);
        return utils.countActiveSubprocesses();
    }

    @Override
    public synchronized String getWorkflowRevision() {
        return workflowDefinitionDescriptor.getId();
    }
    
    @Override
    public synchronized String getWorkflowName() {
    	return workflowDefinitionDescriptor.getName();
    }

    @Override
    public synchronized DefaultTasksStatistics getTasksStatistics() {
        return stats;
    }
}
