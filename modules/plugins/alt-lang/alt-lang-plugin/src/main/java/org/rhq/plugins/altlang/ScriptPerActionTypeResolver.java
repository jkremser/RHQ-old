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
import org.rhq.core.domain.configuration.PropertySimple;

public class ScriptPerActionTypeResolver implements ScriptResolver {

    public ScriptMetadata resolveScript(Configuration pluginConfiguration, Action action) {
        ScriptMetadata scriptMetadata = new ScriptMetadata();
        scriptMetadata.setExtension(getExtension(pluginConfiguration));
        scriptMetadata.setLang(getLang(pluginConfiguration));
        scriptMetadata.setClasspathDirectory(getScriptDirectory(pluginConfiguration));
        scriptMetadata.setScriptName(action.getType() + "." + scriptMetadata.getExtension());

        return scriptMetadata;
    }

    private String getExtension(Configuration pluginConfiguration) {
        return getSimpleValue(pluginConfiguration, "scriptExtension");
    }

    private String getLang(Configuration pluginConfiguration) {
        return getSimpleValue(pluginConfiguration, "lang");
    }

    private String getScriptDirectory(Configuration pluginConfiguration) {
        return getSimpleValue(pluginConfiguration, "scriptsDirectory");
    }

    private String getSimpleValue(Configuration pluginConfiguration, String name) {
        PropertySimple property = pluginConfiguration.getSimple(name);
        return property.getStringValue();
    }

}
