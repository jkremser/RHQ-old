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

import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoDetailsUIBean {

    private static final Map<Enum<ContentSyncStatus>, String> statusMap = new HashMap<Enum<ContentSyncStatus>, String>();
    static {
        statusMap.put(ContentSyncStatus.ADVISORYMETADATA, "Advisories (Step 5 of 5)");
        statusMap.put(ContentSyncStatus.CANCELLED, "Cancelled");
        statusMap.put(ContentSyncStatus.CANCELLING, "Cancelling");
        statusMap.put(ContentSyncStatus.DISTROBITS, "Distro Bits (Step 4 of 5)");
        statusMap.put(ContentSyncStatus.DISTROMETADATA, "Distro Metadata (Step 3 of 5)");
        statusMap.put(ContentSyncStatus.FAILURE, "Failure");
        statusMap.put(ContentSyncStatus.INPROGRESS, "In Progress");
        statusMap.put(ContentSyncStatus.NONE, "None");
        statusMap.put(ContentSyncStatus.PACKAGEBITS, "Package Bits (Step 2 of 5)");
        statusMap.put(ContentSyncStatus.PACKAGEMETADATA, "Package Metadata (Step 1 of 5)");
        statusMap.put(ContentSyncStatus.SUCCESS, "Success");
    }

    private final Log log = LogFactory.getLog(this.getClass());

    private Repo repo;

    public Repo getRepo() {
        loadRepo();
        return this.repo;
    }

    public String edit() {
        return "edit";
    }

    private ContentSyncStatus getSyncStatusInternal() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        ContentSyncStatus status = LookupUtil.getRepoManagerLocal().calculateSyncStatus(subject, id);
        return status;
    }

    public boolean getCurrentlySyncing() {
        String syncStatus = getSyncStatusInternal().toString();
        if (!syncStatus.equals(ContentSyncStatus.SUCCESS.toString())
            && !syncStatus.equals(ContentSyncStatus.FAILURE.toString())
            && !syncStatus.equals(ContentSyncStatus.NONE.toString())
            && !syncStatus.equals(ContentSyncStatus.CANCELLED.toString())
            && !syncStatus.equals(ContentSyncStatus.CANCELLING.toString())) {
            return true;
        } else {
            return false;
        }
    }

    public String getSyncStatus() {
        return statusMap.get(getSyncStatusInternal());
    }

    public RepoSyncResults getSyncResults() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        return LookupUtil.getRepoManagerLocal().getMostRecentSyncResults(subject, id);
    }

    public String getPercentComplete() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        RepoSyncResults r = LookupUtil.getRepoManagerLocal().getMostRecentSyncResults(subject, id);
        String retval;
        if (r != null && r.getPercentComplete() != null) {
            retval = r.getPercentComplete().toString();
        } else {
            retval = "0";
        }
        return retval;
    }

    public String sync() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Integer[] repoIds = { FacesContextUtility.getRequiredRequestParameter("id", Integer.class) };
        int syncCount = 0;
        try {
            syncCount = LookupUtil.getRepoManagerLocal().synchronizeRepos(subject, repoIds);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + e.getMessage());
            log.error("Error synchronizing repo ID [" + repoIds + "]", e);
            return "edit";
        }
        if (syncCount > 0) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The repository is syncing.");
        } else {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Nothing to sync for this repository.");
        }
        return "success";
    }

    public String save() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

        try {
            manager.updateRepo(subject, repo);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The repository has been updated.");
        } catch (ContentException ce) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ce.getMessage());
            return "edit"; // stay in edit mode on failure
        }

        return "success";
    }

    public String cancelSync() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();
        Integer repoId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        try {
            manager.cancelSync(subject, repoId);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The synchronization has been cancelled.");
        } catch (Exception ce) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ce.getMessage());
            return "success"; // stay in edit mode on failure
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
            this.repo.setSyncStatus(manager.calculateSyncStatus(subject, id).toString());
        }
    }
}