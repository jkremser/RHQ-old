/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.apache;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.OsProcessUtility;
import org.rhq.plugins.www.snmp.SNMPClient;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;

/**
 * The discovery component for Apache 1.3/2.x servers.
 *
 * @author Ian Springer
 */
public class ApacheServerDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {
    private static final String PRODUCT_DESCRIPTION = "Apache Web Server";

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        // Process any PC-discovered OS processes...
        List<ProcessScanResult> processes = discoveryContext.getAutoDiscoveredProcesses();
        for (ProcessScanResult process : processes) {
            //String executablePath = process.getProcessInfo().getName();
            String executableName = getExecutableName(process);
            File executablePath = OsProcessUtility.getProcExe(process.getProcessInfo().getPid(), executableName);
            if (executablePath == null) {
                log.error("Executable path could not be determined for Apache [" + process.getProcessInfo() + "].");
                continue;
            }
            if (!executablePath.isAbsolute()) {
                log.error("Executable path (" + executablePath + ") is not absolute for Apache [" +
                        process.getProcessInfo() + "]." +
                        "Please restart Apache specifying an absolute path for the executable.");
                continue;
            }
            log.debug("Apache executable path: " + executablePath);
            ApacheBinaryInfo binaryInfo;
            try {
                binaryInfo = ApacheBinaryInfo.getInfo(executablePath.getPath(),
                        discoveryContext.getSystemInformation());
            } catch (Exception e) {
                log.error("'" + executablePath + "' is not a valid Apache executable (" + e + ").");
                continue;
            }

            if (isSupportedVersion(binaryInfo.getVersion())) {
                String serverRoot = getServerRoot(binaryInfo, process.getProcessInfo());
                if (serverRoot == null) {
                    log.error("Unable to determine server root for Apache process: " + process.getProcessInfo());
                    continue;
                }

                File serverConfigFile = getServerConfigFile(binaryInfo, process.getProcessInfo(), serverRoot);
                if (serverConfigFile == null) {
                    log.error("Unable to determine server config file for Apache process: " + process.getProcessInfo());
                    continue;
                }

                Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();

                PropertySimple executablePathProp = new PropertySimple(
                    ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, executablePath);
                pluginConfig.put(executablePathProp);

                PropertySimple serverRootProp = new PropertySimple(
                    ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT, serverRoot);
                pluginConfig.put(serverRootProp);

                String url = getUrl(pluginConfig);
                Property urlProp = new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_URL, url);
                pluginConfig.put(urlProp);

                PropertySimple configFile = new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF,
                        serverConfigFile);
                pluginConfig.put(configFile);

                discoveredResources.add(createResourceDetails(discoveryContext, pluginConfig, process.getProcessInfo(),
                    binaryInfo));
            }
        }

        return discoveredResources;
    }



    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext discoveryContext)
            throws InvalidPluginConfigurationException {
        validateServerRootAndServerConfigFile(pluginConfig);

        String executablePath = pluginConfig
            .getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH,
                ApacheServerComponent.DEFAULT_EXECUTABLE_PATH);
        String absoluteExecutablePath = ApacheServerComponent.resolvePathRelativeToServerRoot(pluginConfig,
            executablePath).getPath();
        ApacheBinaryInfo binaryInfo;
        try {
            binaryInfo = ApacheBinaryInfo.getInfo(absoluteExecutablePath, discoveryContext.getSystemInformation());
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("'" + absoluteExecutablePath
                + "' is not a valid Apache executable (" + e + "). Please make sure the '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH
                + "' connection property is set correctly.");
        }

        if (!isSupportedVersion(binaryInfo.getVersion())) {
            throw new InvalidPluginConfigurationException("Version of Apache executable ("
                + binaryInfo.getVersion() + ") is not a supported version; supported versions are 1.3.x and 2.x.");
        }

        ProcessInfo processInfo = null;
        try {
            DiscoveredResourceDetails resourceDetails = createResourceDetails(discoveryContext, pluginConfig,
                processInfo, binaryInfo);
            return resourceDetails;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create resource details during manual add.");
        }
    }

    private boolean isSupportedVersion(String version) {
        // TODO: Compare against a version range defined in the plugin descriptor.
        return (version != null) && (version.startsWith("1.3") || version.startsWith("2."));
    }

    private DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfig, ProcessInfo processInfo, ApacheBinaryInfo binaryInfo) throws Exception {
        String httpdConf = pluginConfig.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF).getStringValue();
        String version = binaryInfo.getVersion();
        String hostname = discoveryContext.getSystemInformation().getHostname();
        String name = hostname + " Apache " + version + " (" + httpdConf + ")";

        DiscoveredResourceDetails resourceDetails = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
            httpdConf, name, version, PRODUCT_DESCRIPTION, pluginConfig, processInfo);
        log.debug("Apache Server resource details created: " + resourceDetails);
        return resourceDetails;
    }

    /**
     * Return the root URL of the first virtual host (i.e. the "main" Apache server). The URL's host and port is
     * determined by querying the Apache SNMP agent. The URL's protocol is assumed to be "http" and its path is assumed
     * to be "/". If the SNMP agent cannot be reached, null will be returned.
     *
     * @param  pluginConfig
     *
     * @return
     *
     * @throws Exception
     */
    @Nullable
    private static String getUrl(Configuration pluginConfig) throws Exception {
        SNMPClient snmpClient = new SNMPClient();
        try {
            SNMPSession snmpSession = ApacheServerComponent.getSNMPSession(snmpClient, pluginConfig);
            if (!snmpSession.ping()) {
                return null;
            }

            SNMPValue nameValue;
            SNMPValue portValue;
            try {
                nameValue = snmpSession.getNextValue(SNMPConstants.COLUMN_VHOST_NAME);
            } catch (SNMPException e) {
                throw new Exception("Error getting SNMP value: " + SNMPConstants.COLUMN_VHOST_NAME + ": "
                    + e.getMessage(), e);
            }

            try {
                portValue = snmpSession.getNextValue(SNMPConstants.COLUMN_VHOST_PORT);
            } catch (SNMPException e) {
                throw new Exception("Error getting SNMP column: " + SNMPConstants.COLUMN_VHOST_PORT + ": "
                    + e.getMessage(), e);
            }

            String host = nameValue.toString();
            String fullPort = portValue.toString();

            // The port value will be in the form "1.3.6.1.2.1.6.XXXXX",
            // where "1.3.6.1.2.1.6" represents the TCP protocol ID,
            // and XXXXX is the actual port number
            int port = Integer.parseInt(fullPort.substring(fullPort.lastIndexOf(".") + 1));

            return "http://" + host + ":" + port + "/";
        } finally {
            snmpClient.close();
        }
    }

    @Nullable
    private String getServerRoot(@NotNull ApacheBinaryInfo binaryInfo, @NotNull ProcessInfo processInfo) {
        String[] cmdLine = processInfo.getCommandLine();
        String root = getCommandLineOption(cmdLine, "-d");

        if (root == null) {
            root = binaryInfo.getRoot();
        }

        if (root != null) {
            root = FileUtils.getCanonicalPath(root);
        }

        return root;
    }

    @Nullable
    private File getServerConfigFile(ApacheBinaryInfo binaryInfo, ProcessInfo processInfo, String serverRoot) {
        String[] cmdLine = processInfo.getCommandLine();
        // First see if -f was specified on the httpd command line.
        String serverConfigFile = getCommandLineOption(cmdLine, "-f");

        // If not, extract the path from the httpd binary.
        if (serverConfigFile == null) {
            serverConfigFile = binaryInfo.getCtl();
        }

        if (serverConfigFile == null) {
            // We have failed to determine the config file path  :(
            return null;
        }

        // If the path is relative, convert it to an absolute path, resolving it relative to the server root dir.
        File file = new File(serverConfigFile);
        if (!file.isAbsolute()) {
            file = new File(serverRoot, serverConfigFile);
            serverConfigFile = file.getPath();
        }

        // And now canonicalize the path, but using our own getCanonicalPath() method, which preserves symlinks.
        serverConfigFile = FileUtils.getCanonicalPath(serverConfigFile);

        return new File(serverConfigFile);
    }

    private String getCommandLineOption(String[] cmdLine, String option) {
        String root = null;
        for (int i = 1; i < cmdLine.length; i++) {
            String arg = cmdLine[i];
            if (arg.startsWith(option)) {
                root = arg.substring(2, arg.length());
                if (root.length() == 0) {
                    root = cmdLine[i + 1];
                }
                break;
            }
        }
        return root;
    }

    private static String getExecutableName(ProcessScanResult processScanResult) {
        String query = processScanResult.getProcessScan().getQuery().toLowerCase();
        String executableName;
        if (query.contains("apache.exe")) {
            executableName = "apache.exe";
        } else if (query.contains("httpd.exe")) {
            executableName = "httpd.exe";
        } else if (query.contains("apache2")) {
            executableName = "apache2";
        } else if (query.contains("httpd")) {
            executableName = "httpd";
        } else {
            executableName = null;
        }
        return executableName;
    }

    private static void validateServerRootAndServerConfigFile(Configuration pluginConfig) {
        String serverRoot = pluginConfig.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT).getStringValue();
        File serverRootFile;
        try {
            serverRootFile = new File(serverRoot).getCanonicalFile(); // this will resolve symlinks
        }
        catch (IOException e) {
            serverRootFile = null;
        }
        if (serverRootFile == null || !serverRootFile.isDirectory()) {
            throw new InvalidPluginConfigurationException("'" + serverRoot
                + "' does not exist or is not a directory. Please make sure the '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT + "' connection property is set correctly.");
        }
        String httpdConf = pluginConfig.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF).getStringValue();
        File httpdConfFile;
        try {
            httpdConfFile = new File(httpdConf).getCanonicalFile(); // this will resolve symlinks
        }
        catch (IOException e) {
            httpdConfFile = null;
        }
        if (httpdConfFile == null || !httpdConfFile.isFile()) {
            throw new InvalidPluginConfigurationException("'" + httpdConf
                + "' does not exist or is not a regular file. Please make sure the '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF + "' connection property is set correctly.");
        }
    }
}