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
package org.rhq.plugins.cron;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * @author Lukas Krejci
 */
public class CronDiscoveryComponent extends AugeasConfigurationDiscoveryComponent<PlatformComponent> {

    public static final String CRON_RESOURCE_KEY = "Cron";

    @Override
    protected DiscoveredResourceDetails createResourceDetails(
        ResourceDiscoveryContext<PlatformComponent> discoveryContext, Configuration pluginConfig) {
        
        ResourceType resourceType = discoveryContext.getResourceType();
        
        return new DiscoveredResourceDetails(resourceType, CRON_RESOURCE_KEY, resourceType
            .getName(), null, resourceType.getDescription(), pluginConfig, null);
    }
}
