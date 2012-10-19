/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.json;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Heiko W. Rupp
 */
public class Domain {


    public String name;
    @JsonProperty("management-interfaces") Map<String,String> managementInterfaces;
    @JsonProperty("profile-name") String profileName;
    public Map<String,String> path;
    public Map<String,String> subsystem;
    @JsonProperty("interface")
    public Map<String,String> interfaces;
    @JsonProperty("socket-binding-group")
    public Map<String,String> socketBindingGroup;

    @Override
    public String toString() {
        return "Domain{" +
                "name='" + name + '\'' +
                ", managementInterfaces=" + managementInterfaces +
                ", profileName='" + profileName + '\'' +
                ", path=" + path +
                ", subsystem=" + subsystem +
                ", interfaces=" + interfaces +
                ", socketBindingGroup=" + socketBindingGroup +
                '}';
    }
}
