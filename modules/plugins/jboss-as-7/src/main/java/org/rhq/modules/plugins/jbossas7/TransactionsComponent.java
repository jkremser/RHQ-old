/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * Support for transactions subsystem configuration updates.
 *
 * The special case for this subsystem:
 *
 * If [process-id-uuid] == true then do not send updates for [process-id-socket-binding], this property
 * will be undefined by the AS7 on the next server reload/restart
 *
 * If [process-id-uuid] == false then send [process-id-socket-binding] value and
 * allow AS7 to perform property validation
 *
 *
 * @author Stefan Negrea
 */
public class TransactionsComponent extends BaseComponent<ResourceComponent<?>> {

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration config = report.getConfiguration();
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();

        boolean processIdUuid = Boolean.valueOf(config.getSimpleValue("process-id-uuid"));
        if (processIdUuid == true) {
            //Do not even send the updates for [process-id-socket-binding] because the AS7 will
            //undefine it during the next reload/restart.
            //Also, sending null for [process-id-socket-binding] with [process-id-uuid] == true causes
            //a validation error on the server.
            configDef.getPropertyDefinitions().remove("process-id-socket-binding");
            config.remove("process-id-socket-binding");
        }

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }
}
