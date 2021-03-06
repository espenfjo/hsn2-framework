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

package pl.nask.hsn2.framework.workflow.hwl;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import pl.nask.hsn2.framework.workflow.builder.WorkflowBuilder;

@XmlRootElement
public class Script implements ExecutionPoint, Transformable {

    @XmlValue
    private String scriptBody = null;

    @XmlTransient
    public final String getScriptBody() {
        return scriptBody;
    }
    
    public final void setScriptBody(String scriptBody) {
    	this.scriptBody = scriptBody;
    }

    @Override
    public final void transformToWorkflow(WorkflowBuilder builder) {
        builder.addScript(scriptBody);
    }

    @Override
    public final List<Output> getOutputs() {
        return Collections.emptyList();
    }

    @Override
    public final List<? extends Service> getAllServices() {
        return Collections.emptyList();
    }

}
