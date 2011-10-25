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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceCriteria extends TaggedCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private String filterResourceKey;
    private InventoryStatus filterInventoryStatus = InventoryStatus.COMMITTED; // default
    private String filterVersion;
    private String filterDescription;
    private Integer filterResourceTypeId; // needs overrides
    private String filterResourceTypeName; // needs overrides
    private List<ResourceCategory> filterResourceCategories; // needs overrides
    private String filterPluginName; // needs overrides
    private Integer filterParentResourceId; // needs overrides
    private String filterParentResourceName; // needs overrides
    private Integer filterParentResourceTypeId; // needs overrides
    private String filterAgentName; // needs overrides
    private Integer filterAgentId; // needs overrides
    private AvailabilityType filterCurrentAvailability; // needs overrides
    private Long filterStartItime;
    private Long filterEndItime;
    private List<Integer> filterIds; // needs overrides
    private List<Integer> filterExplicitGroupIds; // requires overrides
    private List<Integer> filterImplicitGroupIds; // requires overrides
    private Integer filterRootResourceId; // requires overrides

    private boolean fetchResourceType;
    private boolean fetchChildResources;
    private boolean fetchParentResource;
    private boolean fetchResourceConfiguration;
    private boolean fetchPluginConfiguration;
    private boolean fetchAgent;
    private boolean fetchAlertDefinitions;
    private boolean fetchResourceConfigurationUpdates;
    private boolean fetchPluginConfigurationUpdates;
    private boolean fetchImplicitGroups;
    private boolean fetchExplicitGroups;
    private boolean fetchContentServiceRequests;
    private boolean fetchCreateChildResourceRequests;
    private boolean fetchDeleteResourceRequests;
    private boolean fetchOperationHistories;
    private boolean fetchInstalledPackages;
    private boolean fetchInstalledPackageHistory;
    private boolean fetchResourceRepos;
    private boolean fetchSchedules;
    private boolean fetchCurrentAvailability;
    private boolean fetchResourceErrors;
    private boolean fetchEventSources;
    private boolean fetchProductVersion;
    private boolean fetchDriftDefinitions;

    private PageOrdering sortName;
    private PageOrdering sortInventoryStatus;
    private PageOrdering sortVersion;
    private PageOrdering sortResourceTypeName; // needs overrides
    private PageOrdering sortResourceCategory; // needs overrides
    private PageOrdering sortPluginName; // needs overrides
    private PageOrdering sortParentResourceName; // needs overrides
    private PageOrdering sortAgentName; // needs overrides
    private PageOrdering sortCurrentAvailability; // needs overrides
    private PageOrdering sortResourceAncestry; // needs overrides

    public ResourceCriteria() {
        filterOverrides.put("resourceTypeId", "resourceType.id = ?");
        filterOverrides.put("resourceTypeName", "resourceType.name like ?");
        filterOverrides.put("resourceCategories", "resourceType.category IN ( ? )");
        filterOverrides.put("pluginName", "resourceType.plugin like ?");
        filterOverrides.put("parentResourceId", "parentResource.id = ?");
        filterOverrides.put("parentResourceName", "parentResource.name like ?");
        filterOverrides.put("parentResourceTypeId", "parentResource.resourceType.id = ?");
        filterOverrides.put("agentName", "agent.name like ?");
        filterOverrides.put("agentId", "agent.id = ?");
        filterOverrides.put("currentAvailability", "currentAvailability.availabilityType = ?");
        filterOverrides.put("startItime", "itime >= ?");
        filterOverrides.put("endItime", "itime <= ?");
        filterOverrides.put("ids", "id IN ( ? )");
        filterOverrides.put("explicitGroupIds", "" //
            + "id IN ( SELECT ires.id " //
            + "          FROM Resource ires " //
            + "          JOIN ires.explicitGroups explicitGroup " //
            + "         WHERE explicitGroup.id IN ( ? ) )");
        filterOverrides.put("implicitGroupIds", "" //
            + "id IN ( SELECT ires.id " //
            + "          FROM Resource ires " //
            + "          JOIN ires.implicitGroups implicitGroup " //
            + "         WHERE implicitGroup.id IN ( ? ) )");
        filterOverrides.put("rootResourceId", "agent.id = (SELECT r2.agent.id FROM Resource r2 where r2.id = ?)");

        sortOverrides.put("resourceTypeName", "resourceType.name");
        sortOverrides.put("resourceCategory", "resourceType.category");
        sortOverrides.put("pluginName", "resourceType.plugin");
        sortOverrides.put("parentResourceName", "parentResource.name");
        sortOverrides.put("agentName", "agent.name");
        sortOverrides.put("currentAvailability", "currentAvailability.availabilityType");
        sortOverrides.put("resourceAncestry", "ancestry");
    }

    @Override
    public Class<Resource> getPersistentClass() {
        return Resource.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterResourceKey(String filterResourceKey) {
        this.filterResourceKey = filterResourceKey;
    }

    public void addFilterInventoryStatus(InventoryStatus filterInventoryStatus) {
        this.filterInventoryStatus = filterInventoryStatus;
    }

    public void addFilterVersion(String filterVersion) {
        this.filterVersion = filterVersion;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterResourceTypeId(Integer filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterResourceCategories(ResourceCategory... filterResourceCategories) {
        this.filterResourceCategories = CriteriaUtils.getListIgnoringNulls(filterResourceCategories);
    }

    public void addFilterPluginName(String filterPluginName) {
        this.filterPluginName = filterPluginName;
    }

    public void addFilterParentResourceId(Integer filterParentResourceId) {
        this.filterParentResourceId = filterParentResourceId;
    }

    public void addFilterParentResourceName(String filterParentResourceName) {
        this.filterParentResourceName = filterParentResourceName;
    }

    public void addFilterParentResourceTypeId(int filterParentResourceTypeId) {
        this.filterParentResourceTypeId = filterParentResourceTypeId;
    }

    public void addFilterAgentName(String filterAgentName) {
        this.filterAgentName = filterAgentName;
    }

    public void addFilterAgentId(Integer filterAgentId) {
        this.filterAgentId = filterAgentId;
    }

    public void addFilterCurrentAvailability(AvailabilityType filterCurrentAvailability) {
        this.filterCurrentAvailability = filterCurrentAvailability;
    }

    public void addFilterStartItime(long itime) {
        filterStartItime = itime;
    }

    public void addFilterEndItime(long itime) {
        filterEndItime = itime;
    }

    public void addFilterIds(Integer... filterIds) {
        this.filterIds = CriteriaUtils.getListIgnoringNulls(filterIds);
    }

    public void addFilterExplicitGroupIds(Integer... filterExplicitGroupIds) {
        this.filterExplicitGroupIds = CriteriaUtils.getListIgnoringNulls(filterExplicitGroupIds);
    }

    public void addFilterImplicitGroupIds(Integer... filterImplicitGroupIds) {
        this.filterImplicitGroupIds = CriteriaUtils.getListIgnoringNulls(filterImplicitGroupIds);
    }

    public void addFilterRootResourceId(Integer filterRootResourceId) {
        this.filterRootResourceId = filterRootResourceId;
    }

    public void fetchResourceType(boolean fetchResourceType) {
        this.fetchResourceType = fetchResourceType;
    }

    /**
     * Requires MANAGE_INVENTORY
     * @param fetchChildResources
     */
    public void fetchChildResources(boolean fetchChildResources) {
        this.fetchChildResources = fetchChildResources;
    }

    /**
     * Requires MANAGE_INVENTORY
     * @param fetchParentResource
     */
    public void fetchParentResource(boolean fetchParentResource) {
        this.fetchParentResource = fetchParentResource;
    }

    public void fetchResourceConfiguration(boolean fetchResourceConfiguration) {
        this.fetchResourceConfiguration = fetchResourceConfiguration;
    }

    public void fetchPluginConfiguration(boolean fetchPluginConfiguration) {
        this.fetchPluginConfiguration = fetchPluginConfiguration;
    }

    public void fetchAgent(boolean fetchAgent) {
        this.fetchAgent = fetchAgent;
    }

    public void fetchAlertDefinitions(boolean fetchAlertDefinitions) {
        this.fetchAlertDefinitions = fetchAlertDefinitions;
    }

    public void fetchResourceConfigurationUpdates(boolean fetchResourceConfigurationUpdates) {
        this.fetchResourceConfigurationUpdates = fetchResourceConfigurationUpdates;
    }

    public void fetchPluginConfigurationUpdates(boolean fetchPluginConfigurationUpdates) {
        this.fetchPluginConfigurationUpdates = fetchPluginConfigurationUpdates;
    }

    public void fetchImplicitGroups(boolean fetchImplicitGroups) {
        this.fetchImplicitGroups = fetchImplicitGroups;
    }

    public void fetchExplicitGroups(boolean fetchExplicitGroups) {
        this.fetchExplicitGroups = fetchExplicitGroups;
    }

    public void fetchContentServiceRequests(boolean fetchContentServiceRequests) {
        this.fetchContentServiceRequests = fetchContentServiceRequests;
    }

    public void fetchCreateChildResourceRequests(boolean fetchCreateChildResourceRequests) {
        this.fetchCreateChildResourceRequests = fetchCreateChildResourceRequests;
    }

    public void fetchDeleteResourceRequests(boolean fetchDeleteResourceRequests) {
        this.fetchDeleteResourceRequests = fetchDeleteResourceRequests;
    }

    public void fetchOperationHistories(boolean fetchOperationHistories) {
        this.fetchOperationHistories = fetchOperationHistories;
    }

    public void fetchInstalledPackages(boolean fetchInstalledPackages) {
        this.fetchInstalledPackages = fetchInstalledPackages;
    }

    public void fetchInstalledPackageHistory(boolean fetchInstalledPackageHistory) {
        this.fetchInstalledPackageHistory = fetchInstalledPackageHistory;
    }

    public void fetchResourceRepos(boolean fetchResourceRepos) {
        this.fetchResourceRepos = fetchResourceRepos;
    }

    public void fetchSchedules(boolean fetchSchedules) {
        this.fetchSchedules = fetchSchedules;
    }

    public void fetchCurrentAvailability(boolean fetchCurrentAvailability) {
        this.fetchCurrentAvailability = fetchCurrentAvailability;
    }

    public void fetchResourceErrors(boolean fetchResourceErrors) {
        this.fetchResourceErrors = fetchResourceErrors;
    }

    public void fetchEventSources(boolean fetchEventSources) {
        this.fetchEventSources = fetchEventSources;
    }

    public void fetchProductVersion(boolean fetchProductVersion) {
        this.fetchProductVersion = fetchProductVersion;
    }

    public void fetchDriftDefinitions(boolean fetchDriftDefinitions) {
        this.fetchDriftDefinitions = fetchDriftDefinitions;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortInventoryStatus(PageOrdering sortInventoryStatus) {
        addSortField("inventoryStatus");
        this.sortInventoryStatus = sortInventoryStatus;
    }

    public void addSortVersion(PageOrdering sortVersion) {
        addSortField("version");
        this.sortVersion = sortVersion;
    }

    public void addSortResourceTypeName(PageOrdering sortResourceTypeName) {
        addSortField("resourceTypeName");
        this.sortResourceTypeName = sortResourceTypeName;
    }

    public void addSortResourceCategory(PageOrdering sortResourceCategory) {
        addSortField("resourceCategory");
        this.sortResourceCategory = sortResourceCategory;
    }

    public void addSortPluginName(PageOrdering sortPluginName) {
        addSortField("pluginName");
        this.sortPluginName = sortPluginName;
    }

    public void addSortParentResourceName(PageOrdering sortParentResourceName) {
        addSortField("parentResourceName");
        this.sortParentResourceName = sortParentResourceName;
    }

    public void addSortAgentName(PageOrdering sortAgentName) {
        addSortField("agentName");
        this.sortAgentName = sortAgentName;
    }

    public void addSortCurrentAvailability(PageOrdering sortCurrentAvailability) {
        addSortField("currentAvailability");
        this.sortCurrentAvailability = sortCurrentAvailability;
    }

    public void addSortResourceAncestry(PageOrdering sortAncestry) {
        addSortField("resourceAncestry");
        this.sortResourceAncestry = sortAncestry;
    }

    /** subclasses should override as necessary */
    public boolean isInventoryManagerRequired() {
        return (this.fetchChildResources || this.fetchParentResource);
    }

}
