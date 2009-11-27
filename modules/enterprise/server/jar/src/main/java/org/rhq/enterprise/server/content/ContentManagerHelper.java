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
package org.rhq.enterprise.server.content;

import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * ContentManagerHelper - Helper class to contain common methods needed by the Content managers.
 */
public class ContentManagerHelper {
    public static ContentServerPluginContainer getPluginContainer() throws Exception {
        ContentServerPluginContainer pc = null;

        try {
            ServerPluginServiceManagement mbean = LookupUtil.getServerPluginService();
            if (!mbean.isMasterPluginContainerStarted()) {
                throw new IllegalStateException("The master plugin container is not started!");
            }

            MasterServerPluginContainer master = mbean.getMasterPluginContainer();
            pc = master.getPluginContainerByClass(ContentServerPluginContainer.class);
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new Exception("Cannot obtain the content source plugin container", e);
        }

        if (pc == null) {
            throw new Exception("Content source plugin container is null!");
        }

        return pc;
    }

    public static ResourcePackageDetails installedPackageToDetails(InstalledPackage installedPackage) {
        PackageVersion packageVersion = installedPackage.getPackageVersion();
        ResourcePackageDetails details = packageVersionToDetails(packageVersion);

        return details;
    }

    public static ResourcePackageDetails packageVersionToDetails(PackageVersion packageVersion) {
        Package generalPackage = packageVersion.getGeneralPackage();

        PackageDetailsKey key = new PackageDetailsKey(generalPackage.getName(), packageVersion.getVersion(),
            packageVersion.getGeneralPackage().getPackageType().getName(), packageVersion.getArchitecture().getName());
        ResourcePackageDetails details = new ResourcePackageDetails(key);

        details.setClassification(generalPackage.getClassification());
        details.setDisplayName(packageVersion.getDisplayName());
        details.setFileCreatedDate(packageVersion.getFileCreatedDate());
        details.setFileName(packageVersion.getFileName());
        details.setFileSize(packageVersion.getFileSize());
        details.setLicenseName(packageVersion.getLicenseName());
        details.setLicenseVersion(packageVersion.getLicenseVersion());
        details.setLongDescription(packageVersion.getLongDescription());
        details.setMD5(packageVersion.getMD5());
        details.setMetadata(packageVersion.getMetadata());
        details.setSHA256(packageVersion.getSHA256());
        details.setShortDescription(packageVersion.getShortDescription());

        return details;
    }

}