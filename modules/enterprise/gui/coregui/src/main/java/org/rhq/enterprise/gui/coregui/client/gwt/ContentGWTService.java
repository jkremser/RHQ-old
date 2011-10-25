/*
 * RHQ Management Platform
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.PackageAndLatestVersionComposite;
import org.rhq.core.domain.content.composite.PackageTypeAndVersionFormatComposite;
import org.rhq.core.domain.criteria.InstalledPackageHistoryCriteria;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public interface ContentGWTService extends RemoteService {

    void deletePackageVersion(int packageVersionId) throws RuntimeException;

    PageList<PackageVersion> findPackageVersionsByCriteria(PackageVersionCriteria criteria) throws RuntimeException;

    PageList<Package> findPackagesByCriteria(PackageCriteria criteria) throws RuntimeException;

    PageList<PackageAndLatestVersionComposite> findPackagesWithLatestVersion(PackageCriteria criteria)
        throws RuntimeException;;

    PageList<InstalledPackageHistory> getInstalledPackageHistoryForResource(int resourceId, int count)
        throws RuntimeException;;

    PageList<InstalledPackageHistory> findInstalledPackageHistoryByCriteria(InstalledPackageHistoryCriteria criteria)
        throws RuntimeException;;

    List<Architecture> getArchitectures() throws RuntimeException;

    PackageType getResourceCreationPackageType(int resourceTypeId) throws RuntimeException;

    PackageTypeAndVersionFormatComposite findPackageType(Integer resourceTypeId, String packageTypeName)
        throws RuntimeException;

}
