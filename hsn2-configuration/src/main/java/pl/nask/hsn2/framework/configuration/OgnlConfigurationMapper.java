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

package pl.nask.hsn2.framework.configuration;

import java.util.Map;

import ognl.DefaultMemberAccess;
import ognl.Ognl;
import ognl.OgnlException;

public class OgnlConfigurationMapper implements ConfigurationMapper {

    private final static DefaultMemberAccess MEMBER_ACCESS = new DefaultMemberAccess(true);

    private Map<String, String> mapping;

    @Override
    public boolean mapsKey(String key) {
        if (mapping != null) {
            return mapping.containsKey(key);
        } else {
            return true;
        }
    }

    @Override
    public void setValue(Configuration configuration, String key, String value) throws MappingException {
        String mapped;
        if (mapping != null) {
            mapped = mapping.get(key);
        } else {
            mapped = key;
        }

        if (mapped == null) {
            throw new MappingException("Unmapped configuraion key: " + key);
        }

        try {
            Map ctx = Ognl.createDefaultContext(configuration);
            Ognl.setMemberAccess(ctx, MEMBER_ACCESS);
            Ognl.setValue(mapped, ctx, configuration, value);
        } catch (OgnlException e) {
            throw new MappingException(e);
        }
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }
}
