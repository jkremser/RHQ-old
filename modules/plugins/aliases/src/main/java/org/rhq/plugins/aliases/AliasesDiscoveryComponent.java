/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.aliases;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;

import java.util.Set;
import java.util.List;

/**
 * The ResourceDiscoveryComponent for the "Aliases File" ResourceType.
 */
public class AliasesDiscoveryComponent extends AugeasConfigurationDiscoveryComponent {

    @Override
    public Set discoverResources(ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException,
            Exception {
        Set discoverResources = super.discoverResources(discoveryContext);
        return discoverResources;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
         ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException {
        DiscoveredResourceDetails details = super.discoverResource(pluginConfig, discoveryContext);
        return details;
    }

    @Override
    protected List<String> determineIncludeGlobs(ResourceDiscoveryContext discoveryContext) {
        List<String> s = super.determineIncludeGlobs(discoveryContext);
        return s;
    }
}
