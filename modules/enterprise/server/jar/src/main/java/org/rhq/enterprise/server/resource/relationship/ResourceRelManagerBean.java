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
package org.rhq.enterprise.server.resource.relationship;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceRelAssignCriteria;
import org.rhq.core.domain.criteria.ResourceRelDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceRelHistoryCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.relationship.RelationshipCardinality;
import org.rhq.core.domain.resource.relationship.ResourceRelAssign;
import org.rhq.core.domain.resource.relationship.ResourceRelDefinition;
import org.rhq.core.domain.resource.relationship.ResourceRelHistory;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;

/**
 * @author Noam Malki
 */
@Stateless
public class ResourceRelManagerBean implements ResourceRelManagerLocal {

    private final Log log = LogFactory.getLog(ResourceRelManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    ////

    public Set<ResourceRelAssign> findRelationshipsByResource(Subject user, Resource resource) {

        Query query;

        query = entityManager.createNamedQuery(ResourceRelAssign.QUERY_FIND_RELATIONSHIP_BY_RESOURCE);
        query.setParameter("resource", resource);

        Set<ResourceRelAssign> assigns = new HashSet<ResourceRelAssign>(query.getResultList());

        return assigns;

    }

    public Set<ResourceRelAssign> findRelationshipsByResources(Subject user, Resource firstResource,
        Resource secondResource) {
        Query query;

        query = entityManager.createNamedQuery(ResourceRelAssign.QUERY_FIND_RELATIONSHIP_BY_RESOURCES);
        query.setParameter("firstResource", firstResource);
        query.setParameter("secondResource", secondResource);

        Set<ResourceRelAssign> assigns = new HashSet<ResourceRelAssign>(query.getResultList());

        return assigns;
    }

    public Set<ResourceRelAssign> findRelationshipsByCriteria(Subject user, ResourceRelAssignCriteria assignCriteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(assignCriteria);

        Query query = generator.getQuery(entityManager);

        Set<ResourceRelAssign> results = new HashSet<ResourceRelAssign>(query.getResultList());

        return results;
    }

    public void addRelationship(Subject user, ResourceRelAssign relationshipAssign) throws RuntimeException {

        if (checkRelationshipTypes(relationshipAssign) == false) {
            throw new ResourceRelWrongTypeException(
                "The types of the relationship definition is different than the types of the resources of the relationship assign");
        }

        if (checkCrdinalirty(user, relationshipAssign) == false) {
            throw new ResourceRelCardinalityException(
                "Cannot add relationship assignment because of cardinality definition");
        }

        entityManager.persist(relationshipAssign);
    }

    public void removeRelationship(Subject user, ResourceRelAssign relationshipAssign) {

        ResourceRelHistory relationshipHistory = new ResourceRelHistory();
        relationshipHistory.setRelationshipDefinition(relationshipAssign.getRelationshipDefinition());
        relationshipHistory.setSourceResource(relationshipAssign.getSourceResource());
        relationshipHistory.setTargetResource(relationshipAssign.getTargetResource());
        relationshipHistory.setStartTime(relationshipAssign.getCtime());
        relationshipHistory.setEndTime(System.currentTimeMillis());

        entityManager.persist(relationshipHistory);
        entityManager.remove(relationshipAssign);
    }

    public void updateRelationship(Subject user, ResourceRelAssign relationshipAssign) {
        //TODO: check Cardinality
        ResourceRelAssign currentRelAssign = entityManager.find(ResourceRelAssign.class, relationshipAssign.getId());

        if (checkRelationshipTypes(relationshipAssign) == false) {
            throw new ResourceRelWrongTypeException(
                "The types of the relationship definition is different than the types of the resources of the relationship assign");
        }

        //if the definition is going to be changed
        if (currentRelAssign.getRelationshipDefinition().getId() != relationshipAssign.getRelationshipDefinition()
            .getId()) {
            if (checkCrdinalirty(user, relationshipAssign) == false) {
                throw new ResourceRelCardinalityException(
                    "Cannot add relationship assignment because of cardinality definition");
            }
        } else //if the definition is not going to be changed
        {
            //if the cardinality is one to many we need to check if we can change the source/target
            if (relationshipAssign.getRelationshipDefinition().getCardinality() == RelationshipCardinality.ONE_TO_MANY) {
                //in one to many we need to check just if the target is going to be changed because we need only one target for all the relationship assignment
                if (currentRelAssign.getTargetResource().getId() != relationshipAssign.getTargetResource().getId()) {
                    if (checkCrdinalirty(user, relationshipAssign) == false) {
                        throw new ResourceRelCardinalityException(
                            "Cannot add relationship assignment because of cardinality definition");
                    }
                }
            }
        }

        entityManager.merge(relationshipAssign);

    }

    private boolean checkRelationshipTypes(ResourceRelAssign relationshipAssign) {
        ResourceRelDefinition relationshipDefinition = relationshipAssign.getRelationshipDefinition();

        if (relationshipDefinition.getSourceResourceType() != null)
            if (relationshipDefinition.getSourceResourceType().getId() != relationshipAssign.getSourceResource()
                .getResourceType().getId()) {
                return false;
            }

        if (relationshipDefinition.getTargetResourceType() != null)
            if (relationshipDefinition.getTargetResourceType().getId() != relationshipAssign.getTargetResource()
                .getResourceType().getId()) {
                return false;
            }

        return true;
    }

    public ResourceRelDefinition getRelationshipDefinitionById(Subject user, int resourceDefinitionId) {
        return entityManager.find(ResourceRelDefinition.class, resourceDefinitionId);
    }

    public ResourceRelDefinition getRelationshipDefinitionByName(Subject user, String name) {
        Query query;

        query = entityManager.createNamedQuery(ResourceRelDefinition.QUERY_FIND_RELATIONSHIP_DEF_BY_NAME);
        query.setParameter("name", name);

        ResourceRelDefinition relationshipDefinition;

        try {
            relationshipDefinition = (ResourceRelDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }

        return relationshipDefinition;
    }

    public Set<ResourceRelDefinition> findRelationshipDefinitionsByCriteria(Subject user,
        ResourceRelDefinitionCriteria definitionCriteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(definitionCriteria);

        Query query = generator.getQuery(entityManager);

        Set<ResourceRelDefinition> results = new HashSet<ResourceRelDefinition>(query.getResultList());

        return results;
    }

    public void modifyRelationshipDefinition(Subject user, ResourceRelDefinition relationshipDefinition) {
        //TODO: check for null in all the if

        ResourceRelDefinition currentRelDef = entityManager.find(ResourceRelDefinition.class, relationshipDefinition
            .getId());

        if (currentRelDef.getId() != relationshipDefinition.getId())
            throw new ResourceRelChangeableException("Id field of ResourceRelDefinition is not changeable");

        if (!currentRelDef.getPlugin().equals(relationshipDefinition.getPlugin()))
            throw new ResourceRelChangeableException("Plugin field of ResourceRelDefinition is not changeable");

        if (currentRelDef.getSourceResourceType().getId() != relationshipDefinition.getSourceResourceType().getId())
            throw new ResourceRelChangeableException("Source Type field of ResourceRelDefinition is not changeable");

        if (currentRelDef.getTargetResourceType().getId() != relationshipDefinition.getTargetResourceType().getId())
            throw new ResourceRelChangeableException("Target Type field of ResourceRelDefinition is not changeable");

        //if changing the cardinality from many-many to any or from one-many to one-one then all the assignment will be deleted
        if ((currentRelDef.getCardinality() == RelationshipCardinality.MANY_TO_MANY && relationshipDefinition
            .getCardinality() != RelationshipCardinality.MANY_TO_MANY)
            || (currentRelDef.getCardinality() == RelationshipCardinality.ONE_TO_MANY && relationshipDefinition
                .getCardinality() != RelationshipCardinality.ONE_TO_ONE)) {

            ResourceRelAssignCriteria assignCriteria = new ResourceRelAssignCriteria();
            assignCriteria.addFilterRelationshipDefinitionId(relationshipDefinition.getId());
            Set<ResourceRelAssign> assigns = findRelationshipsByCriteria(user, assignCriteria);

            for (ResourceRelAssign assign : assigns) {
                removeRelationship(user, assign);
            }
        }

        entityManager.merge(relationshipDefinition);
    }

    public void removeRelationshipDefinition(Subject user, ResourceRelDefinition relationshipDefinition) {
        relationshipDefinition = entityManager.find(ResourceRelDefinition.class, relationshipDefinition.getId());

        ResourceRelAssignCriteria assignCriteria = new ResourceRelAssignCriteria();
        assignCriteria.addFilterRelationshipDefinitionId(relationshipDefinition.getId());
        Set<ResourceRelAssign> assigns = findRelationshipsByCriteria(user, assignCriteria);

        //cascade delete the relationship assigns
        for (ResourceRelAssign assign : assigns) {
            removeRelationship(user, assign);
        }

        ResourceRelHistoryCriteria historyCriteria = new ResourceRelHistoryCriteria();
        historyCriteria.addFilterRelationshipDefinitionId(relationshipDefinition.getId());
        Set<ResourceRelHistory> histories = findRelationshipHistoriesByCriteria(user, historyCriteria);

        //cascade delete the relationship history
        for (ResourceRelHistory history : histories) {
            removeRelationshipHistory(history);
        }

        entityManager.remove(relationshipDefinition);
    }

    public void addRelationshipDefinition(Subject user, ResourceRelDefinition relationshipDefinition) {
        entityManager.persist(relationshipDefinition);
    }

    public Set<ResourceRelHistory> findRelationshipHistoriesByCriteria(Subject user,
        ResourceRelHistoryCriteria historyCriteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(historyCriteria);

        Query query = generator.getQuery(entityManager);

        Set<ResourceRelHistory> results = new HashSet<ResourceRelHistory>(query.getResultList());

        return results;
    }

    private void removeRelationshipHistory(ResourceRelHistory relationshipHistory) {
        entityManager.remove(relationshipHistory);
    }

    private boolean checkCrdinalirty(Subject user, ResourceRelAssign relAssign) {
        ResourceRelDefinition relDefinition = relAssign.getRelationshipDefinition();
        if (relDefinition.getCardinality() == RelationshipCardinality.ONE_TO_ONE) {
            ResourceRelAssignCriteria assignCriteria = new ResourceRelAssignCriteria();
            assignCriteria.addFilterRelationshipDefinitionId(relDefinition.getId());
            Set<ResourceRelAssign> assigns = findRelationshipsByCriteria(user, assignCriteria);

            if (assigns.size() == 0)
                return true;
            else
                return false;
        }
        if (relDefinition.getCardinality() == RelationshipCardinality.ONE_TO_MANY) {
            ResourceRelAssignCriteria assignCriteria = new ResourceRelAssignCriteria();
            assignCriteria.addFilterRelationshipDefinitionId(relDefinition.getId());
            assignCriteria.addFilterTargetResourceId(relAssign.getTargetResource().getId());
            Set<ResourceRelAssign> assigns = findRelationshipsByCriteria(user, assignCriteria);

            if (assigns.size() == 0)
                return true;
            else {
                return false;
            }
        }
        return true;
    }
}
