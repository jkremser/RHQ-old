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

package org.rhq.core.domain.resource.relationship;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.Resource;

/**
 * @author Noam Malki
 */
@Entity
@NamedQueries( { @NamedQuery(name = ResourceRelAssign.QUERY_FIND_RELATIONSHIP_BY_RESOURCE, query = "" //
    + "  SELECT DISTINCT assign" //
    + "  FROM ResourceRelAssign assign"//
    + "  WHERE assign.sourceResource = :resource" //
    + "     OR assign.targetResource = :resource"),
    @NamedQuery(name = ResourceRelAssign.QUERY_FIND_RELATIONSHIP_BY_RESOURCES, query = "" //
        + "  SELECT DISTINCT assign" //
        + "  FROM ResourceRelAssign assign"//
        + "  WHERE (assign.sourceResource = :firstResource" //
        + "         AND assign.targetResource = :secondResource)" //
        + "    OR (assign.sourceResource = :secondResource" //
        + "         AND assign.targetResource = :firstResource)") })
@Table(name = ResourceRelAssign.TABLE_NAME)
@SequenceGenerator(name = "idGenerator", sequenceName = "RHQ_REL_ASSIGN_ID_SEQ")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceRelAssign implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_REL_ASSIGN";

    public static final String QUERY_FIND_RELATIONSHIP_BY_RESOURCE = "ResourceRelAssign.findRelationshipsByResource";
    public static final String QUERY_FIND_RELATIONSHIP_BY_RESOURCES = "ResourceRelAssign.findRelationshipsByResources";

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "idGenerator")
    private int id;

    @JoinColumn(name = "RELATIONSHIP_DEFINITION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ResourceRelDefinition relationshipDefinition;

    @JoinColumn(name = "SOURCE_RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource sourceResource;

    @JoinColumn(name = "TARGET_RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource targetResource;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @Column(name = "MTIME", nullable = false)
    private long mtime;

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    @PreUpdate
    public void setAgentSynchronizationNeeded() {
        this.mtime = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ResourceRelDefinition getRelationshipDefinition() {
        return relationshipDefinition;
    }

    public void setRelationshipDefinition(ResourceRelDefinition relationshipDefinition) {
        this.relationshipDefinition = relationshipDefinition;
    }

    public Resource getSourceResource() {
        return sourceResource;
    }

    public void setSourceResource(Resource sourceResource) {
        this.sourceResource = sourceResource;
    }

    public Resource getTargetResource() {
        return targetResource;
    }

    public void setTargetResource(Resource targetResource) {
        this.targetResource = targetResource;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

}
