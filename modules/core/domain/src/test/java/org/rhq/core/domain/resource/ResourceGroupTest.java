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
package org.rhq.core.domain.resource;

import java.util.Random;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.test.JPATest;

public class ResourceGroupTest extends JPATest {
    private Resource newResource;
    private ResourceGroup newGroup;

    @Test
    public void testCreate() throws Exception {
        assert newResource != null;
        assert newGroup != null;
        assert newResource.getId() > 0;
        assert newGroup.getId() > 0;

        assert newGroup.getExplicitResources() != null;
        assert newGroup.getExplicitResources().size() == 1;
        assert newGroup.getExplicitResources().contains(newResource);

        assert newResource.getExplicitGroups() != null;
        assert newResource.getExplicitGroups().size() == 1;
        assert newResource.getExplicitGroups().iterator().next().getId() == newGroup.getId();
    }

    private Resource createNewResource() throws Exception {
        Resource resource;

        ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
            ResourceCategory.PLATFORM, null);
        entityMgr.persist(resourceType);

        resource = new Resource("key" + System.currentTimeMillis(), "name", resourceType);
        resource.setUuid("" + new Random().nextInt());
        entityMgr.persist(resource);

        ResourceGroup group = new ResourceGroup("testgroup" + System.currentTimeMillis(), resourceType);
        entityMgr.persist(group);
        group.addExplicitResource(resource);

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (resource != null) {
            ResourceType type = entityMgr.find(ResourceType.class, resource.getResourceType().getId());
            Resource res = entityMgr.find(Resource.class, resource.getId());
            ResourceGroup group = res.getExplicitGroups().iterator().next();

            group.removeExplicitResource(resource);
            entityMgr.remove(group);
            entityMgr.remove(res);
            entityMgr.remove(type);
        }
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        try {
            newResource = createNewResource();
            newGroup = newResource.getExplicitGroups().iterator().next();
        }
        catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

}