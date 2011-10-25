package org.rhq.core.domain.drift.dto;

import java.io.Serializable;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;

public class DriftDTO implements Drift<DriftChangeSetDTO, DriftFileDTO>, Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private Long ctime;

    private DriftChangeSetDTO changeSet;

    private DriftCategory category;

    private String path;

    private String directory;

    private DriftFileDTO oldDriftFile;

    private DriftFileDTO newDriftFile;

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

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    @Override
    public DriftChangeSetDTO getChangeSet() {
        return changeSet;
    }

    @Override
    public void setChangeSet(DriftChangeSetDTO changeSet) {
        this.changeSet = changeSet;
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
    public String getDirectory() {
        return this.directory;
    }

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public DriftFileDTO getOldDriftFile() {
        return oldDriftFile;
    }

    @Override
    public void setOldDriftFile(DriftFileDTO oldDriftFile) {
        this.oldDriftFile = oldDriftFile;
    }

    @Override
    public DriftFileDTO getNewDriftFile() {
        return newDriftFile;
    }

    @Override
    public void setNewDriftFile(DriftFileDTO newDriftFile) {
        this.newDriftFile = newDriftFile;
    }
}
