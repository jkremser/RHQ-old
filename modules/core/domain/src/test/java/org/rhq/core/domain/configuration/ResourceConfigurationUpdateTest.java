/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.configuration;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;

public class ResourceConfigurationUpdateTest extends AbstractConfigurationUpdateTest {

    @Test
    public void serializationShouldCopyResource() throws Exception {
        Resource resource = new Resource();
        resource.setUuid("1234");

        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate();
        update.setResource(resource);

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        assertEquals(
            serializedUpdate.getResource(),
            update.getResource(),
            "Failed to properly serialize the 'resource' property"
        );
    }

    @Test
    public void serializationShouldCopyGroupConfigurationUpdate() throws Exception {
        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate();
        update.setGroupConfigurationUpdate(new GroupResourceConfigurationUpdate(new ResourceGroup("resourceGroup"),
            "rhqadmin"));

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        assertEquals(
            serializedUpdate.getGroupConfigurationUpdate(),
            update.getGroupConfigurationUpdate(),
            "Failed to properly serialize the 'groupConfigurationUpdate' property"
        );
    }

}
