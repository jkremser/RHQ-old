/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
 * * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.bundle.ant.type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

import org.rhq.bundle.ant.BundleAntProject.AuditStatus;
import org.rhq.core.util.updater.DestinationComplianceMode;
import org.rhq.bundle.ant.DeployPropertyNames;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.updater.DeployDifferences;
import org.rhq.core.util.updater.Deployer;
import org.rhq.core.util.updater.DeploymentData;
import org.rhq.core.util.updater.DeploymentProperties;

/**
 * An Ant task for deploying a bundle or previewing the deployment.
 *
 * @author Ian Springer
 */
public class DeploymentUnitType extends AbstractBundleType {
    private String name;

    private DestinationComplianceMode compliance;

    private Map<File, File> files = new LinkedHashMap<File, File>();
    private Map<URL, File> urlFiles = new LinkedHashMap<URL, File>();
    private Set<File> rawFilesToReplace = new LinkedHashSet<File>();
    private Set<URL> rawUrlFilesToReplace = new LinkedHashSet<URL>();
    private Map<File, String> localFileNames = new LinkedHashMap<File, String>();

    private Set<File> archives = new LinkedHashSet<File>();
    private Set<URL> urlArchives = new LinkedHashSet<URL>();
    private Map<File, Pattern> archiveReplacePatterns = new HashMap<File, Pattern>();
    private Map<URL, Pattern> urlArchiveReplacePatterns = new HashMap<URL, Pattern>();
    private Map<File, Boolean> archivesExploded = new HashMap<File, Boolean>();
    private Map<URL, Boolean> urlArchivesExploded = new HashMap<URL, Boolean>();
    private Map<File, String> localArchiveNames = new LinkedHashMap<File, String>();

    private SystemServiceType systemService;
    private Pattern ignorePattern;
    private String preinstallTarget;
    private String postinstallTarget;

    public void init() throws BuildException {
        if (this.systemService != null) {
            this.systemService.init();
        }
    }

    public void install(boolean revert, boolean clean) throws BuildException {
        if (clean) {
            getProject().auditLog(
                AuditStatus.INFO,
                "Clean Requested",
                "A clean deployment has been requested. Files will be deleted!",
                "A clean deployment has been requested. Files will be deleted"
                    + " from the destination directory prior to the new deployment files getting written", null);
        }
        if (revert) {
            getProject().auditLog(
                AuditStatus.INFO,
                "Revert Requested",
                "The previous deployment will be reverted!",
                "The previous deployment will be reverted. An attempt to restore"
                    + " backed up files and the old deployment content will be made", null);
        }

        try {
            boolean dryRun = getProject().isDryRun();

            DestinationComplianceMode complianceToUse = DestinationComplianceMode.instanceOrDefault(this.compliance);

            File deployDir = getProject().getDeployDir();
            TemplateEngine templateEngine = createTemplateEngine(getProject().getUserProperties());
            int deploymentId = getProject().getDeploymentId();
            DeploymentProperties deploymentProps = new DeploymentProperties(deploymentId, getProject().getBundleName(),
                getProject().getBundleVersion(), getProject().getBundleDescription(), complianceToUse);

            if (this.preinstallTarget != null) {
                getProject().auditLog(AuditStatus.SUCCESS, "Pre-Install Started", "The pre install target will start",
                    "The pre install target named [" + this.preinstallTarget + "] will start", null);
                Target target = (Target) getProject().getTargets().get(this.preinstallTarget);
                if (target == null) {
                    try {
                        getProject().auditLog(
                            AuditStatus.FAILURE,
                            "Pre-Install Failure",
                            "The pre install target does not exist",
                            "The pre install target specified in the recipe [" + this.preinstallTarget
                                + "] does not exist.", null);
                    } catch (Throwable ignore) {
                        // swallow any errors that occur here, we want to throw the real build exception
                    }
                    throw new BuildException("Specified preinstall target (" + this.preinstallTarget
                        + ") does not exist.");
                }
                target.performTasks();
                getProject().auditLog(AuditStatus.SUCCESS, "Pre-Install Finished",
                    "The pre install target has finished", null, null);
            }

            boolean haveSomethingToDo = false;
            if (!this.files.isEmpty()) {
                haveSomethingToDo = true;
                log("Deploying files " + this.files + "...", Project.MSG_VERBOSE);
            }
            if (!this.urlFiles.isEmpty()) {
                haveSomethingToDo = true;
                log("Deploying files from URL " + this.urlFiles + "...", Project.MSG_VERBOSE);
            }
            if (!this.archives.isEmpty()) {
                haveSomethingToDo = true;
                log("Deploying archives " + this.archives + "...", Project.MSG_VERBOSE);
            }
            if (!this.urlArchives.isEmpty()) {
                haveSomethingToDo = true;
                log("Deploying archives from URL " + this.urlArchives + "...", Project.MSG_VERBOSE);
            }
            if (!haveSomethingToDo) {
                throw new BuildException(
                    "You must specify at least one file to deploy via nested file, archive, url-file, url-archive types in your recipe");
            }

            log("Destination compliance set to '" + complianceToUse + "'.", Project.MSG_VERBOSE);
            switch (complianceToUse) {
            case full:
                if (!dryRun) {
                    getProject()
                        .auditLog(
                            AuditStatus.INFO,
                            "Managing Top Level Deployment Directory",
                            "The top level deployment directory will be managed - files found there will be backed up and removed!",
                            "The bundle recipe has requested that the top level deployment directory be fully managed by RHQ." +
                            "This means any files currently located in the top level deployment directory will be removed and backed up",
                            null);
                }
                break;
            case filesAndDirectories:
                log("Files and directories in the destination directory not contained in the bundle will be kept intact.\n" +
                    "Note that the contents of the directories that ARE contained in the bundle will be synced with " +
                    "the contents as specified in the bundle. I.e. the subdirectories in the destination that are also " +
                    "contained in the bundle are made compliant with the bundle.", Project.MSG_VERBOSE);
                break;
            default:
                throw new IllegalStateException("Unhandled destination compliance mode: " + complianceToUse.toString());
            }

            Set<File> allArchives = new HashSet<File>(this.archives);
            Map<File, File> allFiles = new HashMap<File, File>(this.files);
            Map<File, Pattern> allArchiveReplacePatterns = new HashMap<File, Pattern>(this.archiveReplacePatterns);
            Set<File> allRawFilesToReplace = new HashSet<File>(this.rawFilesToReplace);
            Map<File, Boolean> allArchivesExploded = new HashMap<File, Boolean>(this.archivesExploded);
            downloadFilesFromUrlEndpoints(allArchives, allFiles, allArchiveReplacePatterns, allRawFilesToReplace,
                allArchivesExploded);

            try {
                DeploymentData deploymentData = new DeploymentData(deploymentProps, allArchives, allFiles, getProject()
                    .getBaseDir(), deployDir, allArchiveReplacePatterns, allRawFilesToReplace, templateEngine,
                    this.ignorePattern, allArchivesExploded);
                Deployer deployer = new Deployer(deploymentData);
                DeployDifferences diffs = getProject().getDeployDifferences();

                // we only want to emit audit trail when something is really going to happen on disk; don't log if doing a dry run
                if (!dryRun) {
                    getProject().auditLog(AuditStatus.SUCCESS, "Deployer Started", "The deployer has started its work",
                        null, null);
                }

                if (revert) {
                    deployer.redeployAndRestoreBackupFiles(diffs, clean, dryRun);
                } else {
                    deployer.deploy(diffs, clean, dryRun);
                }

                // we only want to emit audit trail when something is really going to happen on disk; don't log if doing a dry run
                if (!dryRun) {
                    getProject().auditLog(AuditStatus.SUCCESS, "Deployer Finished",
                        "The deployer has finished its work", null, diffs.toString());
                }
            } catch (Throwable t) {
                try {
                    getProject().auditLog(AuditStatus.FAILURE, "Deployer Failed",
                        "The deployer encountered an error and could not finished", ThrowableUtil.getAllMessages(t),
                        ThrowableUtil.getStackAsString(t));
                } catch (Throwable ignore) {
                    // swallow any errors that occur here, we want to throw the real build exception
                }
                throw new BuildException("Failed to deploy bundle [" + getProject().getBundleName() + "] version ["
                    + getProject().getBundleVersion() + "]: " + t, t);
            }

            if (this.systemService != null) {
                this.systemService.install();
            }

            if (this.postinstallTarget != null) {
                getProject().auditLog(AuditStatus.SUCCESS, "Post-Install Started",
                    "The post install target will start",
                    "The post install target named [" + this.postinstallTarget + "] will start", null);
                Target target = (Target) getProject().getTargets().get(this.postinstallTarget);
                if (target == null) {
                    try {
                        getProject().auditLog(
                            AuditStatus.FAILURE,
                            "Post-Install Failure",
                            "The post install target does not exist",
                            "The post install target specified in the recipe [" + this.postinstallTarget
                                + "] does not exist.", null);
                    } catch (Throwable ignore) {
                        // swallow any errors that occur here, we want to throw the real build exception
                    }
                    throw new BuildException("Specified postinstall target (" + this.postinstallTarget
                        + ") does not exist.");
                }
                target.performTasks();
                getProject().auditLog(AuditStatus.SUCCESS, "Post-Install Finished",
                    "The post install target has finished", null, null);
            }
        } catch (Throwable t) {
            try {
                getProject().auditLog(AuditStatus.FAILURE, "Error Occurred",
                    "The deployment could not complete successfully.", ThrowableUtil.getAllMessages(t),
                    ThrowableUtil.getStackAsString(t));
            } catch (Throwable ignore) {
                // swallow any errors that occur here, we want to throw the real build exception
            }
            if (t instanceof BuildException) {
                throw (BuildException) t;
            } else {
                throw new BuildException(t);
            }
        }
        return;
    }

    /**
     * This will download any files/archives that are found at URL endpoints as declared in the ant recipe.
     *
     * @param allArchives when a new archive is downloaded, its information is added to this
     * @param allFiles when a new raw file is downloaded, its information is added to this
     * @param allArchiveReplacePatterns when a new archive is downloaded, its information is added to this
     * @param allRawFilesToReplace when a new raw file is downloaded, its information is added to this
     * @param allArchivesExploded when a new archive is downloaded, its information is added to this
     */
    private void downloadFilesFromUrlEndpoints(Set<File> allArchives, Map<File, File> allFiles,
        Map<File, Pattern> allArchiveReplacePatterns, Set<File> allRawFilesToReplace,
        Map<File, Boolean> allArchivesExploded) throws Exception {

        // check to see if we even need to download anything, if not, do nothing and return immediately
        if (this.urlFiles.isEmpty() && this.urlArchives.isEmpty()) {
            return;
        }

        // download all our files in the base dir, as if they came with the bundle like normal files
        File downloadDir = getProject().getBaseDir();
        Set<File> downloadedFiles = getProject().getDownloadedFiles();

        try {
            // do the raw files first
            for (Map.Entry<URL, File> fileEntry : this.urlFiles.entrySet()) {
                URL url = fileEntry.getKey();
                File destFile = fileEntry.getValue();
                File tmpFile = new File(downloadDir, destFile.getPath()); // use getPath in case they have 2+ raw files with the same name
                download(url, tmpFile);
                downloadedFiles.add(tmpFile);
                allFiles.put(tmpFile, destFile);
                if (this.rawUrlFilesToReplace.contains(url)) {
                    allRawFilesToReplace.add(tmpFile);
                }
            }

            // do the archives next
            for (URL url : this.urlArchives) {
                // determine what the base filename should be of our downloaded tmp archive file
                String baseFileName = url.getPath();
                if (baseFileName.endsWith("/")) {
                    baseFileName = baseFileName.substring(0, baseFileName.length());
                }
                int lastSlash = baseFileName.lastIndexOf('/');
                if (lastSlash != -1) {
                    baseFileName = baseFileName.substring(lastSlash + 1);
                }
                if (baseFileName.length() == 0) {
                    baseFileName = url.getHost();
                }

                File tmpFile = new File(downloadDir, baseFileName);
                download(url, tmpFile);
                downloadedFiles.add(tmpFile);
                allArchives.add(tmpFile);
                if (this.urlArchiveReplacePatterns.containsKey(url)) {
                    allArchiveReplacePatterns.put(tmpFile, this.urlArchiveReplacePatterns.get(url));
                }
                if (this.urlArchivesExploded.containsKey(url)) {
                    allArchivesExploded.put(tmpFile, this.urlArchivesExploded.get(url));
                }
            }

            return;

        } catch (Exception e) {
            // can't do anything with any files we did download - be nice and clean up
            try {
                for (File doomed : downloadedFiles) {
                    doomed.delete();
                }
            } catch (Exception ignore) {
                // ignore this, we just can't delete them - but we want to throw our original exception
            }
            throw e;
        }
    }

    private void download(URL url, File tmpFile) throws Exception {
        getProject().auditLog(AuditStatus.SUCCESS, "File Download Started", "Downloading file from URL",
            "Downloading file from URL: " + url, null);

        long size;
        try {
            InputStream in = url.openStream();
            tmpFile.getParentFile().mkdirs(); // if this fails, our next line will throw a file-not-found error and we'll abort
            OutputStream out = new FileOutputStream(tmpFile);
            size = StreamUtil.copy(in, out);
        } catch (Exception e) {
            getProject().auditLog(AuditStatus.FAILURE, "File Download Failed",
                "Failed to download content from a remote server", "Failed to download file from: " + url,
                ThrowableUtil.getStackAsString(e));
            throw e;
        }

        getProject().auditLog(AuditStatus.SUCCESS, "File Download Finished", "Successfully downloaded file from URL",
            "Downloaded file of size [" + size + "] bytes from URL: " + url, null);
    }

    public void start() throws BuildException {
        if (this.systemService != null) {
            this.systemService.start();
        }
    }

    public void stop() throws BuildException {
        if (this.systemService != null) {
            this.systemService.stop();
        }
    }

    public void upgrade(boolean revert, boolean clean) throws BuildException {
        install(revert, clean);
    }

    public void uninstall() throws BuildException {
        if (this.systemService != null) {
            this.systemService.uninstall();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @deprecated since RHQ 4.9.0, use {@link #getCompliance()}
     */
    public String getManageRootDir() {
        return Boolean.toString(getCompliance() == DestinationComplianceMode.full);
    }

    /**
     * @deprecated since RHQ 4.9.0, use {@link #setCompliance(org.rhq.core.util.updater.DestinationComplianceMode)}
     */
    public void setManageRootDir(String booleanString) {
        if (!Boolean.TRUE.toString().equalsIgnoreCase(booleanString)
            && !Boolean.FALSE.toString().equalsIgnoreCase(booleanString)) {
            throw new BuildException("manageRootDir attribute must be 'true' or 'false': " + booleanString);
        }

        log("The deprecated 'manageRootDir' attribute was detected. Please consider replacing it with the 'compliance' attribute.",
            Project.MSG_INFO);

        boolean val = Boolean.parseBoolean(booleanString);

        setCompliance(val ? DestinationComplianceMode.full : DestinationComplianceMode.filesAndDirectories);
    }

    /**
     * @since 4.9.0
     */
    public DestinationComplianceMode getCompliance() {
        return compliance;
    }

    /**
     * @since 4.9.0
     */
    public void setCompliance(DestinationComplianceMode value) {
        this.compliance = value;
    }

    /**
     * Returns a map of all raw files. The key is the full absolute path
     * to the file as it does or would appear on the file system. The value
     * is a path that is either absolute or relative - it is the destination
     * where the file is to be placed when being deployed on the destination file system;
     * if the value is relative, then it is relative to the root destination directory.
     * 
     * @return map of raw files
     */
    public Map<File, File> getFiles() {
        return files;
    }

    /**
     * Returns a map of all raw files. The key is the full absolute path
     * to the file as it does or would appear on the file system (the same key
     * as the keys in map {@link #getFiles()}).
     * The value is a path relative to the file as it is found in the bundle distro (this 
     * is the "name" attribute of the "file" type tag).
     * 
     * @return map of local file names
     */
    public Map<File, String> getLocalFileNames() {
        return localFileNames;
    }

    public Set<File> getArchives() {
        return archives;
    }

    /**
     * Returns a map of all archive files. The key is the full absolute path
     * to the archive as it does or would appear on the file system (the same key
     * as the keys in map {@link #getArchives()}).
     * The value is a path relative to the file as it is found in the bundle distro (this 
     * is the "name" attribute of the "archive" type tag).
     * 
     * @return map of local file names
     */
    public Map<File, String> getLocalArchiveNames() {
        return localArchiveNames;
    }

    /**
     * Returns a map keyed on {@link #getArchives() archive names} whose values
     * are either true or false, where true means the archive is to be deployed exploded
     * and false means the archive should be deployed in compressed form.
     * 
     * @return map showing how an archive should be deployed in its final form
     */
    public Map<File, Boolean> getArchivesExploded() {
        return archivesExploded;
    }

    public String getPreinstallTarget() {
        return preinstallTarget;
    }

    public void setPreinstallTarget(String preinstallTarget) {
        this.preinstallTarget = preinstallTarget;
    }

    public String getPostinstallTarget() {
        return postinstallTarget;
    }

    public void setPostinstallTarget(String postinstallTarget) {
        this.postinstallTarget = postinstallTarget;
    }

    public void addConfigured(SystemServiceType systemService) {
        if (this.systemService != null) {
            throw new IllegalStateException(
                "A rhq:deployment-unit element can only have one rhq:system-service child element.");
        }
        this.systemService = systemService;
        this.systemService.validate();

        // Add the init script and its config file to the list of bundle files.
        this.files.put(this.systemService.getScriptFile(), this.systemService.getScriptDestFile());
        this.localFileNames.put(this.systemService.getScriptFile(), this.systemService.getScriptFileName());
        if (this.systemService.getConfigFile() != null) {
            this.files.put(this.systemService.getConfigFile(), this.systemService.getConfigDestFile());
            this.localFileNames.put(this.systemService.getConfigFile(), this.systemService.getConfigFileName());
            this.rawFilesToReplace.add(this.systemService.getConfigFile());
        }
    }

    public void addConfigured(FileType file) {
        File destFile = file.getDestinationFile();
        if (destFile == null) {
            File destDir = file.getDestinationDir();
            destFile = new File(destDir, file.getSource().getName());
        }
        this.files.put(file.getSource(), destFile); // key=full absolute path, value=could be relative or absolute
        this.localFileNames.put(file.getSource(), file.getName());
        if (file.isReplace()) {
            this.rawFilesToReplace.add(file.getSource());
        }
    }

    public void addConfigured(ArchiveType archive) {
        this.archives.add(archive.getSource());
        this.localArchiveNames.put(archive.getSource(), archive.getName());
        Pattern replacePattern = archive.getReplacePattern();
        if (replacePattern != null) {
            this.archiveReplacePatterns.put(archive.getSource(), replacePattern);
        }
        Boolean exploded = Boolean.valueOf(archive.getExploded());
        this.archivesExploded.put(archive.getSource(), exploded);
    }

    public void addConfigured(UrlFileType file) {
        File destFile = file.getDestinationFile();
        if (destFile == null) {
            File destDir = file.getDestinationDir();
            destFile = new File(destDir, file.getBaseName());
        }
        this.urlFiles.put(file.getSource(), destFile); // key=full absolute path, value=could be relative or absolute
        if (file.isReplace()) {
            this.rawUrlFilesToReplace.add(file.getSource());
        }
    }

    public void addConfigured(UrlArchiveType archive) {
        this.urlArchives.add(archive.getSource());
        Pattern replacePattern = archive.getReplacePattern();
        if (replacePattern != null) {
            this.urlArchiveReplacePatterns.put(archive.getSource(), replacePattern);
        }
        Boolean exploded = Boolean.valueOf(archive.getExploded());
        this.urlArchivesExploded.put(archive.getSource(), exploded);
    }

    public void addConfigured(IgnoreType ignore) {
        List<FileSet> fileSets = ignore.getFileSets();
        this.ignorePattern = getPattern(fileSets);
    }

    private TemplateEngine createTemplateEngine(Hashtable<String, String> properties) {
        TemplateEngine templateEngine = SystemInfoFactory.fetchTemplateEngine();

        // add tags to Template Engine tokens
        if (properties != null) {
            for (String s : properties.keySet()) {
                if (s.startsWith(DeployPropertyNames.DEPLOY_TAG_PREFIX)) {
                    templateEngine.getTokens().put(s, properties.get(s));
                }
            }
        }

        // Add the deployment props to the template engine's tokens.
        Configuration config = getProject().getConfiguration();
        for (PropertySimple prop : config.getSimpleProperties().values()) {
            templateEngine.getTokens().put(prop.getName(), prop.getStringValue());
        }
        // And add the special rhq.deploy.dir prop.
        templateEngine.getTokens().put(DeployPropertyNames.DEPLOY_DIR,
            getProject().getProperty(DeployPropertyNames.DEPLOY_DIR));
        return templateEngine;
    }
}
