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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public abstract class AltLangAbstractComponent {

    private ScriptResolverFactory scriptResolverFactory = new ScriptResolverFactoryImpl();

    public void setScriptResolverFactory(ScriptResolverFactory scriptResolverFactory) {
        this.scriptResolverFactory = scriptResolverFactory;
    }

    protected ScriptMetadata resolveScript(Configuration pluginConfiguration, Action action) {
//        ScriptResolver resolver = new ScriptPerActionTypeResolver();
        ScriptResolver resolver = scriptResolverFactory.getScriptResolver();
        ScriptMetadata scriptMetadata = resolver.resolveScript(pluginConfiguration, action);

        return scriptMetadata;
    }

    protected Reader loadScript(ScriptMetadata metadata) {
        InputStream stream = getClass().getResourceAsStream(metadata.getScriptPath());
        return new InputStreamReader(stream);
    }

}