package org.rhq.enterprise.server.resource.relationship.test;

import java.util.Set;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceRelAssignCriteria;
import org.rhq.core.domain.criteria.ResourceRelDefinitionCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.relationship.RelationshipCardinality;
import org.rhq.core.domain.resource.relationship.RelationshipConstraint;
import org.rhq.core.domain.resource.relationship.RelationshipType;
import org.rhq.core.domain.resource.relationship.ResourceRelAssign;
import org.rhq.core.domain.resource.relationship.ResourceRelDefinition;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.relationship.ResourceRelManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceRelationshipTest extends AbstractEJB3Test {

    private ResourceRelManagerLocal resourceRealManager;
    private SubjectManagerLocal subjectManager;
    private Resource srcResource;
    private Resource anotherSrcResource;
    private Resource trgResource;
    private ResourceType srcResourceType;
    private ResourceType trgResourceType;
    private ResourceRelDefinition relDefinition;
    private ResourceRelAssign relAssign;

    @BeforeSuite
    @SuppressWarnings( { "unused" })
    private void init() throws Throwable {
        resourceRealManager = LookupUtil.getResourceRelManager();
        //subjectManager = LookupUtil.getSubjectManager();

        srcResourceType = createNewResourceType();
        trgResourceType = createNewResourceType();

        srcResource = createNewResource(srcResourceType);
        anotherSrcResource = createNewResource(srcResourceType);
        trgResource = createNewResource(trgResourceType);
    }

    @Test
    public void testResourceDefinition() throws Throwable {
        //getTransactionManager().begin();
        try {

            EntityManager em = getEntityManager();

            Subject subject = null;// SessionTestHelper.createNewSubject(em, "fake subject");

            ResourceRelDefinition relationshipDefinition = getNewRelDef();

            resourceRealManager.addRelationshipDefinition(subject, relationshipDefinition);
            relationshipDefinition = resourceRealManager.getRelationshipDefinitionByName(subject,
                relationshipDefinition.getName());
            relationshipDefinition = resourceRealManager.getRelationshipDefinitionById(subject, relationshipDefinition
                .getId());

            //relationshipDefinition.setUserEditable(false);
            //resourceRealManager.modifyRelationshipDefinition(subject, relationshipDefinition);

            ResourceRelDefinitionCriteria definitionCriteria = new ResourceRelDefinitionCriteria();
            definitionCriteria.addFilterName(relationshipDefinition.getName());
            Set<ResourceRelDefinition> relations = resourceRealManager.findRelationshipDefinitionsByCriteria(subject,
                definitionCriteria);

            resourceRealManager.removeRelationshipDefinition(subject, relations.iterator().next());

            relDefinition = getNewRelDef();
            resourceRealManager.addRelationshipDefinition(subject, relDefinition);

            //getTransactionManager().commit();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            //getTransactionManager().rollback();
        }
    }

    @Test(dependsOnMethods = "testResourceDefinition")
    public void testRelationshipAssign() throws Throwable {
        getTransactionManager().begin();
        try {

            EntityManager em = getEntityManager();

            Subject subject = null;// SessionTestHelper.createNewSubject(em, "fake subject");

            relAssign = new ResourceRelAssign();
            relAssign.setRelationshipDefinition(relDefinition);
            relAssign.setSourceResource(srcResource);
            relAssign.setTargetResource(trgResource);
            resourceRealManager.addRelationship(subject, relAssign);

            Set<ResourceRelAssign> assigns = resourceRealManager.findRelationshipsByResource(subject, srcResource);
            assert assigns.size() == 1;

            assigns = resourceRealManager.findRelationshipsByResource(subject, trgResource);
            assert assigns.size() == 1;

            assigns = resourceRealManager.findRelationshipsByResources(subject, trgResource, srcResource);
            assert assigns.size() == 1;

            ResourceRelAssignCriteria assignCriteria = new ResourceRelAssignCriteria();
            assignCriteria.addFilterSourceResourceId(srcResource.getId());
            assignCriteria.addFilterRelationshipDefinitionName(relDefinition.getName());
            assignCriteria.addFilterId(relAssign.getId());
            assigns = resourceRealManager.findRelationshipsByCriteria(subject, assignCriteria);
            assert assigns.size() == 1;

            relAssign.setSourceResource(anotherSrcResource);
            resourceRealManager.updateRelationship(subject, relAssign);
            assigns = resourceRealManager.findRelationshipsByResource(subject, trgResource);
            assert assigns.iterator().next().getSourceResource().getId() == anotherSrcResource.getId();

            //resourceRealManager.removeRelationshipDefinition(subject, relDefinition);
            //resourceRealManager.removeRelationship(subject, relAssign);

            getTransactionManager().commit();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            //getTransactionManager().rollback();
        }
    }

    @Test(dependsOnMethods = "testRelationshipAssign")
    public void testRelationshipCascadeDelete() throws Throwable {
        try {
            Subject subject = null;// SessionTestHelper.createNewSubject(em, "fake subject");

            resourceRealManager.removeRelationshipDefinition(subject, relDefinition);

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            //getTransactionManager().rollback();
        }
    }

    private ResourceType createNewResourceType() throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        ResourceType resourceType;
        try {
            try {
                resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test", ResourceCategory.PLATFORM,
                    null);

                em.persist(resourceType);
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resourceType;
    }

    private Resource createNewResource(ResourceType resourceType) throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {

                Agent agent = new Agent("testagent" + System.currentTimeMillis(), "testaddress"
                    + System.currentTimeMillis(), 1, "", "testtoken" + System.currentTimeMillis());
                em.persist(agent);
                em.flush();

                resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
                resource.setAgent(agent);
                em.persist(resource);
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private ResourceRelDefinition getNewRelDef() {
        ResourceRelDefinition relationshipDefinition = new ResourceRelDefinition();
        relationshipDefinition.setName("testname" + System.currentTimeMillis());
        relationshipDefinition.setPlugin("tesePlugin");
        relationshipDefinition.setUserEditable(true);
        relationshipDefinition.setSourceConstraint(RelationshipConstraint.CASCADE_DELETE);
        relationshipDefinition.setCardinality(RelationshipCardinality.MANY_TO_MANY);
        relationshipDefinition.setType(RelationshipType.ASSOCIATION);

        return relationshipDefinition;
    }
}