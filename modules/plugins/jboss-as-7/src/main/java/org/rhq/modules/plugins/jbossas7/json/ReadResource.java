/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.modules.plugins.jbossas7.json;

import java.util.List;

/**
 * Reads data for one AS 7 resource
 * @author Heiko W. Rupp
 */
public class ReadResource extends Operation {

    private static final String READ_RESOURCE = "read-resource";

    public ReadResource(String resourceType,String typeValue) {
        super(READ_RESOURCE,resourceType,typeValue);
    }

    public ReadResource(String resourceType,String typeValue,boolean includeDefaults) {
        super(READ_RESOURCE,resourceType,typeValue);
        includeDefaults(includeDefaults);
    }

    public ReadResource(Address address) {
        super(READ_RESOURCE,address);
    }

    public void includeRuntime(boolean arg) {
        addAdditionalProperty("include-runtime",arg);
    }

    public void includeDefaults(boolean arg) {
        addAdditionalProperty("include-defaults",arg);
    }

    public void recursive(boolean arg) {
        addAdditionalProperty("recursive",arg);
    }
}
