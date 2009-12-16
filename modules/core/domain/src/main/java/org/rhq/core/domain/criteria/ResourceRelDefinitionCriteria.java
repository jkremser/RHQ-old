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
package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.relationship.RelationshipCardinality;
import org.rhq.core.domain.resource.relationship.RelationshipConstraint;
import org.rhq.core.domain.resource.relationship.RelationshipType;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Noam Malki
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceRelDefinitionCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterPlugin;
    private String filterName;
    private RelationshipCardinality filterCardinaliry;
    private Integer filterSourceResourceTypeId; // needs overrides
    private String filterSourceResourceTypeName; // needs overrides
    private Integer filterTargetResourceTypeId; // needs overrides
    private String filterTargetResourceTypeName; // needs overrides
    private RelationshipConstraint filterSourceConstraint;
    private RelationshipType filterType;
    private Boolean filterUserEditable;

    private boolean fetchSourceResourceType;
    private boolean fetchTargetResourceType;

    private PageOrdering sortName;
    private PageOrdering sortPlugin;
    private PageOrdering sortSourceResourceTypeName; // needs overrides
    private PageOrdering sortTargetResourceTypeName; // needs overrides

    public ResourceRelDefinitionCriteria() {
        super(ResourceRelDefinitionCriteria.class);

        filterOverrides.put("sourceResourceTypeId", "sourceResourceType.id = ?");
        filterOverrides.put("sourceResourceTypeName", "sourceResourceType.name like ?");
        filterOverrides.put("targetResourceTypeId", "targetResourceType.id = ?");
        filterOverrides.put("targetResourceTypeName", "targetResourceType.name = ?");

        sortOverrides.put("sourceResourceTypeName", "sourceResourceType.name");
        sortOverrides.put("targetResourceTypeName", "targetResourceType.name");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterPlugin(String filterPlugin) {
        this.filterPlugin = filterPlugin;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterCardinaliry(RelationshipCardinality filterCardinaliry) {
        this.filterCardinaliry = filterCardinaliry;
    }

    public void addFilterSourceResourceTypeId(Integer filterSourceResourceTypeId) {
        this.filterSourceResourceTypeId = filterSourceResourceTypeId;
    }

    public void addFilterSourceResourceTypeName(String filterSourceResourceTypeName) {
        this.filterSourceResourceTypeName = filterSourceResourceTypeName;
    }

    public void addFilterTargetResourceTypeId(Integer filterTargetResourceTypeId) {
        this.filterTargetResourceTypeId = filterTargetResourceTypeId;
    }

    public void addFilterTargetResourceTypeName(String filterTargetResourceTypeName) {
        this.filterTargetResourceTypeName = filterTargetResourceTypeName;
    }

    public void addFilterSourceConstraint(RelationshipConstraint filterSourceConstraint) {
        this.filterSourceConstraint = filterSourceConstraint;
    }

    public void addFilterType(RelationshipType filterType) {
        this.filterType = filterType;
    }

    public void addFilterUserEditable(Boolean filterUserEditable) {
        this.filterUserEditable = filterUserEditable;
    }

    public void fetchSourceResourceType(boolean fetchSourceResourceType) {
        this.fetchSourceResourceType = fetchSourceResourceType;
    }

    public void fetchTargetResourceType(boolean fetchTargetResourceType) {
        this.fetchTargetResourceType = fetchTargetResourceType;
    }

    public void addSortName(PageOrdering sortName) {
        this.sortName = sortName;
    }

    public void addSortPlugin(PageOrdering sortPlugin) {
        this.sortPlugin = sortPlugin;
    }

    public void addSortSourceResourceTypeName(PageOrdering sortSourceResourceTypeName) {
        this.sortSourceResourceTypeName = sortSourceResourceTypeName;
    }

    public void addSortTargetResourceTypeName(PageOrdering sortTargetResourceTypeName) {
        this.sortTargetResourceTypeName = sortTargetResourceTypeName;
    }

}
