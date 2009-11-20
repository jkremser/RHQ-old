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

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoDetailsUIBean {
    private Repo repo;

    public Repo getRepo() {
        loadRepo();
        return this.repo;
    }

    public String edit() {
        return "edit";
    }

    public String getSyncStatus() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        return LookupUtil.getRepoManagerLocal().calculateSyncStatus(subject, id);
    }

    public String sync() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Integer[] repoIds = { FacesContextUtility.getRequiredRequestParameter("id", Integer.class) };
        int syncCount = LookupUtil.getRepoManagerLocal().synchronizeRepos(subject, repoIds);
        if (syncCount > 0) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The repository is syncing.");
        } else {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Nothing to sync for this Repository.");
        }
        return "success";
    }

    public String save() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

        try {
            manager.updateRepo(subject, repo);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The repo has been updated.");
        } catch (ContentException ce) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ce.getMessage());
            return "edit"; // stay in edit mode on failure
        }

        return "success";
    }

    public String cancel() {
        return "success";
    }

    private void loadRepo() {
        if (this.repo == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
            RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();
            this.repo = manager.getRepo(subject, id);
            this.repo.setSyncStatus(manager.calculateSyncStatus(subject, id));
        }
    }
}