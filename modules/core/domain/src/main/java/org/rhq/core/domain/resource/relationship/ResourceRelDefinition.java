/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.ResourceType;

//should be: ResourceRelationshipDefinition
/**
 * @author Noam Malki
 */
@Entity
@NamedQueries( { @NamedQuery(name = ResourceRelDefinition.QUERY_FIND_RELATIONSHIP_DEF_BY_NAME, query = "" //
    + "  SELECT DISTINCT definition" //
    + "  FROM ResourceRelDefinition definition"//
    + "  WHERE definition.name = :name") })
@Table(name = ResourceRelDefinition.TABLE_NAME)
@SequenceGenerator(name = "idGenerator", sequenceName = "RHQ_REL_DEF_ID_SEQ")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceRelDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_REL_DEF";

    public static final String QUERY_FIND_RELATIONSHIP_DEF_BY_NAME = "ResourceRelDefinition.findRelationshipDefinitionByName";

    @Column(name = "ID", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "idGenerator")
    private int id;

    @Column(name = "PLUGIN", nullable = false)
    private String plugin;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CARDINALITY", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private RelationshipCardinality cardinality;

    @JoinColumn(name = "SOURCE_TYPE_ID", referencedColumnName = "ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ResourceType sourceResourceType;

    @JoinColumn(name = "TARGET_TYPE_ID", referencedColumnName = "ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ResourceType targetResourceType;

    @Column(name = "SOURCE_CONSTRAINT", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private RelationshipConstraint sourceConstraint;

    @Column(name = "TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private RelationshipType type;

    @Column(name = "USER_EDITABLE", nullable = false)
    private boolean userEditable;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RelationshipCardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(RelationshipCardinality cardinality) {
        this.cardinality = cardinality;
    }

    public ResourceType getSourceResourceType() {
        return sourceResourceType;
    }

    public void setSourceResourceType(ResourceType sourceResourceType) {
        this.sourceResourceType = sourceResourceType;
    }

    public ResourceType getTargetResourceType() {
        return targetResourceType;
    }

    public void setTargetResourceType(ResourceType targetResourceType) {
        this.targetResourceType = targetResourceType;
    }

    public RelationshipConstraint getSourceConstraint() {
        return sourceConstraint;
    }

    public void setSourceConstraint(RelationshipConstraint sourceConstraint) {
        this.sourceConstraint = sourceConstraint;
    }

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }

    public boolean isUserEditable() {
        return userEditable;
    }

    public void setUserEditable(boolean userEditable) {
        this.userEditable = userEditable;
    }

}
