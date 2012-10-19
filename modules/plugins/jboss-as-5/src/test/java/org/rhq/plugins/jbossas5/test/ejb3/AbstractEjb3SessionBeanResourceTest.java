/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5.test.ejb3;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.rhq.plugins.jbossas5.test.util.EjbSessionBeanTestTemplate;

/**
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractEjb3SessionBeanResourceTest extends AbstractEjb3ResourceTest {

    protected static abstract class Ejb3SessionBeanTestTemplate extends EjbSessionBeanTestTemplate {

        public Object getRemoteBean() throws Exception {
            String jndiName = getTestedBeanName() + "/remote";
            
            return AppServerUtils.getRemoteObject(jndiName, Object.class);
        }

        public boolean isTestedResource(Resource resource) {
            String resourceKey = resource.getResourceKey();
            
            Resource parentResource = resource.getParentResource();
            
            String parentResourceKey = parentResource == null ? null : parentResource.getResourceKey();
            
            boolean resourceKeyMatch = resourceKey.equals(getExpectedResourceKey());
            boolean parentResourceKeyMatch = (parentResourceKey == null && getExpectedParentResourceKeyUniquePart() == null)
                || (parentResourceKey.contains(getExpectedParentResourceKeyUniquePart()));
            
            
            return resourceKeyMatch && parentResourceKeyMatch;
        }
        

        public abstract String getExpectedResourceKey();
        public abstract String getExpectedParentResourceKeyUniquePart();
    }
    
    private Ejb3SessionBeanTestTemplate testTemplate;
    
    protected AbstractEjb3SessionBeanResourceTest(Ejb3SessionBeanTestTemplate testTemplate) {
        this.testTemplate = testTemplate;
    }
    
    protected void setupBean() {
        testTemplate.setupBean();
    }

    @Override
    protected void validateOperationResult(String name, OperationResult result, Resource resource) {
        if (!testTemplate.validateOperationResult(name, result, resource)) {
            super.validateOperationResult(name, result, resource);
        }
    }

    @Override
    protected void validateNumericMetricValue(String metricName, Double value, Resource resource) {
        if ("createCount".equals(metricName)) {
            assert value > 0 : "Expected Session Bean CreateCount greater than 0.";
        } else {
            super.validateNumericMetricValue(metricName, value, resource);
        }
    }
    
    
}
