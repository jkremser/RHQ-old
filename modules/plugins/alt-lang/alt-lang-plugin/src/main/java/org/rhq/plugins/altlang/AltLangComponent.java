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

public class AltLangComponent extends AltLangAbstractComponent implements ResourceComponent {

    private ResourceContext resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Action action = createAction("start");
        ScriptMetadata metadata = resolveScript(resourceContext.getPluginConfiguration(), action);

        ScriptEngine scriptEngine = createScriptEngine(metadata);
        setBindings(scriptEngine, action);

        scriptEngine.eval(loadScript(metadata));
    }

    public void stop() {
        try {
            Action action = createAction("stop");
            ScriptMetadata metadata = resolveScript(resourceContext.getPluginConfiguration(), action);

            ScriptEngine scriptEngine = createScriptEngine(metadata);
            setBindings(scriptEngine, action);

            scriptEngine.eval(loadScript(metadata));
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }        
    }

    public AvailabilityType getAvailability() {
        try {
            Action action = createAction("get_availability");
            ScriptMetadata metadata = resolveScript(resourceContext.getPluginConfiguration(), action);

            ScriptEngine scriptEngine = createScriptEngine(metadata);
            setBindings(scriptEngine, action);

            AvailabilityType availabilityType = (AvailabilityType) scriptEngine.eval(loadScript(metadata));

            return availabilityType;
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }        
    }

    private Action createAction(String action) {
        return new Action("resource_component", action, resourceContext.getResourceType());
    }

    private ScriptEngine createScriptEngine(ScriptMetadata metadata) {
        ScriptEngineManager scriptEngineMrg = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineMrg.getEngineByName(metadata.getLang());
        return scriptEngine;
    }

    private Bindings setBindings(ScriptEngine scriptEngine, Action action) {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("resourceContext", resourceContext);
        bindings.put("action", action);
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        return bindings;
    }

}
