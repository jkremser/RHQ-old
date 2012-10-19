/*
* Jopr Management Platform
* Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.helper;

import java.util.jar.Attributes;

/**
 * The product type of a JBoss 5.x/6.x application server installation - AS, EAP, EWP, or SOA.
 *
 * @author Jessica Sant
 * @author Ian Springer
 */
public enum JBossProductType {
    AS("JBoss AS", "JBoss Application Server", "default"), // the public offering
    EAP("JBoss EAP", "JBoss Enterprise Application Platform", "default"), // the customer offering
    EWP("JBoss EWP", "JBoss Enterprise Web Platform", "default"), // the customer offering    
    SOA("JBoss SOA-P", "JBoss Enterprise SOA Platform", "default"); // the customer SOA platform

    public final String NAME;
    public final String DESCRIPTION;
    public final String DEFAULT_CONFIG_NAME;

    private static final String EAP_IMPLEMENTATION_TITLE = "JBoss [EAP]";
    private static final String EWP_IMPLEMENTATION_TITLE = "JBoss [EWP]";
    private static final String SOA_IMPLEMENTATION_TITLE = "JBoss [SOA]";

    JBossProductType(String name, String description, String defaultConfigName) {
        this.NAME = name;
        this.DESCRIPTION = description;
        this.DEFAULT_CONFIG_NAME = defaultConfigName;
    }

    /**
     * Determines the product type (AS, EAP, EWP, or SOA) based on the Implementation-Title MANIFEST.MF attribute.
     *
     * @param attributes the attributes from a manifest file (typically run.jar or jboss-j2ee.jar)
     *
     * @return the product type (AS, EAP, EWP, or SOA)
     */
    public static JBossProductType determineJBossProductType(Attributes attributes) {
        JBossProductType result = JBossProductType.AS;
        String implementationTitle = (attributes != null) ? attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE)
            : null;
        if (implementationTitle != null) {
            if (implementationTitle.equalsIgnoreCase(EAP_IMPLEMENTATION_TITLE)) {
                result = JBossProductType.EAP;
            } else if (implementationTitle.equalsIgnoreCase(EWP_IMPLEMENTATION_TITLE)) {
                result = JBossProductType.EWP;
            } else if (implementationTitle.equalsIgnoreCase(SOA_IMPLEMENTATION_TITLE)) {
                result = JBossProductType.SOA;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return this.NAME;
    }
}