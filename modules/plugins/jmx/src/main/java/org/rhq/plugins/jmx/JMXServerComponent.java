/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.jmx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.support.ConnectionProvider;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.util.ConnectionProviderFactory;

/**
 * The generic JMX server component used to create and cache a connection to a local or
 * remote JMX MBeanServer. This component is responsible for building an isolated connection/classloader
 * to the managed resource's JMX MBeanServer. Each connection is isolated from other connections
 * created by other instances of this component. This allows for it to do things like manage
 * multiple JBossAS servers that are running on the same box, even if they are of different JBossAS
 * versions. The same holds true for Hibernate applications - multiple connections can be created
 * to different versions of the Hibernate MBean and due to the isolation of each connection, there
 * are no version incompatibility errors that will occur.
 *  
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class JMXServerComponent<T extends ResourceComponent<?>> implements JMXComponent<T> {

    private static Log log = LogFactory.getLog(JMXServerComponent.class);

    private EmsConnection connection;
    private ConnectionProvider connectionProvider;

    /**
     * The context of a component that is started. Note, other classes should use #getResourceContext(), rather than
     * this field.
     */
    ResourceContext context;

    public void start(ResourceContext context) throws Exception {
        this.context = context;
        log.debug("Starting connection to " + context.getResourceType() + "[" + context.getResourceKey() + "]...");

        // If connecting to the EMS fails, log a warning but still succeed in starting. getAvailablity() will keep
        // trying to connect each time it is called.
        try {
            internalStart();
        } catch (Exception e) {
            log.warn("Failed to connect to " + context.getResourceType() + "[" + context.getResourceKey() + "].", e);

        }

        if (connection == null) {
            log.warn("Unable to connect to " + context.getResourceType() + "[" + context.getResourceKey() + "].");
        }
    }

    protected void internalStart() throws Exception {
        Configuration pluginConfig = context.getPluginConfiguration();
        String connectionTypeDescriptorClassName = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
            .getStringValue();
        if (JMXDiscoveryComponent.PARENT_TYPE.equals(connectionTypeDescriptorClassName)) {
            // Our parent is itself a JMX component, so just reuse its connection.
            this.connection = ((JMXComponent) context.getParentResourceComponent()).getEmsConnection();
            this.connectionProvider = this.connection.getConnectionProvider();
        } else {
            this.connectionProvider = ConnectionProviderFactory.createConnectionProvider(pluginConfig,
                this.context.getNativeProcess(), this.context.getTemporaryDirectory());
            this.connection = this.connectionProvider.connect();
            this.connection.loadSynchronous(false);
        }
    }

    public void stop() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Error closing EMS connection: " + e);
            }
            connection = null;
        }
    }

    public EmsConnection getEmsConnection() {
        return this.connection;
    }

    public AvailabilityType getAvailability() {
        if (connectionProvider == null || !connectionProvider.isConnected()) {
            try {
                internalStart();
            } catch (Exception e) {
                log.debug("Still unable to reconnect to " + context.getResourceType() + "[" + context.getResourceKey()
                    + "] due to error: " + e);
            }
        }

        return ((connectionProvider != null) && connectionProvider.isConnected()) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    protected ResourceContext getResourceContext() {
        return this.context;
    }

}