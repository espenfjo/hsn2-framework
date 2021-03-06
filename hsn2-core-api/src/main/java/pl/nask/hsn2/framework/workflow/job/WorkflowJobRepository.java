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

package pl.nask.hsn2.framework.workflow.job;

import java.util.List;

/**
 * This is interface of repository for jobs running in any workflow engine.
 * 
 * NOTO: Any implementation of this interface MUST be thread-safe.
 * 
 *
 */
public interface WorkflowJobRepository {

	/**
	 * Takes a job identified by <code>id</code> from the repository.
	 * 
	 * @param id
	 *            Identifier of looking job.
	 * @return The job or <code>null</code> if job doesn't exist.
	 * @throws WorkflowJobRepositoryException
	 *             Any problem with reading repository will rise the exception.
	 */
	WorkflowJob get(long id) throws WorkflowJobRepositoryException;
	
	/**
	 * Adds single job to the repository.
	 * 
	 * @param job
	 *            Job to be added.
	 * @return Identifier of the job in the repository.
	 * 
	 * @throws WorkflowJobRepositoryException
	 *             Any problem with addinh the job to the repository will rise
	 *             the execption.
	 */
	long add(WorkflowJob job) throws WorkflowJobRepositoryException;
	
	/**
	 * Returns all jobs stored in the repository.
	 * 
	 * @return List of jobs or empty list if there are no jobs.
	 * 
	 * @throws WorkflowJobRepositoryException
	 *             Any problem with receiving list of jobs will rise this
	 *             exception.
	 */
	List<WorkflowJobInfo> getJobs() throws WorkflowJobRepositoryException;

}
