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
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class AltLangComponent extends AltLangAbstractComponent
    implements ResourceComponent, OperationFacet {

    private ResourceContext resourceContext;

    private ScriptExecutorService scriptExecutor = new ScriptExecutorServiceImpl();

    void setScriptExecutor(ScriptExecutorService scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    /**
     * This is just a test hook for unit tests. The resourceContext field is initialized in the
     * {@link #start(org.rhq.core.pluginapi.inventory.ResourceContext)} method.
     * 
     * @param resourceContext
     */
    void setResourceContext(ResourceContext resourceContext) {
        this.resourceContext = resourceContext;
    }

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Action action = createAction("resource_component", "start");
        ScriptMetadata metadata = resolveScript(resourceContext.getPluginConfiguration(), action);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("action", action);
        bindings.put("resourceContext", resourceContext);

        scriptExecutor.executeScript(metadata, bindings);
    }

    public void stop() {
        Action action = createAction("resource_component", "stop");
        ScriptMetadata metadata = resolveScript(resourceContext.getPluginConfiguration(), action);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("action", action);
        bindings.put("resourceContext", resourceContext);

        scriptExecutor.executeScript(metadata, bindings);
    }

    public AvailabilityType getAvailability() {
        Action action = createAction("resource_component", "get_availability");
        ScriptMetadata metadata = resolveScript(resourceContext.getPluginConfiguration(), action);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("action", action);
        bindings.put("resourceContext", resourceContext);

        AvailabilityType availabilityType = scriptExecutor.executeScript(metadata, bindings);

        return availabilityType;
    }

    public OperationResult invokeOperation(String name, Configuration parameters)
        throws InterruptedException, Exception {

        Action action = createAction("operations", name);
        ScriptMetadata metadata = resolveScript(resourceContext.getPluginConfiguration(), action);

        ScriptEngine scriptEngine = createScriptEngine(metadata);

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("operation", name);
        vars.put("parameters", parameters);

        setBindings(scriptEngine, action, vars);

        OperationResult result = (OperationResult) scriptEngine.eval(loadScript(metadata));
        return result;
    }

    private Action createAction(String actionType, String action) {
        return new Action(actionType, action, resourceContext.getResourceType());
    }

    private ScriptEngine createScriptEngine(ScriptMetadata metadata) {
        ScriptEngineManager scriptEngineMrg = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineMrg.getEngineByName(metadata.getLang());
        return scriptEngine;
    }

    private Bindings setBindings(ScriptEngine scriptEngine, Action action, Map<String, ?> vars) {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("resourceContext", resourceContext);
        bindings.put("action", action);

        if (vars != null) {
            for (Map.Entry<String, ?> entry : vars.entrySet()) {
                bindings.put(entry.getKey(), entry.getValue());
            }
        }

        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        return bindings;
    }

}
