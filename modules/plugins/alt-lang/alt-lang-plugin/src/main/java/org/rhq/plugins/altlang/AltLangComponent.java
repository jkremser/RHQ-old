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
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class AltLangComponent implements ResourceComponent {

    private ResourceContext resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        String script = buildScriptName();
        ScriptEngine scriptEngine = createScriptEngine();
        createBindings(scriptEngine, "start");
        scriptEngine.eval(loadScript(script));
    }

    public void stop() {
        try {
            String script = buildScriptName();
            ScriptEngine scriptEngine = createScriptEngine();
            createBindings(scriptEngine, "stop");
            scriptEngine.eval(loadScript(script));
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AvailabilityType getAvailability() {
        try {
            String script = buildScriptName();
            ScriptEngine scriptEngine = createScriptEngine();
            createBindings(scriptEngine, "get_availability");

            AvailabilityType availabilityType = (AvailabilityType) scriptEngine.eval(loadScript(script));

            return availabilityType;
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildScriptName() {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        String extension = pluginConfiguration.getSimple("scriptExtension").getStringValue();
        String scriptDir = pluginConfiguration.getSimple("scriptsDirectory").getStringValue();

        return scriptDir + "/resource_component." + extension;
    }

    private String getScriptLang() {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();
        return pluginConfiguration.getSimple("lang").getStringValue();
    }

    private Reader loadScript(String script) throws IOException {
        InputStream stream = getClass().getResourceAsStream(script);
        return new InputStreamReader(stream);
    }

    private ScriptEngine createScriptEngine() {
        ScriptEngineManager scriptEngineMrg = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineMrg.getEngineByName(getScriptLang());
        return scriptEngine;
    }

    private Bindings createBindings(ScriptEngine scriptEngine, String action) {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("resourceContext", resourceContext);
        bindings.put("action", new Action("resourcecomponent", action, resourceContext.getResourceType()));
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        return bindings;
    }

}
