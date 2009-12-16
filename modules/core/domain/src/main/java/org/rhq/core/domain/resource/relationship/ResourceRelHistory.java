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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.Resource;

/**
 * @author Noam Malki
 */
@Entity
@Table(name = ResourceRelHistory.TABLE_NAME)
@SequenceGenerator(name = "idGenerator", sequenceName = "RHQ_REL_HISTORY_ID_SEQ")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceRelHistory {

    public static final String TABLE_NAME = "RHQ_REL_HISTORY";

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "idGenerator")
    private int id;

    @JoinColumn(name = "RELATIONSHIP_DEFINITION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ResourceRelDefinition relationshipDefinition;

    @JoinColumn(name = "SOURCE_RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource sourceResource;

    @JoinColumn(name = "TARGET_RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource targetResource;

    @Column(name = "START_TIME", nullable = false)
    private long startTime;

    @Column(name = "END_TIME", nullable = false)
    private long endTime;

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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

}
