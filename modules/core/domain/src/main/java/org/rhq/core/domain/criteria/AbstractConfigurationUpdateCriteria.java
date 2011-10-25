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
package org.rhq.core.domain.criteria;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public abstract class AbstractConfigurationUpdateCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    public static final String SORT_FIELD_CREATED_TIME = "createdTime";

    public static final String SORT_FIELD_STATUS = "status";

    public static final String FETCH_FIELD_CONFIGURATION = "configuration";

    private Integer filterId;
    private List<Integer> filterIds; // requires overrides
    private Long filterStartTime; // requires overrides
    private Long filterEndTime; // requires overrides
    private ConfigurationUpdateStatus filterStatus;
    private List<ConfigurationUpdateStatus> filterStatuses; // requires overrides

    private boolean fetchConfiguration;

    private PageOrdering sortCreatedTime;
    private PageOrdering sortStatus; // requires sort override

    public AbstractConfigurationUpdateCriteria() {
        filterOverrides.put("ids", "id IN ( ? )");
        filterOverrides.put("startTime", "createdTime >= ?");
        filterOverrides.put("endTime", "createdTime <= ?");
        filterOverrides.put("statuses", "status IN ( ? )");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterIds(List<Integer> filterIds) {
        this.filterIds = filterIds;
    }

    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public void addFilterStatus(ConfigurationUpdateStatus status) {
        this.filterStatus = status;
    }

    public void addFilterStatuses(ConfigurationUpdateStatus... configUpdateStatus) {
        this.filterStatuses = CriteriaUtils.getListIgnoringNulls(configUpdateStatus);
    }

    public void fetchConfiguration(boolean fetchConfiguration) {
        this.fetchConfiguration = fetchConfiguration;
    }

    public void addSortCreatedTime(PageOrdering sortCreatedTime) {
        addSortField(SORT_FIELD_CREATED_TIME);
        this.sortCreatedTime = sortCreatedTime;
    }

    public void addSortStatus(PageOrdering sortStatus) {
        addSortField(SORT_FIELD_STATUS);
        this.sortStatus = sortStatus;
    }
}
