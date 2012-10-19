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
package org.rhq.plugins.samba;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.*;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;


/**
 * TODO
 */
public class SambaServerComponent extends AugeasConfigurationComponent {
    static final String ENABLE_RECYCLING = "enableRecycleBin";
    static final String AUTHCONFIG_PATH = "/usr/bin/authconfig";
    static final String NET_PATH = "/usr/bin/net";
    static final String SPACE = " ";

    private ResourceContext resourceContext;


    public void start(ResourceContext resourceContext) throws Exception {
        this.resourceContext = resourceContext;
        super.start(resourceContext);
        updateSmbAds(resourceContext);
    }

    public void stop() {
        super.stop();
    }

    public AvailabilityType getAvailability() {
        return super.getAvailability();
    }

    public Configuration loadResourceConfiguration() throws Exception {
        return super.loadResourceConfiguration();
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport reportIn) {
        CreateResourceReport report = reportIn;
        Configuration config = report.getResourceConfiguration();
        String name = config.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP).getStringValue();
        report.setResourceKey(name);
        report.setResourceName(name);
        return super.createResource(report);
    }

    @Override
    protected String getChildResourceConfigurationRootPath(ResourceType resourceType, Configuration resourceConfig) {
        if (resourceType.getName().equals(SambaShareComponent.RESOURCE_TYPE_NAME)) {
            String targetName = resourceConfig.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP)
                .getStringValue();
            return "/files/etc/samba/smb.conf/target[.='" + targetName + "']";
        } else {
            throw new IllegalArgumentException("Unsupported child Resource type: " + resourceType);
        }
    }

    @Override
    protected String getChildResourceConfigurationRootLabel(ResourceType resourceType, Configuration resourceConfig) {
        if (resourceType.getName().equals(SambaShareComponent.RESOURCE_TYPE_NAME)) {
            return resourceConfig.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP).getStringValue();
        } else {
            throw new IllegalArgumentException("Unsupported child Resource type: " + resourceType);
        }
    }

    @Override
    protected void setNodeFromPropertySimple(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        if (ENABLE_RECYCLING.equals(propDefSimple.getName())) {
            if (propSimple.getBooleanValue()) {
                String path = node.getParent().getPath();
                augeas.set(path + "/vfs\\ objects", "recycle");
                augeas.set(path + "/recycle:repository", ".recycle");
                augeas.set(path + "/recycle:keeptree", "yes");
                augeas.set(path + "/recycle:versions", "yes");
            } else {
                String path = node.getParent().getPath();
                augeas.remove(path + "/vfs\\ objects");
                augeas.remove(path + "/recycle:repository");
                augeas.remove(path + "/recycle:keeptree");
                augeas.remove(path + "/recycle:versions");
            }
        } else {
            super.setNodeFromPropertySimple(augeas, node, propDefSimple, propSimple);
        }
    }

    protected Object toPropertyValue(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if (ENABLE_RECYCLING.equals(propDefSimple.getName())) {
            return "recycle".equals(augeas.get(node.getParent().getPath() + "/vfs\\ objects"));
        }
        return super.toPropertyValue(propDefSimple, augeas, node);
    }

    private void updateSmbAds(ResourceContext resourceContext) throws Exception {

        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        Configuration resourceConfig = loadResourceConfiguration();

        String realm = pluginConfig.getSimple("realm").getStringValue();
        String controller = pluginConfig.getSimple("controller").getStringValue();
        String username = pluginConfig.getSimple("username").getStringValue();
        String password = pluginConfig.getSimple("password").getStringValue();

        String workgroup = resourceConfig.getSimple("workgroup").getStringValue();
        String idmapuid = resourceConfig.getSimple("idmap uid").getStringValue();
        String idmapgid = resourceConfig.getSimple("idmap gid").getStringValue();
        String shell = resourceConfig.getSimple("template shell").getStringValue();

        if (realm == null || controller == null || username == null || password == null || workgroup == null) {
            // no point in doing anything
            return;
        }

        StringBuilder authArgs = new StringBuilder();
        StringBuilder netArgs = new StringBuilder();

        // AuthConfig arguments...ugly and would be a lot prettier using python
        authArgs.append("--smbservers=" + controller);
        authArgs.append(SPACE + "--smbrealm=" + realm);
        authArgs.append(SPACE + "--enablewinbind --smbsecurity=ads");

        if (idmapuid != null) {
            authArgs.append(SPACE + "--smbidmapuid=" + idmapuid);
        }

        if (idmapgid != null) {
            authArgs.append(SPACE + "--smbidmapgid=" + idmapgid);
        }

        if (shell != null) {
            authArgs.append(SPACE + "--winbindtemplateshell=" + shell);
        }

        authArgs.append(SPACE + "--update");

        // Net join arguments
        netArgs.append("join");
        netArgs.append(SPACE + "-w " + workgroup);
        netArgs.append(SPACE + "-S " + controller);
        //TODO is there more secure way to do this.
        netArgs.append(SPACE + "-U " + username + "%" + password);

        execute(AUTHCONFIG_PATH, authArgs.toString());
        execute(NET_PATH, netArgs.toString());
    }

    private ProcessExecutionResults execute(String path, String args) throws InvalidPluginConfigurationException {

        ProcessExecution processExecution = new ProcessExecution(path);
        SystemInfo sysInfo = this.resourceContext.getSystemInformation();

        if (args != null) {
            processExecution.setArguments(args.split(" "));
        }
        
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(1000L);
        processExecution.setKillOnTimeout(true);

        ProcessExecutionResults results = sysInfo.executeProcess(processExecution);

        return results;
   }

}
