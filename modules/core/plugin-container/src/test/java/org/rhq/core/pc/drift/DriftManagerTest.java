/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.pc.drift;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.util.ZipUtil.unzipFile;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class DriftManagerTest extends DriftTest {

    private File tmpDir;

    private TestDriftServerService driftServerService;

    private PluginContainerConfiguration pcConfig;

    private TestDriftManager driftMgr;

    @BeforeMethod
    public void initTest() throws Exception {
        tmpDir = mkdir(basedir(), "tmp");

        pcConfig = new PluginContainerConfiguration();
        ServerServices serverServices = new ServerServices();
        driftServerService = new TestDriftServerService();
        serverServices.setDriftServerService(driftServerService);
        pcConfig.setServerServices(serverServices);
        pcConfig.setDataDirectory(basedir());
        pcConfig.setTemporaryDirectory(tmpDir);

        driftMgr = new TestDriftManager();
        driftMgr.setConfiguration(pcConfig);
        driftMgr.setChangeSetMgr(changeSetMgr);
    }

    @Test
    public void writeChangeSetZipFileToChangeSetDirectory() throws Exception {
        final DriftConfiguration config = driftConfiguration("write-changeset-file", resourceDir.getAbsolutePath());
        final File changeSetDir = changeSetDir(config.getName());
        createRandomFile(changeSetDir, "changeset.txt");

        setDriftServiceCallback(new DriftServiceCallback() {
            @Override
            public void execute() {
                assertThatZipFileExists(changeSetDir, "changeset_",
                    "Expected to find change set zip file " +
                        "in " + changeSetDir.getPath() + ". The file name should follow the pattern " +
                        "changeset_<integer_timestamp>.zip");
            }
        });

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), config);
        DriftDetectionSummary detectionSummary = new DriftDetectionSummary();
        detectionSummary.setSchedule(schedule);
        detectionSummary.setType(COVERAGE);

        driftMgr.setChangeSetMgr(changeSetMgr);
        driftMgr.getSchedulesQueue().addSchedule(schedule);
        driftMgr.sendChangeSetToServer(detectionSummary);
    }

    @Test
    public void sendChangeSetReportInZipFile() throws Exception {
        final DriftConfiguration config = driftConfiguration("send-changeset-in-zip", resourceDir.getAbsolutePath());
        final File changeSetDir = changeSetDir(config.getName());
        final File changeSetFile = createRandomFile(changeSetDir, "changeset.txt");

        setDriftServiceCallback(new DriftServiceCallback() {
            @Override
            public void execute() {
                assertZipFileMatches(driftServerService.inputStream, changeSetFile);
            }
        });

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), config);
        DriftDetectionSummary detectionSummary = new DriftDetectionSummary();
        detectionSummary.setSchedule(schedule);
        detectionSummary.setType(COVERAGE);

        driftMgr.setChangeSetMgr(changeSetMgr);
        driftMgr.getSchedulesQueue().addSchedule(new DriftDetectionSchedule(resourceId(), config));
        driftMgr.sendChangeSetToServer(detectionSummary);
    }

    @Test
    public void cleanUpWhenServerAcksChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("clean-up-when-server-acks-changeset",
            resourceDir.getAbsolutePath());
        File changeSetDir = changeSetDir(config.getName());
        File snapshotFile = createRandomFile(changeSetDir, "changeset.txt");
        File previousSnapshotFile = createRandomFile(changeSetDir, "changeset.txt.previous");
        File changeSetZipFile = createRandomFile(changeSetDir, "changeset_" + System.currentTimeMillis() + ".zip");

        driftMgr.ackChangeSet(resourceId(), config.getName());

        assertTrue(snapshotFile.exists(), "Snapshot file should exist after server acks change set");
        assertFalse(previousSnapshotFile.exists(), "Previous version snapshot file should be deleted when server " +
            "acks change set");
        assertEquals(findChangeSetZipFiles(changeSetDir).size(), 0, "All change set zip files should be deleted when " +
            "server acks change set");
    }

    @Test
    public void writeContentZipFileToChangeSetContentDirectory() throws Exception {
        String configName = "send-content-in-zip";
        final File changeSetDir = changeSetDir(configName);
        final File contentDir = mkdir(changeSetDir, "content");

        createRandomFile(contentDir, "content-1");
        createRandomFile(contentDir, "content-2");

        setDriftServiceCallback(new DriftServiceCallback() {
            @Override
            public void execute() {
                assertThatZipFileExists(changeSetDir, "content_", "Expected to find content zip file in " +
                    changeSetDir.getPath() + ". The file name should follow the pattern " +
                    "content_<integer_timestamp>.zip");
            }
        });

        driftMgr.sendChangeSetContentToServer(resourceId(), configName, contentDir);
    }

    @Test
    public void sendContentToServerInZipFile() throws Exception {
        String configName = "send-content-in-zip";
        File changeSetDir = changeSetDir(configName);
        File contentDir = mkdir(changeSetDir, "content");

        final File content1 = createRandomFile(contentDir, "content-1");
        final File content2 = createRandomFile(contentDir, "content-2");

        setDriftServiceCallback(new DriftServiceCallback() {
            @Override
            public void execute() {
                assertZipFileMatches(driftServerService.inputStream, content1, content2);
            }
        });

        driftMgr.sendChangeSetContentToServer(resourceId(), configName, contentDir);
    }

    @Test
    public void cleanUpAfterSendingContentToServer() throws Exception {
        String configName = "clean-up-after-sending-content";
        File changeSetDir = changeSetDir(configName);
        File contentDir = mkdir(changeSetDir, "content");

        createRandomFile(contentDir, "content-1");
        createRandomFile(contentDir, "content-2");

        driftMgr.sendChangeSetContentToServer(resourceId(), configName, contentDir);

        // clean up should not happen until after the input stream is closed. The remote
        // input stream is consumed asynchronously; so, we have to wait until the stream
        // is closed. We could otherwise disrupt the transmission of bits.
        File contentZipFile = assertThatZipFileExists(changeSetDir, "content_",
            "Expected to find content zip file in " +
                changeSetDir.getPath() + ". The file name should have a pattern of content_integer_timestamp>.zip");

        driftServerService.inputStream.close();

        assertFalse(contentZipFile.exists(), "The content zip should be deleted when the remote input stream is closed");
        assertThatDirectoryIsEmpty(contentDir);
    }

    @Test
    public void unschedulingDetectionRemovesScheduleFromQueue() throws Exception {
        DriftConfiguration config = driftConfiguration("remove-from-queue", resourceDir.getAbsolutePath());

        driftMgr.scheduleDriftDetection(resourceId(), config);
        driftMgr.scheduleDriftDetection(resourceId() + 5, driftConfiguration("another-config", "."));
        driftMgr.unscheduleDriftDetection(resourceId(), config);

        assertFalse(driftMgr.getSchedulesQueue().contains(resourceId(), config),
            new DriftDetectionSchedule(resourceId(), config) + " should have been removed from the schedule queue");
    }

    @Test
    public void unschedulingDetectionRemovesDriftConfigFromResourceContainer() throws Exception {
        DriftConfiguration config = driftConfiguration("remove-from-queue", resourceDir.getAbsolutePath());
        DriftConfiguration config2 = driftConfiguration("do-not-remove", resourceDir.getAbsolutePath());

        driftMgr.scheduleDriftDetection(resourceId(), config);
        driftMgr.scheduleDriftDetection(resourceId(), config2);
        driftMgr.unscheduleDriftDetection(resourceId(), config);

        ResourceContainer container = driftMgr.getInventoryManager().getResourceContainer(resourceId());

        assertCollectionMatchesNoOrder(config + " should have been removed from the resource container ",
            asList(config2), container.getDriftConfigurations());
    }

    @Test
    public void unschedulingDetectionDeletesChangeSetDirectoryWhenScheduleIsNotActive() throws Exception {
        DriftConfiguration config = driftConfiguration("delete-changeset-dir", resourceDir.getAbsolutePath());
        File changeSetDir = changeSetDir(config.getName());
        File contentDir = mkdir(changeSetDir, "content");

        createRandomFile(contentDir, "my_content");
        createRandomFile(changeSetDir, "changeset.txt");

        driftMgr.scheduleDriftDetection(resourceId(), config);
        driftMgr.unscheduleDriftDetection(resourceId(), config);

        assertFalse(changeSetDir.exists(), "The change set directory should have been deleted.");
    }

    @Test
    public void unschedulingDetectionDeletesChangeSetDirectoryWhenScheduleIsDeactivated() throws Exception {
        DriftConfiguration config = driftConfiguration("delete-changeset-dir", resourceDir.getAbsolutePath());
        File changeSetDir = changeSetDir(config.getName());
        File contentDir = mkdir(changeSetDir, "content");

        createRandomFile(contentDir, "my_content");
        createRandomFile(changeSetDir, "changeset.txt");

        driftMgr.scheduleDriftDetection(resourceId(), config);
        driftMgr.getSchedulesQueue().getNextSchedule();
        driftMgr.unscheduleDriftDetection(resourceId(), config);

        assertTrue(changeSetDir.exists(), "The change set directory should not be deleted while the schedule is " +
            "still active.");

        driftMgr.getSchedulesQueue().deactivateSchedule();
        assertFalse(changeSetDir.exists(), "The change set directory should have been deleted after the schedule is " +
            "deactivated.");
    }

    private List<File> findChangeSetZipFiles(File dir) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("changeset_") && name.endsWith(".zip");
            }
        });
        return asList(files);
    }

    /**
     * This method first verifies that each of the expected files is contained in the the
     * zip file. Then it verifies that the content for each file in the zip file matches
     * the expected files by comparing their SHA-256 hashes.
     *
     * @param zipStream
     * @param expectedFiles
     * @throws IOException
     */
    private void assertZipFileMatches(InputStream zipStream, File... expectedFiles) {
        File zipDir = new File(tmpDir, "output");
        try {
            deleteDirectory(zipDir);
            zipDir.mkdirs();
            unzipFile(zipStream, zipDir);
        } catch (IOException e) {
            fail("An error occurred while trying to unzip " + zipDir.getPath(), e);
        }

        assertEquals(zipDir.listFiles().length, expectedFiles.length, "The zip file has the wrong number of files");

        for (File expectedFile : expectedFiles) {

            File actualFile = findFile(zipDir, expectedFile);
            assertNotNull(actualFile, "Expected zip file to contain " + expectedFile.getName());

            String expectedHash = sha256(expectedFile);
            String actualHash = sha256(actualFile);

            assertEquals(actualHash, expectedHash, "The zip file content is wrong. The SHA-256 hash does not match " +
                "for " + expectedFile.getName());
        }
    }

    private void assertThatDirectoryIsEmpty(File dir) {
        assertEquals(dir.listFiles().length, 0, "Expected " + dir.getPath() + " to be empty");
    }

    private File findFile(File dir, File file) {
        for (File f : dir.listFiles()) {
            if (f.getName().equals(file.getName())) {
                return f;
            }
        }
        return null;
    }

    /**
     * This method searches the specified directory for a zip file with the specified
     * prefix. The file name must start with the prefix and end with an extension of .zip.
     * This method assumes that there should be one and only one match. If there is not
     * exactly one match, it will fail the test.
     *
     * @param dir The directory to search (sub directories are not searched)
     * @param fileNamePrefix The zip file name prefix
     * @param msg An error message
     * @return The matching zip file
     */
    private File assertThatZipFileExists(final File dir, final String fileNamePrefix, String msg) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(fileNamePrefix) && name.endsWith(".zip");
            }
        });
        assertEquals(files.length, 1, msg);
        return files[0];
    }

    /**
     * Sets a callback that will be invoked immediately after DriftManager calls
     * {@link DriftServerService#sendChangesetZip(int, long, java.io.InputStream)} or
     * {@link DriftServerService#sendFilesZip(int, long, java.io.InputStream)}. The callback
     * can perform any verification as necessary, and that will happen before the call to
     * to DriftServerService returns.
     *
     * @param callback
     */
    private void setDriftServiceCallback(DriftServiceCallback callback) {
        driftServerService.callback = callback;
    }

    private static class TestDriftServerService implements DriftServerService {

        public int resourceId;
        public long fileSize;
        public InputStream inputStream;

        public DriftServiceCallback callback;

        @Override
        public void sendChangesetZip(int resourceId, long zipSize, InputStream zipStream) {
            this.resourceId = resourceId;
            fileSize = zipSize;
            inputStream = zipStream;

            if (callback != null) {
                callback.execute();
            }
        }

        @Override
        public void sendFilesZip(int resourceId, long zipSize, InputStream zipStream) {
            this.resourceId = resourceId;
            fileSize = zipSize;
            inputStream = zipStream;

            if (callback != null) {
                callback.execute();
            }
        }

        @Override
        public Map<Integer, List<DriftConfiguration>> getDriftConfigurations(Set<Integer> resourceIds) {
            return null;
        }

        @Override
        public DriftSnapshot getCurrentSnapshot(int driftConfigurationId) {
            return null;
        }
    }

    /**
     * This callback interface provides a hook for doing any verification immediately after
     * DriftManager calls DriftServerService.
     */
    private static interface DriftServiceCallback {
        void execute();
    }

    private static class TestDriftManager extends DriftManager {

        FakeInventoryManager inventoryMgr = new FakeInventoryManager();

        @Override
        public InventoryManager getInventoryManager() {
            return inventoryMgr;
        }
    }

    private static class FakeInventoryManager extends InventoryManager {
        private Map<Integer, ResourceContainer> resourceContainers = new HashMap<Integer, ResourceContainer>();

        @Override
        public ResourceContainer getResourceContainer(Integer resourceId) {
            ResourceContainer container = resourceContainers.get(resourceId);
            if (container == null) {
                Resource resource = new Resource();
                resource.setId(resourceId);
                container = new ResourceContainer(resource, getClass().getClassLoader());
                resourceContainers.put(resourceId, container);
            }
            return container;
        }
    }

}
