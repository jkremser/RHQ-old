/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

package org.jboss.on.plugins.tomcat;

import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discover Application Datasource
 * 
 * @author Jay Shaughnessy
 */
public class TomcatDatasourceDiscoveryComponent extends MBeanResourceDiscoveryComponent<TomcatWarComponent> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<TomcatWarComponent> discoveryContext) {

        Configuration defaultPluginConfig = discoveryContext.getDefaultPluginConfiguration();
        String objectNameTemplate = defaultPluginConfig.getSimple(PROPERTY_OBJECT_NAME).getStringValue();
        String host = discoveryContext.getParentResourceContext().getPluginConfiguration()
            .getSimpleValue(TomcatWarComponent.PROPERTY_VHOST, null);
        String path = discoveryContext.getParentResourceContext().getPluginConfiguration()
            .getSimpleValue(TomcatWarComponent.PROPERTY_CONTEXT_ROOT, null);
        objectNameTemplate = objectNameTemplate.replace("%host%", host);
        objectNameTemplate = objectNameTemplate.replace("%path%", path);
        defaultPluginConfig.put(new PropertySimple(PROPERTY_OBJECT_NAME, objectNameTemplate));

        Set<DiscoveredResourceDetails> resources = super.performDiscovery(defaultPluginConfig,
            discoveryContext.getParentResourceComponent(), discoveryContext.getResourceType());

        for (DiscoveredResourceDetails detail : resources) {
            Configuration pluginConfiguration = detail.getPluginConfiguration();
            pluginConfiguration.put(new PropertySimple(TomcatDatasourceComponent.PROPERTY_HOST, host));
            pluginConfiguration.put(new PropertySimple(TomcatDatasourceComponent.PROPERTY_PATH, path));
            String resourceName = detail.getResourceName();
            detail.setResourceName(resourceName);
        }
        return resources;
    }
}
