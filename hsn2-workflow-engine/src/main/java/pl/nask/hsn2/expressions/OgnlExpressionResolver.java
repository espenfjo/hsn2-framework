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

package pl.nask.hsn2.expressions;

import pl.nask.hsn2.bus.connector.objectstore.ObjectStoreConnector;

public class OgnlExpressionResolver implements ExpressionResolver {

    private final ObjectStoreConnector connector;

    public OgnlExpressionResolver(ObjectStoreConnector connector) {
    	if (connector == null) {
    		throw new NullPointerException("ObjectStoreConnector");
    	}
        this.connector = connector;
    }

    @Override
    public Object evaluateExpression(long jobId, long objectDataId, String expression) throws EvaluationException {
        OgnlExpressionEvaluation eval = new OgnlExpressionEvaluation(connector, jobId, objectDataId, expression);
        return eval.eval();
    }

    @Override
    public boolean evaluateBoolean(long jobId, long objectDataId, String expression) throws EvaluationException {
        try{
	    	Object res = evaluateExpression(jobId, objectDataId, expression);
	        return (Boolean) res;
        }catch (Exception e) {
			throw new EvaluationException(e.getMessage(),e);
		}
    }
}
