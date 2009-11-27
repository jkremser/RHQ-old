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
package org.rhq.enterprise.gui.admin.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.event.UploadEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 */
public class InstalledPluginsUIBean {
    private final Log log = LogFactory.getLog(InstalledPluginsUIBean.class);

    public static final String MANAGED_BEAN_NAME = InstalledPluginsUIBean.class.getSimpleName();

    private ResourceMetadataManagerLocal resourceMetadataManagerBean = LookupUtil.getResourceMetadataManager();
    private ServerPluginsLocal serverPluginsBean = LookupUtil.getServerPlugins();

    public InstalledPluginsUIBean() {
    }

    public Collection<Plugin> getInstalledAgentPlugins() {

        hasPermission();
        return resourceMetadataManagerBean.getPlugins();
    }

    public Collection<Plugin> getInstalledServerPlugins() {

        hasPermission();
        return serverPluginsBean.getServerPlugins();
    }

    public void scan() {
        hasPermission();

        try {
            PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();
            scanner.scanAndRegister();
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Done scanning for updated plugins.");
        } catch (Exception e) {
            processException("Failed to scan for updated plugins", e);
        }
    }

    public void fileUploadListener(UploadEvent event) {
        hasPermission();

        try {
            File uploadedPlugin = event.getUploadItem().getFile();
            String newPluginFilename = event.getUploadItem().getFileName();

            // some browsers (IE in particular) passes an absolute filename, we just want the name of the file, no paths
            if (newPluginFilename != null) {
                newPluginFilename = newPluginFilename.replace('\\', '/');
                if (newPluginFilename.length() > 2 && newPluginFilename.charAt(1) == ':') {
                    newPluginFilename = newPluginFilename.substring(2);
                }
                newPluginFilename = new File(newPluginFilename).getName();
            }

            log.info("A new plugin [" + newPluginFilename + "] has been uploaded to [" + uploadedPlugin + "]");

            if (uploadedPlugin == null || !uploadedPlugin.exists()) {
                throw new FileNotFoundException("The uploaded plugin file [" + uploadedPlugin + "] does not exist!");
            }

            // put the new plugin file in our plugin dropbox location
            File installDir = LookupUtil.getCoreServer().getInstallDir();
            File dir = new File(installDir, "plugins");
            File pluginFile = new File(dir, newPluginFilename);
            FileOutputStream fos = new FileOutputStream(pluginFile);
            FileInputStream fis = new FileInputStream(uploadedPlugin);
            StreamUtil.copy(fis, fos);
            log.info("A new plugin has been deployed [" + pluginFile
                + "]. A scan is required now in order to register it.");
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "New plugin uploaded: " + newPluginFilename);
        } catch (Exception e) {
            processException("Failed to process uploaded plugin", e);
        }

        return;
    }

    public void enableServerPlugins() {
        List<Plugin> selectedPlugins = getSelectedServerPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        for (Plugin selectedPlugin : selectedPlugins) {
            selectedPluginNames.add(selectedPlugin.getName());
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPluginsBean.enableServerPlugins(subject, getIds(selectedPlugins));
            FacesContextUtility
                .addMessage(FacesMessage.SEVERITY_INFO, "Enabled server plugins: " + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to enable server plugins", e);
        }
        return;
    }

    public void disableServerPlugins() {
        List<Plugin> selectedPlugins = getSelectedServerPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        for (Plugin selectedPlugin : selectedPlugins) {
            selectedPluginNames.add(selectedPlugin.getName());
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPluginsBean.disableServerPlugins(subject, getIds(selectedPlugins));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled server plugins: "
                + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to disable server plugins", e);
        }
        return;
    }

    public void undeployServerPlugins() {
        List<Plugin> selectedPlugins = getSelectedServerPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        for (Plugin selectedPlugin : selectedPlugins) {
            selectedPluginNames.add(selectedPlugin.getName());
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPluginsBean.undeployServerPlugins(subject, getIds(selectedPlugins));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Undeployed server plugins: "
                + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to undeploy server plugins", e);
        }
        return;
    }

    private List<Integer> getIds(List<Plugin> plugins) {
        ArrayList<Integer> ids = new ArrayList<Integer>(plugins.size());
        for (Plugin plugin : plugins) {
            ids.add(plugin.getId());
        }
        return ids;
    }

    private List<Plugin> getSelectedServerPlugins() {
        Integer[] integerItems = getSelectedPluginIds();
        List<Integer> ids = Arrays.asList(integerItems);
        List<Plugin> plugins = serverPluginsBean.getServerPluginsById(ids);
        return plugins;
    }

    private Integer[] getSelectedPluginIds() {
        String[] stringItems = FacesContextUtility.getRequest().getParameterValues("selectedPlugin");
        if (stringItems == null || stringItems.length == 0) {
            return new Integer[0];
        }
        Integer[] integerItems = StringUtility.getIntegerArray(stringItems);
        return integerItems;
    }

    /**
     * Throws a permission exception if the user is not allowed to access this functionality. 
     */
    private void hasPermission() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have the proper permissions to view or manage plugins");
        }
    }

    private void processException(String errMsg, Exception e) {
        log.error(errMsg + ". Cause: " + ThrowableUtil.getAllMessages(e));
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, errMsg, e);
    }
}