/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * The JPA Drift Server plugin (the RHQ default) implementation of Drift.
 *  
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@NamedQueries( { @NamedQuery(name = JPADrift.QUERY_DELETE_BY_RESOURCES, query = "" //
    + "DELETE FROM JPADrift d " //
    + " WHERE d.changeSet IN ( SELECT dcs FROM JPADriftChangeSet dcs WHERE dcs.resource.id IN ( :resourceIds ) ) )"), //
    @NamedQuery(name = JPADrift.QUERY_DELETE_BY_DRIFTDEF_RESOURCE, query = "" //
        + "DELETE FROM JPADrift d " //
        + "  WHERE d.changeSet.id IN " //
        + "       (SELECT dcs.id " //
        + "          FROM JPADriftChangeSet dcs " //
        + "         WHERE dcs.driftDefinition.name = :driftDefinitionName AND dcs.resource.id = :resourceId)") })
@Table(name = "RHQ_DRIFT")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_ID_SEQ")
public class JPADrift implements Serializable, Drift<JPADriftChangeSet, JPADriftFile> {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "JPADrift.deleteByResources";
    public static final String QUERY_DELETE_BY_DRIFTDEF_RESOURCE = "JPADrift.deleteByDriftDefResource";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftCategory category;

    @Column(name = "PATH", nullable = false)
    @Enumerated(EnumType.STRING)
    private String path;

    @Column(name = "PATH_DIRECTORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private String directory;

    @JoinColumn(name = "DRIFT_CHANGE_SET_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private JPADriftChangeSet changeSet;

    @JoinColumn(name = "OLD_DRIFT_FILE", referencedColumnName = "HASH_ID", nullable = true)
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private JPADriftFile oldDriftFile;

    @JoinColumn(name = "NEW_DRIFT_FILE", referencedColumnName = "HASH_ID", nullable = true)
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private JPADriftFile newDriftFile;

    protected JPADrift() {
    }

    /**
     * @param resource
     * @param category
     * @param oldDriftFile required for FILE_CHANGED and FILE_REMOVED, null for FILE_ADDED
     * @param newDriftFile required for FILE_CHANGED and FILE_ADDED, null for FILE_REMOVED
     */
    public JPADrift(JPADriftChangeSet changeSet, String path, DriftCategory category, JPADriftFile oldDriftFile,
        JPADriftFile newDriftFile) {
        this.changeSet = changeSet;
        this.path = path;
        int i = path.lastIndexOf("/");
        this.directory = (i != -1) ? path.substring(0, i) : "./";
        this.category = category;
        this.oldDriftFile = oldDriftFile;
        this.newDriftFile = newDriftFile;
    }

    @Override
    public String getId() {
        return Integer.toString(id);
    }

    @Override
    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    @Override
    public Long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public JPADriftChangeSet getChangeSet() {
        return changeSet;
    }

    @Override
    public void setChangeSet(JPADriftChangeSet changeSet) {
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
        return directory;
    }

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public JPADriftFile getOldDriftFile() {
        return oldDriftFile;
    }

    @Override
    public void setOldDriftFile(JPADriftFile oldDriftFile) {
        this.oldDriftFile = oldDriftFile;
    }

    @Override
    public JPADriftFile getNewDriftFile() {
        return newDriftFile;
    }

    @Override
    public void setNewDriftFile(JPADriftFile newDriftFile) {
        this.newDriftFile = newDriftFile;
    }

    @Override
    public String toString() {
        return "JPADrift [ id=" + id + ", category=" + category + ", path=" + path + ", changeSet=" + changeSet + "]";
    }

}
