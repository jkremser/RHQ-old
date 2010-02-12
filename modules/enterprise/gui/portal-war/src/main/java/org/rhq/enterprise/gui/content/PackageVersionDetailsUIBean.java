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
package org.rhq.enterprise.gui.content;

import java.io.ByteArrayOutputStream;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class PackageVersionDetailsUIBean {
    private PackageVersionComposite packageVersionComposite;

    public PackageVersionComposite getPackageVersionComposite() {
        loadPackageVersionComposite();
        return this.packageVersionComposite;
    }

    public String getPackageDownloadUrl() {
        return "";
    }

    public String getPackageBits() {
        Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        PackageVersion pv = LookupUtil.getContentUIManager().getPackageVersion(id);
        Package p = pv.getGeneralPackage();
        if (p.getPackageType().getName().equals(PackageType.TYPE_NAME_CFG)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            LookupUtil.getContentSourceManager().outputPackageVersionBits(pv, bos);
            return new String(bos.toByteArray());
        } else {
            RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            PageList<Repo> repos = repoManager.findReposByPackageVersionId(subject, pv.getId());
            String repoName = repos.get(0).getName();
            String packageName = pv.getFileName();
            String path = "content/" + repoName + "/packages/" + packageName;
            return path;
        }
    }

    private void loadPackageVersionComposite() {
        if (this.packageVersionComposite == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
            ContentUIManagerLocal manager = LookupUtil.getContentUIManager();
            this.packageVersionComposite = manager.loadPackageVersionCompositeWithExtraProperties(subject, id);
        }
    }
}