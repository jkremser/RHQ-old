/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.common.paging;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.admin.ListAlertTemplatesUIBean;
import org.rhq.enterprise.gui.alert.ListAlertDefinitionsUIBean;
import org.rhq.enterprise.gui.alert.ListAlertHistoryUIBean;
import org.rhq.enterprise.gui.alert.ListGroupAlertDefinitionsUIBean;
import org.rhq.enterprise.gui.alert.ListGroupAlertHistoryUIBean;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.configuration.group.GroupResourceConfigurationHistoryDetailsUIBean;
import org.rhq.enterprise.gui.configuration.group.GroupResourceConfigurationHistoryUIBean;
import org.rhq.enterprise.gui.configuration.history.ListConfigurationUpdateUIBean;
import org.rhq.enterprise.gui.content.AuditTrailUIBean;
import org.rhq.enterprise.gui.content.AvailableResourceReposUIBean;
import org.rhq.enterprise.gui.content.ContentSourceCandidateReposUIBean;
import org.rhq.enterprise.gui.content.ContentSourceReposUIBean;
import org.rhq.enterprise.gui.content.ContentSourceSyncResultsUIBean;
import org.rhq.enterprise.gui.content.DeployPackagesUIBean;
import org.rhq.enterprise.gui.content.ListCompletedContentRequestsUIBean;
import org.rhq.enterprise.gui.content.ListContentSourcesUIBean;
import org.rhq.enterprise.gui.content.ListInProgressContentRequestsUIBean;
import org.rhq.enterprise.gui.content.ListPackageHistoryUIBean;
import org.rhq.enterprise.gui.content.ListPackageTypesUIBean;
import org.rhq.enterprise.gui.content.ListPackagesUIBean;
import org.rhq.enterprise.gui.content.ListReposUIBean;
import org.rhq.enterprise.gui.content.RepoAssociationsUIBean;
import org.rhq.enterprise.gui.content.RepoContentSourcesUIBean;
import org.rhq.enterprise.gui.content.RepoDisassociationsUIBean;
import org.rhq.enterprise.gui.content.RepoDistributionUIBean;
import org.rhq.enterprise.gui.content.RepoImportUIBean;
import org.rhq.enterprise.gui.content.RepoPackageVersionsUIBean;
import org.rhq.enterprise.gui.content.RepoResourcesUIBean;
import org.rhq.enterprise.gui.content.RepoSubscriptionsUIBean;
import org.rhq.enterprise.gui.content.RepoUnsubscriptionsUIBean;
import org.rhq.enterprise.gui.content.ResourcePackageVersionsUIBean;
import org.rhq.enterprise.gui.content.ResourceReposUIBean;
import org.rhq.enterprise.gui.content.ShowContentServiceRequestUIBean;
import org.rhq.enterprise.gui.definition.group.GroupDefinitionUIBean;
import org.rhq.enterprise.gui.definition.group.ListGroupDefinitionsUIBean;
import org.rhq.enterprise.gui.discovery.AutoDiscoveryQueueUIBean;
import org.rhq.enterprise.gui.event.EventHistoryUIBean;
import org.rhq.enterprise.gui.ha.AffinityGroupSubscribedAgentsUIBean;
import org.rhq.enterprise.gui.ha.AffinityGroupSubscribedServersUIBean;
import org.rhq.enterprise.gui.ha.AffinityGroupUnsubscribedAgentsUIBean;
import org.rhq.enterprise.gui.ha.AffinityGroupUnsubscribedServersUIBean;
import org.rhq.enterprise.gui.ha.ListAffinityGroupsUIBean;
import org.rhq.enterprise.gui.ha.ListAgentsUIBean;
import org.rhq.enterprise.gui.ha.ListPartitionEventsUIBean;
import org.rhq.enterprise.gui.ha.ListServersUIBean;
import org.rhq.enterprise.gui.ha.ViewAffinityGroupAgentMembersUIBean;
import org.rhq.enterprise.gui.ha.ViewAffinityGroupServerMembersUIBean;
import org.rhq.enterprise.gui.ha.ViewAgentUIBean;
import org.rhq.enterprise.gui.ha.ViewPartitionEventUIBean;
import org.rhq.enterprise.gui.ha.ViewServerUIBean;
import org.rhq.enterprise.gui.inventory.group.ListResourceGroupMembersUIBean;
import org.rhq.enterprise.gui.inventory.group.ResourceGroupSummaryUIBean;
import org.rhq.enterprise.gui.inventory.group.ViewGroupConnectionPropertyDetailsUIBean;
import org.rhq.enterprise.gui.inventory.group.ViewGroupConnectionPropertyHistoryUIBean;
import org.rhq.enterprise.gui.inventory.resource.ListChildResourcesUIBean;
import org.rhq.enterprise.gui.inventory.resource.ListContainingGroupsUIBean;
import org.rhq.enterprise.gui.inventory.resource.ListCreateResourceHistoryUIBean;
import org.rhq.enterprise.gui.inventory.resource.ListDeleteResourceHistoryUIBean;
import org.rhq.enterprise.gui.inventory.resource.history.ListAvailabilityHistoryUIBean;
import org.rhq.enterprise.gui.inventory.resource.history.ListPluginConfigurationUpdateUIBean;
import org.rhq.enterprise.gui.measurement.calltime.CallTimeUIBean;
import org.rhq.enterprise.gui.measurement.schedule.group.ListResourceGroupMeasurementScheduleUIBean;
import org.rhq.enterprise.gui.measurement.schedule.resource.ListResourceMeasurementScheduleUIBean;
import org.rhq.enterprise.gui.measurement.tables.group.ResourceGroupMetricsTableUIBean;
import org.rhq.enterprise.gui.operation.history.group.ResourceGroupOperationCompletedHistoryUIBean;
import org.rhq.enterprise.gui.operation.history.group.ResourceGroupOperationHistoryDetailsUIBean;
import org.rhq.enterprise.gui.operation.history.group.ResourceGroupOperationPendingHistoryUIBean;
import org.rhq.enterprise.gui.operation.history.resource.ResourceOperationCompletedHistoryUIBean;
import org.rhq.enterprise.gui.operation.history.resource.ResourceOperationPendingHistoryUIBean;
import org.rhq.enterprise.gui.subsystem.SubsystemAlertDefinitionUIBean;
import org.rhq.enterprise.gui.subsystem.SubsystemAlertHistoryUIBean;
import org.rhq.enterprise.gui.subsystem.SubsystemConfigurationUpdateUIBean;
import org.rhq.enterprise.gui.subsystem.SubsystemOOBHistoryUIBean;
import org.rhq.enterprise.gui.subsystem.SubsystemOperationHistoryUIBean;

/**
 * @author Joseph Marques
 */
public enum PageControlView {
    // Use this across any view that doesn't want paging of tabular data

    NONE, // this is deprecated in favor of using an unlimited PageControlView

    // Configuration

    /** */
    ConfigurationHistory(ListConfigurationUpdateUIBean.class),
    /** */
    PluginConfigurationHistory(ListPluginConfigurationUpdateUIBean.class),

    // Content

    /** */
    InstalledPackagesList(ListPackagesUIBean.class),
    /** */
    AllPackageVersionsList(ListPackageHistoryUIBean.class),
    /** */
    PackageTypesList(ListPackageTypesUIBean.class),
    /** */
    ContentInProgressRequestsList(ListInProgressContentRequestsUIBean.class),
    /** */
    ContentCompletedRequestsList(ListCompletedContentRequestsUIBean.class),
    /** */
    ContentSourcesList(ListContentSourcesUIBean.class),
    /** */
    ReposToImportList(RepoImportUIBean.class),
    /** */
    ReposList(ListReposUIBean.class),
    /** */
    ContentSourceSyncResultsList(ContentSourceSyncResultsUIBean.class),
    /** */
    ContentSourceReposList(ContentSourceReposUIBean.class),
    /** */
    RepoContentSourcesList(RepoContentSourcesUIBean.class),
    /** */
    RepoResourcesList(RepoResourcesUIBean.class),
    /** */
    ContentSourceCandidateReposList(ContentSourceCandidateReposUIBean.class),
    /** */
    RepoPackageVersionsList(RepoPackageVersionsUIBean.class),
    /** */
    RepoDistributionList(RepoDistributionUIBean.class),
    /** */
    RepoAssociationsList(RepoAssociationsUIBean.class),
    /** */
    RepoDisassociationsList(RepoDisassociationsUIBean.class),
    /** */
    RepoSubscriptionsList(RepoSubscriptionsUIBean.class),
    /** */
    RepoUnsubscriptionsList(RepoUnsubscriptionsUIBean.class),
    /** */
    ResourceReposList(ResourceReposUIBean.class),
    /** */
    AvailableResourceReposList(AvailableResourceReposUIBean.class),
    /** */
    ResourcePackageVersionsList(ResourcePackageVersionsUIBean.class),
    /** */
    PackagesToDeployList(DeployPackagesUIBean.class),
    /** */
    InstalledPackageHistoryList(ShowContentServiceRequestUIBean.class),
    /** */
    AuditTrailList(AuditTrailUIBean.class),

    /** */
    //AvailableResourcePackageVersionList(AvailableResourceReposUIBean.class),
    // Alerts
    /** */
    AlertDefinitionsList(ListAlertDefinitionsUIBean.class),
    /** */
    AlertHistoryList(ListAlertHistoryUIBean.class),
    /** */
    GroupAlertDefinitionsList(ListGroupAlertDefinitionsUIBean.class),
    /** */
    GroupAlertHistoryList(ListGroupAlertHistoryUIBean.class),
    /** */
    AlertTemplatesList(ListAlertTemplatesUIBean.class),

    // Resource Control

    /** */
    ResourceOperationCompletedHistory(ResourceOperationCompletedHistoryUIBean.class),
    /** */
    ResourceOperationPendingHistory(ResourceOperationPendingHistoryUIBean.class),

    // Resource Group Control

    /** */
    ResourceGroupOperationCompletedHistory(ResourceGroupOperationCompletedHistoryUIBean.class),
    /** */
    ResourceGroupOperationPendingHistory(ResourceGroupOperationPendingHistoryUIBean.class),
    /** */
    ResourceGroupOperationHistoryDetails(ResourceGroupOperationHistoryDetailsUIBean.class),

    // Group Definition

    /** */
    GroupDefinitionsList(ListGroupDefinitionsUIBean.class),
    /** */
    GroupDefinitionMembers(GroupDefinitionUIBean.class),

    // Inventory

    /** */
    ChildResourcesList(ListChildResourcesUIBean.class),
    /** */
    CreateResourceHistory(ListCreateResourceHistoryUIBean.class),
    /** */
    DeleteResourceHistory(ListDeleteResourceHistoryUIBean.class),
    /** */
    ContainingGroupsList(ListContainingGroupsUIBean.class),

    // Group Inventory

    /** */
    ResourceGroupMemberList(ListResourceGroupMembersUIBean.class),

    MiniResourceGroupMemberList(ResourceGroupSummaryUIBean.class),
    /** */
    GroupConnectionPropertyUpdateHistory(ViewGroupConnectionPropertyHistoryUIBean.class),
    /** */
    GroupConnectionPropertyUpdateDetails(ViewGroupConnectionPropertyDetailsUIBean.class),
    /** */
    ResourceGroupHealthSummary(ResourceGroupMetricsTableUIBean.class),

    // Group Configuration
    /** */
    GroupResourceConfigurationUpdateHistory(GroupResourceConfigurationHistoryUIBean.class),
    /** */
    GroupResourceConfigurationUpdateDetails(GroupResourceConfigurationHistoryDetailsUIBean.class),

    // Auto Discovery

    /** */
    AutoDiscoveryPlatformList(AutoDiscoveryQueueUIBean.class),

    // High Availability (HA)

    /** */
    ServersList(ListServersUIBean.class),
    /** */
    ServerConnectedAgentsView(ViewServerUIBean.class),
    /** */
    AgentsList(ListAgentsUIBean.class),
    /** */
    ListAffinityGroups(ListAffinityGroupsUIBean.class),
    /** */
    AffinityGroupSubscribedAgents(AffinityGroupSubscribedAgentsUIBean.class),
    /** */
    AffinityGroupUnsubscribedAgents(AffinityGroupUnsubscribedAgentsUIBean.class),
    /** */
    AffinityGroupSubscribedServers(AffinityGroupSubscribedServersUIBean.class),
    /** */
    AffinityGroupUnsubscribedServers(AffinityGroupUnsubscribedServersUIBean.class),
    /** */
    AffinityGroupAgentMembersView(ViewAffinityGroupAgentMembersUIBean.class),
    /** */
    AffinityGroupServerMembersView(ViewAffinityGroupServerMembersUIBean.class),
    /** */
    AgentFailoverListView(ViewAgentUIBean.class),
    /** */
    ListPartitionEventsView(ListPartitionEventsUIBean.class),
    /** */
    PartitionEventsDetailsView(ViewPartitionEventUIBean.class),

    // Monitor

    /** */
    AutoGroupMeasurementScheduleList(ListResourceGroupMeasurementScheduleUIBean.class, true),
    /** */
    ResourceMeasurementScheduleList(ListResourceMeasurementScheduleUIBean.class, true),
    /** */
    ResourceGroupMeasurementScheduleList(ListResourceGroupMeasurementScheduleUIBean.class, true),
    /** */
    EventsHistoryList(EventHistoryUIBean.class),
    /** */
    CallTimeHistory(CallTimeUIBean.class),

    AvailabilityHistoryList(ListAvailabilityHistoryUIBean.class),

    // Subsystem Views

    /** */
    SubsystemConfigurationHistory(SubsystemConfigurationUpdateUIBean.class),
    /** */
    SubsystemResourceOperationHistory(SubsystemOperationHistoryUIBean.class),
    /** */
    SubsystemAlertHistory(SubsystemAlertHistoryUIBean.class),
    /** */
    SubsystemAlertDefinition(SubsystemAlertDefinitionUIBean.class),
    /** */
    SubsystemOOBHistory(SubsystemOOBHistoryUIBean.class);

    private Class<? extends PagedDataTableUIBean> beanClass;
    private boolean isUnlimited = false;

    private PageControlView() {
        this.beanClass = null;
    }

    private <T extends PagedDataTableUIBean> PageControlView(Class<T> beanClass) {
        this.beanClass = beanClass;
    }

    private <T extends PagedDataTableUIBean> PageControlView(Class<T> beanClass, boolean isUnlimited) {
        this.beanClass = beanClass;
        this.isUnlimited = isUnlimited;
    }

    public PagedDataTableUIBean getPagedDataTableUIBean() {
        PagedDataTableUIBean uiBean = FacesContextUtility.getBean(beanClass);
        return uiBean;
    }

    public boolean isUnlimited() {
        return isUnlimited;
    }

    public String toString() {
        return PageControlView.class.getSimpleName() + "["
            + (beanClass == null ? "unlimited" : beanClass.getSimpleName()) + "]";
    }
}
