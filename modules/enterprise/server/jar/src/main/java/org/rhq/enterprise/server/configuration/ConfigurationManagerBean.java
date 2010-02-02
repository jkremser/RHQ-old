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
package org.rhq.enterprise.server.configuration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebParam;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.group.AbstractGroupConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.auth.prefs.SubjectPreferences;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.configuration.job.AbstractGroupConfigurationUpdateJob;
import org.rhq.enterprise.server.configuration.job.GroupPluginConfigurationUpdateJob;
import org.rhq.enterprise.server.configuration.job.GroupResourceConfigurationUpdateJob;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.jaxb.WebServiceTypeAdapter;
import org.rhq.enterprise.server.jaxb.adapter.ConfigurationAdapter;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.system.ServerVersion;
import org.rhq.enterprise.server.util.QuartzUtil;

/**
 * The manager responsible for working with resource and plugin configurations.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Stateless
@XmlType(namespace = ServerVersion.namespace)
public class ConfigurationManagerBean implements ConfigurationManagerLocal, ConfigurationManagerRemote {
    private final Log log = LogFactory.getLog(ConfigurationManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private ResourceManagerLocal resourceManager;
    @EJB
    private ConfigurationManagerLocal configurationManager; // yes, this is ourself
    @EJB
    private SchedulerLocal scheduler;
    @EJB
    private SubjectManagerLocal subjectManager;

    @Nullable
    public @XmlJavaTypeAdapter(ConfigurationAdapter.class)
    Configuration getPluginConfiguration(Subject subject, int resourceId) {
        log.debug("Getting current plugin configuration for resource [" + resourceId + "]");

        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new IllegalStateException("Cannot retrieve plugin config for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view plugin configuration for [" + resource + "]");
        }

        Configuration pluginConfiguration = configurationManager.getPluginConfiguration(resourceId);

        return pluginConfiguration;
    }

    // Use new transaction because this only works if the resource in question has not
    // yet been loaded by Hibernate.  We want the query to return a non-proxied configuration,
    // this is critical for remote API use.
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Configuration getPluginConfiguration(int resourceId) {
        // Ensure that we return a non-proxied Configuration object that can survive after the
        // Hibernate session goes away.
        Query query = entityManager.createNamedQuery(Configuration.QUERY_GET_PLUGIN_CONFIG_BY_RESOURCE_ID);
        query.setParameter("resourceId", resourceId);
        Configuration result = (Configuration) query.getSingleResult();

        return result;
    }

    public void completePluginConfigurationUpdate(Integer updateId) {
        PluginConfigurationUpdate update = entityManager.find(PluginConfigurationUpdate.class, updateId);
        configurationManager.completePluginConfigurationUpdate(update);
    }

    /* 
     * this method will not fire off the update asynchronously (like the completeResourceConfigurationUpdate method
     * does); instead, it will block until an update response is retrieved from the agent-side resource 
     */
    public void completePluginConfigurationUpdate(PluginConfigurationUpdate update) {
        // use EJB3 reference to ourself so that transaction semantics are correct
        ConfigurationUpdateResponse response = configurationManager.executePluginConfigurationUpdate(update);
        Resource resource = update.getResource();

        // link to the newer, persisted configuration object -- regardless of errors
        resource.setPluginConfiguration(update.getConfiguration());

        if (response.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
            update.setStatus(ConfigurationUpdateStatus.SUCCESS);

            resource.setConnected(true);

            removeAnyExistingInvalidPluginConfigurationErrors(subjectManager.getOverlord(), resource);
            // Flush before merging to ensure the update has been persisted and avoid StaleStateExceptions.
            entityManager.flush();
            entityManager.merge(update);

        } else {
            handlePluginConfiguratonUpdateRemoteException(resource, response.getStatus().toString(), response
                .getErrorMessage());

            update.setStatus(response.getStatus());
            update.setErrorMessage(response.getErrorMessage());
        }
    }

    // use requires new so that exceptions bubbling up from the agent.updatePluginConfiguration don't force callers to rollback as well
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ConfigurationUpdateResponse executePluginConfigurationUpdate(PluginConfigurationUpdate update) {
        Resource resource = update.getResource();
        Configuration configuration = update.getConfiguration();

        ConfigurationUpdateResponse response = null;

        try {
            // now let's tell the agent to actually update the resource component's plugin configuration
            AgentClient agentClient = this.agentManager.getAgentClient(resource.getAgent());

            agentClient.getDiscoveryAgentService().updatePluginConfiguration(resource.getId(), configuration);
            try {
                agentClient.getDiscoveryAgentService().executeServiceScanDeferred();
            } catch (Exception e) {
                log.warn("Failed to execute service scan - cannot detect children of the newly connected resource ["
                    + resource + "]", e);
            }

            response = new ConfigurationUpdateResponse(update.getId(), null, ConfigurationUpdateStatus.SUCCESS, null);
        } catch (Exception e) {
            response = new ConfigurationUpdateResponse(update.getId(), null, e);
        }

        return response;
    }

    public PluginConfigurationUpdate updatePluginConfiguration(Subject subject, int resourceId,
        @XmlJavaTypeAdapter(ConfigurationAdapter.class) Configuration configuration) throws ResourceNotFoundException {
        Subject overlord = subjectManager.getOverlord();
        Resource resource = resourceManager.getResourceById(overlord, resourceId);

        // make sure the user has the proper permissions to do this
        ensureModifyPermission(subject, resource);

        // create our new update request and assign it to our resource - its status will initially be "in progress"
        PluginConfigurationUpdate update = new PluginConfigurationUpdate(resource, configuration, subject.getName());

        update.setStatus(ConfigurationUpdateStatus.SUCCESS);
        entityManager.persist(update);

        resource.addPluginConfigurationUpdates(update);

        // agent field is LAZY - force it to load because the caller will need it.
        Agent agent = resource.getAgent();
        if (agent != null) {
            agent.getName();
        }

        configurationManager.completePluginConfigurationUpdate(update);

        return update;
    }

    public @XmlJavaTypeAdapter(ConfigurationAdapter.class)
    Configuration getResourceConfiguration(Subject subject, int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        Configuration result = configurationManager.getResourceConfiguration(resourceId);

        return result;
    }

    // Use new transaction because this only works if the resource in question has not
    // yet been loaded by Hibernate.  We want the query to return a non-proxied configuration,
    // this is critical for remote API use.
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Configuration getResourceConfiguration(int resourceId) {
        // Ensure that we return a non-proxied Configuration object that can survive after the
        // Hibernate session goes away.
        Query query = entityManager.createNamedQuery(Configuration.QUERY_GET_RESOURCE_CONFIG_BY_RESOURCE_ID);
        query.setParameter("resourceId", resourceId);
        Configuration result = (Configuration) query.getSingleResult();

        return result;
    }

    public ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId,
        boolean fromStructured) {
        log.debug("Getting current resource configuration for resource [" + resourceId + "]");

        Resource resource;
        ResourceConfigurationUpdate current;

        // Get the latest configuration as known to the server (i.e. persisted in the DB).
        try {
            Query query = entityManager
                .createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG);
            query.setParameter("resourceId", resourceId);
            current = (ResourceConfigurationUpdate) query.getSingleResult();
            resource = current.getResource();
        } catch (NoResultException nre) {
            current = null; // The resource hasn't been successfully configured yet.

            // We still need the resource, so we can get its agent.
            resource = entityManager.find(Resource.class, resourceId);
            if (resource == null) {
                throw new NoResultException("Cannot get latest resource configuration for unknown resource ["
                    + resourceId + "]");
            }
        }

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        // Check whether or not a resource configuration update is currently in progress.
        ResourceConfigurationUpdate latest;
        try {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            latest = (ResourceConfigurationUpdate) query.getSingleResult();
            if (latest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) {
                // The agent is in the process of a config update, so we do not want to ask it for the live config.
                // Instead, simply return the most recent persisted config w/ a SUCCESS status (possibly null).
                return current;
            }
        } catch (NoResultException nre) {
            // The resource hasn't been successfully configured yet - not a problem, we'll ask the agent for the live
            // config...
        }

        // ask the agent to get us the live, most up-to-date configuration for the resource,
        // then compare it to make sure what we think is the latest configuration is really the latest
        Configuration liveConfig = getLiveResourceConfiguration(resource, true, fromStructured);

        if (liveConfig != null) {
            Configuration currentConfig = (current != null) ? current.getConfiguration() : null;
            // Compare the live values and, if there is a difference with the current, store the live config as a new
            // update. Note that, if there is no current configuration stored, the live config is stored as the first
            // update.
            boolean theSame = (currentConfig != null && currentConfig.equals(liveConfig));

            // Someone dorked with the configuration on the agent side - save the live config as a new update.
            if (!theSame) {
                try {
                    current = persistNewAgentReportedResourceConfiguration(resource, liveConfig);
                } catch (ConfigurationUpdateStillInProgressException e) {
                    // This means a config update is INPROGRESS.
                    // Return the current in this case since it is our latest committed active config.
                    // Note that even though this application exception specifies "rollback=true", it will
                    // not effect our current transaction since the persist call was made with REQUIRES_NEW
                    // and thus only that new tx was rolled back
                    log.debug("Resource is currently in progress of changing its resource configuration - "
                        + "since it hasn't finished yet, will use the last successful resource configuration: " + e);
                }
            }
        } else {
            log.warn("Could not get live resource configuration for resource [" + resource
                + "]; will assume latest resource configuration update is the current resource configuration.");
        }

        return current;
    }

    @Nullable
    public ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId) {
        return getLatestResourceConfigurationUpdate(subject, resourceId, true);
    }

    private ResourceConfigurationUpdate persistNewAgentReportedResourceConfiguration(Resource resource,
        Configuration liveConfig) throws ConfigurationUpdateStillInProgressException {
        /*
        * NOTE: We pass the overlord, since this is a system side-effect.  here, the system
        * and *not* the user, is choosing to persist the most recent configuration because it was different
        * from the last known value.  again, the user isn't attempting to change the value; instead, *JON*
        * is triggering save based on the semantics that we want to provide for configuration updates.
        * For the same reason, we pass null as the subject.
        */
        ResourceConfigurationUpdate update = this.configurationManager.persistNewResourceConfigurationUpdateHistory(
            this.subjectManager.getOverlord(), resource.getId(), liveConfig, ConfigurationUpdateStatus.SUCCESS, null,
            false);
        //resource.setResourceConfiguration(liveConfig.deepCopy(false));
        resource.setResourceConfiguration(liveConfig.deepCopyWithoutProxies());
        return update;
    }

    public PluginConfigurationUpdate getLatestPluginConfigurationUpdate(Subject subject, int resourceId) {
        log.debug("Getting current plugin configuration for resource [" + resourceId + "]");

        Resource resource;
        PluginConfigurationUpdate current;

        // Get the latest configuration as known to the server (i.e. persisted in the DB).
        try {
            Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG);
            query.setParameter("resourceId", resourceId);
            current = (PluginConfigurationUpdate) query.getSingleResult();
            resource = current.getResource();
        } catch (NoResultException nre) {
            current = null; // The resource hasn't been successfully configured yet.

            // We still need the resource, so we can get its agent.
            resource = entityManager.find(Resource.class, resourceId);
            if (resource == null) {
                throw new NoResultException("Cannot get latest plugin configuration for unknown resource ["
                    + resourceId + "]");
            }
        }

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view plugin configuration for [" + resource + "]");
        }

        return current;
    }

    public boolean isResourceConfigurationUpdateInProgress(Subject subject, int resourceId) {
        boolean updateInProgress;
        try {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            ResourceConfigurationUpdate latestConfigUpdate = (ResourceConfigurationUpdate) query.getSingleResult();
            if (!authorizationManager.canViewResource(subject, latestConfigUpdate.getResource().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view Resource configuration for ["
                    + latestConfigUpdate.getResource() + "]");
            }
            updateInProgress = (latestConfigUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }
        return updateInProgress;
    }

    public boolean isPluginConfigurationUpdateInProgress(Subject subject, int resourceId) {
        boolean updateInProgress;
        try {
            Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            PluginConfigurationUpdate latestConfigUpdate = (PluginConfigurationUpdate) query.getSingleResult();
            if (!authorizationManager.canViewResource(subject, latestConfigUpdate.getResource().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view plugin configuration for ["
                    + latestConfigUpdate.getResource() + "]");
            }
            updateInProgress = (latestConfigUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }
        return updateInProgress;
    }

    public boolean isGroupResourceConfigurationUpdateInProgress(Subject subject, int groupId) {
        boolean updateInProgress;
        try {
            Query query = entityManager
                .createNamedQuery(GroupResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID);
            query.setParameter("groupId", groupId);
            GroupResourceConfigurationUpdate latestConfigGroupUpdate = (GroupResourceConfigurationUpdate) query
                .getSingleResult();
            if (!authorizationManager.canViewGroup(subject, latestConfigGroupUpdate.getGroup().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view group Resource configuration for ["
                    + latestConfigGroupUpdate.getGroup() + "]");
            }

            updateInProgress = (latestConfigGroupUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The group resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }

        return updateInProgress;
    }

    public boolean isGroupPluginConfigurationUpdateInProgress(Subject subject, int groupId) {
        boolean updateInProgress;
        try {
            Query query = entityManager.createNamedQuery(GroupPluginConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID);
            query.setParameter("groupId", groupId);
            GroupPluginConfigurationUpdate latestConfigGroupUpdate = (GroupPluginConfigurationUpdate) query
                .getSingleResult();
            if (!authorizationManager.canViewGroup(subject, latestConfigGroupUpdate.getGroup().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view group plugin configuration for ["
                    + latestConfigGroupUpdate.getGroup() + "]");
            }

            updateInProgress = (latestConfigGroupUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The group resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }

        return updateInProgress;
    }

    public Map<Integer, Configuration> getResourceConfigurationsForCompatibleGroup(Subject subject, int groupId)
        throws ConfigurationUpdateStillInProgressException, Exception {
        // The below call will also handle the check to see if the subject has perms to view the group.
        ResourceGroupComposite groupComposite = this.resourceGroupManager.getResourceGroupComposite(subject, groupId);

        // if the group is empty (has no members) the availability will be null
        if (groupComposite.getExplicitAvail() == null) {
            return new HashMap<Integer, Configuration>();
        }

        AvailabilityType availability = (groupComposite.getExplicitAvail() == 1) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
        if (availability == AvailabilityType.DOWN)
            throw new Exception("Current group Resource configuration for " + groupId
                + " cannot be calculated, because one or more of this group's member Resources are DOWN.");

        // If we got this far, all member Resources are UP. Now check to make sure no config updates, group-level or
        // resource-level, are in progress.
        ResourceGroup group = groupComposite.getResourceGroup();
        ensureNoResourceConfigurationUpdatesInProgress(group);

        // If we got this far, no updates are in progress. Now try to obtain the live configs from the Agents.
        // If any of the requests for live configs fail (e.g. because an Agent is down) or if all of the live
        // configs can't be obtained within the specified timeout, this call will throw an exception.
        int userPreferencesTimeout = new SubjectPreferences(subject).getGroupConfigurationTimeoutPeriod();
        Set<Resource> groupMembers = group.getExplicitResources();
        Map<Integer, Configuration> liveConfigs = LiveConfigurationLoader.getInstance().loadLiveResourceConfigurations(
            groupMembers, userPreferencesTimeout);

        // If we got this far, we were able to retrieve all of the live configs from the Agents. Now load the current
        // persisted configs from the DB and compare them to the corresponding live configs. For any that are not equal,
        // persist the live config to the DB as the new current config.
        Map<Integer, Configuration> currentPersistedConfigs = getPersistedResourceConfigurationsForCompatibleGroup(group);
        for (Resource memberResource : groupMembers) {
            Configuration liveConfig = liveConfigs.get(memberResource.getId());
            // NOTE: The persisted config may be null if no config has been persisted yet.
            Configuration currentPersistedConfig = currentPersistedConfigs.get(memberResource.getId());
            if (!liveConfig.equals(currentPersistedConfig)) {
                // If the live config is different than the persisted config, persist it as the new current config.
                ResourceConfigurationUpdate update = persistNewAgentReportedResourceConfiguration(memberResource,
                    liveConfig);
                if (update != null)
                    currentPersistedConfigs.put(memberResource.getId(), update.getConfiguration());
                else
                    log.error("Current Configuration for " + memberResource
                        + " does not match latest associated ResourceConfigurationUpdate with SUCCESS status.");
            }
        }
        return currentPersistedConfigs;
    }

    public Map<Integer, Configuration> getPluginConfigurationsForCompatibleGroup(Subject subject, int groupId)
        throws ConfigurationUpdateStillInProgressException, Exception {
        // The below call will also handle the check to see if the subject has perms to view the group.
        ResourceGroup group = this.resourceGroupManager
            .getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);

        // Check to make sure no config updates, group-level or resource-level, are in progress.
        ensureNoPluginConfigurationUpdatesInProgress(group);

        // If we got this far, no updates are in progress, so go ahead and load the plugin configs from the DB.
        Map<Integer, Configuration> currentPersistedConfigs = getPersistedPluginConfigurationsForCompatibleGroup(group);

        return currentPersistedConfigs;
    }

    @SuppressWarnings("unchecked")
    private void ensureNoResourceConfigurationUpdatesInProgress(ResourceGroup compatibleGroup)
        throws ConfigurationUpdateStillInProgressException {
        if (isGroupResourceConfigurationUpdateInProgress(this.subjectManager.getOverlord(), compatibleGroup.getId())) {
            throw new ConfigurationUpdateStillInProgressException("Current group Resource configuration for "
                + compatibleGroup
                + " cannot be calculated, because a group Resource configuration update is currently in progress "
                + " (please wait for this update to complete or delete it from the history).");
        }

        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID_AND_STATUS);
        countQuery.setParameter("groupId", compatibleGroup.getId());
        countQuery.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
        long count = (Long) countQuery.getSingleResult();
        if (count != 0) {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID_AND_STATUS);
            query.setParameter("groupId", compatibleGroup.getId());
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<Resource> resources = query.getResultList();
            throw new ConfigurationUpdateStillInProgressException("Current group Resource configuration for "
                + compatibleGroup
                + " cannot be calculated, because Resource configuration updates are currently in progress for the"
                + " following Resources (please wait for these updates to complete or delete them from the history): "
                + resources);
        }
    }

    private void ensureNoPluginConfigurationUpdatesInProgress(ResourceGroup compatibleGroup)
        throws ConfigurationUpdateStillInProgressException {
        if (isGroupPluginConfigurationUpdateInProgress(this.subjectManager.getOverlord(), compatibleGroup.getId())) {
            throw new ConfigurationUpdateStillInProgressException("Current group plugin configuration for "
                + compatibleGroup
                + " cannot be calculated, because a group plugin configuration update is currently in progress.");
        }
        List<Resource> resourcesWithPluginConfigUpdatesInProgress = new ArrayList<Resource>();
        for (Resource memberResource : compatibleGroup.getExplicitResources()) {
            if (isPluginConfigurationUpdateInProgress(this.subjectManager.getOverlord(), memberResource.getId()))
                resourcesWithPluginConfigUpdatesInProgress.add(memberResource);
        }
        if (!resourcesWithPluginConfigUpdatesInProgress.isEmpty())
            throw new ConfigurationUpdateStillInProgressException(
                "Current group plugin configuration for "
                    + compatibleGroup
                    + " cannot be calculated, because plugin configuration updates are currently in progress for the following Resources: "
                    + resourcesWithPluginConfigUpdatesInProgress);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Configuration> getPersistedResourceConfigurationsForCompatibleGroup(
        ResourceGroup compatibleGroup) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID);
        countQuery.setParameter("resourceGroupId", compatibleGroup.getId());
        long count = (Long) countQuery.getSingleResult();
        if (count != compatibleGroup.getExplicitResources().size())
            throw new IllegalStateException("Size of group changed from "
                + compatibleGroup.getExplicitResources().size() + " to " + count + " - please retry the operation.");

        // Configurations are very expensive to load, so load 'em in chunks to ease the strain on the DB.
        PageControl pageControl = new PageControl(0, 10);
        Query query = entityManager.createNamedQuery(Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID);
        query.setParameter("resourceGroupId", compatibleGroup.getId());

        Map<Integer, Configuration> results = new HashMap<Integer, Configuration>((int) count);
        int rowsProcessed = 0;
        while (true) {
            PersistenceUtility.setDataPage(query, pageControl); // retrieve one page at a time
            List<Object[]> pagedResults = query.getResultList();

            if (pagedResults.size() <= 0)
                break;

            for (Object[] result : pagedResults)
                results.put((Integer) result[0], (Configuration) result[1]);

            rowsProcessed += pagedResults.size();
            if (rowsProcessed >= count)
                break;

            pageControl.setPageNumber(pageControl.getPageNumber() + 1); // advance the page
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Configuration> getPersistedPluginConfigurationsForCompatibleGroup(ResourceGroup compatibleGroup) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID);
        countQuery.setParameter("resourceGroupId", compatibleGroup.getId());
        long count = (Long) countQuery.getSingleResult();
        if (count != compatibleGroup.getExplicitResources().size())
            throw new IllegalStateException("Size of group changed from "
                + compatibleGroup.getExplicitResources().size() + " to " + count + " - please retry the operation.");

        // Configurations are very expensive to load, so load 'em in chunks to ease the strain on the DB.
        PageControl pageControl = new PageControl(0, 20);
        Query query = entityManager.createNamedQuery(Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID);
        query.setParameter("resourceGroupId", compatibleGroup.getId());

        Map<Integer, Configuration> results = new HashMap<Integer, Configuration>((int) count);
        int rowsProcessed = 0;
        while (true) {
            List<Object[]> pagedResults = query.getResultList();

            if (pagedResults.size() <= 0)
                break;

            for (Object[] result : pagedResults)
                results.put((Integer) result[0], (Configuration) result[1]);

            rowsProcessed += pagedResults.size();
            if (rowsProcessed >= count)
                break;

            pageControl.setPageNumber(pageControl.getPageNumber() + 1); // advance the page
            PersistenceUtility.setDataPage(query, pageControl); // and update the query to retrieve the new page
        }
        return results;
    }

    public Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst)
        throws Exception {
        return getLiveResourceConfiguration(subject, resourceId, pingAgentFirst, true);
    }

    public Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst,
        boolean fromStructured) throws Exception {
        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        // ask the agent to get us the live, most up-to-date configuration for the resource
        Configuration liveConfig = getLiveResourceConfiguration(resource, pingAgentFirst, fromStructured);

        return liveConfig;
    }

    public void checkForTimedOutConfigurationUpdateRequests() {
        log.debug("Begin scanning for timed out configuration update requests");
        checkForTimedOutResourceConfigurationUpdateRequests();
        checkForTimedOutGroupResourceConfigurationUpdateRequests();
        log.debug("Finished scanning for timed out configuration update requests");
    }

    @SuppressWarnings("unchecked")
    private void checkForTimedOutResourceConfigurationUpdateRequests() {
        try {
            // TODO (ips): Optimize this so the query actually does the timeout check too,
            //             i.e. "WHERE cu.createdtime > :maxCreatedTime".
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS);
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<ResourceConfigurationUpdate> requests = query.getResultList();
            for (ResourceConfigurationUpdate request : requests) {
                // TODO [mazz]: should we make this configurable?
                long timeout = 1000L * 60 * 10; // 10 minutes - should be more than enough time

                long duration = request.getDuration();
                if (duration > timeout) {
                    log.info("Resource configuration update request seems to have been orphaned - timing it out: "
                        + request);
                    request.setErrorMessage("Timed out - did not complete after " + duration + " ms"
                        + " (the timeout period was " + timeout + " ms)");
                    request.setStatus(ConfigurationUpdateStatus.FAILURE);
                    // If it's part of a group update, check if all member updates of the group update have completed,
                    // and, if so, update the group update's status.
                    checkForCompletedGroupResourceConfigurationUpdate(request.getId());
                }
            }
        } catch (Throwable t) {
            log.error("Failed to check for timed out Resource configuration update requests. Cause: " + t);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkForTimedOutGroupResourceConfigurationUpdateRequests() {
        try {
            // TODO (ips): Optimize this so the query actually does the timeout check too,
            //             i.e. "WHERE cu.createdtime > :maxCreatedTime".
            Query query = entityManager.createNamedQuery(GroupResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS);
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<GroupResourceConfigurationUpdate> requests = query.getResultList();
            for (GroupResourceConfigurationUpdate request : requests) {
                // Note: Make this a little longer than the timeout used for individual Resource config updates
                //       (see checkForTimedOutResourceConfigurationUpdateRequests()), to ensure a group update never
                //       gets timed out before one or more of its member updates.
                long timeout = 1000L * 60 * 11; // 11 minutes

                long duration = request.getDuration();
                if (duration > timeout) {
                    log
                        .info("Group Resource configuration update request seems to have been orphaned - timing it out: "
                            + request);
                    request.setErrorMessage("Timed out - did not complete after " + duration + " ms"
                        + " (the timeout period was " + timeout + " ms)");
                    request.setStatus(ConfigurationUpdateStatus.FAILURE);
                }
            }
        } catch (Throwable t) {
            log.error("Failed to check for timed out group Resource configuration update requests. Cause: " + t);
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<PluginConfigurationUpdate> findPluginConfigurationUpdates(Subject subject, int resourceId,
        Long beginDate, Long endDate, PageControl pc) {

        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource.getResourceType().getPluginConfigurationDefinition() == null
            || resource.getResourceType().getPluginConfigurationDefinition().getPropertyDefinitions().isEmpty()) {
            return new PageList<PluginConfigurationUpdate>(pc);
        }

        pc.initDefaultOrderingField("cu.id", PageOrdering.DESC);

        String queryName = PluginConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        queryCount.setParameter("startTime", beginDate);
        query.setParameter("startTime", beginDate);

        queryCount.setParameter("endTime", endDate);
        query.setParameter("endTime", endDate);

        long totalCount = (Long) queryCount.getSingleResult();
        List<PluginConfigurationUpdate> updates = query.getResultList();

        if ((updates == null) || (updates.size() == 0)) {
            // there is no configuration yet - get the latest from the agent, if possible
            updates = new ArrayList<PluginConfigurationUpdate>();
            PluginConfigurationUpdate latest = getLatestPluginConfigurationUpdate(subject, resourceId);
            if (latest != null) {
                updates.add(latest);
            }
        } else if (updates.size() > 0) {
            resource = updates.get(0).getResource();
            if (!authorizationManager.canViewResource(subject, resource.getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view resource [" + resource + "]");
            }
        }

        return new PageList<PluginConfigurationUpdate>(updates, (int) totalCount, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(Subject subject, Integer resourceId,
        Long beginDate, Long endDate, boolean suppressOldest, PageControl pc) {

        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource[id=" + resourceId + "]");
        }

        Resource resource = entityManager.find(Resource.class, resourceId);

        pc.initDefaultOrderingField("cu.id", PageOrdering.DESC);

        String queryName = ResourceConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        queryCount.setParameter("startTime", beginDate);
        query.setParameter("startTime", beginDate);

        queryCount.setParameter("endTime", endDate);
        query.setParameter("endTime", endDate);

        int includeAll = suppressOldest ? 0 : 1;
        queryCount.setParameter("includeAll", includeAll);
        query.setParameter("includeAll", includeAll);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ResourceConfigurationUpdate> updates = query.getResultList();

        if (suppressOldest == false && updates.size() == 0) {
            // there is no configuration yet - get the latest from the agent, if possible
            updates = new ArrayList<ResourceConfigurationUpdate>();
            ResourceConfigurationUpdate latest = getLatestResourceConfigurationUpdate(subject, resourceId);
            if (latest != null) {
                updates.add(latest);
            }
        }

        return new PageList<ResourceConfigurationUpdate>(updates, (int) totalCount, pc);
    }

    public PluginConfigurationUpdate getPluginConfigurationUpdate(Subject subject, int configurationUpdateId) {
        PluginConfigurationUpdate update = entityManager.find(PluginConfigurationUpdate.class, configurationUpdateId);

        if (!authorizationManager.canViewResource(subject, update.getResource().getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view plugin configuration update for [" + update.getResource() + "]");
        }

        update.getConfiguration(); // this is EAGER loaded, so this really doesn't do anything

        return update;
    }

    public ResourceConfigurationUpdate getResourceConfigurationUpdate(Subject subject, int configurationUpdateId) {
        ResourceConfigurationUpdate update = entityManager.find(ResourceConfigurationUpdate.class,
            configurationUpdateId);

        if (!authorizationManager.canViewResource(subject, update.getResource().getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration update for [" + update.getResource() + "]");
        }

        update.getConfiguration(); // this is EAGER loaded, so this really doesn't do anything

        return update;
    }

    public void purgePluginConfigurationUpdate(Subject subject, int configurationUpdateId, boolean purgeInProgress) {
        PluginConfigurationUpdate doomedRequest = entityManager.find(PluginConfigurationUpdate.class,
            configurationUpdateId);

        if (doomedRequest == null) {
            log.debug("Asked to purge a non-existing config update request [" + configurationUpdateId + "]");
            return;
        }

        if ((doomedRequest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) && !purgeInProgress) {
            throw new IllegalStateException(
                "The update request is still in the in-progress state. Please wait for it to complete: "
                    + doomedRequest);
        }

        // make sure the user has the proper permissions to do this
        Resource resource = doomedRequest.getResource();
        if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to purge a plugin configuration update audit trail for resource ["
                + resource + "]");
        }

        resource.getPluginConfigurationUpdates().remove(doomedRequest);
        entityManager.remove(doomedRequest);
        entityManager.flush();

        return;
    }

    public void purgeResourceConfigurationUpdate(Subject subject, int configurationUpdateId, boolean purgeInProgress) {
        ResourceConfigurationUpdate doomedRequest = entityManager.find(ResourceConfigurationUpdate.class,
            configurationUpdateId);

        if (doomedRequest == null) {
            log.debug("Asked to purge a non-existing config update request [" + configurationUpdateId + "]");
            return;
        }

        if ((doomedRequest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) && !purgeInProgress) {
            throw new IllegalStateException(
                "The update request is still in the in-progress state. Please wait for it to complete: "
                    + doomedRequest);
        }

        // make sure the user has the proper permissions to do this
        Resource resource = doomedRequest.getResource();
        if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to purge a configuration update audit trail for resource [" + resource
                + "]");
        }

        resource.getResourceConfigurationUpdates().remove(doomedRequest);
        entityManager.remove(doomedRequest);
        entityManager.flush();

        return;
    }

    public void purgeResourceConfigurationUpdates(Subject subject, int[] configurationUpdateIds, boolean purgeInProgress) {
        if ((configurationUpdateIds == null) || (configurationUpdateIds.length == 0)) {
            return;
        }

        // TODO [mazz]: ugly - let's make this more efficient, just getting this to work first
        for (int configurationUpdateId : configurationUpdateIds) {
            purgeResourceConfigurationUpdate(subject, configurationUpdateId, purgeInProgress);
        }

        return;
    }

    public ResourceConfigurationUpdate updateStructuredOrRawConfiguration(Subject subject, int resourceId,
        Configuration newConfiguration, boolean fromStructured)
        throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException {

        Configuration configToUpdate = newConfiguration;

        if (isStructuredAndRawSupported(resourceId)) {
            configToUpdate = translateResourceConfiguration(subject, resourceId, newConfiguration,
            fromStructured);
        }

	if (isRawSupported(resourceId)){
	    try{
	    validateResourceConfiguration(subject, resourceId, newConfiguration,false);
	    } catch(PluginContainerException e){
		ResourceConfigurationUpdate response = new ResourceConfigurationUpdate(null, newConfiguration, subject.getName());
		response.setErrorMessage(e.getMessage());
		response.setStatus(ConfigurationUpdateStatus.UNSENT);
		return response;
	    } 
	}

        ResourceConfigurationUpdate newUpdate =
            configurationManager.persistNewResourceConfigurationUpdateHistory(subject, resourceId, configToUpdate,
                ConfigurationUpdateStatus.INPROGRESS, subject.getName(), false);
        executeResourceConfigurationUpdate(newUpdate, fromStructured);
        return newUpdate;
    }
    private void validateResourceConfiguration(Subject subject, int resourceId, Configuration configuration,boolean isStructured) throws PluginContainerException {
     Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }
        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }
        Agent agent = resource.getAgent();
        AgentClient agentClient = this.agentManager.getAgentClient(agent);
        ConfigurationAgentService configService = agentClient.getConfigurationAgentService();
        configService.validate(configuration, resourceId, isStructured);
    }

    private boolean isRawSupported(int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        ConfigurationDefinition configDef = resource.getResourceType().getResourceConfigurationDefinition();

        return (ConfigurationFormat.STRUCTURED_AND_RAW == configDef.getConfigurationFormat() || (ConfigurationFormat.RAW == configDef.getConfigurationFormat()));
    }


    private boolean isStructuredAndRawSupported(int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        ConfigurationDefinition configDef = resource.getResourceType().getResourceConfigurationDefinition();

        return ConfigurationFormat.STRUCTURED_AND_RAW == configDef.getConfigurationFormat();
    }

    @Nullable
    public ResourceConfigurationUpdate updateResourceConfiguration(Subject subject, int resourceId,
        @XmlJavaTypeAdapter(ConfigurationAdapter.class) Configuration newConfiguration)
        throws ResourceNotFoundException {

        if (isStructuredAndRawSupported(resourceId)) {
            throw new ConfigurationUpdateNotSupportedException("Cannot update a resource configuration that " +
                "supports both structured and raw configuration using this method because there is insufficient " +
                "information. You should instead call updateStructuredOrRawConfiguration() which requires you " +
                "whether the structured or raw was updated.");
        }

        // must do this in a separate transaction so it is committed prior to sending the agent request
        // (consider synchronizing to avoid the condition where someone calls this method twice quickly
        // in two different txs which would put two updates in INPROGRESS and cause havoc)
        ResourceConfigurationUpdate newUpdate;
        // here we call ourself, but we do so via the EJB interface so we pick up the REQUIRES_NEW semantics
        // this can return null if newConfiguration is not actually different.
        newUpdate = configurationManager.persistNewResourceConfigurationUpdateHistory(subject, resourceId,
            newConfiguration, ConfigurationUpdateStatus.INPROGRESS, subject.getName(), false);

        executeResourceConfigurationUpdate(newUpdate, true);

        return newUpdate;
    }

    public void executeResourceConfigurationUpdate(int updateId) {
        ResourceConfigurationUpdate update = getResourceConfigurationUpdate(subjectManager.getOverlord(), updateId);
        executeResourceConfigurationUpdate(update, true);
    }

    /**
     * Tells the Agent to asynchonously update a managed resource's configuration as per the specified
     * <code>ResourceConfigurationUpdate</code>.
     */
    private void executeResourceConfigurationUpdate(ResourceConfigurationUpdate update, boolean fromStructured) {
        try {
            AgentClient agentClient = agentManager.getAgentClient(update.getResource().getAgent());
            ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(update.getId(), update
                .getConfiguration(), update.getResource().getId());
            agentClient.getConfigurationAgentService().updateResourceConfiguration(request);
        } catch (RuntimeException e) {
            // Any exception means the remote call itself failed - make sure to change the status on the update to FAILURE
            // and set its error message field.
            if (null != update) {
                update.setStatus(ConfigurationUpdateStatus.FAILURE);
                update.setErrorMessageFromThrowable(e);

                // here we call ourself, but we do so via the EJB interface so we pick up the REQUIRES_NEW semantics
                this.configurationManager.mergeConfigurationUpdate(update);
            }
        }
    }

    public void rollbackResourceConfiguration(Subject subject, int resourceId, int configHistoryId)
        throws ConfigurationUpdateException {
        ResourceConfigurationUpdate configurationUpdateHistory = entityManager.find(ResourceConfigurationUpdate.class,
            configHistoryId);
        Configuration configuration = configurationUpdateHistory.getConfiguration();
        if (configuration == null) {
            throw new ConfigurationUpdateException("No configuration history element exists with id = '"
                + configHistoryId + "'");
        }

        if (isStructuredAndRawSupported(resourceId)) {
            updateStructuredOrRawConfiguration(subject, resourceId, configuration, false);
        }
        else {
            updateResourceConfiguration(subject, resourceId, configuration);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResourceConfigurationUpdate persistNewResourceConfigurationUpdateHistory(Subject subject, int resourceId,
        Configuration newConfiguration, ConfigurationUpdateStatus newStatus, String newSubject,
        boolean isPartofGroupUpdate) throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException {
        // get the resource that we will be updating
        Resource resource = resourceManager.getResourceById(subject, resourceId);

        // make sure the user has the proper permissions to do this
        if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to modify configuration for resource [" + resource + "]");
        }

        // see if there was a previous update request and make sure it isn't still in progress
        List<ResourceConfigurationUpdate> previousRequests = resource.getResourceConfigurationUpdates();

        String errorMessage = null;
        if (previousRequests != null) {
            for (ResourceConfigurationUpdate previousRequest : previousRequests) {
                if (previousRequest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) {
                    // A previous update is still in progresss for this Resource. If this update is part of a group
                    // update, persist it with FAILURE status, so it is still listed as part of the group history.
                    // Otherwise, throw an exception that can bubble up to the GUI.
                    if (isPartofGroupUpdate) {
                        newStatus = ConfigurationUpdateStatus.FAILURE;
                        errorMessage = "Resource configuration Update was aborted because an update request for the Resource was already in progress.";
                    } else {
                        // NOTE: If you change this to another exception, make sure you change getLatestResourceConfigurationUpdate().
                        throw new ConfigurationUpdateStillInProgressException(
                            "Resource ["
                                + resource
                                + "] has a resource configuration update request already in progress - please wait for it to finish: "
                                + previousRequest);
                    }
                }
            }
        }

        ResourceConfigurationUpdate current;

        // Get the latest configuration as known to the server (i.e. persisted in the DB).
        try {
            Query query = entityManager
                .createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG);
            query.setParameter("resourceId", resourceId);
            current = (ResourceConfigurationUpdate) query.getSingleResult();
            resource = current.getResource();
        } catch (NoResultException nre) {
            current = null; // The resource hasn't been successfully configured yet.
        }

        // If this update is not part of an group update, don't bother persisting a new entry if the Configuration
        // hasn't changed. If it's part of an group update, persist a new entry no matter what, so the group
        // update isn't missing any member updates.
        if (!isPartofGroupUpdate && current != null && newConfiguration.equals(current.getConfiguration())) {
            return null;
        }

        //Configuration zeroedConfiguration = newConfiguration.deepCopy(false);
        Configuration zeroedConfiguration = newConfiguration.deepCopyWithoutProxies();

        // create our new update request and assign it to our resource - its status will initially be "in progress"
        ResourceConfigurationUpdate newUpdateRequest = new ResourceConfigurationUpdate(resource, zeroedConfiguration,
            newSubject);

        newUpdateRequest.setStatus(newStatus);
        if (newStatus == ConfigurationUpdateStatus.FAILURE) {
            newUpdateRequest.setErrorMessage(errorMessage);
        }

        entityManager.persist(newUpdateRequest);
        if (current != null) {
            if (newStatus == ConfigurationUpdateStatus.SUCCESS) {
                // If this is the first configuration update since the resource was imported, don't alert
                notifyAlertConditionCacheManager("persistNewResourceConfigurationUpdateHistory", newUpdateRequest);
            }
        }

        resource.addResourceConfigurationUpdates(newUpdateRequest);

        resource.getChildResources().size();

        // agent field is LAZY - force it to load because the caller will need it.
        Agent agent = resource.getAgent();
        if (agent != null) {
            agent.getName();
        }

        return newUpdateRequest;
    }

    private void notifyAlertConditionCacheManager(String callingMethod, ResourceConfigurationUpdate update) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(update);

        log.debug(callingMethod + ": " + stats);
    }

    public void completeResourceConfigurationUpdate(ConfigurationUpdateResponse response) {
        log.debug("Received a configuration-update-completed message: " + response);

        // find the current update request that is persisted - this is the one that is being reported as being complete
        ResourceConfigurationUpdate update = entityManager.find(ResourceConfigurationUpdate.class, response
            .getConfigurationUpdateId());
        if (update == null) {
            throw new IllegalStateException(
                "The completed request passed in does not match any request for any resource in inventory: " + response);
        }

        if (response.getStatus() == ConfigurationUpdateStatus.FAILURE) {
            // TODO [mazz]: what happens if the plugin dorked with the configuration ID? need to assert it hasn't changed
            if (response.getConfiguration() != null) {
                Configuration failedConfiguration = response.getConfiguration();
                failedConfiguration = entityManager.merge(failedConfiguration); // merge in any property error messages
                update.setConfiguration(failedConfiguration);
            }
        } else if (response.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
            // link to the newer, persisted configuration object
            Resource resource = update.getResource();
            resource.setResourceConfiguration(update.getConfiguration().deepCopyWithoutProxies());
            notifyAlertConditionCacheManager("completeResourceConfigurationUpdate", update);
        }

        // Make sure we update the persisted request with the new status and any error message.
        update.setStatus(response.getStatus());
        update.setErrorMessage(response.getErrorMessage());

        /* 
         * instead of checking for completed group resource configuration updates here, let our caller (the
         * ConfigurationServerService) do it so that this transaction completes before the check begins
         */
        return;
    }

    @SuppressWarnings("unchecked")
    public void checkForCompletedGroupResourceConfigurationUpdate(int resourceConfigUpdateId) {
        ResourceConfigurationUpdate resourceConfigUpdate = entityManager.find(ResourceConfigurationUpdate.class,
            resourceConfigUpdateId);
        if (resourceConfigUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS)
            // If this update isn't done, then, by definition, the group update isn't done either.
            return;

        GroupResourceConfigurationUpdate groupConfigUpdate = resourceConfigUpdate.getGroupConfigurationUpdate();
        if (groupConfigUpdate == null)
            // The update's not part of a group update - nothing we need to do.
            return;

        Query inProgressResourcesCountQuery = PersistenceUtility.createCountQuery(this.entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID_AND_STATUS);
        inProgressResourcesCountQuery.setParameter("parentUpdateId", groupConfigUpdate.getId());
        inProgressResourcesCountQuery.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
        long inProgressResourcesCount = (Long) inProgressResourcesCountQuery.getSingleResult();
        if (inProgressResourcesCount == 0) {
            // No more member updates in progress - the group update is complete.
            Query failedResourcesQuery = this.entityManager
                .createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID_AND_STATUS);
            failedResourcesQuery.setParameter("parentUpdateId", groupConfigUpdate.getId());
            failedResourcesQuery.setParameter("status", ConfigurationUpdateStatus.FAILURE);
            List<Resource> failedResources = failedResourcesQuery.getResultList();
            ConfigurationUpdateStatus groupStatus;
            if (failedResources.isEmpty()) {
                groupStatus = ConfigurationUpdateStatus.SUCCESS;
            } else {
                groupStatus = ConfigurationUpdateStatus.FAILURE;
                groupConfigUpdate.setErrorMessage("The following Resources failed to update their Configurations: "
                    + failedResources);
            }
            groupConfigUpdate.setStatus(groupStatus);
            log.info("Group Resource configuration update [" + groupConfigUpdate.getId() + "] for "
                + groupConfigUpdate.getGroup() + " has completed with status [" + groupStatus + "].");
            // TODO: Add support for alerting on completion of group resource config updates.
            //notifyAlertConditionCacheManager("checkForCompletedGroupResourceConfigurationUpdate", groupUpdate);
        } else {
            log.debug("Group Resource configuration update [" + groupConfigUpdate.getId() + "] for "
                + groupConfigUpdate.getGroup() + " has " + inProgressResourcesCount
                + " member updates still in progress.");
        }
        return;
    }

    public ConfigurationDefinition getPackageTypeConfigurationDefinition(Subject subject, int packageTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_DEPLOYMENT_BY_PACKAGE_TYPE_ID);
        query.setParameter("packageTypeId", packageTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            PackageType packageType = entityManager.find(PackageType.class, packageTypeId);
            if (packageType == null) {
                throw new EntityNotFoundException("A package type with id " + packageTypeId + " does not exist.");
            }
        }

        return configurationDefinition;
    }

    @Nullable
    public ConfigurationDefinition getResourceConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_RESOURCE_BY_RESOURCE_TYPE_ID);
        query.setParameter("resourceTypeId", resourceTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            if (resourceType == null) {
                throw new EntityNotFoundException("A resource type with id " + resourceTypeId + " does not exist.");
            }
        }

        return configurationDefinition;
    }

    @Nullable
    public ConfigurationDefinition getResourceConfigurationDefinitionWithTemplatesForResourceType(Subject subject,
        int resourceTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_RESOURCE_BY_RESOURCE_TYPE_ID);
        query.setParameter("resourceTypeId", resourceTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            if (resourceType == null) {
                throw new EntityNotFoundException("A resource type with id " + resourceTypeId + " does not exist.");
            }
        }

        // Eager Load the templates
        if ((configurationDefinition != null) && (configurationDefinition.getTemplates() != null)) {
            configurationDefinition.getTemplates().size();
        }

        return configurationDefinition;
    }

    public boolean hasPluginConfiguration(int resourceTypeId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ConfigurationDefinition.QUERY_FIND_PLUGIN_BY_RESOURCE_TYPE_ID);

        countQuery.setParameter("resourceTypeId", resourceTypeId);
        long count = (Long) countQuery.getSingleResult();

        return (count != 0L);
    }

    @Nullable
    public ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_PLUGIN_BY_RESOURCE_TYPE_ID);
        query.setParameter("resourceTypeId", resourceTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            if (resourceType == null) {
                throw new EntityNotFoundException("A resource type with id " + resourceTypeId + " does not exist.");
            }
        }

        // Eager Load the templates
        if ((configurationDefinition != null) && (configurationDefinition.getTemplates() != null)) {
            configurationDefinition.getTemplates().size();
        }

        return configurationDefinition;
    }

    /**
     * Given an actual resource, this asks the agent to return that resource's live configuration. Note that this does
     * not perform any authorization checks - it is assumed the caller has permissions to view the configuration. This
     * also assumes <code>resource</code> is a non-<code>null</code> and existing resource.
     *
     * <p>If failed to contact the agent or any other communications problem occurred, <code>null</code> will be
     * returned.</p>
     *
     * @param  resource an existing resource whose live configuration is to be retrieved
     * @param  pingAgentFirst true if the underlying Agent should be pinged successfully before attempting to retrieve
     *                        the configuration, or false otherwise
     *
     * @return the resource's live configuration or <code>null</code> if it could not be retrieved from the agent
     */
    private Configuration getLiveResourceConfiguration(Resource resource, boolean pingAgentFirst, boolean fromStructured) {
        Configuration liveConfig = null;

        try {
            Agent agent = resource.getAgent();
            AgentClient agentClient = this.agentManager.getAgentClient(agent);

            boolean agentPingedSuccessfully = false;
            // Getting live configuration is mostly for the UI's benefit - as such, do not hang
            // for a long time in the event the agent is down or can't be reached.  Let's make the UI
            // responsive even in the case of an agent down by pinging it quickly to verify the agent is up.
            if (pingAgentFirst)
                agentPingedSuccessfully = agentClient.ping(5000L);

            if (!pingAgentFirst || agentPingedSuccessfully) {
                liveConfig = agentClient.getConfigurationAgentService().loadResourceConfiguration(resource.getId());
                if (liveConfig == null) {
                    // This should really never occur - the PC should never return a null, always at least an empty config.
                    log.debug("ConfigurationAgentService.loadResourceConfiguration() returned a null Configuration.");
                    liveConfig = new Configuration();
                }
            } else {
                log.warn("Agent is unreachable [" + agent + "] - cannot get live configuration for resource ["
                    + resource + "]");
            }
        } catch (Exception e) {
            log.warn("Could not get live configuration for resource [" + resource + "]"
                + ThrowableUtil.getAllMessages(e, true));
        }

        return liveConfig;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AbstractResourceConfigurationUpdate mergeConfigurationUpdate(
        AbstractResourceConfigurationUpdate configurationUpdate) {
        return this.entityManager.merge(configurationUpdate);
    }

    public Configuration getConfigurationById(int id) {
        return entityManager.find(Configuration.class, id);
    }

    public Configuration getConfiguration(Subject subject, int configurationId) {
        return getConfigurationById(configurationId);
    }

    public Configuration getConfigurationFromDefaultTemplate(ConfigurationDefinition definition) {
        ConfigurationDefinition managedDefinition = entityManager.find(ConfigurationDefinition.class, definition
            .getId());
        return managedDefinition.getDefaultTemplate().getConfiguration();
    }

    private void handlePluginConfiguratonUpdateRemoteException(Resource resource, String summary, String detail) {
        resource.setConnected(false);
        ResourceError invalidPluginConfigError = new ResourceError(resource,
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, summary, detail, Calendar.getInstance().getTimeInMillis());
        this.resourceManager.addResourceError(invalidPluginConfigError);
    }

    private void removeAnyExistingInvalidPluginConfigurationErrors(Subject subject, Resource resource) {

        this.resourceManager.clearResourceConfigError(resource.getId());

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int createGroupConfigurationUpdate(AbstractGroupConfigurationUpdate update) throws SchedulerException {
        entityManager.persist(update);
        return update.getId();
    }

    public int scheduleGroupPluginConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> memberPluginConfigurations) throws SchedulerException {

        if (memberPluginConfigurations == null) {
            throw new IllegalArgumentException(
                "GroupPluginConfigurationUpdate must have non-null member configurations.");
        }

        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, compatibleGroupId);

        ensureModifyResourcePermission(subject, group);

        /*
         * we need to create and persist the group in a new/separate transaction before the rest of the
         * processing of this method; if we try to create and attach the PluginConfigurationUpdate children
         * to the parent group before the group update is actually persisted, we'll get StaleStateExceptions
         * from Hibernate because of our use of flush/clear (we're trying to update it before it actually
         * actually exists)
         */
        GroupPluginConfigurationUpdate groupUpdate = new GroupPluginConfigurationUpdate(group, subject.getName());
        int updateId = configurationManager.createGroupConfigurationUpdate(groupUpdate);

        // Create and persist updates for each of the members.
        for (Integer resourceId : memberPluginConfigurations.keySet()) {
            Configuration memberResourceConfiguration = memberPluginConfigurations.get(resourceId);
            Resource flyWeight = new Resource(resourceId);
            PluginConfigurationUpdate memberUpdate = new PluginConfigurationUpdate(flyWeight,
                memberResourceConfiguration, subject.getName());
            memberUpdate.setGroupConfigurationUpdate(groupUpdate);
            entityManager.persist(memberUpdate);
        }

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_CONFIG_GROUP_UPDATE_ID, updateId);
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_SUBJECT_ID, subject.getId());

        /*
         * acquire quartz objects and schedule the group update, but deferred the execution for 10 seconds
         * because we need this transaction to complete so that the data is available when the quartz job triggers
         */
        JobDetail jobDetail = GroupPluginConfigurationUpdateJob.getJobDetail(group, subject, jobDataMap);
        Trigger trigger = QuartzUtil.getFireOnceOffsetTrigger(jobDetail, 10000);
        scheduler.scheduleJob(jobDetail, trigger);

        log.debug("Scheduled plugin configuration update against compatibleGroup[id=" + compatibleGroupId + "]");

        return updateId;
    }

    public int scheduleGroupResourceConfigurationUpdate(Subject subject, int compatibleGroupId,//
        @WebParam(targetNamespace = ServerVersion.namespace)//
        @XmlJavaTypeAdapter(WebServiceTypeAdapter.class)//
        Map<Integer, Configuration> newResourceConfigurationMap) {
        if (newResourceConfigurationMap == null) {
            throw new IllegalArgumentException(
                "GroupResourceConfigurationUpdate must have non-null member configurations.");
        }

        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, compatibleGroupId);

        if (!authorizationManager.hasGroupPermission(subject, Permission.CONFIGURE, group.getId())) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission "
                + "to modify Resource configurations for members of group [" + group + "].");
        }

        ensureNoResourceConfigurationUpdatesInProgress(group);

        /*
         * we need to create and persist the group update in a new/separate transaction before the rest of the
         * processing of this method; if we try to create and attach the PluginConfigurationUpdate children
         * to the parent group before the group update is actually persisted, we'll get StaleStateExceptions
         * from Hibernate because of our use of flush/clear (we're trying to update it before it actually exists)
         */
        GroupResourceConfigurationUpdate groupUpdate = new GroupResourceConfigurationUpdate(group, subject.getName());
        int updateId = -1;
        try {
            updateId = configurationManager.createGroupConfigurationUpdate(groupUpdate);
        } catch (SchedulerException sche) {
            String message = "Error scheduling update for group[id=" + group.getId() + "]:";
            log.error(message, sche);
            new ResourceGroupUpdateException(message + sche.getMessage());
        }

        // Create and persist updates for each of the members.
        for (Integer resourceId : newResourceConfigurationMap.keySet()) {
            Configuration memberResourceConfiguration = newResourceConfigurationMap.get(resourceId);
            Resource flyWeight = new Resource(resourceId);
            ResourceConfigurationUpdate memberUpdate = new ResourceConfigurationUpdate(flyWeight,
                memberResourceConfiguration, subject.getName());
            memberUpdate.setGroupConfigurationUpdate(groupUpdate);
            entityManager.persist(memberUpdate);
        }

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_CONFIG_GROUP_UPDATE_ID, updateId);
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_SUBJECT_ID, subject.getId());

        /*
         * Acquire Quartz objects and schedule the group update, but defer the execution for 10 seconds,
         * because we need this transaction to complete so that the data is available when the Quartz job triggers.
         */
        JobDetail jobDetail = GroupResourceConfigurationUpdateJob.getJobDetail(group, subject, jobDataMap);
        Trigger trigger = QuartzUtil.getFireOnceOffsetTrigger(jobDetail, 10000);
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            String message = "Error scheduling job named '" + jobDetail.getName() + "':";
            log.error(message, e);
            new ResourceGroupUpdateException(message + e.getMessage());
        }

        log.debug("Scheduled Resource configuration update against compatible ResourceGroup[id=" + compatibleGroupId
            + "].");

        return updateId;
    }

    private ResourceGroup getCompatibleGroupIfAuthorized(Subject subject, int compatibleGroupId) {
        ResourceGroup group;

        try {
            // resourceGroupManager will test for necessary permissions too
            group = resourceGroupManager.getResourceGroupById(subject, compatibleGroupId, GroupCategory.COMPATIBLE);
        } catch (ResourceGroupNotFoundException e) {
            throw new RuntimeException("Cannot get support operations for unknown group [" + compatibleGroupId + "]: "
                + e, e);
        }

        return group;
    }

    private void ensureModifyPermission(Subject subject, Resource resource) throws PermissionException {
        if (!authorizationManager.hasResourcePermission(subject, Permission.MODIFY_RESOURCE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission "
                + "to modify plugin configuration for resource [" + resource + "]");
        }
    }

    private void ensureModifyResourcePermission(Subject subject, ResourceGroup group) throws PermissionException {
        if (!authorizationManager.hasGroupPermission(subject, Permission.MODIFY_RESOURCE, group.getId())) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission "
                + "to modify plugin configuration for members of group [" + group + "]");
        }
    }

    public GroupPluginConfigurationUpdate getGroupPluginConfigurationById(int configurationUpdateId) {
        GroupPluginConfigurationUpdate update = entityManager.find(GroupPluginConfigurationUpdate.class,
            configurationUpdateId);
        return update;
    }

    public GroupResourceConfigurationUpdate getGroupResourceConfigurationById(int configurationUpdateId) {
        GroupResourceConfigurationUpdate update = entityManager.find(GroupResourceConfigurationUpdate.class,
            configurationUpdateId);
        return update;
    }

    @SuppressWarnings("unchecked")
    public PageList<ConfigurationUpdateComposite> findPluginConfigurationUpdateCompositesByParentId(
        int configurationUpdateId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", configurationUpdateId);

        long count = getPluginConfigurationUpdateCountByParentId(configurationUpdateId);

        List<ConfigurationUpdateComposite> results = query.getResultList();

        return new PageList<ConfigurationUpdateComposite>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ConfigurationUpdateComposite> findResourceConfigurationUpdateCompositesByParentId(
        int configurationUpdateId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", configurationUpdateId);

        long count = getResourceConfigurationUpdateCountByParentId(configurationUpdateId);

        List<ConfigurationUpdateComposite> results = query.getResultList();

        return new PageList<ConfigurationUpdateComposite>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Integer> findPluginConfigurationUpdatesByParentId(int configurationUpdateId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", configurationUpdateId);

        long count = getPluginConfigurationUpdateCountByParentId(configurationUpdateId);

        List<Integer> results = query.getResultList();

        return new PageList<Integer>(results, (int) count, pageControl);
    }

    public long getPluginConfigurationUpdateCountByParentId(int configurationUpdateId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID);
        countQuery.setParameter("groupConfigurationUpdateId", configurationUpdateId);
        return (Long) countQuery.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public PageList<Integer> findResourceConfigurationUpdatesByParentId(int groupConfigurationUpdateId,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", groupConfigurationUpdateId);

        long count = getResourceConfigurationUpdateCountByParentId(groupConfigurationUpdateId);

        List<Integer> results = query.getResultList();

        return new PageList<Integer>(results, (int) count, pageControl);
    }

    public long getResourceConfigurationUpdateCountByParentId(int groupConfigurationUpdateId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID);
        countQuery.setParameter("groupConfigurationUpdateId", groupConfigurationUpdateId);
        return (Long) countQuery.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Configuration> getResourceConfigurationMapForGroupUpdate(
        Integer groupResourceConfigurationUpdateId) {
        Tuple<String, Object> groupIdParameter = new Tuple<String, Object>("groupConfigurationUpdateId",
            groupResourceConfigurationUpdateId);
        return executeGetConfigurationMapQuery(Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_UPDATE_ID, 100,
            groupIdParameter);
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Configuration> getPluginConfigurationMapForGroupUpdate(Integer groupPluginConfigurationUpdateId) {
        Tuple<String, Object> groupIdParameter = new Tuple<String, Object>("groupConfigurationUpdateId",
            groupPluginConfigurationUpdateId);
        return executeGetConfigurationMapQuery(Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_UPDATE_ID, 100,
            groupIdParameter);
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Configuration> getResourceConfigurationMapForCompatibleGroup(ResourceGroup compatibleGroup) {
        Tuple<String, Object> groupIdParameter = new Tuple<String, Object>("resourceGroupId", compatibleGroup.getId());
        return executeGetConfigurationMapQuery(Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID, 100,
            groupIdParameter);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Configuration> executeGetConfigurationMapQuery(String memberQueryName, int maxSize,
        Tuple<String, Object>... parameters) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, memberQueryName);
        Query query = entityManager.createNamedQuery(memberQueryName);

        for (Tuple<String, Object> param : parameters) {
            countQuery.setParameter(param.lefty, param.righty);
            query.setParameter(param.lefty, param.righty);
        }

        PersistenceUtility.setDataPage(query, new PageControl(0, maxSize)); // limit the results

        long count = (Long) countQuery.getSingleResult();
        int resultsSize;
        if (count > maxSize) {
            log.error("Configuration set contains more than " + maxSize + " members - " + "returning only " + maxSize
                + " Configurations (the maximum allowed).");
            resultsSize = maxSize;
        } else {
            resultsSize = (int) count;
        }

        // initialize the map to be 150% more than the results, so that the fill factor only reached 66%
        Map<Integer, Configuration> results = new HashMap<Integer, Configuration>((int) (resultsSize * 1.5));
        List<Object[]> pagedResults = query.getResultList();
        for (Object[] result : pagedResults) {
            results.put((Integer) result[0], (Configuration) result[1]);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<GroupPluginConfigurationUpdate> findGroupPluginConfigurationUpdates(int groupId, PageControl pc) {
        pc.initDefaultOrderingField("modifiedTime", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            GroupPluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            GroupPluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID);
        query.setParameter("groupId", groupId);
        countQuery.setParameter("groupId", groupId);

        long count = (Long) countQuery.getSingleResult();

        List<GroupPluginConfigurationUpdate> results = null;
        results = query.getResultList();

        return new PageList<GroupPluginConfigurationUpdate>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<GroupResourceConfigurationUpdate> findGroupResourceConfigurationUpdates(int groupId, PageControl pc) {
        pc.initDefaultOrderingField("modifiedTime", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            GroupResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            GroupResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID);
        query.setParameter("groupId", groupId);
        countQuery.setParameter("groupId", groupId);

        long count = (Long) countQuery.getSingleResult();

        List<GroupResourceConfigurationUpdate> results = null;
        results = query.getResultList();

        return new PageList<GroupResourceConfigurationUpdate>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public ConfigurationUpdateStatus updateGroupPluginConfigurationUpdateStatus(int groupConfigurationUpdateId,
        String errorMessages) {

        GroupPluginConfigurationUpdate groupUpdate = configurationManager
            .getGroupPluginConfigurationById(groupConfigurationUpdateId);

        Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_STATUS_BY_PARENT_UPDATE_ID);
        query.setParameter("groupConfigurationUpdateId", groupConfigurationUpdateId);

        // NOTE: None of the individual updates should still be INPROGRESS at the time this method is called!
        ConfigurationUpdateStatus groupUpdateStatus;
        List<ConfigurationUpdateStatus> updateStatusTuples = query.getResultList();
        if (updateStatusTuples.contains(ConfigurationUpdateStatus.FAILURE) || errorMessages != null) {
            groupUpdateStatus = ConfigurationUpdateStatus.FAILURE;
        } else {
            groupUpdateStatus = ConfigurationUpdateStatus.SUCCESS;
        }

        groupUpdate.setStatus(groupUpdateStatus);
        groupUpdate.setErrorMessage(errorMessages);
        configurationManager.updateGroupConfigurationUpdate(groupUpdate);

        return groupUpdateStatus; // if the caller wants to know what the new status was
    }

    public int deleteGroupPluginConfigurationUpdates(Subject subject, Integer resourceGroupId,
        Integer[] groupPluginConfigurationUpdateIds) {
        //TODO: use subject and resourceGroupId to perform security check
        int removed = 0;
        for (Integer apcuId : groupPluginConfigurationUpdateIds) {
            /*
             * use this strategy instead of GroupPluginConfigurationUpdate.QUERY_DELETE_BY_ID because removing via
             * the entityManager will respect cascading rules, using a JPQL DELETE statement will not
             */
            try {
                // break the plugin configuration update links in order to preserve individual change history
                Query q = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_DELETE_GROUP_UPDATE);
                q.setParameter("apcuId", apcuId);
                q.executeUpdate();

                GroupPluginConfigurationUpdate update = getGroupPluginConfigurationById(apcuId);
                entityManager.remove(update);
                removed++;
            } catch (Exception e) {
                log.error("Problem removing group plugin configuration update", e);
            }
        }

        return removed;
    }

    public int deleteGroupResourceConfigurationUpdates(Subject subject, Integer resourceGroupId,
        Integer[] groupResourceConfigurationUpdateIds) {

        if (authorizationManager.hasGroupPermission(subject, Permission.CONFIGURE, resourceGroupId) == false) {
            log.error(subject + " attempted to delete " + groupResourceConfigurationUpdateIds.length
                + " group resource configuration updates for ResourceGroup[id" + resourceGroupId
                + "], but did not have the " + Permission.CONFIGURE.name() + " permission for this group");
            return 0;
        }

        int removed = 0;
        for (Integer arcuId : groupResourceConfigurationUpdateIds) {
            /*
             * use this strategy instead of GroupResourceConfigurationUpdate.QUERY_DELETE_BY_ID because removing via
             * the entityManager will respect cascading rules, using a JPQL DELETE statement will not
             */
            try {
                // break the resource configuration update links in order to preserve individual change history
                Query q = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_DELETE_GROUP_UPDATE);
                q.setParameter("arcuId", arcuId);
                q.executeUpdate();

                GroupResourceConfigurationUpdate update = getGroupResourceConfigurationById(arcuId);
                entityManager.remove(update);
                removed++;
            } catch (Exception e) {
                log.error("Problem removing group resource configuration update", e);
            }
        }

        return removed;
    }

    public void updateGroupConfigurationUpdate(AbstractGroupConfigurationUpdate groupUpdate) {
        // TODO jmarques: if (errorMessages != null) set any remaining INPROGRESS children to FAILURE
        entityManager.merge(groupUpdate);
    }

    public void deleteConfigurations(List<Integer> configurationIds) {
        if (configurationIds == null || configurationIds.size() == 0) {
            return;
        }

        Query propertiesQuery = entityManager
            .createNamedQuery(Configuration.QUERY_DELETE_PROPERTIES_BY_CONFIGURATION_IDS);
        Query configurationsQuery = entityManager
            .createNamedQuery(Configuration.QUERY_DELETE_PROPERTIES_BY_CONFIGURATION_IDS);

        propertiesQuery.setParameter("configurationIds", configurationIds);
        configurationsQuery.setParameter("configurationIds", configurationIds);

        propertiesQuery.executeUpdate();
        configurationsQuery.executeUpdate();
    }

    public void deleteProperties(int[] propertyIds) {
        if (propertyIds == null || propertyIds.length == 0) {
            return;
        }

        Query propertiesQuery = entityManager.createNamedQuery(Property.QUERY_DELETE_BY_PROPERTY_IDS);
        propertiesQuery.setParameter("propertyIds", ArrayUtils.wrapInList(propertyIds));
        propertiesQuery.executeUpdate();
    }

    public GroupPluginConfigurationUpdate getGroupPluginConfigurationUpdate(Subject subject, int configurationUpdateId) {
        GroupPluginConfigurationUpdate update = getGroupPluginConfigurationById(configurationUpdateId);

        int groupId = update.getGroup().getId();
        if (authorizationManager.canViewGroup(subject, groupId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view group resourceConfiguration[id=" + configurationUpdateId + "]");
        }

        return update;
    }

    public GroupResourceConfigurationUpdate getGroupResourceConfigurationUpdate(Subject subject,
        int configurationUpdateId) {
        GroupResourceConfigurationUpdate update = getGroupResourceConfigurationById(configurationUpdateId);

        int groupId = update.getGroup().getId();
        if (authorizationManager.canViewGroup(subject, groupId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view group resourceConfiguration[id=" + configurationUpdateId + "]");
        }

        return update;
    }

    public Configuration translateResourceConfiguration(Subject subject, int resourceId, Configuration configuration,
        boolean fromStructured) {

        if (!isStructuredAndRawSupported(resourceId)) {
            throw new TranslationNotSupportedException("The translation operation is only supported for " +
                "configurations that support both structured and raw.");
        }

        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        try {
            Agent agent = resource.getAgent();
            AgentClient agentClient = this.agentManager.getAgentClient(agent);
            ConfigurationAgentService configService = agentClient.getConfigurationAgentService();

            return configService.merge(configuration, resourceId, fromStructured);
        } catch (PluginContainerException e) {
            log.error("An error occurred while trying to translate the configuration.", e);
            return null;
        }
    }

}
