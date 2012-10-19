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
package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Discover subsystems
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class SubsystemDiscovery implements ResourceDiscoveryComponent<BaseComponent> {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent> context)
            throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING,true);

        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();


        Configuration config = context.getDefaultPluginConfiguration();
        String confPath = config.getSimpleValue("path", null);
        if (confPath==null) {
            log.error("Path plugin config is null for ResourceType [" + context.getResourceType().getName() +"].");
            return details;
        }

        List<String> subTypes = new ArrayList<String>();
        if (confPath.contains("|")) {
            subTypes.addAll(Arrays.asList(confPath.split("\\|")));
        }
        else
            subTypes.add(confPath);


        for (String cpath : subTypes) {


            boolean recursive = false;

            String parentPath = parentComponent.getPath();

            String path;
            String childType = null;
            if (!cpath.contains("=")) { // NO = -> no sub path, but a type
                recursive = true;
                childType = cpath;

            }

            if (parentPath==null || parentPath.isEmpty())
                path = "";
            else
                path = parentPath;

            if (cpath.contains("="))
                path += "," + cpath;

            if (Boolean.getBoolean("as7plugin.verbose"))
                log.info("total path: [" + path + "]");


            JsonNode json ;
            if (!recursive)
                json = connection.executeRaw(new ReadResource(parentComponent.pathToAddress(path)));
            else {
                List<PROPERTY_VALUE> addr ;
                addr = parentComponent.pathToAddress(parentPath);
                json = connection.executeRaw(new ReadChildrenNames(addr, childType));
            }
            if (!ASConnection.isErrorReply(json)) {
                if (recursive) {

                    JsonNode subNode = json.findPath("result");

                    if (subNode!=null && subNode.isContainerNode()){

                        Iterator<JsonNode> iter = subNode.getElements();
                        while (iter.hasNext()) {

                            JsonNode node = iter.next();
                            String val = node.getTextValue();


                            String newPath = cpath + "=" + val;
                            Configuration config2 = context.getDefaultPluginConfiguration();


                            String resKey;

                            if (path==null||path.isEmpty())
                                resKey = newPath;
                            else {
                                if (path.startsWith(","))
                                    path = path.substring(1);
                                resKey = path + "," +childType + "=" + val;
                            }

                            PropertySimple pathProp = new PropertySimple("path",resKey);
                            config2.put(pathProp);

                            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                                    context.getResourceType(), // DataType
                                    resKey, // Key
                                    val, // Name
                                    null, // Version
                                    context.getResourceType().getDescription(), // subsystem.description
                                    config2,
                                    null);
                            details.add(detail);
                        }
                    }
                    else {

                        if (subNode==null) {
                            log.error("subNode was null for " + path + " and type " + context.getResourceType().getName());
                        }
                        else if (!subNode.isNull())
                            log.info("subnode was no container");
                    }

                }
                else {
                    if (path.startsWith(","))
                        path = path.substring(1);

                    String resKey = path;
                    String name = resKey.substring(resKey.lastIndexOf("=") + 1);
                    Configuration config2 = context.getDefaultPluginConfiguration();
                    PropertySimple pathProp = new PropertySimple("path",path);
                    config2.put(pathProp);



                    DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                            context.getResourceType(), // DataType
                            path, // Key
                            name, // Name
                            null, // Version
                            context.getResourceType().getDescription(), // Description
                            config2,
                            null);
                    details.add(detail);
                }

            }

        }
        return details;
    }

}
