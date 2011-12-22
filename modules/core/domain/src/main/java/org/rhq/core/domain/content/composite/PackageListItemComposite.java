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
package org.rhq.core.domain.content.composite;

import java.io.Serializable;

/**
 * Contains information displayed for each package in the package list UI.
 *
 * @author Jason Dobies
 */
public class PackageListItemComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String packageName;
    private final String packageTypeName;
    private final String version;
    private final String displayVersion;
    private final Long timestamp;

    public PackageListItemComposite(int id, String packageName, String packageTypeName, String version,
        String displayVersion, Long timestamp) {
        this.id = id;
        this.packageName = packageName;
        this.packageTypeName = packageTypeName;
        this.version = version;
        this.timestamp = timestamp;
        this.displayVersion = displayVersion;
    }

    public int getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getPackageTypeName() {
        return packageTypeName;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayVersion() {
        return displayVersion;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}