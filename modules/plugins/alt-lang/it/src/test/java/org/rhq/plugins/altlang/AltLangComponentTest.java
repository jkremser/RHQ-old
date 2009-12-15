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

import org.apache.commons.io.FileUtils;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.PluginFinder;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public abstract class AltLangComponentTest {
    private static final File ITEST_DIR = new File("target/itest");
    private static final String M2_REPO_DIR = System.getProperty("user.home") + "/.m2/repository";

    @BeforeSuite
    public static void initSuite() throws Exception {
        cleanTestDir();
        startPluginContainer();
    }

    private static void startPluginContainer() throws Exception {
        PluginContainerConfiguration pcContainerConfig = createPluginContainerConfiguration();

        PluginContainer pluginContainer = PluginContainer.getInstance();
        pluginContainer.setConfiguration(pcContainerConfig);
        pluginContainer.initialize();

        Set<String> pluginNames = pluginContainer.getPluginManager().getMetadataManager().getPluginNames();
        System.out.println("Plugin container started with the following plugins: " + pluginNames);
    }

    private static PluginContainerConfiguration createPluginContainerConfiguration()
            throws IOException {
        PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();

        File pluginsDir = new File(ITEST_DIR, "plugins");
        pluginsDir.mkdirs();

        pcConfig.setPluginFinder(new GroovyComponentTest.M2RepoPluginFinder());
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

    private static void cleanTestDir() throws IOException {
        File testDir = getTestDir();
        FileUtils.deleteDirectory(testDir);
        boolean created = testDir.mkdir();

        assertTrue(created, "Failed to recreate " + testDir.getName());
    }

    protected static File getTestDir() {
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
