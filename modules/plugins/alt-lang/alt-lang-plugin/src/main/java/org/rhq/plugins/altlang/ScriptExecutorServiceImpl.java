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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

public class ScriptExecutorServiceImpl implements ScriptExecutorService {

    public <T> T executeScript(ScriptMetadata scriptMetadata, Map<String, ?> scriptBindings) {
        ScriptEngineManager scriptEngineMrg = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineMrg.getEngineByName(scriptMetadata.getLang());
        Bindings bindings = scriptEngine.createBindings();

        setBindings(scriptBindings, scriptEngine, bindings);
        Reader scriptReader = loadScript(scriptMetadata);

        try {
            return (T) scriptEngine.eval(scriptReader);
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private Reader loadScript(ScriptMetadata metadata) {
        InputStream stream = getClass().getResourceAsStream(metadata.getScriptPath());
        return new InputStreamReader(stream);
    }

    private <T> void setBindings(Map<String, ?> scriptBindings, ScriptEngine scriptEngine, Bindings bindings) {
        if (scriptBindings != null) {
            bindings.putAll(scriptBindings);
        }
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    }

}
