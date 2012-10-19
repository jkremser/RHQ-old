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
package org.rhq.plugins.apache;

import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.HttpdAddressUtility.Address;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;

/**
 * Discovers VirtualHosts under the Apache server by reading them out from Augeas tree constructed
 * in the parent component. If Augeas is not present, an attempt is made to discover the vhosts using
 * SNMP module.
 * 
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class ApacheVirtualHostServiceDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent> {

    private static final String COULD_NOT_DETERMINE_THE_VIRTUAL_HOST_ADDRESS = "*** Could not determine the virtual host address ***";

    public static final String LOGS_DIRECTORY_NAME = "logs";

    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    private static final Log log = LogFactory.getLog(ApacheVirtualHostServiceDiscoveryComponent.class);
    
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApacheServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        //BZ 612189 - prepare for the legacy overrides. We need to revert to the old-style resource keys until
        //resource upgrade functionality is ready.
        SnmpWwwServiceIndexes snmpDiscoveries = getSnmpDiscoveries(context);
        
        ApacheServerComponent serverComponent = context.getParentResourceComponent();
        ApacheDirectiveTree tree = serverComponent.loadParser();
        //first define the root server as one virtual host
        discoverMainServer(context, discoveredResources, snmpDiscoveries);

        ResourceType resourceType = context.getResourceType();

        File configPath = serverComponent.getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        List<ApacheDirective> virtualHosts = tree.search("/<VirtualHost");

        for (ApacheDirective node : virtualHosts) {
            List<String> hosts = node.getValues();
            String firstAddress = hosts.get(0);

            List<ApacheDirective> serverNames = node.getChildByName("ServerName");
            String serverName = null;
            if (serverNames.size() > 0) {
                serverName = serverNames.get(0).getValuesAsString();
            }

            StringBuilder keyBuilder = new StringBuilder();
            if (serverName != null) {
                keyBuilder.append(serverName).append("|");
            }
            keyBuilder.append(firstAddress);

           
            for (int i=1;i<hosts.size();i++){
                keyBuilder.append(" ").append(hosts.get(i));
            }

            String resourceKey = keyBuilder.toString();

            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

            Address address = serverComponent.getAddressUtility().getVirtualHostSampleAddress(tree, firstAddress, serverName, false);
            if (address != null) {
                String hostToPing = address.host;
                int portToPing = address.port;
                if (address.isPortWildcard()) {
                    Address serverAddress = serverComponent.getAddressUtility().getMainServerSampleAddress(tree, hostToPing, 0);
                    if (serverAddress != null) {
                        portToPing = serverAddress.port;
                    } else {
                        portToPing = Address.PORT_WILDCARD_VALUE;
                    }
                }
                if (address.isHostDefault() || address.isHostWildcard()) {
                    Address serverAddress = serverComponent.getAddressUtility().getMainServerSampleAddress(tree, null, portToPing);
                    
                    if (serverAddress != null) {
                        hostToPing = serverAddress.host;
                    } else {
                        hostToPing = null;
                    }
                }
                
                String url;
                if (hostToPing != null && portToPing != Address.PORT_WILDCARD_VALUE && portToPing != Address.NO_PORT_SPECIFIED_VALUE) {
                    url = "http://" + hostToPing + ":" + portToPing + "/";
                } else {
                    url = COULD_NOT_DETERMINE_THE_VIRTUAL_HOST_ADDRESS;
                }

                PropertySimple urlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url);
                pluginConfiguration.put(urlProp);
                
            }

            File rtLogFile = new File(logsDir, address.host + address.port + RT_LOG_FILE_NAME_SUFFIX);

            PropertySimple rtLogProp = new PropertySimple(
                ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtLogFile.toString());
            pluginConfiguration.put(rtLogProp);

            String resourceName;
            if (serverName != null) {
                resourceName = address.toString();
            } else {
                resourceName = resourceKey;
            }

            //BZ 612189 - remove this once we have resource upgrade
            if (snmpDiscoveries != null) {
                String legacyResourceKey = getLegacyResourceKey(context, resourceKey, snmpDiscoveries);
                resourceKey = legacyResourceKey != null ? legacyResourceKey : resourceKey;
            }
            
            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
                pluginConfiguration, null));
        }

        return discoveredResources;
    }


    private void discoverMainServer(ResourceDiscoveryContext<ApacheServerComponent> context,
        Set<DiscoveredResourceDetails> discoveredResources, SnmpWwwServiceIndexes snmpDiscoveries) throws Exception {

        ResourceType resourceType = context.getResourceType();
        Configuration mainServerPluginConfig = context.getDefaultPluginConfiguration();

        File configPath = context.getParentResourceComponent().getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        String mainServerUrl = context.getParentResourceContext().getPluginConfiguration().getSimple(
            ApacheServerComponent.PLUGIN_CONFIG_PROP_URL).getStringValue();
        if (mainServerUrl != null && !"null".equals(mainServerUrl)) {
            PropertySimple mainServerUrlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP,
                mainServerUrl);

            mainServerPluginConfig.put(mainServerUrlProp);

            URI mainServerUri = new URI(mainServerUrl);
            String host = mainServerUri.getHost();
            int port = mainServerUri.getPort();
            if (port == -1) {
                port = 80;
            }

            File rtLogFile = new File(logsDir, host + port + RT_LOG_FILE_NAME_SUFFIX);

            PropertySimple rtLogProp = new PropertySimple(
                ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtLogFile.toString());
            mainServerPluginConfig.put(rtLogProp);
        }

        String key = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
        
        //BZ 612189 - remove this once we have resource upgrade
        if (snmpDiscoveries != null) {
            String legacyKey = getLegacyResourceKey(context, key, snmpDiscoveries);
            key = legacyKey != null ? legacyKey : key;
        }
        
        DiscoveredResourceDetails mainServer = new DiscoveredResourceDetails(resourceType,
            key, "Main", null, null,
            mainServerPluginConfig, null);
        discoveredResources.add(mainServer);
    }
    
    /**
     * @deprecated remove this once we have resource upgrade
     * @param discoveryContext
     * @param newStyleResourceKey
     * @param snmpDiscoveries
     * @return
     */
    @Deprecated
    private String getLegacyResourceKey(ResourceDiscoveryContext<ApacheServerComponent> discoveryContext, String newStyleResourceKey, SnmpWwwServiceIndexes snmpDiscoveries) {
        int snmpWwwServiceIndex = ApacheVirtualHostServiceComponent.getMatchingWwwServiceIndex(discoveryContext.getParentResourceComponent(), newStyleResourceKey, snmpDiscoveries.names, snmpDiscoveries.ports);
        
        if (snmpWwwServiceIndex < 1) {
            return null;
        } else {
            String host = snmpDiscoveries.names.get(snmpWwwServiceIndex - 1).toString();
            String fullPort = snmpDiscoveries.ports.get(snmpWwwServiceIndex - 1).toString();

            // The port value will be in the form "1.3.6.1.2.1.6.XXXXX",
            // where "1.3.6.1.2.1.6" represents the TCP protocol ID,
            // and XXXXX is the actual port number
            String port = fullPort.substring(fullPort.lastIndexOf(".") + 1);
            return host + ":" + port;
        }
    }
    
    /**
     * @deprecated remove this once we have resource upgrade
     * @param discoveryContext
     * @return
     */
    @Deprecated
    private SnmpWwwServiceIndexes getSnmpDiscoveries(ResourceDiscoveryContext<ApacheServerComponent> discoveryContext) {
        try {
            SNMPSession snmpSession = discoveryContext.getParentResourceComponent().getSNMPSession();
            List<SNMPValue> nameValues;
            List<SNMPValue> portValues;
            SNMPValue descValue;
    
            try {
                nameValues = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_NAME);
            } catch (SNMPException e) {
                throw new Exception(
                    "Error getting SNMP column: " + SNMPConstants.COLUMN_VHOST_NAME + ": " + e.getMessage(), e);
            }
    
            try {
                portValues = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_PORT);
            } catch (SNMPException e) {
                throw new Exception(
                    "Error getting SNMP column: " + SNMPConstants.COLUMN_VHOST_PORT + ": " + e.getMessage(), e);
            }
            
            try {
                // Just get the first one - they are all the same.
                descValue = snmpSession.getNextValue(SNMPConstants.COLUMN_VHOST_DESC);
            } catch (SNMPException e) {
                throw new Exception("Error getting SNMP value: " + SNMPConstants.COLUMN_VHOST_DESC + ": " + e.getMessage(),
                    e);
            }
    
            SnmpWwwServiceIndexes ret = new SnmpWwwServiceIndexes();
            ret.names = nameValues;
            ret.ports = portValues;
            ret.desc = descValue;
            
            return ret;
        } catch (Exception e) {
            log.warn("Error while trying to contact SNMP of the apache server " + discoveryContext.getParentResourceContext().getResourceKey());
            return null;
        }
    }
    
    /**
     * @deprecated remove this once we have resource upgrade
     *
     * @author Lukas Krejci
     */
    @Deprecated
    private static class SnmpWwwServiceIndexes {
        public List<SNMPValue> names;
        public List<SNMPValue> ports;
        public SNMPValue desc;
    }
}