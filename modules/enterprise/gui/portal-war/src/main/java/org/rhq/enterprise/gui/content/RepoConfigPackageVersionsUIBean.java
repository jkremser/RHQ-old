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

import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoConfigPackageVersionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "RepoConfigPackageVersionsUIBean";
    public static final String FORM_ID = "repoConfigPackageVersionsListForm";
    public static final String FILTER_ID = FORM_ID + ":" + "packageFilter";

    private String packageFilter;

    public RepoConfigPackageVersionsUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new RepoConfigPackageVersionsDataModel(PageControlView.RepoConfigPackageVersionsList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public void init() {
        if (this.packageFilter == null) {
            this.packageFilter = FacesContextUtility.getOptionalRequestParameter(FILTER_ID);
        }
    }

    private class RepoConfigPackageVersionsDataModel extends PagedListDataModel<PackageVersion> {
        public RepoConfigPackageVersionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<PackageVersion> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            RepoConfigPackageVersionsUIBean.this.init();

            RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

            PageList<PackageVersion> results;
            results = manager.findConfigPackageVersionsInRepo(subject, id, getPackageFilter(), pc);
            return results;
        }
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public void setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
    }
}