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

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

public class AltLangDiscoveryServerComponent implements ResourceDiscoveryComponent {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        ScriptEngineManager scriptEngineMrg = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineMrg.getEngineByName("groovy");

        Bindings bindings = scriptEngine.createBindings();
        bindings.put("resourceDiscoveryContext", resourceDiscoveryContext);
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        Set<DiscoveredResourceDetails> details =
            (Set<DiscoveredResourceDetails>) scriptEngine.eval(getDiscoveryScript());

        return details;
    }

    private Reader getDiscoveryScript() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/discovery.groovy");
        return new InputStreamReader(stream);
    }
    
}
