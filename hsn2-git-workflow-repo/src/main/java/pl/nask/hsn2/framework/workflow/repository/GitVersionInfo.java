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

package pl.nask.hsn2.framework.workflow.repository;

import org.eclipse.jgit.revwalk.RevCommit;

public class GitVersionInfo implements WorkflowVersionInfo {
    private final int time;
    private final String version;

    public GitVersionInfo(RevCommit commit) {
        this.time = commit.getCommitTime();
        this.version = commit.getName();
    }

    @Override
    public long getVersionTimestamp() {
        return time;
    }

    @Override
    public String getVersion() {
        return version;
    }
}
