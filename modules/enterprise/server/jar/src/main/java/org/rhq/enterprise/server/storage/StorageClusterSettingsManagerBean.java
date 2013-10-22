package org.rhq.enterprise.server.storage;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
@Stateless
public class StorageClusterSettingsManagerBean implements StorageClusterSettingsManagerLocal {

    private static final String UPDATE_PASSWORD_QUERY = "ALTER USER '%s' WITH PASSWORD '%s'";

    @EJB
    private SystemManagerLocal systemManager;

    @EJB
    private StorageClientManagerBean storageClienManager;

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageClusterSettings getClusterSettings(Subject subject) {
        SystemSettings settings = systemManager.getUnmaskedSystemSettings(true);
        StorageClusterSettings clusterSettings = new StorageClusterSettings();

        if (!settings.containsKey(SystemSetting.STORAGE_CQL_PORT)) {
            return null;
        } else {
            clusterSettings.setCqlPort(Integer.parseInt(settings.get(
                SystemSetting.STORAGE_CQL_PORT)));
        }

        if (!settings.containsKey(SystemSetting.STORAGE_GOSSIP_PORT)) {
            return null;
        } else {
            clusterSettings.setGossipPort(Integer.parseInt(settings.get(
                SystemSetting.STORAGE_GOSSIP_PORT)));
        }
        
        if (!settings.containsKey(SystemSetting.STORAGE_AUTOMATIC_DEPLOYMENT)) {
            return null;
        } else {
            clusterSettings.setAutomaticDeployment(Boolean.parseBoolean(settings
                .get(SystemSetting.STORAGE_AUTOMATIC_DEPLOYMENT)));
        }
        
        if (!settings.containsKey(SystemSetting.STORAGE_USERNAME)) {
            return null;
        } else {
            clusterSettings.setUsername(settings.get(SystemSetting.STORAGE_USERNAME));
        }
        
        if (!settings.containsKey(SystemSetting.STORAGE_PASSWORD)) {
            return null;
        } else {
            clusterSettings.setPasswordHash(settings.get(SystemSetting.STORAGE_PASSWORD));
        }

        return clusterSettings;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void setClusterSettings(Subject subject, StorageClusterSettings clusterSettings) {
        SystemSettings settings = new SystemSettings();
        settings.put(SystemSetting.STORAGE_CQL_PORT, Integer.toString(clusterSettings.getCqlPort()));
        settings.put(SystemSetting.STORAGE_GOSSIP_PORT, Integer.toString(clusterSettings.getGossipPort()));
        if (clusterSettings.getAutomaticDeployment() != null) {
            settings.put(SystemSetting.STORAGE_AUTOMATIC_DEPLOYMENT,
                Boolean.toString(clusterSettings.getAutomaticDeployment()));
        }
        if (clusterSettings.getUsername() != null) { 
            settings.put(SystemSetting.STORAGE_USERNAME, clusterSettings.getUsername());
        }
        if (clusterSettings.getPasswordHash() != null) {
            this.updateStorageClusterCredentials(clusterSettings);
            settings.put(SystemSetting.STORAGE_PASSWORD, clusterSettings.getPasswordHash());
        }
        systemManager.setStorageClusterSettings(subject, settings);
    }

    private void updateStorageClusterCredentials(StorageClusterSettings newClusterSettings) {
        SystemSettings currentSettings = systemManager.getUnmaskedSystemSettings(true);
        String currentPassword = currentSettings.get(SystemSetting.STORAGE_PASSWORD);

        if (!currentPassword.equals(newClusterSettings.getPasswordHash())) {
            StorageSession session = this.storageClienManager.getSession();
            session.execute(String.format(UPDATE_PASSWORD_QUERY, currentSettings.get(SystemSetting.STORAGE_USERNAME),
                newClusterSettings.getPasswordHash()));
        }
    }
}
