package org.rhq.enterprise.server.discovery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

public class DeletedResourceTypeFilter {

    private SubjectManagerLocal subjectMgr;

    private ResourceTypeManagerLocal resourceTypeMgr;

    private PluginManagerLocal pluginMgr;

    private Set<String> deletedTypes;

    private Set<String> installedPlugins;

    public DeletedResourceTypeFilter(SubjectManagerLocal subjectManager, ResourceTypeManagerLocal resourceTypeManager,
        PluginManagerLocal pluginManager) {
        subjectMgr = subjectManager;
        resourceTypeMgr = resourceTypeManager;
        pluginMgr = pluginManager;
        deletedTypes = new HashSet<String>();
        installedPlugins = new HashSet<String>();
        loadDeletedTypes();
        loadInstalledPlugins();
    }

    private void loadDeletedTypes() {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterDeleted(true); // we'll filter on all deleted types ...
        criteria.addFilterIgnored(null); // ... whether they are ignored or not

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<ResourceType, ResourceTypeCriteria> queryExecutor = new CriteriaQueryExecutor<ResourceType, ResourceTypeCriteria>() {
            @Override
            public PageList<ResourceType> execute(ResourceTypeCriteria criteria) {
                return resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), criteria);
            }
        };

        CriteriaQuery<ResourceType, ResourceTypeCriteria> results = new CriteriaQuery<ResourceType, ResourceTypeCriteria>(
            criteria, queryExecutor);
        for (ResourceType type : results) {
            deletedTypes.add(type.getName() + "::" + type.getPlugin());
        }
    }

    private void loadInstalledPlugins() {
        List<Plugin> plugins = pluginMgr.getInstalledPlugins();
        for (Plugin plugin : plugins) {
            installedPlugins.add(plugin.getName());
        }
    }

    public Set<ResourceType> apply(InventoryReport report) {
        Set<ResourceType> resourceTypes = getResourceTypes(report.getAddedRoots());
        Set<ResourceType> staleTypes = new HashSet<ResourceType>();

        for (ResourceType type : resourceTypes) {
            // We check two things to determine whether a report should be rejected. First we check
            // that the plugin from which the type comes is installed. We check that the plugin is
            // installed as opposed to checking that it is deleted because the plugin could also
            // be purged in which case checking against deleted plugins wouldn't catch it. Secondly,
            // we check to see if the type is a deleted type. Here we check against deleted types
            // instead of installed types because the number of deleted types at any given time
            // should be much smaller than the number of installed types, resulting in faster look
            // ups. If we have a stale type in the report and that type has already been purged from
            // the database, it will get flagged by the check against installed plugins.
            if (!installedPlugins.contains(type.getPlugin()) ||
                deletedTypes.contains(type.getName() + "::" + type.getPlugin())) {
                staleTypes.add(type);
            }
        }
        return staleTypes;
    }

    private Set<ResourceType> getResourceTypes(Set<Resource> resources) {
        Set<ResourceType> types = new HashSet<ResourceType>();
        for (Resource resource : resources) {
            types.add(resource.getResourceType());
            types.addAll(getResourceTypes(resource.getChildResources()));
        }
        return types;
    }

}
