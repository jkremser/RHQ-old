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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.faces.application.FacesMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.model.UploadItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentException;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Collects data necessary to create a config file and associate it to a repository.
 *
 * @author Sayli Karmarkar
 */
public class CreateNewConfigPackageUIBean {

    /**
     * Option value for uploading the config file to a repo the resource is already subscribed to.
     */
    private static final String REPO_OPTION_SUBSCRIBED = "subscribed";

    /**
     * Option value for uploading the config file to a repo the resource is not subscribed to, as well as automatically
     * subscribing the resource to that repo.
     */
    private static final String REPO_OPTION_UNSUBSCRIBED = "unsubscribed";

    /**
     * Option value for creating a new repo, subscribing the resource to it, and uploading config file to that
     * repo.
     */
    private static final String REPO_OPTION_NEW = "new";

    private String packageName;
    private String version;
    private String packageCategory;
    private int selectedArchitectureId;
    private int selectedPackageTypeId;

    /**
     * If the user selects to add config file to an existing repo that the resource is already subscribed to,
     * this will be populated with that repo ID.
     */
    private String subscribedRepoId;

    /**
     * If the user selects to add config file to an existing repo that the resource is not already subscribed to,
     * this will be populated with that repo ID.
     */
    private String unsubscribedRepoId;

    /**
     * If the user selects to add config file to a new repo, this will be populated with the new repo's name.
     */
    private String newRepoName;

    private final Log log = LogFactory.getLog(this.getClass());

    public String cancel() {
        UploadNewPackageUIBean uploadUIBean = FacesContextUtility.getManagedBean(UploadNewPackageUIBean.class);
        if (uploadUIBean != null) {
            uploadUIBean.clear();
        }
        return "cancel";
    }

    public String createConfigPackage() {
        HttpServletRequest request = FacesContextUtility.getRequest();

        String response = null;
        if (request.getParameter("newPackage") != null) {
            response = createNewPackage(packageName, version, selectedArchitectureId, selectedPackageTypeId,
                packageCategory);
        }
        return response;
    }

    public String createNewPackage(String packageName, String version, int architectureId, int packageTypeId,
        String packageCategory) {

        // Collect the necessary information
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        HttpServletRequest request = FacesContextUtility.getRequest();
        UploadNewPackageUIBean uploadUIBean = FacesContextUtility.getManagedBean(UploadNewPackageUIBean.class);

        String repoOption = request.getParameter("repoOption");
        UploadItem fileItem = uploadUIBean.getFileItem();

        // Validate
        if (packageName == null || packageName.trim().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package name must be specified");
            return null;
        }

        if (version == null || version.trim().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package version must be specified");
            return null;
        }

        if (repoOption == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "A repository deployment option must be specified");
            return null;
        }

        if (repoOption.equals(REPO_OPTION_NEW) && (newRepoName == null || newRepoName.trim().equals(""))) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "When creating a new repo, the name of the repository to be created must be specified");
            return null;
        }

        if ((fileItem == null) || fileItem.getFile() == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "A package file must be uploaded");
            return null;
        }

        // Determine which repo the package will go into
        String repoId = null;
        try {
            repoId = determineRepo(repoOption, subject, resource.getId());
        } catch (ContentException ce) {
            String errorMessages = ThrowableUtil.getAllMessages(ce);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to determine repository. Cause: "
                + errorMessages);
            return "failure";
        }

        try {
            // Grab a stream for the file being uploaded
            InputStream packageStream;

            try {
                log.debug("Streaming new package bits from uploaded file: " + fileItem.getFile());
                packageStream = new FileInputStream(fileItem.getFile());
            } catch (IOException e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to retrieve the input stream. Cause: " + errorMessages);
                return "failure";
            }

            // Ask the bean to create the package
            PackageVersion packageVersion;
            try {
                ContentManagerLocal contentManager = LookupUtil.getContentManager();
                packageVersion = contentManager.createConfigPackageVersion(packageName, packageTypeId, packageCategory,
                    version, architectureId, packageStream);
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to create config package ["
                    + packageName + "] in repo. Cause: " + errorMessages);
                return "failure";
            }

            int[] packageVersionList = new int[] { packageVersion.getId() };

            // Add the package to the repo
            try {
                int iRepoId = Integer.parseInt(repoId);

                RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
                repoManager.addPackageVersionsToRepo(subject, iRepoId, packageVersionList);
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to associate package ["
                    + packageName + "] with repository ID [" + repoId + "]. Cause: " + errorMessages);
                return "failure";
            }

            // Put the package ID in the session so it can fit into the deploy existing package workflow
            HttpSession session = request.getSession();
            session.setAttribute("selectedPackages", packageVersionList);
        } finally {
            // clean up the temp file
            uploadUIBean.clear();
        }

        return "success";
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getSelectedArchitectureId() {
        return selectedArchitectureId;
    }

    public void setSelectedArchitectureId(int selectedArchitectureId) {
        this.selectedArchitectureId = selectedArchitectureId;
    }

    public int getSelectedPackageTypeId() {
        return selectedPackageTypeId;
    }

    public void setSelectedPackageTypeId(int selectedPackageTypeId) {
        this.selectedPackageTypeId = selectedPackageTypeId;
    }

    public String getPackageCategory() {
        return packageCategory;
    }

    public void setPackageCategory(String packageCategory) {
        this.packageCategory = packageCategory;
    }

    public String getSubscribedRepoId() {
        return subscribedRepoId;
    }

    public void setSubscribedRepoId(String subscribedRepoId) {
        this.subscribedRepoId = subscribedRepoId;
    }

    public String getUnsubscribedRepoId() {
        return unsubscribedRepoId;
    }

    public void setUnsubscribedRepoId(String unsubscribedRepoId) {
        this.unsubscribedRepoId = unsubscribedRepoId;
    }

    public String getNewRepoName() {
        return newRepoName;
    }

    public void setNewRepoName(String newRepoName) {
        this.newRepoName = newRepoName;
    }

    private String determineRepo(String repoOption, Subject subject, int resourceId) throws ContentException {
        String repoId = null;

        if (repoOption.equals(REPO_OPTION_SUBSCRIBED)) {
            repoId = subscribedRepoId;
        } else if (repoOption.equals(REPO_OPTION_UNSUBSCRIBED)) {
            repoId = unsubscribedRepoId;
            int iRepoId = Integer.parseInt(repoId);

            RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
            repoManager.subscribeResourceToRepos(subject, resourceId, new int[] { iRepoId });

            // Change the subscribedRepoId so if we fall back to the page with a different error,
            // the drop down for selecting an existing subscribed repo will be populated with this
            // new repo
            subscribedRepoId = repoId;
        } else if (repoOption.equals(REPO_OPTION_NEW)) {
            RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();

            Repo newRepo = new Repo(newRepoName);
            newRepo = repoManager.createRepo(subject, newRepo);

            repoId = Integer.toString(newRepo.getId());

            repoManager.subscribeResourceToRepos(subject, resourceId, new int[] { newRepo.getId() });

            // Change the subscribedRepoId so if we fall back to the page with a different error,
            // the drop down for selecting an existing subscribed repo will be populated with this
            // new repo
            subscribedRepoId = repoId;
        }

        return repoId;
    }
}