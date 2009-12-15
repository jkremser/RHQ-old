/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.altlang;

import static org.testng.Assert.*;

import org.apache.commons.io.FileUtils;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.PluginFinder;
import org.rhq.core.pc.plugin.PluginManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AltLangComponentTest {

    private static final File ITEST_DIR = new File("target/itest");

    private static final String ALT_LANG_TEST_SERVER = "GroovyServer";

    private static final String M2_REPO_DIR = System.getProperty("user.home") + "/.m2/repository";

    private Resource testServer;

    @BeforeSuite
    public void initSuite() throws Exception {
        cleanTestDir();
        startPluginContainer();
    }

    private void startPluginContainer() throws Exception {
        PluginContainerConfiguration pcContainerConfig = createPluginContainerConfiguration();

        PluginContainer pluginContainer = PluginContainer.getInstance();
        pluginContainer.setConfiguration(pcContainerConfig);
        pluginContainer.initialize();

        Set<String> pluginNames = pluginContainer.getPluginManager().getMetadataManager().getPluginNames();
        System.out.println("Plugin container started with the following plugins: " + pluginNames);
    }

    private PluginContainerConfiguration createPluginContainerConfiguration()
            throws IOException {
        PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();

        File pluginsDir = new File(ITEST_DIR, "plugins");
        pluginsDir.mkdirs();

        pcConfig.setPluginFinder(new M2RepoPluginFinder());
        pcConfig.setPluginDirectory(pluginsDir);
        pcConfig.setInsideAgent(false);
        pcConfig.setCreateResourceClassloaders(true);

        File tmpDir = new File(ITEST_DIR, "tmp");
        tmpDir.mkdirs();
        if (!tmpDir.isDirectory() || !tmpDir.canWrite()) {
            throw new IOException("Failed to create temporary directory (" + tmpDir + ").");
        }
        pcConfig.setTemporaryDirectory(tmpDir);
        return pcConfig;
    }

    private void cleanTestDir() throws IOException {
        File testDir = getTestDir();
        FileUtils.deleteDirectory(testDir);
        boolean created = testDir.mkdir();

        assertTrue(created, "Failed to recreate " + testDir.getName());
    }

    @Test
    public void verifyDiscovery() throws Exception {
        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        inventoryMgr.executeServerScanImmediately();

        ResourceType resourceType = getResourceType(ALT_LANG_TEST_SERVER, "AltLangTest");
        Set<Resource> resources = inventoryMgr.getResourcesWithType(resourceType);

        for (Resource resource : resources) {
            if (resource.getName().equals(ALT_LANG_TEST_SERVER)) {
                testServer = resource;
            }
        }
        assertNotNull(testServer, "Failed to discover " + ALT_LANG_TEST_SERVER);
    }

    @Test(dependsOnMethods = {"verifyDiscovery"})
    public void verifyResourceComponentStarted() throws Exception {
        File testDir = getTestDir();
        File startFile = new File(testDir, ALT_LANG_TEST_SERVER + ".start");

        assertTrue(startFile.exists(), "Resource component script may not have been called. Failed to find " +
            startFile.getName() + " which should have been generated by the script.");
    }

    @Test(dependsOnMethods = {"verifyResourceComponentStarted"})
    public void scriptShouldBeCalledToCheckResourceAvailability() throws Exception {
        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        Availability availability = inventoryMgr.getCurrentAvailability(testServer);

        assertEquals(
            availability.getAvailabilityType(),
            AvailabilityType.UP,
            "Resource component script may not have been called. Expected resource to be available."
        );
    }

    @AfterClass
    public void scriptShouldBeCalledToStopResourceComponent() throws Exception {
        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        inventoryMgr.removeResource(testServer.getId());

        File testDir = getTestDir();
        File stopFile = new File(testDir, ALT_LANG_TEST_SERVER + ".stop");

        assertTrue(stopFile.exists(), "Resource component script may not have been called. Failed to find " +
            stopFile.getName() + " which should have been generated by the script");
    }

    ResourceType getResourceType(String resourceTypeName, String pluginName) {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();
        return pluginMetadataManager.getType(resourceTypeName, pluginName);
    }

    private File getTestDir() {
        return new File(System.getProperty("java.io.tmpdir"), "altlang");
    }

    static class M2RepoPluginFinder implements PluginFinder {
        String version = "1.4.0-SNAPSHOT";


        public Collection<URL> findPlugins() {
            List<URL> pluginURLs = new ArrayList<URL>();
            pluginURLs.add(toURL("org/rhq/rhq-platform-plugin/" + version + "/rhq-platform-plugin-" + version + ".jar"));
            pluginURLs.add(toURL("org/rhq/plugins/altlang/rhq-alt-lang-plugin/" + version + "/rhq-alt-lang-plugin-" +
                version + ".jar"));
            pluginURLs.add(toURL("org/rhq/plugins/altlang/rhq-alt-lang-test-plugin/" + version +
                "/rhq-alt-lang-test-plugin-" + version + ".jar"));

            return pluginURLs;
        }

        URL toURL(String path) {
            try {
                File file = new File(M2_REPO_DIR, path);
                if (!file.exists()) {
                    throw new RuntimeException(file.getAbsolutePath() + " does not exist");
                }
                return new File(M2_REPO_DIR, path).toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
