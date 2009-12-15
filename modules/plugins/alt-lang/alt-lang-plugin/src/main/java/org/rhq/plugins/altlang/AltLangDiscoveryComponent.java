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

package org.rhq.plugins.altlang;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Set;

public class AltLangDiscoveryComponent extends AltLangAbstractComponent implements ResourceDiscoveryComponent {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {

        Configuration defaultPluginConfig = context.getDefaultPluginConfiguration();
        Action action = new Action("discovery", "discovery", context.getResourceType());

        ScriptMetadata scriptMetadata = resolveScript(defaultPluginConfig, action);
        
        ScriptEngineManager scriptEngineMrg = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineMrg.getEngineByName(scriptMetadata.getLang());

        Bindings bindings = scriptEngine.createBindings();
        bindings.put("discoveryContext", context);
        bindings.put("action", action);
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        
        Set<DiscoveredResourceDetails> details =
            (Set<DiscoveredResourceDetails>) scriptEngine.eval(loadScript(scriptMetadata));

        return details;
    }
    
}
