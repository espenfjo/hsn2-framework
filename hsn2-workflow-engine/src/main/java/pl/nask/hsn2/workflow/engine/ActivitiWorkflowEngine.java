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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.activiti.engine.impl.pvm.PvmProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.bus.operations.JobStatus;
import pl.nask.hsn2.bus.operations.TaskErrorReasonType;
import pl.nask.hsn2.framework.workflow.engine.ProcessBasedWorkflowDescriptor;
import pl.nask.hsn2.framework.workflow.engine.WorkflowDescriptor;
import pl.nask.hsn2.framework.workflow.engine.WorkflowEngine;
import pl.nask.hsn2.framework.workflow.engine.WorkflowEngineException;
import pl.nask.hsn2.framework.workflow.engine.WorkflowNotDeployedException;
import pl.nask.hsn2.framework.workflow.job.MapWorkflowJobRepository;
import pl.nask.hsn2.framework.workflow.job.WorkflowJob;
import pl.nask.hsn2.framework.workflow.job.WorkflowJobInfo;
import pl.nask.hsn2.framework.workflow.job.WorkflowJobRepository;
import pl.nask.hsn2.framework.workflow.job.WorkflowJobRepositoryException;
import pl.nask.hsn2.utils.FileIdGenerator;
import pl.nask.hsn2.utils.IdGenerator;

public class ActivitiWorkflowEngine implements WorkflowEngine {

	private final static Logger LOG = LoggerFactory.getLogger(ActivitiWorkflowEngine.class);
    
    private WorkflowJobRepository jobRepository;
    
    public ActivitiWorkflowEngine(IdGenerator jobIdGenerator) {
    	this.jobRepository = new MapWorkflowJobRepository(jobIdGenerator);
    }
    
    public ActivitiWorkflowEngine(String jobSeqDir) {
    	FileIdGenerator idGenerator = new FileIdGenerator();
    	idGenerator.setSequenceFile(jobSeqDir, "jobId.seq");
    	this.jobRepository = new MapWorkflowJobRepository(idGenerator);
    }

    private long startJob(WorkflowJob job) throws WorkflowEngineException {
        long id = jobRepository.add(job);
        job.start(id);
        return id;
    }

    @Override
    public void taskAccepted(long jobId, int taskId) {
    	LOG.debug("Got TaskAccepted (jobId={}, taskId={})", new Object[] {jobId, taskId});
        try {
            WorkflowJob job = getJob(jobId);
            job.markTaskAsAccepted(taskId);
        } catch (Exception e) {
            LOG.warn("Error marking task {} in job {} as accepted", taskId, jobId);
            LOG.warn(e.getMessage(), e);
        }
    }

    @Override
    public void taskCompleted(long jobId, int requestId, Set<Long> newObjects) {
    	LOG.debug("Got TaskCompleted (jobId={}, taskId={}, newObjects={})", new Object[] {jobId, requestId, newObjects});
    	try {
    		WorkflowJob job = getJob(jobId);
    		job.markTaskAsCompleted(requestId, newObjects);
    	} catch (WorkflowJobRepositoryException e) {
    		LOG.error("Cannot complete task.", e);
    	}
        
    }

    protected WorkflowJob getJob(long jobId) throws WorkflowJobRepositoryException {
        WorkflowJob job = jobRepository.get(jobId);
        if (job == null) {
            throw new WorkflowJobRepositoryException("No job with id=" + jobId);
        }
        return job;
    }       

    @Override
    public List<WorkflowJobInfo> getJobs() {
    	try {
    		return jobRepository.getJobs();
    	} catch (WorkflowJobRepositoryException e) {
    		LOG.error("Cannot get list of jobs from jobs repository.", e);
    		return new ArrayList<WorkflowJobInfo>();
    	}
    }

    @Override
    public void taskError(long jobId, int requestId, TaskErrorReasonType reason, String description) {
    	LOG.debug("Got TaskError (jobId={}, taskId={}, reason={}, errorMsg={})", new Object[] {jobId, requestId, reason, description});
    	try {
	        WorkflowJob job = getJob(jobId);
	        // TODO: policy dependent, should be configurable (by the workflow?)
	        job.markTaskAsFailed(requestId, reason, description);
    	} catch (WorkflowJobRepositoryException e) {
    		LOG.error("Cannot process task error.", e);
    	}
    }

    @Override
    public void resume(long jobId) {
    	try {
	        WorkflowJob job = getJob(jobId);
	        job.resume();
    	} catch (WorkflowJobRepositoryException e) {
    		LOG.error("Cannot resume job.", e);
    	}
    }

    @Override
    public WorkflowJobInfo getJobInfo(long jobId) {
    	WorkflowJobInfo job = null;
    	try {
    		job = jobRepository.get(jobId);
    	} catch (WorkflowJobRepositoryException e) {
    		LOG.error("Cannot find job id.", e);
    	}
        return job;
    }

	@Override
	public long startJob(WorkflowDescriptor desc,
			String processName, Map<String, Properties> jobParameters) throws WorkflowEngineException {
		@SuppressWarnings("unchecked")
		ProcessBasedWorkflowDescriptor<PvmProcessDefinition> descriptor = (ProcessBasedWorkflowDescriptor<PvmProcessDefinition>) desc;
        if (!descriptor.isDeployed()) {
            throw new WorkflowNotDeployedException(descriptor.getId());
        } else {
            PvmProcessDefinition def = descriptor.getProcessDefinition(processName);
            if (def == null) {
                throw new IllegalStateException("process " + processName + " not found in workflow " + descriptor.getId());
            }

            WorkflowJob job = new ActivitiJob(def, descriptor, jobParameters);

            return startJob(job);
        }
	}

	@Override
	public long startJob(WorkflowDescriptor descriptor,
			String processName) throws WorkflowEngineException {
		return startJob(descriptor, processName, null);
	}

	@Override
	public long startJob(WorkflowDescriptor descriptor)
			throws WorkflowEngineException {
		return startJob(descriptor, "main", null);
	}

	@Override
	public int getJobsCount(JobStatus status) {
		int i = 0;
		for (WorkflowJobInfo info : this.getJobs()) {
			if (info.getStatus().equals(status)) {
				i++;
			}
		}
		return i;
	}
}
