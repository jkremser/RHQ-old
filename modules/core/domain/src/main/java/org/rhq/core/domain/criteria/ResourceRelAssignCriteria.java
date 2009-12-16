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

import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Noam Malki
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceRelAssignCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;

    private Integer filterRelationshipDefinitionId; // needs overrides
    private String filterRelationshipDefinitionName; // needs overrides
    private Integer filterSourceResourceId; // needs overrides
    private Integer filterTargetResourceId; // needs overrides

    private boolean fetchRelationshipDefinition;
    private boolean fetchSourceResource;
    private boolean fetchTargetResource;

    private PageOrdering sortMtime;
    private PageOrdering sortCtime;

    public ResourceRelAssignCriteria() {
        super(ResourceRelAssignCriteria.class);

        filterOverrides.put("relationshipDefinitionId", "relationshipDefinition.id = ?");
        filterOverrides.put("relationshipDefinitionName", "relationshipDefinition.name like ?");
        filterOverrides.put("sourceResourceId", "sourceResource.id = ?");
        filterOverrides.put("targetResourceId", "targetResource.id = ?");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterRelationshipDefinitionId(Integer filterRelationshipDefinitionId) {
        this.filterRelationshipDefinitionId = filterRelationshipDefinitionId;
    }

    public void addFilterSourceResourceId(Integer filterSourceResourceId) {
        this.filterSourceResourceId = filterSourceResourceId;
    }

    public void addFilterTargetResourceId(Integer filterTargetResourceId) {
        this.filterTargetResourceId = filterTargetResourceId;
    }

    public void addFilterRelationshipDefinitionName(String filterRelationshipDefinitionName) {
        this.filterRelationshipDefinitionName = filterRelationshipDefinitionName;
    }

    public void fetchRelationshipDefinition(boolean fetchRelationshipDefinition) {
        this.fetchRelationshipDefinition = fetchRelationshipDefinition;
    }

    public void fetchSourceResource(boolean fetchSourceResource) {
        this.fetchSourceResource = fetchSourceResource;
    }

    public void fetchTargetResource(boolean fetchTargetResource) {
        this.fetchTargetResource = fetchTargetResource;
    }

    public void addSortMtime(PageOrdering sortMtime) {
        this.sortMtime = sortMtime;
    }

    public void addSortCtime(PageOrdering sortCtime) {
        this.sortCtime = sortCtime;
    }

}
