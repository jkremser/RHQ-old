/*
 * Jopr Management Platform
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
package org.rhq.plugins.jbossas.helper;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * A class that aggregates various static information about a JBoss AS/EAP/SOA-P installation.
 *
 * @author Ian Springer
 * @author Jessica Sant
 */
public class JBossInstallationInfo {
    private static final String ANY_ADDRESS = "0.0.0.0";
    private static final String LOCALHOST_ADDRESS = "127.0.0.1";
    private static final String EPP_IMPL_VERSION_PREFIX = "JBoss-EPP";
    private static final String SOA_IMPL_VERSION_PREFIX = "SOA-";
    private static final ComparableVersion VERSION_4_2 = new ComparableVersion("4.2");

    private JBossProductType productType;
    private String version;
    private String defaultBindAddress;
    private final String majorVersion;

    public JBossInstallationInfo(File installationDir) throws IOException {
        File binDir = new File(installationDir, "bin");
        File runJar = new File(binDir, "run.jar");
        if (!runJar.exists()) {
            throw new RuntimeException(runJar + " does not exist - " + installationDir
                + " does not appear to be a JBoss installation/home directory.");
        }
        Attributes jarManifestAttributes = loadManifestAttributesFromJar(runJar);
        this.productType = JBossProductType.determineJBossProductType(jarManifestAttributes);
        this.version = getVersion(jarManifestAttributes);
        int majorVersionIndex = version.indexOf('.');
        if (-1 == majorVersionIndex) {
            throw new RuntimeException("Unexpected run.jar implementation version, can't parse: " + this.version);
        }
        this.defaultBindAddress = getDefaultServerName(this.version);
        this.majorVersion = version.substring(0, majorVersionIndex);
    }

    public JBossProductType getProductType() {
        return this.productType;
    }

    /**
     * Returns the version of this JBoss installation. AS versions 4.0.4 or later will be OSGi-style (e.g. 4.0.4.GA);
     * earlier versions will not (e.g. 4.0.1sp1, 4.0.2).
     *
     * @return the version of this JBoss installation
     */
    public String getVersion() {
        return this.version;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    public String getDefaultBindAddress() {
        return this.defaultBindAddress;
    }

    /**
     * Loads the top-level attributes from the manifest file of the given jar file.
     *
     * @param jarFile the jar file
     * @return the top-level attributes from the manifest file
     * @throws IOException on failure to read the jar file
     */
    private static Attributes loadManifestAttributesFromJar(File jarFile) throws IOException {
        JarFile jar = new JarFile(jarFile);
        Attributes mainAttributes = jar.getManifest().getMainAttributes();
        jar.close();
        return mainAttributes;
    }

    private static String getVersion(Attributes jarManifestAttributes) {
        String implementationVersion = jarManifestAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        // e.g. Implementation-Version: 4.2.2.GA (build: SVNTag=JBoss_4_2_2_GA date=200710221139)
        if (implementationVersion == null) {
            throw new IllegalStateException("'" + Attributes.Name.IMPLEMENTATION_VERSION
                + "' MANIFEST.MF attribute not found.");
        }
        if (implementationVersion.startsWith(EPP_IMPL_VERSION_PREFIX)) {
            implementationVersion = implementationVersion.substring(EPP_IMPL_VERSION_PREFIX.length()).trim();
        }

        int spaceIndex = implementationVersion.indexOf(' ');
        if (spaceIndex == -1) {
            throw new IllegalStateException("'" + Attributes.Name.IMPLEMENTATION_VERSION
                + "' MANIFEST.MF attribute has an invalid value: " + implementationVersion);
        }

        String version = implementationVersion.substring(0, spaceIndex);
        if (version.startsWith(SOA_IMPL_VERSION_PREFIX)) {
            version = version.substring(SOA_IMPL_VERSION_PREFIX.length());
        }

        return version;
    }

    private static String getDefaultServerName(String serverVersion) {
        ComparableVersion comparableVersion = new ComparableVersion(serverVersion);
        return (comparableVersion.compareTo(VERSION_4_2) >= 0) ? ANY_ADDRESS : LOCALHOST_ADDRESS;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[productType=" + this.productType + ", version=" + this.version
            + ", defaultBindAddress=" + this.defaultBindAddress + "]";
    }
}
