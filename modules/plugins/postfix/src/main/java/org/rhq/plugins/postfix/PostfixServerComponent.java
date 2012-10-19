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
package org.rhq.plugins.postfix;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;

/**
 * TODO
 */
public class PostfixServerComponent extends AugeasConfigurationComponent {

    public void start(ResourceContext resourceContext) throws Exception {
        super.start(resourceContext);
    }

    public void stop() {
        super.stop();
    }

    public AvailabilityType getAvailability() {
        return super.getAvailability();
    }

    public Configuration loadResourceConfiguration() throws Exception {
        return super.loadResourceConfiguration();
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);
    }

    protected Object toPropertyValue(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if (propDefSimple.getType().equals(PropertySimpleType.BOOLEAN)) {
            return "yes".equals(augeas.get(node.getPath()));
        }
        return super.toPropertyValue(propDefSimple, augeas, node);
    }

    @Override
    protected String toNodeValue(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        if (propDefSimple.getType().equals(PropertySimpleType.BOOLEAN)) {
            if (propSimple.getBooleanValue()) {
                return "yes";
            }
            return "no";
        }
        return super.toNodeValue(augeas, node, propDefSimple, propSimple);
    }

    /*

        @Override
        public CreateResourceReport createResource(CreateResourceReport reportIn) {
            CreateResourceReport report = reportIn;
            Configuration config = report.getResourceConfiguration();
            String name = config.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP).getStringValue();
            report.setResourceKey(name);
            report.setResourceName(name);
            return super.createResource(report);
        }
        @Override
        protected String getChildResourceConfigurationRootPath(ResourceType resourceType, Configuration resourceConfig) {
            if (resourceType.getName().equals(SambaShareComponent.RESOURCE_TYPE_NAME)) {
                String targetName = resourceConfig.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP).getStringValue();
                return "/files/etc/samba/smb.conf/target[.='" + targetName + "']";
            } else {
                throw new IllegalArgumentException("Unsupported child Resource type: " + resourceType);
            }
        }
        @Override
        protected String getChildResourceConfigurationRootLabel(ResourceType resourceType, Configuration resourceConfig) {
            if (resourceType.getName().equals(SambaShareComponent.RESOURCE_TYPE_NAME)) {
                return resourceConfig.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP).getStringValue();
            } else {
                throw new IllegalArgumentException("Unsupported child Resource type: " + resourceType);
            }
        }
    */

}
