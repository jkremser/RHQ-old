/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.modcluster;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.modcluster.helper.JBossHelper;
import org.rhq.plugins.modcluster.model.ProxyInfo;

/**
 * Manages a mod_cluster context entity.
 * 
 * @author Stefan Negrea
 */
@SuppressWarnings({ "deprecation" })
public class WebappContextComponent extends MBeanResourceComponent<MBeanResourceComponent<?>> {

    private static final Log log = LogFactory.getLog(WebappContextComponent.class);

    @Override
    protected EmsBean loadBean() {
        return getResourceContext().getParentResourceComponent().getEmsBean();
    }

    @Override
    public AvailabilityType getAvailability() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getEmsBean().getClass().getClassLoader());
            String rawProxyInfo = JBossHelper.getRawProxyInfo(getEmsBean());

            ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);

            ProxyInfo.Context context = ProxyInfo.Context.fromString(resourceContext.getResourceKey());

            int indexOfCurrentContext = proxyInfo.getAvailableContexts().indexOf(context);

            if (indexOfCurrentContext != -1) {
                ProxyInfo.Context currentContext = proxyInfo.getAvailableContexts().get(indexOfCurrentContext);

                if (currentContext.isEnabled()) {
                    return AvailabilityType.UP;
                }
            }

            return AvailabilityType.DOWN;
        } catch (Exception e) {
            log.info(e);
            return AvailabilityType.DOWN;
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("enableContext".equals(name) || "disableContext".equals(name) || "stopContext".equals(name)) {

            ProxyInfo.Context context = ProxyInfo.Context.fromString(resourceContext.getResourceKey());

            Object[] configuration = null;
            if ("stopContext".equals(name)) {
                configuration = new Object[] { context.getHost(), context.getPath(),
                    parameters.getSimple("timeout").getLongValue(),
                    TimeUnit.valueOf(parameters.getSimple("unit").getStringValue()) };
            } else {
                configuration = new Object[] { context.getHost(), context.getPath() };
            }

            if (context.getPath().equals("/")) {
                configuration[1] = "ROOT";
            }

            log.info(name + " - " + context.getHost() + " " + context.getPath());

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getEmsBean().getClass().getClassLoader());
                Object resultObject = getEmsBean().getOperation(name).invoke(configuration);

                if (resultObject instanceof OperationResult) {
                    return (OperationResult) resultObject;
                } else {
                    return new OperationResult(String.valueOf(resultObject));
                }
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        throw new Exception("Operation " + name + " not available mod_cluster WebApp service.");
    }
}