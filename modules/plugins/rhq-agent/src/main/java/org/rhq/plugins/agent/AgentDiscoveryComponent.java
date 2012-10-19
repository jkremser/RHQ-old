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
package org.rhq.plugins.agent;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.agent.AgentManagement;
import org.rhq.enterprise.agent.AgentManagementMBean;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * This is the main discovery component for the agent plugin that discovers the agent core.
 *
 * @author John Mazzitelli
 */
public class AgentDiscoveryComponent implements ResourceDiscoveryComponent, ResourceUpgradeFacet {
    
    private static final String RESOURCE_NAME = "RHQ Agent";
    
    private final Log log = LogFactory.getLog(AgentDiscoveryComponent.class);
    
    /**
     * Simply returns the agent resource.
     *
     * @see ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.debug("Discovering RHQ Agent...");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        try {
            AgentManagementMBean mbean = getAgentManagementMBean();            
            
            String name = RESOURCE_NAME;
            String key = getResourceKey(mbean);
            String version = mbean.getVersion();
            String description = "RHQ Management Agent";

            DiscoveredResourceDetails localVM = new DiscoveredResourceDetails(context.getResourceType(), key, name,
                version, description, null, null);

            Configuration pluginConfiguration = localVM.getPluginConfiguration();
            pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
                "Local Connection"));
            pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                InternalVMTypeDescriptor.class.getName()));

            initLogEventSourcesConfigProp(mbean, pluginConfiguration);

            set.add(localVM);
        } catch (Exception e) {
            log.error("An error occurred while attempting to auto-discover the agent's own management interface", e);
        }

        return set;
    }

    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        AgentManagementMBean mbean = getAgentManagementMBean();
        
        String oldResourceKey = inventoriedResource.getResourceKey();        
        String newResourceKey = getResourceKey(mbean);
        
        ResourceUpgradeReport ret = null;
        
        if (!oldResourceKey.equals(newResourceKey)) {
            ret = new ResourceUpgradeReport();
            ret.setNewResourceKey(newResourceKey);            
            return ret;
        }   
        
        /* if we ever allow resource name upgrades on the server, we can uncomment
         * this but I'm leaving it out for now so that we don't litter the upgrade
         * report with data that will never get used. No need to increase the traffic
         * between agent and server.
         */
        /*
        if (!RESOURCE_NAME.equals(inventoriedResource.getName())) {
            if (ret == null) {
                ret = new ResourceUpgradeReport();
            }
            
            ret.setNewName(RESOURCE_NAME);
        }
        */
        
        return ret;
    }
    
    private static String getResourceKey(AgentManagementMBean mbean) {
        String agentName = mbean.getAgentConfiguration().getProperty(AgentConfigurationConstants.NAME);
        
        //DO NOT CHANGE THIS EVER UNLESS YOU UPDATE THE upgrade() METHOD TO HANDLE THE
        //CHANGES OF THE RESOURCE KEY FORMAT!!!
        //This doesn't use the RESOURCE_NAME constant on purpose so that a change of that constant
        //doesn't modify the resource key format.
        return agentName + " RHQ Agent";
    }
    
    private void initLogEventSourcesConfigProp(AgentManagementMBean agent, Configuration pluginConfiguration) {
        File logsDir = new File(agent.getAgentHomeDirectory(), "logs");

        PropertyList logEventSources = pluginConfiguration
            .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);
        if (logEventSources == null) {
            logEventSources = new PropertyList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);
            pluginConfiguration.put(logEventSources);
        }

        // agent.log
        File agentLogFile = new File(logsDir, "agent.log");
        if (agentLogFile.exists() && !agentLogFile.isDirectory()) {
            PropertyMap agentLogEventSource = new PropertyMap(
                LogFileEventResourceComponentHelper.LOG_EVENT_SOURCE_CONFIG_PROP);
            agentLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.LOG_FILE_PATH, agentLogFile));
            agentLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.ENABLED, Boolean.FALSE));
            agentLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.MINIMUM_SEVERITY, EventSeverity.ERROR
                    .name()));
            logEventSources.add(agentLogEventSource);
        }

        // command-trace.log
        File commandTraceLogFile = new File(logsDir, "command-trace.log");
        if (commandTraceLogFile.exists() && !commandTraceLogFile.isDirectory()) {
            PropertyMap commandTraceLogEventSource = new PropertyMap(
                LogFileEventResourceComponentHelper.LOG_EVENT_SOURCE_CONFIG_PROP);
            commandTraceLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.LOG_FILE_PATH, commandTraceLogFile));
            commandTraceLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.ENABLED, Boolean.FALSE));
            logEventSources.add(commandTraceLogEventSource);
        }

        return;
    }

    /**
     * Method that gets the MBeanServer that houses the agent's management MBean interface.
     *
     * <p>This is package-scoped so other components can use this to connect to the agent's management MBeanServer.</p>
     *
     * @return agent's management MBean Server
     *
     * @throws RuntimeException if failed to get the agent management MBeanServer - this should really never occur since
     *                          that MBeanServer should always been available whenever the agent is started
     */
    static MBeanServer getAgentManagementMBeanServer() {
        try {
            return ManagementFactory.getPlatformMBeanServer();
        } catch (Exception e) {
            throw new RuntimeException("Cannot get the agent's management MBeanServer", e);
        }
    }

    /**
     * Method that gets a proxy to the agent's management MBean interface.
     *
     * <p>This is package-scoped so other components can use this to connect to the agent's management interface.</p>
     *
     * @return agent's management MBean
     *
     * @throws RuntimeException if failed to get the agent management MBean - this should really never occur since that
     *                          MBean should always been available whenever the agent is started
     */
    static AgentManagementMBean getAgentManagementMBean() {
        try {
            AgentManagementMBean mbean;

            mbean = (AgentManagementMBean) MBeanServerInvocationHandler
                .newProxyInstance(getAgentManagementMBeanServer(), AgentManagement.singletonObjectName,
                    AgentManagementMBean.class, false);

            return mbean;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get the agent's management MBean", e);
        }
    }
}