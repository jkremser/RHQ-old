/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.metadata.SubCategoriesMetadataParser;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * This class manages the metadata for resources. Plugins are registered against this bean so that their metadata can be
 * pulled out and stored as necessary.
 *
 * @author Greg Hinkle
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class ResourceMetadataManagerBean implements ResourceMetadataManagerLocal {
    private final Log log = LogFactory.getLog(ResourceMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private ResourceMetadataManagerLocal resourceMetadataManager; // self

    @EJB
    private ContentMetadataManagerLocal contentMetadataMgr;

    @EJB
    private OperationMetadataManagerLocal operationMetadataMgr;

    @EJB
    private EventMetdataManagerLocal eventMetadataMgr;

    @EJB
    private MeasurementMetadataManagerLocal measurementMetadataMgr;

    @EJB
    private AlertMetadataManagerLocal alertMetadataMgr;

    @EJB
    private ResourceConfigurationMetadataManagerLocal resourceConfigMetadataMgr;

    @EJB
    private PluginConfigurationMetadataManagerLocal pluginConfigMetadataMgr;

    public void updateTypes(Set<ResourceType> resourceTypes) throws Exception {
        // Only process the type if it is a non-runs-inside type (i.e. not a child of some other type X at this same
        // level in the type hierarchy). runs-inside types which we skip here will get processed at the next level down
        // when we recursively process type X's children.
        Set<ResourceType> allChildren = new HashSet<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            allChildren.addAll(resourceType.getChildResourceTypes());
        }
        Set<ResourceType> nonRunsInsideResourceTypes = new LinkedHashSet<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            if (!allChildren.contains(resourceType)) {
                nonRunsInsideResourceTypes.add(resourceType);
            }
        }

        // Iterate the resource types breadth-first, so all platform types get added before any server types or platform
        // service types. This way, we'll be able to set all of the platform types as parents of the server types and
        // platform service types. It's also helpful for other types with multiple "runs-inside" parent types (e.g
        // Hibernate Entities), since it ensures the parent types will get persisted prior to the child types.
        if (log.isDebugEnabled()) {
            log.debug("Processing types: " + nonRunsInsideResourceTypes + "...");
        }
        Set<ResourceType> legitimateChildren = new HashSet<ResourceType>();
        for (ResourceType resourceType : nonRunsInsideResourceTypes) {
            long startTime = System.currentTimeMillis();
            updateType(resourceType);
            long endTime = System.currentTimeMillis();
            log.debug("Updated resource type [" + toConciseString(resourceType) + "] in " + (endTime - startTime)
                + " ms");

            legitimateChildren.addAll(resourceType.getChildResourceTypes());
        }
        // Only recurse if there are actually children - this prevents infinite recursion.
        if (!legitimateChildren.isEmpty()) {
            updateTypes(legitimateChildren);
        }
    }

    // NO TRANSACTION SHOULD BE ACTIVE ON ENTRY 
    // Start with no transaction so we can control the transactional boundaries. Obsolete type removal removes
    // resources of the obsolete type. We need to avoid an umbrella transaction for the type removal because large
    // inventories of obsolete resources will generate very large transactions. Potentially resulting in timeouts
    // or other issues.
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void removeObsoleteTypes(Subject subject, String pluginName, PluginMetadataManager metadataCache) {

        Set<ResourceType> obsoleteTypes = new HashSet<ResourceType>();
        Set<ResourceType> legitTypes = new HashSet<ResourceType>();

        try {
            resourceMetadataManager.getPluginTypes(subject, pluginName, legitTypes, obsoleteTypes, metadataCache);

            if (!obsoleteTypes.isEmpty()) {
                log.debug("Removing " + obsoleteTypes.size() + " obsolete types: " + obsoleteTypes + "...");
                removeResourceTypes(subject, obsoleteTypes, new HashSet<ResourceType>(obsoleteTypes));
            }

            // Now it's safe to remove any obsolete subcategories on the legit types.
            for (ResourceType legitType : legitTypes) {
                ResourceType updateType = metadataCache.getType(legitType.getName(), legitType.getPlugin());

                // If we've got a type from the descriptor which matches an existing one,
                // then let's see if we need to remove any subcategories from the existing one.

                // NOTE: I don't think updateType will ever be null here because we have previously verified
                // its existence above when we called resourceMetadataManager.getPluginTypes. All of the types contained
                // in legitTypes are all types found to exist in metadataCache. Therefore, I think that this null
                // check can be removed.
                //
                // jsanda - 11/11/2010
                if (updateType != null) {
                    try {
                        resourceMetadataManager.removeObsoleteSubCategories(subject, updateType, legitType);
                    } catch (Exception e) {
                        throw new Exception("Failed to delete obsolete subcategories from " + legitType + ".", e);
                    }
                }
            }
        } catch (Exception e) {
            // Catch all exceptions, so a failure here does not cause the outer tx to rollback.
            log.error("Failure during removal of obsolete ResourceTypes and Subcategories.", e);
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @SuppressWarnings("unchecked")
    public void getPluginTypes(Subject subject, String pluginName, Set<ResourceType> legitTypes,
        Set<ResourceType> obsoleteTypes, PluginMetadataManager metadataCache) {
        try {
            Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_PLUGIN);
            query.setParameter("plugin", pluginName);
            List<ResourceType> existingTypes = query.getResultList();

            if (existingTypes != null) {

                for (ResourceType existingType : existingTypes) {
                    if (metadataCache.getType(existingType.getName(), existingType.getPlugin()) == null) {
                        // The type is obsolete - (i.e. it's no longer defined by the plugin).
                        obsoleteTypes.add(existingType);
                    } else {
                        legitTypes.add(existingType);
                    }
                }
            }
        } catch (Exception e) {
            // Catch all exceptions, so a failure here does not cause the outer tx to rollback.
            log.error("Failure during removal of obsolete ResourceTypes and Subcategories.", e);
        }
    }

    // NO TRANSACTION SHOULD BE ACTIVE ON ENTRY
    private void removeResourceTypes(Subject subject, Set<ResourceType> candidateTypes,
        Set<ResourceType> typesToBeRemoved) throws Exception {
        for (ResourceType candidateType : candidateTypes) {
            // Remove obsolete descendant types first.
            //Set<ResourceType> childTypes = candidateType.getChildResourceTypes();
            List<ResourceType> childTypes = resourceTypeManager.getChildResourceTypes(subject, candidateType);
            if (childTypes != null && !childTypes.isEmpty()) {
                // Wrap child types in new HashSet to avoid ConcurrentModificationExceptions.
                removeResourceTypes(subject, new HashSet<ResourceType>(childTypes), typesToBeRemoved);
            }
            if (typesToBeRemoved.contains(candidateType)) {
                try {
                    removeResourceType(subject, candidateType);
                } catch (Exception e) {
                    throw new Exception("Failed to remove " + candidateType + ".", e);
                }
                typesToBeRemoved.remove(candidateType);
            }
        }
    }

    // NO TRANSACTION SHOULD BE ACTIVE ON ENTRY
    private void removeResourceType(Subject subject, ResourceType existingType) {
        log.info("Removing ResourceType [" + toConciseString(existingType) + "]...");

        // Remove all Resources that are of the type.
        ResourceCriteria c = new ResourceCriteria();
        c.addFilterResourceTypeId(existingType.getId());
        List<Resource> resources = resourceManager.findResourcesByCriteria(subject, c);
        if (resources != null) {
            Iterator<Resource> resIter = resources.iterator();
            while (resIter.hasNext()) {
                Resource res = resIter.next();
                List<Integer> deletedIds = resourceManager.uninventoryResource(subject, res.getId());
                // do this out of band because the current transaction is locking rows that due to
                // updates that may need to get deleted. If you do it here the NewTrans used below
                // may deadlock with the current transactions locks.
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.uninventoryResourceAsyncWork(subject, deletedResourceId);
                }
                resIter.remove();
            }
        }

        resourceMetadataManager.completeRemoveResourceType(subject, existingType);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void completeRemoveResourceType(Subject subject, ResourceType existingType) {
        existingType = entityManager.find(ResourceType.class, existingType.getId());

        if (entityManager.contains(existingType)) {
            entityManager.refresh(existingType);
        }

        // Completely remove the type from the type hierarchy.
        removeFromParents(existingType);
        removeFromChildren(existingType);
        entityManager.merge(existingType);

        contentMetadataMgr.deleteMetadata(subject, existingType);

        entityManager.flush();
        existingType = entityManager.find(existingType.getClass(), existingType.getId());

        try {
            alertMetadataMgr.deleteAlertTemplates(subject, existingType);
        } catch (Exception e) {
            throw new RuntimeException("Alert template deletion failed. Cannot finish deleting " + existingType, e);
        }

        entityManager.flush();
        existingType = entityManager.find(existingType.getClass(), existingType.getId());

        // Remove all compatible groups that are of the type.
        List<ResourceGroup> compatGroups = existingType.getResourceGroups();
        if (compatGroups != null) {
            Iterator<ResourceGroup> compatGroupIterator = compatGroups.iterator();
            while (compatGroupIterator.hasNext()) {
                ResourceGroup compatGroup = compatGroupIterator.next();
                try {
                    resourceGroupManager.deleteResourceGroup(subject, compatGroup.getId());
                } catch (ResourceGroupDeleteException e) {
                    throw new RuntimeException(e);
                }
                compatGroupIterator.remove();
            }
        }
        entityManager.flush();

        measurementMetadataMgr.deleteMetadata(existingType);
        entityManager.flush();

        // TODO: Clean out event definitions?

        // Finally, remove the type itself.
        // Refresh it first to make sure any newly discovered Resources of the type get added to the persistence
        // context and hopefully get removed via cascade when we remove the type.
        entityManager.refresh(existingType);
        entityManager.remove(existingType);
        entityManager.flush();
    }

    private void removeFromParents(ResourceType typeToBeRemoved) {
        // Wrap in new HashSet to avoid ConcurrentModificationExceptions.
        Set<ResourceType> parentTypes = new HashSet<ResourceType>(typeToBeRemoved.getParentResourceTypes());
        for (ResourceType parentType : parentTypes) {
            parentType.removeChildResourceType(typeToBeRemoved);
            entityManager.merge(parentType);
        }
    }

    private void removeFromChildren(ResourceType typeToBeRemoved) {
        // Wrap in new HashSet to avoid ConcurrentModificationExceptions.
        Set<ResourceType> childTypes = new HashSet<ResourceType>(typeToBeRemoved.getChildResourceTypes());
        for (ResourceType childType : childTypes) {
            childType.removeParentResourceType(typeToBeRemoved);
            entityManager.merge(childType);
        }
    }

    private void updateType(ResourceType resourceType) {
        entityManager.flush();

        // see if there is already an existing type that we need to update
        if (log.isDebugEnabled()) {
            log.debug("Updating resource type [" + resourceType.getName() + "] from plugin ["
                + resourceType.getPlugin() + "]...");
        }

        ResourceType existingType;
        try {
            existingType = resourceTypeManager.getResourceTypeByNameAndPlugin(resourceType.getName(), resourceType
                .getPlugin());
        } catch (NonUniqueResultException nure) {
            log.debug("Found more than one existing ResourceType for " + resourceType);
            // TODO: Delete the redundant ResourceTypes to get the DB into a valid state.
            throw new IllegalStateException(nure);
        }

        // Connect the parent types if they exist, which they should.
        // We'll do this no matter if the resourceType exists or not - but we use existing vs. resourceType appropriately
        // This is to support the case when an existing type gets a new parent resource type in <runs-inside>
        updateParentResourceTypes(resourceType, existingType);

        if (existingType == null) {
            persistNewType(resourceType);
        } else {
            mergeExistingType(resourceType, existingType);
        }
    }

    private void mergeExistingType(ResourceType resourceType, ResourceType existingType) {
        log.debug("Merging type [" + resourceType + "] + into existing type [" + existingType + "]...");

        // Make sure to first add/update any subcategories on the parent before trying to update children.
        // Otherwise, the children may try to save themselves with subcategories which wouldn't exist yet.
        updateChildSubCategories(resourceType, existingType);

        entityManager.flush();

        // even though we've updated our child types to use new subcategory references, its still
        // not safe to delete the old sub categories yet, because we haven't yet deleted all of the old
        // child types which may still be referencing these sub categories

        // Update the rest of these related resources
        long startTime = System.currentTimeMillis();
        pluginConfigMetadataMgr.updatePluginConfigurationDefinition(existingType, resourceType);
        long endTime = System.currentTimeMillis();
        log.debug("Updated plugin configuration definition for ResourceType[" + toConciseString(existingType) + "] in "
            + (endTime - startTime) + " ms");

        resourceConfigMetadataMgr.updateResourceConfigurationDefinition(existingType, resourceType);

        measurementMetadataMgr.updateMetadata(existingType, resourceType);
        contentMetadataMgr.updateMetadata(existingType, resourceType);
        operationMetadataMgr.updateMetadata(existingType, resourceType);

        resourceMetadataManager.updateDriftMetadata(existingType, resourceType);

        updateProcessScans(resourceType, existingType);

        eventMetadataMgr.updateMetadata(existingType, resourceType);

        // Update the type itself
        existingType.setDescription(resourceType.getDescription());
        existingType.setCreateDeletePolicy(resourceType.getCreateDeletePolicy());
        existingType.setCreationDataType(resourceType.getCreationDataType());
        existingType.setSingleton(resourceType.isSingleton());
        existingType.setSupportsManualAdd(resourceType.isSupportsManualAdd());

        // We need to be careful updating the subcategory. If it is not null and the same ("equals")
        // to the new one, we need to copy over the attributes, as the existing will be kept and
        // the new one not persisted. Otherwise, we can just use the new one.
        ResourceSubCategory oldSubCat = existingType.getSubCategory();
        ResourceSubCategory newSubCat = resourceType.getSubCategory();
        if (oldSubCat != null && oldSubCat.equals(newSubCat)) {
            // Subcategory hasn't changed - nothing to do (call to addAndUpdateChildSubCategories()
            // above already took care of any modifications to the ResourceSubCategories themselves).
        } else if (newSubCat == null) {
            if (oldSubCat != null) {
                log.debug("Metadata update: Subcategory of ResourceType [" + resourceType.getName() + "] changed from "
                    + oldSubCat + " to " + newSubCat);
                existingType.setSubCategory(null);
            }
        } else {
            // New subcategory is non-null and not equal to the old subcategory.
            ResourceSubCategory existingSubCat = SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
                existingType, newSubCat.getName());
            if (existingSubCat == null)
                throw new IllegalStateException("Resource type [" + resourceType.getName() + "] in plugin ["
                    + resourceType.getPlugin() + "] has a subcategory (" + newSubCat.getName()
                    + ") which was not defined as a child subcategory of one of its ancestor resource types.");
            log.debug("Metadata update: Subcategory of ResourceType [" + resourceType.getName() + "] changed from "
                + oldSubCat + " to " + existingSubCat);
            existingType.setSubCategory(existingSubCat);
        }

        existingType = entityManager.merge(existingType);
        entityManager.flush();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDriftMetadata(ResourceType existingType, ResourceType resourceType) {
        boolean isSame = true;

        existingType = entityManager.find(ResourceType.class, existingType.getId());

        //
        // First, we need to see if the drift definitions have changed. If the existing and new drift definitions
        // are the same, then we can skip the update and do nothing. Only if one or more drift definitions
        // are different do we have to do anything to the persisted metadata.
        //

        Set<DriftDefinitionTemplate> existingDriftTemplates = existingType.getDriftDefinitionTemplates();
        Set<DriftDefinitionTemplate> newDriftTemplates = resourceType.getDriftDefinitionTemplates();
        if (existingDriftTemplates.size() != newDriftTemplates.size()) {
            isSame = false;
        } else {
            // note: the size of the sets are typically really small (usually between 0 and 3),
            // so iterating through them is fast.

            // look at all the configs to ensure we detect any changes to individual settings on the templates
            Set<String> existingNames = new HashSet<String>(existingDriftTemplates.size());
            Set<String> newNames = new HashSet<String>(newDriftTemplates.size());
            for (DriftDefinitionTemplate existingTemplate : existingDriftTemplates) {
                String existingName = existingTemplate.getName();
                Configuration existingConfig = existingTemplate.getConfiguration();

                existingNames.add(existingName); // for later

                for (DriftDefinitionTemplate newTemplate : newDriftTemplates) {
                    String newName = newTemplate.getName();
                    newNames.add(newName); // for later, do this here, not in the if-stmt below, so we can catch if new has names not in existing
                    if (newName.equals(existingName)) {
                        Configuration newConfig = newTemplate.getConfiguration();
                        if (!existingConfig.equals(newConfig)) {
                            isSame = false;
                        }
                        break;
                    }
                }

                if (!isSame) {
                    break;
                }
            }

            if (isSame) {
                // make sure they all have the same names and no duplicate names existed
                isSame = existingNames.equals(newNames) && existingNames.size() == existingDriftTemplates.size();
            }
        }

        //
        // If one or more drift definitions are different between new and existing,
        // then we need to remove the old drift definition and persist the new drift definition.
        //

        if (!isSame) {
            for (DriftDefinitionTemplate doomed : existingDriftTemplates) {
                entityManager.remove(doomed);
            }
            existingType.getDriftDefinitionTemplates().clear();

            for (DriftDefinitionTemplate toPersist : newDriftTemplates) {
                entityManager.persist(toPersist);
                existingType.addDriftDefinitionTemplate(toPersist);
            }
        }

        return;
    }

    private void persistNewType(ResourceType resourceType) {
        log.info("Persisting new ResourceType [" + toConciseString(resourceType) + "]...");
        // If the type didn't exist then we'll persist here which will cascade through
        // all child types as well as plugin and resource configs and their delegate types and
        // metric and operation definitions and their dependent types,
        // but first do some validity checking.

        // Check if the subcategories as children of resourceType are valid
        // Those are the subcategories we offer for children of us
        checkForValidSubcategories(resourceType.getChildSubCategories());

        // Check if we have a subcategory attached that needs to be linked to one of the parents
        // This is a subcategory of our parent where we are supposed to be grouped in.
        linkSubCategoryToParents(resourceType);

        entityManager.persist(resourceType);
        entityManager.flush();
    }

    private void linkSubCategoryToParents(ResourceType resourceType) {
        if (resourceType.getSubCategory() == null) {
            return; // Nothing to do
        }

        ResourceSubCategory mySubCategory = resourceType.getSubCategory();
        ResourceSubCategory existingCat = SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
            resourceType, mySubCategory.getName());
        if (existingCat != null) {
            resourceType.setSubCategory(existingCat);
        } else {
            throw new IllegalStateException("Subcategory " + mySubCategory.getName() + " defined on resource type "
                + resourceType.getName() + " in plugin " + resourceType.getPlugin()
                + " is not defined in a parent type");
        }
    }

    private void updateParentResourceTypes(ResourceType newType, ResourceType existingType) {
        if (log.isDebugEnabled()) {
            if (existingType != null) {
                log.debug("Setting parent types on existing type: " + existingType + " to ["
                    + newType.getParentResourceTypes() + "] - current parent types are ["
                    + existingType.getParentResourceTypes() + "]...");
            } else {
                log.debug("Setting parent types on new type: " + newType + " to [" + newType.getParentResourceTypes()
                    + "]...");
            }
        }

        Set<ResourceType> newParentTypes = newType.getParentResourceTypes();
        newType.setParentResourceTypes(new HashSet<ResourceType>());
        Set<ResourceType> originalExistingParentTypes = new HashSet<ResourceType>();
        if (existingType != null) {
            originalExistingParentTypes.addAll(existingType.getParentResourceTypes());
        }
        for (ResourceType newParentType : newParentTypes) {
            try {
                boolean isExistingParent = originalExistingParentTypes.remove(newParentType);
                if (existingType == null || !isExistingParent) {
                    ResourceType realParentType = (ResourceType) entityManager.createNamedQuery(
                        ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN).setParameter("name", newParentType.getName())
                        .setParameter("plugin", newParentType.getPlugin()).getSingleResult();
                    ResourceType type = (existingType != null) ? existingType : newType;
                    if (existingType != null) {
                        log.info("Adding ResourceType [" + toConciseString(type) + "] as child of ResourceType ["
                            + toConciseString(realParentType) + "]...");
                    }
                    realParentType.addChildResourceType(type);
                }
            } catch (NoResultException nre) {
                throw new RuntimeException("Couldn't persist type [" + newType + "] because parent [" + newParentType
                    + "] wasn't already persisted.");
            }
        }

        for (ResourceType obsoleteParentType : originalExistingParentTypes) {
            log.info("Removing type [" + toConciseString(existingType) + "] from parent type ["
                + toConciseString(obsoleteParentType) + "]...");
            obsoleteParentType.removeChildResourceType(existingType);
            moveResourcesToNewParent(existingType, obsoleteParentType, newParentTypes);
        }

        entityManager.flush();
    }

    private static String toConciseString(ResourceType type) {
        return (type != null) ? (type.getPlugin() + ":" + type.getName() + "(id=" + type.getId() + ")") : "null";
    }

    private void moveResourcesToNewParent(ResourceType existingType, ResourceType obsoleteParentType,
        Set<ResourceType> newParentTypes) {
        Subject overlord = subjectManager.getOverlord();
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceTypeId(existingType.getId());
        criteria.addFilterParentResourceTypeId(obsoleteParentType.getId());
        List<Resource> resources = resourceManager.findResourcesByCriteria(overlord, criteria);
        for (Resource resource : resources) {
            Resource newParent = null;
            newParentTypes: for (ResourceType newParentType : newParentTypes) {
                Resource ancestorResource = resource.getParentResource();
                while (ancestorResource != null) {
                    if (ancestorResource.getResourceType().equals(newParentType)) {
                        // We found an ancestor to be the new parent of our orphaned Resource.
                        newParent = ancestorResource;
                        break newParentTypes;
                    }
                    ancestorResource = ancestorResource.getParentResource();
                }
                for (Resource childResource : resource.getChildResources()) {
                    if (childResource.getResourceType().equals(newParentType)) {
                        // We found a child to be the new parent of our orphaned Resource.
                        // TODO: Check if there are are multiple children of the new parent type. If so,
                        //       log an error and don't move the resource.
                        newParent = childResource;
                        break newParentTypes;
                    }
                }
            }
            if (newParent != null) {
                if (resource.getParentResource() != null) {
                    resource.getParentResource().removeChildResource(resource);
                }
                newParent.addChildResource(resource);
                // Assigning a new parent changes the ancestry for the resource and its children. Since the
                // children are not handled in this method, update their ancestry now.
                resourceManager.updateAncestry(subjectManager.getOverlord(), resource.getId());
            } else {
                log.debug("We were unable to move " + resource + " from invalid parent " + resource.getParentResource()
                    + " to a new valid parent with one of the following types: " + newParentTypes);
            }
        }
    }

    private void checkForValidSubcategories(List<ResourceSubCategory> subCategories) {
        Set<String> subCatNames = new HashSet<String>();

        for (ResourceSubCategory subCategory : subCategories) {
            List<ResourceSubCategory> allSubcategories = getAllSubcategories(subCategory);
            for (ResourceSubCategory subCategory2 : allSubcategories) {
                if (subCatNames.contains(subCategory2.getName())) {
                    throw new RuntimeException("Subcategory [" + subCategory.getName() + "] is duplicated");
                }
                subCatNames.add(subCategory2.getName());
            }
        }
    }

    private List<ResourceSubCategory> getAllSubcategories(ResourceSubCategory cat) {
        List<ResourceSubCategory> result = new ArrayList<ResourceSubCategory>();

        if (cat.getChildSubCategories() != null) {
            for (ResourceSubCategory cat2 : cat.getChildSubCategories()) {
                result.addAll(getAllSubcategories(cat2));
            }
        }

        result.add(cat);
        return result;
    }

    /**
     * Update the set of process scans for a given resource type
     *
     * @param resourceType
     * @param existingType
     */
    private void updateProcessScans(ResourceType resourceType, ResourceType existingType) {
        Set<ProcessScan> existingScans = existingType.getProcessScans();
        Set<ProcessScan> newScans = resourceType.getProcessScans();

        Set<ProcessScan> scansToPersist = CollectionsUtil.missingInFirstSet(existingScans, newScans);
        Set<ProcessScan> scansToDelete = CollectionsUtil.missingInFirstSet(newScans, existingScans);

        Set<ProcessScan> scansToUpdate = CollectionsUtil.intersection(existingScans, newScans);

        // update scans that may have changed
        for (ProcessScan scan : scansToUpdate) {
            for (ProcessScan nScan : newScans) {
                if (scan.equals(nScan)) {
                    scan.setName(nScan.getName());
                }
            }
        }

        // persist new scans
        for (ProcessScan scan : scansToPersist) {
            existingType.addProcessScan(scan);
        }

        // remove deleted ones
        for (ProcessScan scan : scansToDelete) {
            existingScans.remove(scan);
            entityManager.remove(scan);
        }
    }

    /**
     * Updates the database with new child subcategory definitions found in the new resource type. Any definitions
     * common to both will be merged.
     *
     * @param newType      new resource type containing updated definitions
     * @param existingType old resource type with existing definitions
     */
    private void updateChildSubCategories(ResourceType newType, ResourceType existingType) {
        // we'll do the removal of all definitions that are in the existing type but not in the new type
        // once the child resource types have had a chance to stop referencing any old subcategories

        // Easy case: If the existing type did not have any definitions, simply save the new type defs and return
        if (existingType.getChildSubCategories() == null) {
            for (ResourceSubCategory newSubCategory : newType.getChildSubCategories()) {
                log.info("Metadata update: Adding new child SubCategory [" + newSubCategory.getName()
                    + "] to ResourceType [" + existingType.getName() + "]...");
                existingType.addChildSubCategory(newSubCategory);
                entityManager.persist(newSubCategory);
            }
            return;
        }

        // Merge definitions that were already in the existing type and also in the new type
        //
        // First, put the new subcategories in a map for easier access when iterating over the existing ones
        Map<String, ResourceSubCategory> subCategoriesFromNewType = new HashMap<String, ResourceSubCategory>(newType
            .getChildSubCategories().size());
        for (ResourceSubCategory newSubCategory : newType.getChildSubCategories()) {
            subCategoriesFromNewType.put(newSubCategory.getName(), newSubCategory);
        }

        // Second, loop over the sub categories that need to be merged and update and persist them
        List<ResourceSubCategory> mergedSubCategories = new ArrayList<ResourceSubCategory>(existingType
            .getChildSubCategories());
        mergedSubCategories.retainAll(subCategoriesFromNewType.values());
        for (ResourceSubCategory existingSubCat : mergedSubCategories) {
            updateSubCategory(existingSubCat, subCategoriesFromNewType.get(existingSubCat.getName()));
            entityManager.merge(existingSubCat);
        }

        // Persist all new definitions
        List<ResourceSubCategory> newSubCategories = new ArrayList<ResourceSubCategory>(newType.getChildSubCategories());
        newSubCategories.removeAll(existingType.getChildSubCategories());
        for (ResourceSubCategory newSubCat : newSubCategories) {
            log.info("Metadata update: Adding new child SubCategory [" + newSubCat.getName() + "] to ResourceType ["
                + existingType.getName() + "]...");
            existingType.addChildSubCategory(newSubCat);
            entityManager.persist(newSubCat);
        }
    }

    private void updateSubCategory(ResourceSubCategory existingSubCat, ResourceSubCategory newSubCategory) {
        // update the basic properties
        existingSubCat.update(newSubCategory);

        // we'll do the removal of all child subcategories that are in the existing subcat but not in the new one
        // once the child resource types have had a chance to stop referencing any old subcategories

        // Easy case: If the existing sub category did not have any child sub categories,
        // simply use the ones from the new type
        if ((existingSubCat.getChildSubCategories() == null) || existingSubCat.getChildSubCategories().isEmpty()) {
            for (ResourceSubCategory newChildSubCategory : newSubCategory.getChildSubCategories()) {
                log.debug("Metadata update: Adding new child SubCategory [" + newChildSubCategory.getName()
                    + "] to SubCategory [" + existingSubCat.getName() + "]...");
                existingSubCat.addChildSubCategory(newChildSubCategory);
                entityManager.persist(newChildSubCategory);
            }
            return;
        }

        // Merge definitions that were already in the existing sub cat and also in the new one
        //
        // First, put the new child sub categories in a map for easier access when iterating over the existing ones
        Map<String, ResourceSubCategory> childSubCategoriesFromNewSubCat = new HashMap<String, ResourceSubCategory>(
            newSubCategory.getChildSubCategories().size());
        for (ResourceSubCategory newChildSubCategory : newSubCategory.getChildSubCategories()) {
            childSubCategoriesFromNewSubCat.put(newChildSubCategory.getName(), newChildSubCategory);
        }

        // Second, loop over the sub categories that need to be merged and update and persist them
        List<ResourceSubCategory> mergedChildSubCategories = new ArrayList<ResourceSubCategory>(existingSubCat
            .getChildSubCategories());
        mergedChildSubCategories.retainAll(childSubCategoriesFromNewSubCat.values());
        for (ResourceSubCategory existingChildSubCategory : mergedChildSubCategories) {
            // recursively update childSubCategory
            updateSubCategory(existingChildSubCategory, childSubCategoriesFromNewSubCat.get(existingChildSubCategory
                .getName()));
            entityManager.merge(existingChildSubCategory);
        }

        // Persist all new definitions
        List<ResourceSubCategory> newChildSubCategories = new ArrayList<ResourceSubCategory>(newSubCategory
            .getChildSubCategories());
        newChildSubCategories.removeAll(existingSubCat.getChildSubCategories());
        for (ResourceSubCategory newChildSubCategory : newChildSubCategories) {
            log.info("Metadata update: Adding new child SubCategory [" + newChildSubCategory.getName()
                + "] to SubCategory [" + existingSubCat.getName() + "]...");
            existingSubCat.addChildSubCategory(newChildSubCategory);
            entityManager.persist(newChildSubCategory);
        }
    }

    /**
     * Remove all subcategory definitions that are in the existing type but not in the new type.
     *
     * @param newType      new resource type containing updated definitions
     * @param existingType old resource type with existing definitions
     */
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void removeObsoleteSubCategories(Subject subject, ResourceType newType, ResourceType existingType) {
        // Remove all definitions that are in the existing type but not in the new type
        existingType = entityManager.find(ResourceType.class, existingType.getId());
        List<ResourceSubCategory> removedSubCategories = new ArrayList<ResourceSubCategory>(existingType
            .getChildSubCategories());
        removedSubCategories.removeAll(newType.getChildSubCategories());
        for (ResourceSubCategory removedSubCat : removedSubCategories) {
            // remove it from the resourceType too, so we dont try to persist it again
            // when saving the type
            existingType.getChildSubCategories().remove(removedSubCat);
            entityManager.remove(removedSubCat);
        }

        // now need to recursively remove any child sub categories which no longer appear
        removeChildSubCategories(existingType.getChildSubCategories(), newType.getChildSubCategories());
        entityManager.flush();
    }

    private void removeChildSubCategories(List<ResourceSubCategory> existingSubCategories,
        List<ResourceSubCategory> newSubCategories) {
        // create a map of the new sub categories, for easier retrieval
        Map<String, ResourceSubCategory> mapOfNewSubCategories = new HashMap<String, ResourceSubCategory>(
            newSubCategories.size());
        for (ResourceSubCategory newSubCategory : newSubCategories) {
            mapOfNewSubCategories.put(newSubCategory.getName(), newSubCategory);
        }

        for (ResourceSubCategory existingSubCat : existingSubCategories) {
            // Remove all definitions that are in the existing type but not in the new type
            List<ResourceSubCategory> removedChildSubCategories = new ArrayList<ResourceSubCategory>(existingSubCat
                .getChildSubCategories());
            List<ResourceSubCategory> newChildSubCategories = mapOfNewSubCategories.get(existingSubCat.getName())
                .getChildSubCategories();
            removedChildSubCategories.removeAll(newChildSubCategories);
            for (ResourceSubCategory removedChildSubCat : removedChildSubCategories) {
                // remove subcat and all its children, due to the CASCADE.DELETE
                existingSubCat.removeChildSubCategory(removedChildSubCat);
                entityManager.remove(removedChildSubCat);
            }

            // for any remaining children of this subCat, see if any of their children should be removed
            removeChildSubCategories(existingSubCat.getChildSubCategories(), newChildSubCategories);
        }
    }

}
