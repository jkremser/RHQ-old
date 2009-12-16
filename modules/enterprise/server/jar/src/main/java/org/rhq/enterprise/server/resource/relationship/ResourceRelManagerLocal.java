package org.rhq.enterprise.server.resource.relationship;

import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceRelAssignCriteria;
import org.rhq.core.domain.criteria.ResourceRelDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceRelHistoryCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.relationship.ResourceRelAssign;
import org.rhq.core.domain.resource.relationship.ResourceRelDefinition;
import org.rhq.core.domain.resource.relationship.ResourceRelHistory;

public interface ResourceRelManagerLocal {

    public Set<ResourceRelAssign> findRelationshipsByResource(Subject user, Resource resource);

    public Set<ResourceRelAssign> findRelationshipsByResources(Subject user, Resource firstResource,
        Resource secondResource);

    public Set<ResourceRelAssign> findRelationshipsByCriteria(Subject user, ResourceRelAssignCriteria assignCriteria);

    public void addRelationship(Subject user, ResourceRelAssign relationshipAssign);

    public void removeRelationship(Subject user, ResourceRelAssign relationshipAssign);

    public void updateRelationship(Subject user, ResourceRelAssign relationshipAssign);

    public ResourceRelDefinition getRelationshipDefinitionById(Subject user, int resourceDefinitionId);

    public ResourceRelDefinition getRelationshipDefinitionByName(Subject user, String name);

    public Set<ResourceRelDefinition> findRelationshipDefinitionsByCriteria(Subject user,
        ResourceRelDefinitionCriteria definitionCriteria);

    public void modifyRelationshipDefinition(Subject user, ResourceRelDefinition relationshipDefinition);

    public void removeRelationshipDefinition(Subject user, ResourceRelDefinition relationshipDefinition);

    public void addRelationshipDefinition(Subject user, ResourceRelDefinition relationshipDefinition);

    public Set<ResourceRelHistory> findRelationshipHistoriesByCriteria(Subject user,
        ResourceRelHistoryCriteria historyCriteria);
}
