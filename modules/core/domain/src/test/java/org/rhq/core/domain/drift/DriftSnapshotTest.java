package org.rhq.core.domain.drift;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_CHANGED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftFileStatus.LOADED;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class DriftSnapshotTest {

    @Test
    public void addChangeSetWithAddedFile() {
        int configId = 1;

        FakeDriftChangeSet changeSet = new FakeDriftChangeSet(0, COVERAGE, configId).add(new FakeDrift(FILE_ADDED,
            null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt"));

        DriftSnapshot snapshot = new DriftSnapshot().add(changeSet);

        assertCollectionMatchesNoOrder(changeSet.getDrifts(), snapshot.getEntries(), "Failed to build "
            + "a snapshot that contains a single change set");
        assertEquals(snapshot.getVersion(), changeSet.getVersion(), "Snapshot version is wrong");
    }

    @Test
    public void addChangeSetsWithAddedFiles() {
        int configId = 1;

        Drift<?, ?> entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry1);

        Drift<?, ?> entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("4a5b6c", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet changeSet2 = new FakeDriftChangeSet(1, DRIFT, configId).add(entry2);

        DriftSnapshot snapshot = new DriftSnapshot().add(changeSet1).add(changeSet2);

        // not sure but asSet with one arg was not working for me, so create Set manually
        Set set = new HashSet<Drift<?, ?>>();
        set.add(entry1);
        set.add(entry2);
        assertCollectionMatchesNoOrder(set, snapshot.getEntries(), "Failed to build snapshot "
            + "with two change sets and file added in second change set");
        assertEquals(snapshot.getVersion(), changeSet2.getVersion(), "Snapshot version is wrong");
    }

    @Test
    public void replaceFileWithChangedVersion() {
        int configId = 1;

        Drift<?, ?> entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry1);

        Drift<?, ?> entry2 = new FakeDrift(FILE_CHANGED, new FakeDriftFile("a1b2c3", 1024, LOADED), new FakeDriftFile(
            "4d5e6f", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet2 = new FakeDriftChangeSet(1, DRIFT, configId).add(entry2);

        DriftSnapshot snapshot = new DriftSnapshot().add(changeSet1).add(changeSet2);

        // not sure but asSet with one arg was not working for me, so create Set manually
        Set set = new HashSet<Drift<?, ?>>();
        set.add(entry2);
        assertCollectionMatchesNoOrder(set, snapshot.getEntries(), "Failed to build snapshot with file changed");
    }

    @Test
    public void deleteFileThatHasBeenRemoved() {
        int configId = 1;

        Drift<?, ?> entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        Drift<?, ?> entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("d1f2a3", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry1).add(entry2);

        Drift entry3 = new FakeDrift(FILE_REMOVED, new FakeDriftFile("a1b2c3", 1024, LOADED), null, "/drift/1.txt");
        FakeDriftChangeSet changeSet2 = new FakeDriftChangeSet(1, DRIFT, configId).add(entry3);

        DriftSnapshot snapshot = new DriftSnapshot().add(changeSet1).add(changeSet2);

        // not sure but asSet with one arg was not working for me, so create Set manually
        Set set = new HashSet<Drift<?, ?>>();
        set.add(entry2);
        assertCollectionMatchesNoOrder(set, snapshot.getEntries(), "Failed to build snapshot with a file " + "removed.");
    }

    @Test
    public void diffShowsEntriesInLeftAndNotInRight() {
        int configId = 1;

        Drift entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry1);
        DriftSnapshot right = new DriftSnapshot().add(changeSet1);

        Drift entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a3b6c9", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet changeSet2 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry1).add(entry2);
        DriftSnapshot left = new DriftSnapshot().add(changeSet1).add(changeSet2);

        DriftDiffReport diff = left.diff(right);

        assertCollectionMatchesNoOrder(asList(entry2), diff.getElementsNotInRight(), "Diff report does not contain "
            + "elements that are in the left but not in the right.");
    }

    @Test
    public void diffShowsEntriesInRightAndNotInLeft() {
        int configId = 1;

        Drift entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry1);
        DriftSnapshot right = new DriftSnapshot().add(changeSet1);

        Drift entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a3b6c9", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet changeSet2 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry2);
        DriftSnapshot left = new DriftSnapshot().add(changeSet2);

        DriftDiffReport diff = left.diff(right);

        assertCollectionMatchesNoOrder(asList(entry1), diff.getElementsNotInLeft(), "Diff report does not contain "
            + "elements that are in the left but not in the right");
    }

    @Test
    public void diffShowsEntriesInLeftAndRightThatAreInConflict() {
        int configId = 1;

        Drift entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drfit/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(0, COVERAGE, configId).add(entry1);
        DriftSnapshot right = new DriftSnapshot().add(changeSet1);

        Drift entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("c3b2a1", 1024, LOADED), "/drfit/1.txt");
        FakeDriftChangeSet changeSet2 = new FakeDriftChangeSet(1, DRIFT, configId).add(entry2);
        DriftSnapshot left = new DriftSnapshot().add(changeSet2);

        DriftDiffReport diff = left.diff(right);

        assertCollectionMatchesNoOrder(asList(entry2), diff.getElementsInConflict(), "Diff report does not contain "
            + "element that are in both left and right and are in conflict");
    }

    <E> Set<E> asSet(E... elements) {
        HashSet<E> set = new HashSet<E>();
        for (E element : elements) {
            set.add(element);
        }
        return set;
    }

    static class FakeDriftChangeSet implements DriftChangeSet {

        private String id;
        private int version;
        private DriftChangeSetCategory category;
        private int configId;
        private DriftHandlingMode mode = DriftHandlingMode.normal;
        private Set<Drift> drifts = new HashSet<Drift>();
        private long ctime = System.currentTimeMillis();

        public FakeDriftChangeSet() {
        }

        public FakeDriftChangeSet(int version, DriftChangeSetCategory category, int configId) {
            this.version = version;
            this.category = category;
            this.configId = configId;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Long getCtime() {
            return ctime;
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public DriftChangeSetCategory getCategory() {
            return category;
        }

        @Override
        public void setCategory(DriftChangeSetCategory category) {
            this.category = category;
        }

        @Override
        public int getDriftConfigurationId() {
            return configId;
        }

        @Override
        public void setDriftConfigurationId(int id) {
            configId = id;
        }

        @Override
        public int getResourceId() {
            return 0;
        }

        @Override
        public Set getDrifts() {
            return drifts;
        }

        @Override
        public void setDrifts(Set drifts) {
            this.drifts = drifts;
        }

        public FakeDriftChangeSet add(Drift drift) {
            drifts.add(drift);
            return this;
        }

        @Override
        public DriftHandlingMode getDriftHandlingMode() {
            return this.mode;
        }

        @Override
        public void setDriftHandlingMode(DriftHandlingMode mode) {
            this.mode = mode;
        }
    }

    static class FakeDrift implements Drift {

        private String id;
        private DriftCategory category;
        private String path;
        private DriftFile oldFile;
        private DriftFile newFile;
        private long ctime = System.currentTimeMillis();

        public FakeDrift() {
        }

        public FakeDrift(DriftCategory category, DriftFile oldFile, DriftFile newFile, String path) {
            this.category = category;
            this.oldFile = oldFile;
            this.newFile = newFile;
            this.path = path;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Long getCtime() {
            return ctime;
        }

        @Override
        public DriftChangeSet getChangeSet() {
            return null;
        }

        @Override
        public void setChangeSet(DriftChangeSet changeSet) {

        }

        @Override
        public DriftCategory getCategory() {
            return category;
        }

        @Override
        public void setCategory(DriftCategory category) {
            this.category = category;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public void setPath(String path) {
            this.path = path;
        }

        @Override
        public DriftFile getOldDriftFile() {
            return oldFile;
        }

        @Override
        public void setOldDriftFile(DriftFile oldDriftFile) {
            this.oldFile = oldDriftFile;
        }

        @Override
        public DriftFile getNewDriftFile() {
            return newFile;
        }

        @Override
        public void setNewDriftFile(DriftFile newDriftFile) {
            newFile = newDriftFile;
        }

        @Override
        public String toString() {
            return "Drift[category: " + category.code() + ", oldFile: " + oldFile + ", newFile: " + newFile
                + ", path: " + path + "]";
        }
    }

    static class FakeDriftFile implements Serializable, DriftFile {

        private static final long serialVersionUID = 1L;
        private String hash;
        private long size;
        private DriftFileStatus status;
        private long ctime = System.currentTimeMillis();

        public FakeDriftFile() {
        }

        public FakeDriftFile(String hash, long size, DriftFileStatus status) {
            this.hash = hash;
            this.size = size;
            this.status = status;
        }

        @Override
        public String getHashId() {
            return hash;
        }

        @Override
        public void setHashId(String hashId) {
            hash = hashId;
        }

        @Override
        public Long getCtime() {
            return ctime;
        }

        @Override
        public Long getDataSize() {
            return size;
        }

        @Override
        public void setDataSize(Long size) {
            this.size = size;
        }

        @Override
        public DriftFileStatus getStatus() {
            return status;
        }

        @Override
        public void setStatus(DriftFileStatus status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "DriftFile[hashId: " + hash + ", dataSize: " + size + ", status: " + status.ordinal() + "]";
        }
    }

}
