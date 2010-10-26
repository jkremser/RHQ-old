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

import java.util.List;
import java.util.Random;

import javax.persistence.Query;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.JPATest;

@Test
public class ResourceErrorTest extends JPATest {
    private Resource newResource;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        newResource = createNewResource();
    }


    public void testCreateErrors() throws Exception {
        ResourceError re;

        re = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary",
            "test detail", 12345);

        entityMgr.persist(re);

        ResourceError error = entityMgr.find(ResourceError.class, re.getId());
        assert error != null;
        assert error.getId() > 0;
        assert error.getResource().getId() == newResource.getId();
        assert error.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
        assert error.getSummary().equals("test summary");
        assert error.getDetail().equals("test detail");
        assert error.getTimeOccurred() == 12345;

        re = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary 2",
            "test detail 2", 56789);

        entityMgr.persist(re);

        error = entityMgr.find(ResourceError.class, re.getId());
        assert error != null;
        assert error.getId() > 0;
        assert error.getResource().getId() == newResource.getId();
        assert error.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
        assert error.getSummary().equals("test summary 2");
        assert error.getDetail().equals("test detail 2");
        assert error.getTimeOccurred() == 56789;

        Query q = entityMgr.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID);
        q.setParameter("resourceId", newResource.getId());
        assert q.getResultList().size() == 2;

        q = entityMgr.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE);
        q.setParameter("resourceId", newResource.getId());
        q.setParameter("errorType", ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert q.getResultList().size() == 2;
    }

    @SuppressWarnings("unchecked")
    public void testQueries() throws Exception {
        ResourceError re;
        Query q;
        List<ResourceError> errors;

        q = entityMgr.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID);
        q.setParameter("resourceId", newResource.getId());
        errors = q.getResultList();
        assert errors.size() == 0;

        re = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary",
            "test detail", 12345);

        entityMgr.persist(re);
        errors = q.getResultList();
        assert errors.size() == 1;
        assert errors.get(0).getResource().getId() == newResource.getId();
        assert errors.get(0).getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
        assert errors.get(0).getSummary().equals("test summary");
        assert errors.get(0).getDetail().equals("test detail");
        assert errors.get(0).getTimeOccurred() == 12345;

        q = entityMgr.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE);
        q.setParameter("resourceId", newResource.getId());
        q.setParameter("errorType", ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        errors = q.getResultList();
        assert errors.size() == 1;
        assert errors.get(0).getResource().getId() == newResource.getId();
        assert errors.get(0).getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
        assert errors.get(0).getSummary().equals("test summary");
        assert errors.get(0).getDetail().equals("test detail");
        assert errors.get(0).getTimeOccurred() == 12345;
    }

    private Resource createNewResource() throws Exception {
        Resource resource;

        ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
            ResourceCategory.PLATFORM, null);
        entityMgr.persist(resourceType);
        resource = new Resource("key" + System.currentTimeMillis(), "name", resourceType);
        resource.setUuid("" + new Random().nextInt());
        entityMgr.persist(resource);
        System.out.println("Created resource with id " + resource.getId());

        return resource;
    }

}