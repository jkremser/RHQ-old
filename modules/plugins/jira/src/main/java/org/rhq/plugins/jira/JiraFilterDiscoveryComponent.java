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
package org.rhq.plugins.jira;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.jira.soapclient.jira.RemoteProject;
import org.rhq.plugins.jira.soapclient.jira.RemoteFilter;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class JiraFilterDiscoveryComponent implements ResourceDiscoveryComponent<JiraProjectComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JiraProjectComponent> context) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        JiraClient client = context.getParentResourceComponent().getClient();

        Map<String, List<RemoteFilter>> filterMap = client.getFilters();
        List<RemoteFilter> filters = filterMap.get(context.getParentResourceComponent().getProjectKey());
        if (filters != null) {
            for (RemoteFilter filter : filters) {

                DiscoveredResourceDetails detail =
                        new DiscoveredResourceDetails(
                                context.getResourceType(),
                                filter.getId(),
                                filter.getName(),
                                null,
                                filter.getDescription(),
                                null,
                                null);

                details.add(detail);

            }
        }
        return details;
    }
}