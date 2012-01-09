/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.url;

import java.net.URL;

import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;

/**
 * Contains the full metadata about package content.
 * This encapsulates the information stored in {@link org.rhq.core.clientapi.server.plugin.content.ContentProviderPackageDetails}.
 * 
 * @author John Mazzitelli
 */
public class FullRemotePackageInfo extends RemotePackageInfo {

    private final ContentProviderPackageDetails details;

    public FullRemotePackageInfo(URL url, ContentProviderPackageDetails details) {
        super(details.getLocation(), url, details.getSHA256());
        this.details = details;

        // the metadata provided let's us know our package type
        SupportedPackageType type = new SupportedPackageType();
        type.packageTypeName = details.getPackageTypeName();
        type.architectureName = details.getArchitectureName();
        type.resourceTypeName = details.getContentProviderPackageDetailsKey().getResourceTypeName();
        type.resourceTypePluginName = details.getContentProviderPackageDetailsKey().getResourceTypePluginName();
        setSupportedPackageType(type);
    }

    public ContentProviderPackageDetails getContentSourcePackageDetails() {
        return details;
    }

    public String toString() {
        StringBuilder str = new StringBuilder("FullRemotePackageInfo: ");
        str.append("location=[").append(this.getLocation());
        str.append("], url=[").append(this.getUrl());
        str.append("], sha256=[").append(this.getSHA256());
        if (this.getSupportedPackageType() != null) {
            str.append("], supportedPackageType=[").append(this.getSupportedPackageType().packageTypeName);
            str.append(",").append(this.getSupportedPackageType().architectureName);
            str.append(",").append(this.getSupportedPackageType().resourceTypeName);
            str.append(",").append(this.getSupportedPackageType().resourceTypePluginName);
            str.append("], ");
        } else {
            str.append("], supportedPackageType=[unknown], ");
        }
        str.append("details=[" + this.details + "]");
        return str.toString();
    }
}
