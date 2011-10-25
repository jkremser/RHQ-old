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
package org.rhq.enterprise.server.test;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;
import org.rhq.test.JPAUtils;
import org.rhq.test.TransactionCallbackWithContext;

public abstract class LargeGroupTestBase extends AbstractEJB3Test {
    // for plugin configuration
    protected static final String PC_PROP1_NAME = "LargeGroupTest pc prop1";
    protected static final String PC_PROP1_VALUE = "LargeGroupTest pc property one";
    protected static final String PC_PROP2_NAME = "LargeGroupTest pc prop2";

    // for resource configuration
    protected static final String RC_PROP1_NAME = "LargeGroupTest rc prop1";
    protected static final String RC_PROP1_VALUE = "LargeGroupTest rc property one";
    protected static final String RC_PROP2_NAME = "LargeGroupTest rc prop2";

    protected ConfigurationManagerLocal configurationManager;
    protected ResourceManagerLocal resourceManager;
    protected ResourceGroupManagerLocal resourceGroupManager;
    protected SubjectManagerLocal subjectManager;

    protected class LargeGroupEnvironment {
        public Agent agent;
        public Resource platformResource; // all if its children will be members of the compatible group
        public ResourceType platformType;
        public ResourceType serverType;
        public ConfigurationDefinition serverPluginConfiguration;
        public ConfigurationDefinition serverResourceConfiguration;
        public ResourceGroup compatibleGroup;
        public Subject normalSubject; // user with a role that has access to the group
        public Role normalRole; // role with permissions on the group
        public Subject unauthzSubject; // a subject with no authorization to do anything
    }

    /**
     * Prepares things for the entire test class.
     */
    @BeforeClass
    public void beforeClass() {
        configurationManager = LookupUtil.getConfigurationManager();
        resourceManager = LookupUtil.getResourceManager();
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        subjectManager = LookupUtil.getSubjectManager();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        setupMockAgentServices(agentServiceContainer);

        prepareScheduler();
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        try {
            unprepareForTestAgents();
        } finally {
            unprepareScheduler();
        }
    }

    protected abstract void setupMockAgentServices(TestServerCommunicationsService agentServiceContainer);

    /**
     * Creates a compatible group of the given size. This also creates a normal user and a role
     * and makes sure that user can access the group that was created.  The role will be assigned the
     * given permissions and that role be placed on the new group and the new user.
     *
     * @param groupSize the number of members to create and put into the compatible group that is created.
     * @param permissions permissions to grant the new user via the new role. 
     * @return information about the entities that were created
     */
    protected LargeGroupEnvironment createLargeGroupWithNormalUserRoleAccess(final int groupSize,
        final Permission... permissions) {

        System.out.println("=====Creating a group with [" + groupSize + "] members");

        final LargeGroupEnvironment lge = new LargeGroupEnvironment();
        JPAUtils.executeInTransaction(new TransactionCallbackWithContext<Object>() {
            public Object execute(TransactionManager tm, EntityManager em) throws Exception {
                // create the agent where all resources will be housed
                lge.agent = SessionTestHelper.createNewAgent(em, "LargeGroupTestAgent");

                // create the platform resource type and server resource type
                // the server type will have both a plugin configuration definition and resource config def
                lge.platformType = new ResourceType("LargeGroupTestPlatformType", "testPlugin",
                    ResourceCategory.PLATFORM, null);
                em.persist(lge.platformType);

                lge.serverType = new ResourceType("LargeGroupTestServerType", "testPlugin", ResourceCategory.SERVER,
                    lge.platformType);
                lge.serverPluginConfiguration = new ConfigurationDefinition("LargeGroupTestPCDef", "pc desc");
                lge.serverPluginConfiguration.put(new PropertyDefinitionSimple(PC_PROP1_NAME, "pc prop1desc", false,
                    PropertySimpleType.STRING));
                lge.serverPluginConfiguration.put(new PropertyDefinitionSimple(PC_PROP2_NAME, "pc prop2desc", false,
                    PropertySimpleType.STRING));
                lge.serverType.setPluginConfigurationDefinition(lge.serverPluginConfiguration);

                lge.serverResourceConfiguration = new ConfigurationDefinition("LargeGroupTestRCDef", "rc desc");
                lge.serverResourceConfiguration.put(new PropertyDefinitionSimple(RC_PROP1_NAME, "rc prop1desc", false,
                    PropertySimpleType.STRING));
                lge.serverResourceConfiguration.put(new PropertyDefinitionSimple(RC_PROP2_NAME, "rc prop2desc", false,
                    PropertySimpleType.STRING));
                lge.serverType.setResourceConfigurationDefinition(lge.serverResourceConfiguration);

                em.persist(lge.serverType);
                em.flush();

                // create our platform - all of our server resources will have this as their parent
                lge.platformResource = SessionTestHelper.createNewResource(em, "LargeGroupTestPlatform",
                    lge.platformType);
                lge.platformResource.setAgent(lge.agent);

                // create our subject and role
                lge.normalSubject = new Subject("LargeGroupTestSubject", true, false);
                lge.normalRole = SessionTestHelper.createNewRoleForSubject(em, lge.normalSubject, "LargeGroupTestRole",
                    permissions);

                // create our unauthorized subject
                lge.unauthzSubject = SessionTestHelper.createNewSubject(em, "LargeGroupTestSubjectUnauth");

                // create our compatible group
                lge.compatibleGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, lge.normalRole,
                    "LargeGroupTestCompatGroup", lge.serverType);

                // create our many server resources
                System.out.print("   Creating resources, this might take some time");
                for (int i = 1; i <= groupSize; i++) {
                    System.out.print(((i % 100) == 0) ? String.valueOf(i) : ".");
                    Resource res = SessionTestHelper.createNewResourceForGroup(em, lge.compatibleGroup,
                        "LargeGroupTestServer", lge.serverType, (i % 100) == 0);
                    res.setAgent(lge.agent);
                    lge.platformResource.addChildResource(res);

                    // give it an initial plugin configuration
                    Configuration pc = new Configuration();
                    pc.put(new PropertySimple(PC_PROP1_NAME, PC_PROP1_VALUE));
                    pc.put(new PropertySimple(PC_PROP2_NAME, res.getId()));
                    em.persist(pc);
                    res.setPluginConfiguration(pc);
                }
                System.out.println("Done.");

                em.flush();
                return null;
            }
        });

        System.out.println("=====Created group [" + lge.compatibleGroup.getName() + "] with ["
            + lge.platformResource.getChildResources().size() + "] members");

        return lge;
    }

    /**
     * Purges all the entities that were created by the {@link #createLargeGroupWithNormalUserRoleAccess(int)} method.
     * This includes the user, role, agent and all resources along with the group itself.
     *
     * @param lge contains information that was created which needs to be deleted
     */
    protected void tearDownLargeGroupWithNormalUserRoleAccess(final LargeGroupEnvironment lge) throws Exception {
        System.out.println("=====Cleaning up test data from " + this.getClass().getSimpleName());

        // purge the group itself
        resourceGroupManager.deleteResourceGroup(getOverlord(), lge.compatibleGroup.getId());

        // purge all resources by performing in-band and out-of-band work in quick succession.
        // this takes a long time but trying to get this right using native queries is hard to get right so just do it this way.
        // only need to delete the platform which will delete all children servers AND the agent itself
        System.out.print("   Removing resources, this might take some time");
        final List<Integer> deletedIds = resourceManager.uninventoryResource(getOverlord(),
            lge.platformResource.getId());
        int i = deletedIds.size();
        for (Integer deletedResourceId : deletedIds) {
            System.out.print(((--i % 100) == 0) ? String.valueOf(i) : ".");
            resourceManager.uninventoryResourceAsyncWork(getOverlord(), deletedResourceId);
        }
        System.out.println(" Done.");

        // purge the users and role
        JPAUtils.executeInTransaction(new TransactionCallbackWithContext<Object>() {
            public Object execute(TransactionManager tm, EntityManager em) throws Exception {
                lge.normalRole = em.getReference(Role.class, lge.normalRole.getId());
                lge.normalSubject = em.getReference(Subject.class, lge.normalSubject.getId());
                lge.unauthzSubject = em.getReference(Subject.class, lge.unauthzSubject.getId());
                em.remove(lge.normalRole);
                em.remove(lge.normalSubject);
                em.remove(lge.unauthzSubject);
                return null;
            }
        });

        // purge the resource types
        JPAUtils.executeInTransaction(new TransactionCallbackWithContext<Object>() {
            public Object execute(TransactionManager tm, EntityManager em) throws Exception {
                ResourceType pType = em
                    .getReference(ResourceType.class, lge.platformResource.getResourceType().getId());
                ResourceType sType = em.getReference(ResourceType.class, lge.compatibleGroup.getResourceType().getId());
                em.remove(sType);
                em.remove(pType);
                return null;
            }
        });

        System.out.println("=====Cleaned up test data from " + this.getClass().getSimpleName());
    }

    /**
     * Given the id of a compatible group, this will return the status of the update.
     * 
     * @param groupId compatible group ID
     * @return status, or null if not known
     */
    protected ConfigurationUpdateStatus getGroupPluginConfigurationStatus(final int groupId) {
        return JPAUtils.executeInTransaction(new TransactionCallbackWithContext<ConfigurationUpdateStatus>() {
            public ConfigurationUpdateStatus execute(TransactionManager tm, EntityManager em) throws Exception {
                try {
                    Query query = em.createNamedQuery(GroupPluginConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID);
                    query.setParameter("groupId", groupId);
                    GroupPluginConfigurationUpdate latestConfigGroupUpdate = (GroupPluginConfigurationUpdate) query
                        .getSingleResult();
                    return latestConfigGroupUpdate.getStatus();
                } catch (NoResultException nre) {
                    // The group resource config history is empty, so there's obviously no update in progress.
                    return null;
                }
            }
        });
    }

    /**
     * Given the id of a compatible group, this will return the status of the update.
     * 
     * @param groupId compatible group ID
     * @return status, or null if not known
     */
    protected ConfigurationUpdateStatus getGroupResourceConfigurationStatus(final int groupId) {
        return JPAUtils.executeInTransaction(new TransactionCallbackWithContext<ConfigurationUpdateStatus>() {
            public ConfigurationUpdateStatus execute(TransactionManager tm, EntityManager em) throws Exception {
                try {
                    Query query = em.createNamedQuery(GroupResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID);
                    query.setParameter("groupId", groupId);
                    GroupResourceConfigurationUpdate latestConfigGroupUpdate = (GroupResourceConfigurationUpdate) query
                        .getSingleResult();
                    return latestConfigGroupUpdate.getStatus();
                } catch (NoResultException nre) {
                    // The group resource config history is empty, so there's obviously no update in progress.
                    return null;
                }
            }
        });
    }

    /**
     * Obtain the overlord user.
     * @return overlord
     */
    protected Subject getOverlord() {
        return subjectManager.getOverlord();
    }
}
