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
package org.rhq.plugins.jmx;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * This is meant to be a generic discovery component for MBeans. In order to use it you configure your resource
 * descriptor to use this class for discovery and then provide a plugin configuration default value for the "objectName"
 * property that has a default value of the form defined by {@see ObjectNameQueryUtility}. Also, setting default
 * configurations values for nameTemplate and descriptionTemplate according to the message format also defined by
 * {@see ObjectNameQueryUtility} also lets you customize the detected resources name and description according to a
 * variable replacement strategy. Additionally, any mapped variables found will be set as configuration properties in
 * their own right.
 *
 * @author Greg Hinkle
 */
public class MBeanResourceDiscoveryComponent<T extends JMXComponent<?>> implements ResourceDiscoveryComponent<T> {
    // Constants  --------------------------------------------

    /**
     * Required when using this discovery component. The value of this property is the query for retrieving the MBeans.
     */
    public static final String PROPERTY_OBJECT_NAME = "objectName";

    /**
     * Not required when using this discovery component. If this is specified, it will be used in the generation of the
     * resource name. Any variables specified for replacement (via the {name} syntax) will be substituted prior to
     * setting the resource name. Through this mechanism, unique names can be created for each resource discovered of
     * the same resource type.
     */
    public static final String PROPERTY_NAME_TEMPLATE = "nameTemplate";

    /**
     * Not required when using this discovery component. Similar to {@link #PROPERTY_NAME_TEMPLATE}, unique descriptions
     * can be generated per resource using this template.
     */
    public static final String PROPERTY_DESCRIPTION_TEMPLATE = "descriptionTemplate";

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceDiscoveryContext<T> discoveryContext;

    // ResourceDiscoveryComponent Implementation  --------------------------------------------

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context) {
        return discoverResources(context, true);
    }

    // Public  --------------------------------------------

    /**
     * Same as {@link discoverResources(ResourceDiscoveryContext<T>)} with additional param.
     * @param skipUnknownProps         Should we skip over MBeans that have unknown properties in their ObjectName
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context,
        boolean skipUnknownProps) {
        this.discoveryContext = context;
        return performDiscovery(context.getDefaultPluginConfiguration(), context.getParentResourceComponent(),
            context.getResourceType(), skipUnknownProps);
    }

    /**
     * Performs the actual discovery through MBeans.
     *
     * @param  pluginConfiguration     plugin configuration for the resource type being discovered; used to grab values
     *                                 that govern the MBean query and resource details generation
     * @param  parentResourceComponent parent resource of the resource being discovered
     * @param  resourceType            type of resource being discovered
     * @return set describing the resources discovered; empty set if no resources are found
     */
    public Set<DiscoveredResourceDetails> performDiscovery(Configuration pluginConfiguration,
        JMXComponent parentResourceComponent, ResourceType resourceType) {

        return performDiscovery(pluginConfiguration, parentResourceComponent, resourceType, true);
    }

    /**
     * Performs the actual discovery through MBeans.
     *
     * @param  pluginConfiguration     plugin configuration for the resource type being discovered; used to grab values
     *                                 that govern the MBean query and resource details generation
     * @param  parentResourceComponent parent resource of the resource being discovered
     * @param  resourceType            type of resource being discovered
     * @param skipUnknownProps         Should we skip over MBeans that have unknown properties in their ObjectName
     * @return set describing the resources discovered; empty set if no resources are found
     */
    public Set<DiscoveredResourceDetails> performDiscovery(Configuration pluginConfiguration,
        JMXComponent parentResourceComponent, ResourceType resourceType, boolean skipUnknownProps) {

        String objectNameQueryTemplateOrig = pluginConfiguration.getSimple(PROPERTY_OBJECT_NAME).getStringValue();

        log.debug("Discovering MBean resources with object name query template: " + objectNameQueryTemplateOrig);

        EmsConnection connection = parentResourceComponent.getEmsConnection();

        if (connection == null) {
            throw new NullPointerException("The parent resource component [" + parentResourceComponent
                + "] returned a null connection - cannot discover MBeans without a connection");
        }

        Set<DiscoveredResourceDetails> services = new HashSet<DiscoveredResourceDetails>();
        String templates[] = objectNameQueryTemplateOrig.split("\\|");
        for (String objectNameQueryTemplate : templates) {
            // Get the query template, replacing the parent key variables with the values from the parent configuration
            ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(objectNameQueryTemplate,
                (this.discoveryContext != null) ? this.discoveryContext.getParentResourceContext()
                    .getPluginConfiguration() : null);

            List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());
            if (log.isDebugEnabled()) {
                log.debug("Found [" + beans.size() + "] mbeans for query [" + queryUtility.getTranslatedQuery() + "].");
            }
            for (EmsBean bean : beans) {
                if (queryUtility.setMatchedKeyValues(bean.getBeanName().getKeyProperties())) {
                    // Only use beans that have all the properties we've made variables of

                    // Don't match beans that have unexpected properties
                    if (skipUnknownProps
                        && queryUtility.isContainsExtraKeyProperties(bean.getBeanName().getKeyProperties().keySet())) {
                        continue;
                    }

                    String resourceKey = bean.getBeanName().getCanonicalName(); // The detected object name

                    String nameTemplate = (pluginConfiguration.getSimple(PROPERTY_NAME_TEMPLATE) != null) ? pluginConfiguration
                        .getSimple(PROPERTY_NAME_TEMPLATE).getStringValue() : null;

                    String descriptionTemplate = (pluginConfiguration.getSimple(PROPERTY_DESCRIPTION_TEMPLATE) != null) ? pluginConfiguration
                        .getSimple(PROPERTY_DESCRIPTION_TEMPLATE).getStringValue() : null;

                    String name = resourceKey;
                    if (nameTemplate != null) {
                        name = queryUtility.formatMessage(nameTemplate);
                    }

                    String description = null;
                    if (descriptionTemplate != null) {
                        description = queryUtility.formatMessage(descriptionTemplate);
                    }

                    DiscoveredResourceDetails service = new DiscoveredResourceDetails(resourceType, resourceKey, name,
                        "", description, null, null);
                    Configuration config = service.getPluginConfiguration();
                    config.put(new PropertySimple(PROPERTY_OBJECT_NAME, bean.getBeanName().toString()));

                    Map<String, String> mappedVariableValues = queryUtility.getVariableValues();
                    for (String key : mappedVariableValues.keySet()) {
                        config.put(new PropertySimple(key, mappedVariableValues.get(key)));
                    }

                    services.add(service);

                    // Clear out the variables for the next bean detected
                    queryUtility.resetVariables();
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("[" + services.size() + "] services have been added");
            }

        }

        return services;
    }

    /**
     * Loads the bean with the given object name.
     *
     * Subclasses are free to override this method in order to load the bean.
     * 
     * @param objectName the name of the bean to load
     * @return the bean that is loaded
     */
    protected EmsBean loadBean(T context, String objectName) {
        EmsConnection emsConnection = context.getEmsConnection();

        if (emsConnection != null) {
            EmsBean bean = emsConnection.getBean(objectName);
            if (bean == null) {
                // In some cases, this resource component may have been discovered by some means other than querying its
                // parent's EMSConnection (e.g. ApplicationDiscoveryComponent uses a filesystem to discover EARs and
                // WARs that are not yet deployed). In such cases, getBean() will return null, since EMS won't have the
                // bean in its cache. To cover such cases, make an attempt to query the underlying MBeanServer for the
                // bean before giving up.
                emsConnection.queryBeans(objectName);
                bean = emsConnection.getBean(objectName);
            }

            return bean;
        }

        return null;
    }
}