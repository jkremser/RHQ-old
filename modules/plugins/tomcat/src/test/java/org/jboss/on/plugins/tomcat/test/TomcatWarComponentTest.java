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
package org.jboss.on.plugins.tomcat.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.on.plugins.tomcat.TomcatVHostComponent;
import org.jboss.on.plugins.tomcat.TomcatWarComponent;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;

public class TomcatWarComponentTest {

    @Test
    public void testDiscoverZippedDeployment() throws Exception {
        //create the object under test as a partial mock because only one 
        //public method will be tested, while the rest will be mocked.
        TomcatWarComponent objectUnderTest = mock(TomcatWarComponent.class);

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        File fileUsedInTest = new File(this.getClass().getResource("/sampleWithManifest.war").getFile());

        @SuppressWarnings("unchecked")
        ResourceContext<TomcatVHostComponent> mockResourceContext = mock(ResourceContext.class);
        when(objectUnderTest.getResourceContext()).thenReturn(mockResourceContext);
        Configuration mockConfiguration = mock(Configuration.class);
        when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);
        when(mockConfiguration.getSimpleValue(eq("filename"), isNull(String.class))).thenReturn(
            fileUsedInTest.getAbsolutePath());

        PackageType mockPackageType = mock(PackageType.class);

        when(objectUnderTest.discoverDeployedPackages(any(PackageType.class))).thenCallRealMethod();

        //run code under test
        Set<ResourcePackageDetails> actualResult = objectUnderTest.discoverDeployedPackages(mockPackageType);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(actualResult.size(), 1);

        ResourcePackageDetails actualResourcePackageDetails = (ResourcePackageDetails) actualResult.toArray()[0];
        Assert.assertEquals(actualResourcePackageDetails.getFileName(), fileUsedInTest.getName());
        Assert.assertEquals(actualResourcePackageDetails.getLocation(), fileUsedInTest.getPath());
        Assert.assertEquals((long) actualResourcePackageDetails.getFileSize(), fileUsedInTest.length());
        if (actualResourcePackageDetails.getInstallationTimestamp() > System.currentTimeMillis()) {
            Assert.fail("Timestamp is not in the past.");
        }

        MessageDigestGenerator digest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        String expectedSha256 = digest.calcDigestString(fileUsedInTest);

        Assert.assertEquals(actualResourcePackageDetails.getSHA256(), expectedSha256);
        Assert.assertEquals(actualResourcePackageDetails.getDisplayVersion(), null);

        verify(mockResourceContext, times(1)).getPluginConfiguration();
        verify(objectUnderTest, times(1)).getResourceContext();
        verify(mockConfiguration, times(1)).getSimpleValue(eq("filename"), isNull(String.class));
        verifyNoMoreInteractions(mockPackageType);
    }

    @Test
    public void testDiscoverExplodedDeployments() throws Exception {

        //Presetup resources for the test
        String[] testArchiveFiles = new String[] { "/sampleWithManifest.war", "/sampleWithoutManifest.war",
            "/sampleWithImplementation.war", "/sampleWithSpecification.war",
            "/sampleWithSpecificationImplementation.war" };

        Map<String, String> expectedSha256 = new HashMap<String, String>();
        expectedSha256.put(testArchiveFiles[0], "a30a576b5ee15c709469517e9f529989f9ae85bddde131c014a799ca9d7c1c0f");
        expectedSha256.put(testArchiveFiles[1], "bff7f7d63ae8e4f1efebb54fa727effe1b1a8246492ad9c36779d79a9771fb2b");
        expectedSha256.put(testArchiveFiles[2], "eebb2c1c171af85fff62ae290206bb792c3d4c5becf58fa677b156f808cf3fe5");
        expectedSha256.put(testArchiveFiles[3], "3b2bf1cc3981413454ab4c62b9cad58e81609c3271d39aacbbfd8814585f9d65");
        expectedSha256.put(testArchiveFiles[4], "98c945496e198f75f2653ccf2b9fd9d12c20cd26e2ad13146dcb53f8b8a567fc");

        Map<String, String> expectedDisplayVersion = new HashMap<String, String>();
        expectedDisplayVersion.put(testArchiveFiles[0], null);
        expectedDisplayVersion.put(testArchiveFiles[1], null);
        expectedDisplayVersion.put(testArchiveFiles[2], "9.99");
        expectedDisplayVersion.put(testArchiveFiles[3], "1.234");
        expectedDisplayVersion.put(testArchiveFiles[4], "1.234 (9.990)");

        for (String availableArchiveFile : testArchiveFiles) {
            //create the object under test as a partial mock because only one 
            //public method will be tested, while the rest will be mocked.
            TomcatWarComponent objectUnderTest = mock(TomcatWarComponent.class);

            //tell the method story as it happens: mock dependencies and configure
            //those dependencies to get the method under test to completion.
            File archiveUsedInTest = new File(this.getClass().getResource(availableArchiveFile).getFile());
            File deploymentFolderUsedInTest = new File(this.getClass().getResource("/").getFile() + "deploymentFolder");
            ZipUtil.unzipFile(archiveUsedInTest, deploymentFolderUsedInTest);

            @SuppressWarnings("unchecked")
            ResourceContext<TomcatVHostComponent> mockResourceContext = mock(ResourceContext.class);
            when(objectUnderTest.getResourceContext()).thenReturn(mockResourceContext);
            Configuration mockConfiguration = mock(Configuration.class);
            when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);
            when(mockConfiguration.getSimpleValue(eq("filename"), isNull(String.class))).thenReturn(
                deploymentFolderUsedInTest.getAbsolutePath());

            PackageType mockPackageType = mock(PackageType.class);

            when(objectUnderTest.discoverDeployedPackages(any(PackageType.class))).thenCallRealMethod();

            //run code under test
            Set<ResourcePackageDetails> actualResult = objectUnderTest.discoverDeployedPackages(mockPackageType);

            //verify the results (Assert and mock verification)
            Assert.assertEquals(actualResult.size(), 1);

            ResourcePackageDetails actualResourcePackageDetails = (ResourcePackageDetails) actualResult.toArray()[0];
            Assert.assertEquals(actualResourcePackageDetails.getFileName(), deploymentFolderUsedInTest.getName());
            Assert.assertEquals(actualResourcePackageDetails.getLocation(), deploymentFolderUsedInTest.getPath());
            Assert.assertEquals(actualResourcePackageDetails.getFileSize(), null);
            if (actualResourcePackageDetails.getInstallationTimestamp() > System.currentTimeMillis()) {
                Assert.fail("Timestamp is not in the past.");
            }

            String expectedSha256ForDeployment = expectedSha256.get(availableArchiveFile);

            Assert.assertEquals(actualResourcePackageDetails.getSHA256(), expectedSha256.get(availableArchiveFile));
            Assert.assertEquals(actualResourcePackageDetails.getDisplayVersion(),
                expectedDisplayVersion.get(availableArchiveFile));

            File manifestFile = new File(deploymentFolderUsedInTest.getAbsolutePath() + "/META-INF/MANIFEST.MF");
            Assert.assertTrue(manifestFile.exists(), "Manifest file not created properly!");
            Assert.assertNotEquals(manifestFile.length(), 0, "Empty manifest!!");

            InputStream manifestStream = new FileInputStream(manifestFile);
            Manifest manifest = new Manifest(manifestStream);
            String actualSha256Attribute = manifest.getMainAttributes().getValue("RHQ-Sha256");
            manifestStream.close();

            Assert.assertEquals(actualSha256Attribute, expectedSha256ForDeployment);

            verify(mockResourceContext, times(1)).getPluginConfiguration();
            verify(objectUnderTest, times(1)).getResourceContext();
            verify(mockConfiguration, times(1)).getSimpleValue(eq("filename"), isNull(String.class));
            verifyNoMoreInteractions(mockPackageType);

            //cleanup resources created for this test
            deleteRecursive(deploymentFolderUsedInTest);
        }
    }

    @Test
    public void testDiscoverNoFileOnDisk() throws Exception {
        //create the object under test as a partial mock because only one 
        //public method will be tested, while the rest will be mocked.
        TomcatWarComponent objectUnderTest = mock(TomcatWarComponent.class);

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        File fileUsedInTest = new File(this.getClass().getResource("/").getFile() + "randomNonExistingFile");

        @SuppressWarnings("unchecked")
        ResourceContext<TomcatVHostComponent> mockResourceContext = mock(ResourceContext.class);
        when(objectUnderTest.getResourceContext()).thenReturn(mockResourceContext);
        Configuration mockConfiguration = mock(Configuration.class);
        when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);
        when(mockConfiguration.getSimpleValue(eq("filename"), isNull(String.class))).thenReturn(
            fileUsedInTest.getAbsolutePath());

        PackageType mockPackageType = mock(PackageType.class);

        when(objectUnderTest.discoverDeployedPackages(any(PackageType.class))).thenCallRealMethod();

        //run code under test
        Set<ResourcePackageDetails> actualResult = objectUnderTest.discoverDeployedPackages(mockPackageType);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(actualResult.size(), 0);

        verify(mockResourceContext, times(1)).getPluginConfiguration();
        verify(objectUnderTest, times(1)).getResourceContext();
        verify(mockConfiguration, times(1)).getSimpleValue(eq("filename"), isNull(String.class));

        verifyNoMoreInteractions(mockPackageType);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testDiscoverIncorrectConfiguration() throws Exception {
        //create the object under test as a partial mock because only one 
        //public method will be tested, while the rest will be mocked.
        TomcatWarComponent objectUnderTest = mock(TomcatWarComponent.class);

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        @SuppressWarnings("unchecked")
        ResourceContext<TomcatVHostComponent> mockResourceContext = mock(ResourceContext.class);
        when(objectUnderTest.getResourceContext()).thenReturn(mockResourceContext);
        Configuration mockConfiguration = mock(Configuration.class);
        when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);
        when(mockConfiguration.getSimpleValue(eq("filename"), isNull(String.class))).thenReturn(null);

        PackageType mockPackageType = mock(PackageType.class);

        when(objectUnderTest.discoverDeployedPackages(any(PackageType.class))).thenCallRealMethod();

        //run code under test
        Set<ResourcePackageDetails> actualResult = objectUnderTest.discoverDeployedPackages(mockPackageType);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(actualResult.size(), 0);

        verify(mockResourceContext, times(1)).getPluginConfiguration();
        verify(objectUnderTest, times(1)).getResourceContext();
        verify(mockConfiguration, times(1)).getSimpleValue(eq("filename"), isNull(String.class));

        verifyNoMoreInteractions(mockPackageType);
    }

    private void deleteRecursive(File fileToDelete) throws Exception {
        if (fileToDelete.exists()) {
            if (fileToDelete.isDirectory()) {
                for (File file : fileToDelete.listFiles()) {
                    if (file.isDirectory()) {
                        deleteRecursive(file);
                    } else {
                        file.delete();
                    }
                }
            }

            fileToDelete.delete();
        }
    }

}