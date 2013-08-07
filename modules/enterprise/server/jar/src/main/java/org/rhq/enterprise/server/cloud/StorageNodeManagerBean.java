/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
package org.rhq.enterprise.server.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.authz.RequiredPermissions;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.reporting.MeasurementConverter;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.storage.StorageClusterSettings;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerBean;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 *
 * @author Stefan Negrea, Jiri Kremser
 */
@Stateless
public class StorageNodeManagerBean implements StorageNodeManagerLocal, StorageNodeManagerRemote {

    private final Log log = LogFactory.getLog(StorageNodeManagerBean.class);

    private static final String USERNAME_PROPERTY = "rhq.cassandra.username";
    private static final String PASSWORD_PROPERTY = "rhq.cassandra.password";
    private final static String MAINTENANCE_OPERATION = "addNodeMaintenance";
    private final static String MAINTENANCE_OPERATION_NOTE = "Topology change maintenance.";
    private final static String RUN_REPAIR_PROPERTY = "runRepair";
    private final static String UPDATE_SEEDS_LIST = "updateSeedsList";
    private final static String SEEDS_LIST = "seedsList";

    private static final String RHQ_STORAGE_CQL_PORT_PROPERTY = "nativeTransportPort";
    private static final String RHQ_STORAGE_GOSSIP_PORT_PROPERTY = "storagePort";
    private static final String RHQ_STORAGE_JMX_PORT_PROPERTY = "jmxPort";
    private static final String RHQ_STORAGE_ADDRESS_PROPERTY = "host";

    private static final int OPERATION_QUERY_TIMEOUT = 20000;
    private static final int MAX_ITERATIONS = 10;
    private static final String UPDATE_CONFIGURATION_OPERATION = "updateConfiguration";
    private static final String RESTART_OPERATION = "restart";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private MeasurementDataManagerLocal measurementManager;

    @EJB
    private SchedulerLocal quartzScheduler;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private OperationManagerLocal operationManager;

    @EJB
    private AlertManagerLocal alertManager;

    @EJB
    private ConfigurationManagerLocal configurationManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManger;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private StorageClusterSettingsManagerBean storageClusterSettingsManager;

    @Override
    public void linkResource(Resource resource) {
        Configuration pluginConfig = resource.getPluginConfiguration();
        String address = pluginConfig.getSimpleValue(RHQ_STORAGE_ADDRESS_PROPERTY);

        if (log.isInfoEnabled()) {
            log.info("Linking " + resource + " to storage node at " + address);
        }
        try {
            StorageNode storageNode = findStorageNodeByAddress(InetAddress.getByName(address));

            if (storageNode != null) {
                if (log.isInfoEnabled()) {
                    log.info(storageNode + " is an existing storage node. No cluster maintenance is necessary.");
                }
                storageNode.setResource(resource);
                storageNode.setOperationMode(OperationMode.NORMAL);
                initClusterSettingsIfNecessary(pluginConfig);
                addStorageNodeToGroup(resource);
            } else {
                storageNode = new StorageNode();
                storageNode.setAddress(address);
                storageNode.setCqlPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_CQL_PORT_PROPERTY)));
                storageNode.setJmxPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_JMX_PORT_PROPERTY)));
                storageNode.setResource(resource);
                storageNode.setOperationMode(OperationMode.INSTALLED);

                entityManager.persist(storageNode);

                if (log.isInfoEnabled()) {
                    log.info(storageNode + " is a new storage node and not part of the storage node cluster.");
                    log.info("Scheduling maintenance operations to bring " + storageNode + " into the cluster...");
                }

                announceNewNode(storageNode);
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not resolve address [" + address + "]. The resource " + resource +
                " cannot be linked to a storage node", e);
        }
    }

    private void initClusterSettingsIfNecessary(Configuration pluginConfig) {
        // TODO Need to handle non-repeatable reads here (probably a post 4.9 task)
        //
        // If a user deploys two storage nodes prior to installing the RHQ server, then we
        // could end up in this method concurrently for both storage nodes. The settings
        // would be committed for each node with the second commit winning. The problem is
        // that is the cluster settings differ for the two nodes, it will be silently
        // ignored. This scenario will happen infrequently so it should be sufficient to
        // resolve it with optimistic locking. The second writer should fail with an
        // OptimisticLockException.

        log.info("Initializing storage cluster settings");

        StorageClusterSettings clusterSettings = storageClusterSettingsManager.getClusterSettings(
            subjectManager.getOverlord());
        if (clusterSettings != null) {
            log.info("Cluster settings have already been set. Skipping initialization.");
            return;
        }
        clusterSettings = new StorageClusterSettings();
        clusterSettings.setCqlPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_CQL_PORT_PROPERTY)));
        clusterSettings.setGossipPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_GOSSIP_PORT_PROPERTY)));
        storageClusterSettingsManager.setClusterSettings(subjectManager.getOverlord(), clusterSettings);
    }

    private void announceNewNode(StorageNode newStorageNode) {
        if (log.isInfoEnabled()) {
            log.info("Announcing " + newStorageNode + " to storage node cluster.");
        }

        ResourceGroup storageNodeGroup = getStorageNodeGroup();

        GroupOperationSchedule schedule = new GroupOperationSchedule();
        schedule.setGroup(storageNodeGroup);
        schedule.setHaltOnFailure(false);
        schedule.setExecutionOrder(new ArrayList<Resource>(storageNodeGroup.getExplicitResources()));
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subjectManager.getOverlord());
        schedule.setOperationName("updateKnownNodes");

        Configuration parameters = new Configuration();
    parameters.put(createPropertyListOfAddresses("ipAddresses", combine(getClusteredStorageNodes(), newStorageNode)));
        schedule.setParameters(parameters);

        operationManager.scheduleGroupOperation(subjectManager.getOverlord(), schedule);
    }

    private List<StorageNode> combine(List<StorageNode> storageNodes, StorageNode storageNode) {
        List<StorageNode> newList = new ArrayList<StorageNode>(storageNodes.size() + 1);
        newList.addAll(storageNodes);
        newList.add(storageNode);

        return newList;
    }

    private PropertyList createPropertyListOfAddresses(String propertyName, List<StorageNode> nodes) {
        PropertyList list = new PropertyList(propertyName);
        for (StorageNode storageNode : nodes) {
            list.add(new PropertySimple("address", storageNode.getAddress()));
        }
        return list;
    }

    @Override
    public void createStorageNodeGroup() {
        log.info("Creating resource group [" + STORAGE_NODE_GROUP_NAME + "]");

        ResourceGroup group = new ResourceGroup(STORAGE_NODE_GROUP_NAME);

        ResourceType type = resourceTypeManager.getResourceTypeByNameAndPlugin(STORAGE_NODE_RESOURCE_TYPE_NAME,
            STORAGE_NODE_PLUGIN_NAME);
        group.setResourceType(type);
        group.setRecursive(false);

        resourceGroupManager.createResourceGroup(subjectManager.getOverlord(), group);

        addExistingStorageNodesToGroup();
    }

    private void addExistingStorageNodesToGroup() {
        log.info("Adding existing storage nodes to resource group [" + STORAGE_NODE_GROUP_NAME + "]");

        for (StorageNode node : getStorageNodes()) {
            if (node.getResource() != null) {
                addStorageNodeToGroup(node.getResource());
            }
        }
    }

    private void addStorageNodeToGroup(Resource resource) {
        if (log.isInfoEnabled()) {
            log.info("Adding " + resource + " to resource group [" + STORAGE_NODE_GROUP_NAME + "]");
        }

        ResourceGroup group = getStorageNodeGroup();
        resourceGroupManager.addResourcesToGroup(subjectManager.getOverlord(), group.getId(),
            new int[]{resource.getId()});
    }

    @Override
    public boolean storageNodeGroupExists() {
        Subject overlord = subjectManager.getOverlord();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterResourceTypeName(STORAGE_NODE_RESOURCE_TYPE_NAME);
        criteria.addFilterPluginName(STORAGE_NODE_PLUGIN_NAME);
        criteria.addFilterName(STORAGE_NODE_GROUP_NAME);

        List<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(overlord, criteria);

        return !groups.isEmpty();
    }

    @Override
    public void addToStorageNodeGroup(StorageNode storageNode) {
        storageNode.setOperationMode(OperationMode.NORMAL);
        entityManager.merge(storageNode);
        addStorageNodeToGroup(storageNode.getResource());
    }

    @Override
    public ResourceGroup getStorageNodeGroup() {
        Subject overlord = subjectManager.getOverlord();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterResourceTypeName(STORAGE_NODE_RESOURCE_TYPE_NAME);
        criteria.addFilterPluginName(STORAGE_NODE_PLUGIN_NAME);
        criteria.addFilterName(STORAGE_NODE_GROUP_NAME);
        criteria.fetchExplicitResources(true);

        List<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(overlord, criteria);

        if (groups.isEmpty()) {
            throw new IllegalStateException("Resource group [" + STORAGE_NODE_GROUP_NAME + "] does not exist. This " +
                "group must exist in order for the server to manage storage nodes. Restart the server for the group " +
                "to be recreated.");
        }
        return groups.get(0);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime) {
        int resourceId = getResourceIdFromStorageNode(node);
        Map<String, Integer> scheduleIdsMap = new HashMap<String, Integer>();

        // get the schedule ids for Storage Service resource
        final String tokensMetric = "Tokens", ownershipMetric = "Ownership";
        final String dataDiskUsedPercentageMetric = "Calculated.DataDiskUsedPercentage";
        final String totalDiskUsedPercentageMetric = "Calculated.TotalDiskUsedPercentage";
        final String freeDiskToDataRatioMetric = "Calculated.FreeDiskToDataSizeRatio";
        final String loadMetric = "Load", keyCacheSize = "KeyCacheSize", rowCacheSize = "RowCacheSize", totalCommitLogSize = "TotalCommitlogSize";
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, Object[].class);
        query.setParameter("parrentId", resourceId).setParameter("metricNames",
            Arrays.asList(tokensMetric, ownershipMetric, loadMetric, keyCacheSize, rowCacheSize, totalCommitLogSize,
                dataDiskUsedPercentageMetric, totalDiskUsedPercentageMetric, freeDiskToDataRatioMetric));
        for (Object[] pair : query.getResultList()) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }

        // get the schedule ids for Memory Subsystem resource
        final String heapCommittedMetric = "{HeapMemoryUsage.committed}", heapUsedMetric = "{HeapMemoryUsage.used}", heapUsedPercentageMetric = "Calculated.HeapUsagePercentage";
        query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES,
            Object[].class);
        query.setParameter("grandparrentId", resourceId).setParameter("metricNames",
            Arrays.asList(heapCommittedMetric, heapUsedMetric, heapUsedPercentageMetric));
        for (Object[] pair : query.getResultList()) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }


        StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
        MeasurementAggregate totalDiskUsedaggregate = new MeasurementAggregate(0d, 0d, 0d);
        Integer scheduleId = null;

        // find the aggregates and enrich the result instance
        if (!scheduleIdsMap.isEmpty()) {
            if ((scheduleId = scheduleIdsMap.get(tokensMetric)) != null) {
                MeasurementAggregate tokensAggregate = measurementManager.getAggregate(subject, scheduleId, beginTime,
                    endTime);
                result.setTokens(tokensAggregate);
            }
            if ((scheduleId = scheduleIdsMap.get(ownershipMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits ownershipAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                result.setActuallyOwns(ownershipAggregateWithUnits);
            }

            //calculated disk space related metrics
            if ((scheduleId = scheduleIdsMap.get(dataDiskUsedPercentageMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits dataDiskUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                result.setDataDiskUsedPercentage(dataDiskUsedPercentageAggregateWithUnits);
            }
            if ((scheduleId = scheduleIdsMap.get(totalDiskUsedPercentageMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits totalDiskUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                result.setTotalDiskUsedPercentage(totalDiskUsedPercentageAggregateWithUnits);
            }
            if ((scheduleId = scheduleIdsMap.get(freeDiskToDataRatioMetric)) != null) {
                MeasurementAggregate freeDiskToDataRatioAggregate = measurementManager.getAggregate(subject,
                    scheduleId, beginTime, endTime);
                result.setFreeDiskToDataSizeRatio(freeDiskToDataRatioAggregate);
            }

            if ((scheduleId = scheduleIdsMap.get(loadMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits loadAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                result.setLoad(loadAggregateWithUnits);

                updateAggregateTotal(totalDiskUsedaggregate, loadAggregateWithUnits.getAggregate());
            }
            if ((scheduleId = scheduleIdsMap.get(keyCacheSize)) != null) {
                updateAggregateTotal(totalDiskUsedaggregate,
                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            }
            if ((scheduleId = scheduleIdsMap.get(rowCacheSize)) != null) {
                updateAggregateTotal(totalDiskUsedaggregate,
                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            }
            if ((scheduleId = scheduleIdsMap.get(totalCommitLogSize)) != null) {
                updateAggregateTotal(totalDiskUsedaggregate,
                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            }

            if (totalDiskUsedaggregate.getMax() > 0) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits totalDiskUsedAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    totalDiskUsedaggregate, MeasurementUnits.BYTES);
                totalDiskUsedAggregateWithUnits.setFormattedValue(getSummaryString(totalDiskUsedaggregate,
                    MeasurementUnits.BYTES));
                result.setDataDiskUsed(totalDiskUsedAggregateWithUnits);
            }

            if ((scheduleId = scheduleIdsMap.get(heapCommittedMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapCommittedAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                result.setHeapCommitted(heapCommittedAggregateWithUnits);
            }
            if ((scheduleId = scheduleIdsMap.get(heapUsedMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                result.setHeapUsed(heapUsedAggregateWithUnits);
            }
            if ((scheduleId = scheduleIdsMap.get(heapUsedPercentageMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime,
                    endTime);
                result.setHeapPercentageUsed(heapUsedPercentageAggregateWithUnits);
            }
        }

        return result;
    }

    /**
     * @param accumulator
     * @param input
     */
    private void updateAggregateTotal(MeasurementAggregate accumulator, MeasurementAggregate input) {
        if (accumulator != null && input != null
                && input.getMax() != null && !Double.isNaN(input.getMax())
                && input.getMin() != null && !Double.isNaN(input.getMin())
                && input.getAvg() != null && !Double.isNaN(input.getAvg())) {
            accumulator.setAvg(accumulator.getAvg() + input.getAvg());
            accumulator.setMax(accumulator.getMax() + input.getMax());
            accumulator.setMin(accumulator.getMin() + input.getMin());
        }
    }

    @Override
    public List<StorageNode> getStorageNodes() {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_ALL,
            StorageNode.class);
        return query.getResultList();
    }
    
    @Override
    public PageList<StorageNodeLoadComposite> getStorageNodeComposites() {
        List<StorageNode> nodes = getStorageNodes();
        PageList<StorageNodeLoadComposite> result = new PageList<StorageNodeLoadComposite>();
        long endTime = System.currentTimeMillis();
        long beginTime = endTime - (8 * 60 * 60 * 1000);
        for (StorageNode node : nodes) {
            StorageNodeLoadComposite composite = getLoad(subjectManager.getOverlord(), node, beginTime, endTime);
            int unackAlerts = findNotAcknowledgedStorageNodeAlerts(subjectManager.getOverlord(), node).size();
            composite.setUnackAlerts(unackAlerts);
            result.add(composite);
        }
        return result;
    }

    private List<StorageNode> getClusteredStorageNodes() {
        return entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE, StorageNode.class)
            .setParameter("operationMode", OperationMode.NORMAL).getResultList();
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<StorageNode> runner = new CriteriaQueryRunner<StorageNode>(criteria, generator,
            entityManager);
        return runner.execute();
    }

    public StorageNode findStorageNodeByAddress(InetAddress address) {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
            StorageNode.class);
        query.setParameter("address", address.getHostAddress());
        List<StorageNode> result = query.getResultList();

        if (result != null && result.size() > 0) {
            return result.get(0);
        }

        return null;
    }

    @Override
    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public void prepareNodeForUpgrade(Subject subject, StorageNode storageNode) {
        int storageNodeResourceId = getResourceIdFromStorageNode(storageNode);
        TopologyManagerLocal topologyManager = LookupUtil.getTopologyManager();
        ServerManagerLocal serverManager = LookupUtil.getServerManager();
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        Server server = serverManager.getServer();
        // setting the server mode to maintenance
        topologyManager.updateServerMode(subject, new Integer[] { server.getId() }, Server.OperationMode.MAINTENANCE);

        Configuration parameters = new Configuration();
        parameters.setSimpleValue("snapshotName", String.valueOf(System.currentTimeMillis()));
        // scheduling the operation
        operationManager.scheduleResourceOperation(subject, storageNodeResourceId, "prepareForUpgrade", 0, 0, 0, 0,
            parameters, "Run by StorageNodeManagerBean.prepareNodeForUpgrade()");
    }

    private String getSummaryString(MeasurementAggregate aggregate, MeasurementUnits units) {
        String formattedValue = "Min: " + MeasurementConverter.format(aggregate.getMin(), units, true) + ", Max: "
            + MeasurementConverter.format(aggregate.getMax(), units, true) + ", Avg: "
            + MeasurementConverter.format(aggregate.getAvg(), units, true);
        return formattedValue;
    }

    private StorageNodeLoadComposite.MeasurementAggregateWithUnits getMeasurementAggregateWithUnits(Subject subject,
        int schedId, MeasurementUnits units, long beginTime, long endTime) {
        MeasurementAggregate measurementAggregate = measurementManager.getAggregate(subject, schedId, beginTime,
            endTime);
        StorageNodeLoadComposite.MeasurementAggregateWithUnits measurementAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
            measurementAggregate, units);
        measurementAggregateWithUnits.setFormattedValue(getSummaryString(measurementAggregate, units));
        return measurementAggregateWithUnits;
    }

    private int getResourceIdFromStorageNode(StorageNode storageNode) {
        int resourceId;
        if (storageNode.getResource() == null) {
            storageNode = entityManager.find(StorageNode.class, storageNode.getId());
            if (storageNode.getResource() == null) { // no associated resource
                throw new IllegalStateException("This storage node [" + storageNode.getId() + "] has no associated resource.");
            }
        }
        resourceId = storageNode.getResource().getId();
        return resourceId;
    }

    @Override
    public void runReadRepair() {
        ResourceGroup storageNodeGroup = getStorageNodeGroup();

        if (storageNodeGroup.getExplicitResources().size() < 2) {
            log.info("Skipping read repair since this is a single-node cluster");
            return;
        }

        log.info("Scheduling read repair maintenance for storage cluster");

        GroupOperationSchedule schedule = new GroupOperationSchedule();
        schedule.setGroup(storageNodeGroup);
        schedule.setHaltOnFailure(false);
        schedule.setExecutionOrder(new ArrayList<Resource>(storageNodeGroup.getExplicitResources()));
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subjectManager.getOverlord());
        schedule.setOperationName("readRepair");
        schedule.setDescription("Run scheduled read repair on storage node");

        operationManager.scheduleGroupOperation(subjectManager.getOverlord(), schedule);
    }

    @Override
    public PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject) {
        return findStorageNodeAlerts(subject, false, null);
    }

    @Override
    public PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject, StorageNode storageNode) {
        return findStorageNodeAlerts(subject, false, storageNode);
    }

    @Override
    public PageList<Alert> findAllStorageNodeAlerts(Subject subject) {
        return findStorageNodeAlerts(subject, true, null);
    }

    @Override
    public PageList<Alert> findAllStorageNodeAlerts(Subject subject, StorageNode storageNode) {
        return findStorageNodeAlerts(subject, true, storageNode);
    }

    /**
     * Find the set of alerts related to Storage Node resources and sub-resources.
     *
     * @param subject subject
     * @param allAlerts if [true] then return all alerts; if [false] then return only alerts that are not acknowledged
     * @return alerts
     */
    private PageList<Alert> findStorageNodeAlerts(Subject subject, boolean allAlerts, StorageNode storageNode) {
        Integer[] resouceIdsWithAlertDefinitions = findResourcesWithAlertDefinitions(storageNode);
        PageList<Alert> alerts = new PageList<Alert>();

        if( resouceIdsWithAlertDefinitions != null && resouceIdsWithAlertDefinitions.length != 0 ){
            AlertCriteria criteria = new AlertCriteria();
            criteria.setPageControl(PageControl.getUnlimitedInstance());
            criteria.addFilterResourceIds(resouceIdsWithAlertDefinitions);
            criteria.addSortCtime(PageOrdering.DESC);

            alerts = alertManager.findAlertsByCriteria(subject, criteria);

            if (!allAlerts) {
                //select on alerts that are not acknowledge
                PageList<Alert> trimmedAlerts = new PageList<Alert>();
                for (Alert alert : alerts) {
                    if (alert.getAcknowledgeTime() == null || alert.getAcknowledgeTime() <= 0) {
                        trimmedAlerts.add(alert);
                    }
                }

                alerts = trimmedAlerts;
            }
        }

        return alerts;
    }

    @Override
    public Integer[] findResourcesWithAlertDefinitions() {
        return this.findResourcesWithAlertDefinitions(null);
    }

    @Override
    public Integer[] findResourcesWithAlertDefinitions(StorageNode storageNode) {
        List<StorageNode> initialStorageNodes = getStorageNodes();
        if (storageNode == null) {
            initialStorageNodes = getStorageNodes();
        } else {
            initialStorageNodes = Arrays.asList(storageNode.getResource() == null ? entityManager.find(
                StorageNode.class, storageNode.getId()) : storageNode);
        }
         
        Queue<Resource> unvisitedResources = new LinkedList<Resource>();
        for (StorageNode initialStorageNode : initialStorageNodes) {
            if (initialStorageNode.getResource() != null) {
                unvisitedResources.add(initialStorageNode.getResource());
            }
        }

        List<Integer> resourceIdsWithAlertDefinitions = new ArrayList<Integer>();
        while (!unvisitedResources.isEmpty()) {
            Resource resource = unvisitedResources.poll();
            if (resource.getAlertDefinitions() != null) {
                resourceIdsWithAlertDefinitions.add(resource.getId());
            }

            Set<Resource> childResources = resource.getChildResources();
            if (childResources != null) {
                for (Resource child : childResources) {
                    unvisitedResources.add(child);
                }
            }
        }

        return resourceIdsWithAlertDefinitions.toArray(new Integer[resourceIdsWithAlertDefinitions.size()]);
    }

    @Override
    public StorageNodeConfigurationComposite retrieveConfiguration(Subject subject, StorageNode storageNode) {
        StorageNodeConfigurationComposite configuration = new StorageNodeConfigurationComposite(storageNode);

        if (storageNode != null && storageNode.getResource() != null) {
            Resource storageNodeResource = storageNode.getResource();
            Configuration storageNodeConfiguration = configurationManager.getResourceConfiguration(subject,
                storageNodeResource.getId());

            configuration.setHeapSize(storageNodeConfiguration.getSimpleValue("maxHeapSize"));
            configuration.setHeapNewSize(storageNodeConfiguration.getSimpleValue("heapNewSize"));
            configuration.setThreadStackSize(storageNodeConfiguration.getSimpleValue("threadStackSize"));
            configuration.setJmxPort(storageNode.getJmxPort());
        }

        return configuration;
    }

    @Override
    public boolean updateConfiguration(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration) {
        try {
            StorageNode storageNode = findStorageNodeByAddress(InetAddress.getByName(
                storageNodeConfiguration.getStorageNode().getAddress()));

            if (storageNode != null && storageNode.getResource() != null) {
                Configuration parameters = new Configuration();
                parameters.setSimpleValue("jmxPort", storageNodeConfiguration.getJmxPort() + "");
                if (storageNodeConfiguration.getHeapSize() != null) {
                    parameters.setSimpleValue("heapSize", storageNodeConfiguration.getHeapSize() + "");
                }
                if (storageNodeConfiguration.getHeapNewSize() != null) {
                    parameters.setSimpleValue("heapNewSize", storageNodeConfiguration.getHeapNewSize() + "");
                }
                if (storageNodeConfiguration.getThreadStackSize() != null) {
                    parameters.setSimpleValue("threadStackSize", storageNodeConfiguration.getThreadStackSize() + "");
                }
                parameters.setSimpleValue("restartIfRequired", "false");

                Resource storageNodeResource = storageNode.getResource();

                boolean result = runOperationAndWaitForResult(subject, storageNodeResource, UPDATE_CONFIGURATION_OPERATION,
                    parameters);

                if (result) {
                    //2. Update the JMX port
                    //this is a fast operation compared to the restart
                    storageNode.setJmxPort(storageNodeConfiguration.getJmxPort());
                    entityManager.merge(storageNode);

                    //3. Restart the storage node
                    result = runOperationAndWaitForResult(subject, storageNodeResource, RESTART_OPERATION,
                        new Configuration());

                    //4. Update the plugin configuration to talk with the new server
                    //Up to this point communication with the storage node should not have been affected by the intermediate
                    //changes
                    Configuration storageNodePluginConfig = configurationManager.getPluginConfiguration(subject,
                        storageNodeResource.getId());

                    String existingJMXPort = storageNodePluginConfig.getSimpleValue("jmxPort");
                    String newJMXPort = storageNodeConfiguration.getJmxPort() + "";

                    if (!existingJMXPort.equals(newJMXPort)) {
                        storageNodePluginConfig.setSimpleValue("jmxPort", newJMXPort);

                        String existingConnectionURL = storageNodePluginConfig.getSimpleValue("connectorAddress");
                        String newConnectionURL = existingConnectionURL.replace(":" + existingJMXPort + "/", ":"
                            + storageNodeConfiguration.getJmxPort() + "/");
                        storageNodePluginConfig.setSimpleValue("connectorAddress", newConnectionURL);

                        configurationManager.updatePluginConfiguration(subject, storageNodeResource.getId(),
                            storageNodePluginConfig);
                    }

                    return result;
                }
            }

            return false;
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to resolve address for " + storageNodeConfiguration, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void scheduleOperationInNewTransaction(Subject subject, ResourceOperationSchedule schedule) {
        operationManager.scheduleResourceOperation(subject, schedule);
    }

    private boolean runOperationAndWaitForResult(Subject subject, Resource storageNodeResource, String operationToRun,
        Configuration parameters) {

        //scheduling the operation
        long operationStartTime = System.currentTimeMillis();

        ResourceOperationSchedule newSchedule = new ResourceOperationSchedule();
        newSchedule.setJobTrigger(JobTrigger.createNowTrigger());
        newSchedule.setResource(storageNodeResource);
        newSchedule.setOperationName(operationToRun);
        newSchedule.setDescription("Run by StorageNodeManagerBean");
        newSchedule.setParameters(parameters);

        storageNodeManger.scheduleOperationInNewTransaction(subject, newSchedule);

        //waiting for the operation result then return it
        int iteration = 0;
        boolean successResultFound = false;
        while (iteration < MAX_ITERATIONS && !successResultFound) {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterResourceIds(storageNodeResource.getId());
            criteria.addFilterStartTime(operationStartTime);
            criteria.addFilterOperationName(operationToRun);
            criteria.addFilterStatus(OperationRequestStatus.SUCCESS);
            criteria.setPageControl(PageControl.getUnlimitedInstance());

            PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
                subject, criteria);

            if (results != null && results.size() > 0) {
                successResultFound = true;
            }

            if (successResultFound) {
                break;
            } else {
                try {
                    Thread.sleep(OPERATION_QUERY_TIMEOUT);
                } catch (Exception e) {
                    log.error(e);
                }
            }

            iteration++;
        }

        return successResultFound;
    }

    @Override
    public void prepareNewNodesForBootstrap() {
        List<StorageNode> newStorageNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE)
            .setParameter("operationMode", OperationMode.INSTALLED).getResultList();
        if (newStorageNodes.isEmpty()) {
            throw new RuntimeException("Failed to find storage node to bootstrap into cluster.");
        }
        // Right now, without some user input, we can only reliably bootstrap one node at a
        // time. To support bootstrapping multiple nodes concurrently, a mechanism will have
        // to be put in place for the user to declare in advance the nodes that are coming
        // online. Then we can wait until all declared nodes have been committed into
        // inventory and announced to the cluster
        StorageNode storageNode = newStorageNodes.get(0);

        if (log.isInfoEnabled()) {
            log.info("Preparing to bootstrap " + storageNode + " into cluster...");
        }

        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(storageNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subjectManager.getOverlord());
        schedule.setOperationName("prepareForBootstrap");

        StorageClusterSettings clusterSettings = storageClusterSettingsManager.getClusterSettings(
            subjectManager.getOverlord());
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("cqlPort", clusterSettings.getCqlPort()));
        parameters.put(new PropertySimple("gossipPort", clusterSettings.getGossipPort()));
        parameters.put(createPropertyListOfAddresses("storageNodeIPAddresses", getClusteredStorageNodes()));

        schedule.setParameters(parameters);

        operationManager.scheduleResourceOperation(subjectManager.getOverlord(), schedule);
    }

    @Override
    public void runAddNodeMaintenance() {
        log.info("Preparing to schedule addNodeMaintenance on the storage cluster...");

        List<StorageNode> storageNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", OperationMode.NORMAL).getResultList();

        // The previous cluster size will be the current size - 1 since we currently only
        // support deploying one node at a time.
        int previousClusterSize = storageNodes.size() - 1;
        boolean isReadRepairNeeded;

        if (previousClusterSize >= 4) {
            // At 4 nodes we increase the RF to 3. We are not increasing the RF beyond
            // that for additional nodes; so, there is no need to run repair if we are
            // expanding from a 4 node cluster since the RF remains the same.
            isReadRepairNeeded = false;
        } else if (previousClusterSize == 1) {
            // The RF will increase since we are going from a single to a multi-node
            // cluster; therefore, we want to run repair.
            isReadRepairNeeded = true;
        } else if (previousClusterSize == 2) {
            if (storageNodes.size() > 3) {
                // If we go from 2 to > 3 nodes we will increase the RF to 3; therefore
                // we want to run repair.
                isReadRepairNeeded = true;
            } else {
                // If we go from 2 to 3 nodes, we keep the RF at 2 so there is no need
                // to run repair.
                isReadRepairNeeded = false;
            }
        } else if (previousClusterSize == 3) {
            // We are increasing the cluster size > 3 which means the RF will be
            // updated to 3; therefore, we want to run repair.
            isReadRepairNeeded = true;
        } else {
            // If we cluster size of zero, then something is really screwed up. It
            // should always be > 0.
            isReadRepairNeeded = storageNodes.size() > 1;
        }

        if (isReadRepairNeeded) {
            updateTopology(storageNodes);
        }

        ResourceGroup storageNodeGroup = getStorageNodeGroup();

        GroupOperationSchedule schedule = new GroupOperationSchedule();
        schedule.setGroup(storageNodeGroup);
        schedule.setHaltOnFailure(false);
        schedule.setExecutionOrder(new ArrayList<Resource>(storageNodeGroup.getExplicitResources()));
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subjectManager.getOverlord());
        schedule.setOperationName(MAINTENANCE_OPERATION);
        schedule.setDescription(MAINTENANCE_OPERATION_NOTE);

        Configuration config = new Configuration();
        config.put(createPropertyListOfAddresses(SEEDS_LIST, storageNodes));
        config.put(new PropertySimple(RUN_REPAIR_PROPERTY, isReadRepairNeeded));
        config.put(new PropertySimple(UPDATE_SEEDS_LIST, Boolean.TRUE));

        schedule.setParameters(config);

        operationManager.scheduleGroupOperation(subjectManager.getOverlord(), schedule);
    }

    private void updateTopology(List<StorageNode> storageNodes) {
        String username = getRequiredStorageProperty(USERNAME_PROPERTY);
        String password = getRequiredStorageProperty(PASSWORD_PROPERTY);
        SchemaManager schemaManager = new SchemaManager(username, password, storageNodes);
        try{
            schemaManager.updateTopology();
        } catch (Exception e) {
            log.error("An error occurred while applying schema topology changes", e);
        }
    }

    private String getRequiredStorageProperty(String property) {
        String value = System.getProperty(property);
        if (StringUtil.isEmpty(property)) {
            throw new IllegalStateException("The system property [" + property + "] is not set. The RHQ "
                + "server will not be able connect to the RHQ storage node(s). This property should be defined "
                + "in rhq-server.properties.");
        }
        return value;
    }

}
