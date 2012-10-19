/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.deploy;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.managed.api.DeploymentState;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.jboss.profileservice.spi.ProfileKey;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.FileContentDelegate;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

/**
 * This implementation handles deploying stuff using the standard JBoss deployment APIs.
 * 
 * @author Lukas Krejci
 */
public class ManagedComponentDeployer implements Deployer {
    private static final Log LOG = LogFactory.getLog(ManagedComponentDeployer.class);

    private static final ProfileKey FARM_PROFILE_KEY = new ProfileKey("farm");
    private static final ProfileKey APPLICATIONS_PROFILE_KEY = new ProfileKey("applications");

    private PackageDownloader downloader;
    private ProfileServiceConnection profileServiceConnection;
    private ResourceContext<?> parentResourceContext;

    public ManagedComponentDeployer(ProfileServiceConnection profileServiceConnection, PackageDownloader downloader,
        ResourceContext<?> parentResourceContext) {
        this.downloader = downloader;
        this.profileServiceConnection = profileServiceConnection;
        this.parentResourceContext = parentResourceContext;
    }

    public void deploy(CreateResourceReport createResourceReport, ResourceType resourceType) {
        createResourceReport.setStatus(null);
        File archiveFile = null;

        try {
            ResourcePackageDetails details = createResourceReport.getPackageDetails();
            PackageDetailsKey key = details.getKey();

            archiveFile = downloader.prepareArchive(key, resourceType);

            String archiveName = key.getName();

            if (!DeploymentUtils.hasCorrectExtension(archiveName, resourceType)) {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage("Incorrect extension specified on filename [" + archiveName + "]");
                return;
            }

            abortIfApplicationAlreadyDeployed(resourceType, archiveFile);

            Configuration deployTimeConfig = details.getDeploymentTimeConfiguration();
            boolean deployExploded = deployTimeConfig.getSimple("deployExploded").getBooleanValue();

            DeploymentManager deploymentManager = this.profileServiceConnection.getDeploymentManager();
            boolean deployFarmed = deployTimeConfig.getSimple("deployFarmed").getBooleanValue();
            if (deployFarmed) {
                Collection<ProfileKey> profileKeys = deploymentManager.getProfiles();
                boolean farmSupported = false;
                for (ProfileKey profileKey : profileKeys) {
                    if (profileKey.getName().equals(FARM_PROFILE_KEY.getName())) {
                        farmSupported = true;
                        break;
                    }
                }
                if (!farmSupported) {
                    throw new IllegalStateException("This application server instance is not a node in a cluster, "
                        + "so it does not support farmed deployments. Supported deployment profiles are " + profileKeys
                        + ".");
                }
                if (deployExploded) {
                    throw new IllegalArgumentException(
                        "Deploying farmed applications in exploded form is not supported by the Profile Service.");
                }
                deploymentManager.loadProfile(FARM_PROFILE_KEY);
            }

            String[] deploymentNames;
            try {
                deploymentNames = DeploymentUtils.deployArchive(deploymentManager, archiveFile, deployExploded);
            } finally {
                // Make sure to switch back to the 'applications' profile if we switched to the 'farm' profile above.
                if (deployFarmed) {
                    deploymentManager.loadProfile(APPLICATIONS_PROFILE_KEY);
                }
            }

            if (deploymentNames == null || deploymentNames.length != 1) {
                throw new RuntimeException("deploy operation returned invalid result: " + deploymentNames);
            }

            // e.g.: vfszip:/C:/opt/jboss-6.0.0.Final/server/default/deploy/foo.war
            String deploymentName = deploymentNames[0];

            // If deployed exploded, we need to store the SHA of source package in META-INF/MANIFEST.MF for correct
            // versioning.
            if (deployExploded) {
                URI deploymentURI = URI.create(deploymentName);
                // e.g.: /C:/opt/jboss-6.0.0.Final/server/default/deploy/foo.war
                String deploymentPath = deploymentURI.getPath();
                File deploymentFile = new File(deploymentPath);
                FileContentDelegate fileContentDelegate = new FileContentDelegate();

                fileContentDelegate.saveDeploymentSHA(archiveFile, deploymentFile,
                    parentResourceContext.getFutureChildResourceDataDirectory(archiveName));
            }

            // Reload the management view to pickup the ManagedDeployment for the app we just deployed.
            ManagementView managementView = this.profileServiceConnection.getManagementView();
            managementView.load();

            ManagedDeployment managedDeployment = null;
            try {
                managedDeployment = managementView.getDeployment(deploymentName);
            } catch (NoSuchDeploymentException e) {
                LOG.error("Failed to find managed deployment '" + deploymentName + "' after deploying '"
                        + archiveName + "', so cannot start the application.");
                createResourceReport.setStatus(CreateResourceStatus.INVALID_ARTIFACT);
                createResourceReport.setErrorMessage("Unable to start application '" + deploymentName
                        + "' after deploying it, since lookup of the associated ManagedDeployment failed.");
            }
            if (managedDeployment != null) {
                DeploymentState state = managedDeployment.getDeploymentState();
                if (state != DeploymentState.STARTED) {
                    // The app failed to start - do not consider this a FAILURE, since it was at least deployed
                    // successfully. However, set the status to INVALID_ARTIFACT and set an error message, so
                    // the user is informed of the condition.
                    createResourceReport.setStatus(CreateResourceStatus.INVALID_ARTIFACT);
                    createResourceReport.setErrorMessage("Failed to start application '" + deploymentName
                            + "' after deploying it.");
                }
            }

            createResourceReport.setResourceName(archiveName);
            createResourceReport.setResourceKey(archiveName);
            if (createResourceReport.getStatus() == null) {
                // Deployment was 100% successful, including starting the app.
                createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
            }
        } catch (Throwable t) {
            LOG.error("Error deploying application for request [" + createResourceReport + "].", t);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setException(t);
        } finally {
            if (archiveFile != null) {
                downloader.destroyArchive(archiveFile);
            }
        }
    }

    private void abortIfApplicationAlreadyDeployed(ResourceType resourceType, File archiveFile) throws Exception {
        String archiveFileName = archiveFile.getName();
        KnownDeploymentTypes deploymentType = ConversionUtils.getDeploymentType(resourceType);
        String deploymentTypeString = deploymentType.getType();
        ManagementView managementView = profileServiceConnection.getManagementView();
        managementView.load();
        Set<ManagedDeployment> managedDeployments = managementView.getDeploymentsForType(deploymentTypeString);
        for (ManagedDeployment managedDeployment : managedDeployments) {
            if (managedDeployment.getSimpleName().equals(archiveFileName))
                throw new IllegalArgumentException("An application named '" + archiveFileName
                    + "' is already deployed.");
        }
    }
}
