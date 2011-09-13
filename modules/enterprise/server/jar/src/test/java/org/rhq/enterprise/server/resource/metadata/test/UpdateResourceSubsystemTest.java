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
package org.rhq.enterprise.server.resource.metadata.test;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Note, plugins are registered in new transactions. for tests, this means
 * you can't do everything in a trans and roll back at the end. You must clean up manually.
 */
public class UpdateResourceSubsystemTest extends UpdatePluginMetadataTestBase {

    private static final boolean ENABLED = true;

    @Override
    protected String getSubsystemDirectory() {
        return "resource";
    }

    @Test(enabled = ENABLED)
    public void testSingleSubCategoryCreate() throws Exception {
        try {
            registerPlugin("one-subcat-v1_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            assertAppsSubCategory(subCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryCreate");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSingleSubCategoryAddFromEmpty() throws Exception {
        try {
            registerPlugin("no-subcat.xml", "1.0");
            registerPlugin("one-subcat-v1_0.xml", "2.0");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            assertAppsSubCategory(subCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryAddFromEmpty");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSingleSubCategoryAddSibling() throws Exception {
        try {
            registerPlugin("one-subcat-v1_0.xml", "1.0");
            registerPlugin("two-subcat.xml", "2.0");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 2, 0);
            assertAppsSubCategory(subCat);

            subCat = assertSubCategory(server1.getChildSubCategories(), 2, 1);
            assertServicesSubCategory(subCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryAddSibling");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSingleSubCategoryReplace() throws Exception {
        try {
            registerPlugin("one-subcat-v1_0.xml");
            registerPlugin("one-subcat-v2_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            assertServicesSubCategory(subCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryReplace");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSingleSubCategoryRemoveOneFromTwo() throws Exception {
        try {
            registerPlugin("two-subcat.xml", "1.0");
            registerPlugin("one-subcat-v2_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            assertServicesSubCategory(subCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryRemoveOneFromTwo");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSingleSubCategoryUpdate() throws Exception {
        try {
            registerPlugin("one-subcat-v1_0.xml");
            registerPlugin("one-subcat-v1_1.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            assertApps2SubCategory(subCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryUpdate");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSingleSubCategoryRemove() throws Exception {
        try {
            registerPlugin("one-subcat-v1_0.xml");
            registerPlugin("no-subcat.xml", "2.0");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            assertSubCategory(server1.getChildSubCategories(), 0, null);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryRemove");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryReplace() throws Exception {
        try {
            registerPlugin("nested-subcat-v1_0.xml");
            registerPlugin("nested-subcat-v2_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            assertServicesSubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryReplace");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryAddFromEmpty() throws Exception {
        try {
            registerPlugin("no-subcat.xml", "0.0");
            registerPlugin("nested-subcat-v1_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            assertAppsSubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryAddFromEmpty");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryRemoveOneFromTwo() throws Exception {
        try {
            registerPlugin("nested-subcat-2children.xml", "1.0");
            registerPlugin("nested-subcat-v2_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            assertServicesSubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryRemoveOneFromTwo");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryAddSibling() throws Exception {
        try {
            registerPlugin("nested-subcat-v2_0.xml");
            registerPlugin("nested-subcat-2children.xml", "3.0");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 2, 1);
            assertAppsSubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryAddSibling");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryRemoveChild() throws Exception {
        try {
            registerPlugin("nested-subcat-grandchild.xml", "1.0");
            registerPlugin("nested-subcat-v2_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertServicesSubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryRemoveChild");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryAddChild() throws Exception {
        try {
            registerPlugin("nested-subcat-v2_0.xml");
            registerPlugin("nested-subcat-grandchild.xml", "3.0");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            ResourceSubCategory grandChildSubCat = assertSubCategory(childSubCat.getChildSubCategories(), 1, 0);
            assertAppsSubCategory(grandChildSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryAddChild");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryUpdate() throws Exception {
        try {
            registerPlugin("nested-subcat-v1_0.xml");
            registerPlugin("nested-subcat-v1_1.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertApps2SubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryUpdate");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryAdd() throws Exception {
        try {
            registerPlugin("one-subcat-v3_0.xml");
            registerPlugin("nested-subcat-v1_1.xml", "4.0");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertApps2SubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryAdd");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryCreate() throws Exception {
        try {
            registerPlugin("nested-subcat-v1_1.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertApps2SubCategory(childSubCat);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryCreate");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryRemoveAll() throws Exception {
        try {
            registerPlugin("nested-subcat-v1_0.xml");
            registerPlugin("no-subcat.xml", "2.0");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            assertSubCategory(server1.getChildSubCategories(), 0, null);
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryRemoveAll");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryCreateWithServices() throws Exception {
        try {
            registerPlugin("nested-subcat-services-v1_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 3;

            ResourceType services = getResourceType("testService1");
            assertServicesSubCategory(services.getSubCategory());

            ResourceType apps = getResourceType("testApp1");
            assertAppsSubCategory(apps.getSubCategory());
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryCreateWithServices");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryCreateWithServices2() throws Exception {
        try {
            registerPlugin("nested-subcat-services-v2_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 2;

            ResourceType services = getResourceType("testService2");
            assert services.getSubCategory().getName().equals("applications2");

            ResourceType apps = getResourceType("testApp1");
            assert apps.getSubCategory().getName().equals("applications2");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryCreateWithServices2");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNestedSubCategoryUpdateWithServices() throws Exception {
        try {
            registerPlugin("nested-subcat-services-v1_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 3;
            List<ResourceSubCategory> subCategories = server1.getChildSubCategories();
            assert subCategories != null;
            assert subCategories.size() == 1; // subcat with name "parent"
            // TODO check for 2 children of this subcategory
            getTransactionManager().rollback();

            registerPlugin("nested-subcat-services-v2_0.xml");
            server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            children = server1.getChildResourceTypes();
            assert children.size() == 2 : "Expected 2 children, but got " + children.size();
            subCategories = server1.getChildSubCategories(); // "testServer1"
            assert subCategories != null;
            assert subCategories.size() == 1; // Subcat with name "parent"
            ResourceSubCategory parent = subCategories.get(0);
            assert parent != null;
            assert parent.getName().equals("parent") : "Name was not 'parent', but " + parent.getName();
            subCategories = parent.getChildSubCategories();
            assert subCategories != null;
            assert subCategories.size() == 1; // SubSubcat with name "applications2"
            ResourceSubCategory app2 = subCategories.get(0);
            assert app2 != null;
            assert app2.getName().equals("applications2") : "Name was not 'applications2', but " + app2.getName();

            // Now the services within <server name="testServer1"/> ...
            ResourceType service2 = getResourceType("testService2");
            assert service2 != null;
            assert service2.getParentResourceTypes().iterator().next().equals(server1);
            subCategories = service2.getChildSubCategories();
            assert subCategories.isEmpty() : "Expected subcategories to be empty, but was " + subCategories;

            ResourceSubCategory subCategory = service2.getSubCategory(); // the subcategory attribute
            // TODO the subCategory is currently not persisted to the DB, so can not be found
            subCategory = em.find(ResourceSubCategory.class, subCategory.getId());
            String name = subCategory.getName();
            assert name.equals("applications2") : "Expected 'applictions2', but got " + name;

            ResourceType apps = getResourceType("testApp1");
            assert apps.getSubCategory().getName().equals("applications2");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testNestedSubCategoryUpdateWithServices");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testRemoveService() throws Exception {
        try {
            registerPlugin("services-v1_0.xml");
            registerPlugin("services-v2_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 2 : "Incorrect number of child resource types, should have been 2 but was ["
                + children.size() + "].";
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testRemoveService");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSimpleSubCategoryCreate() throws Exception {
        try {
            registerPlugin("test-subcategories.xml");
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            assert subCat.getName().equals("applications");

            ResourceType server2 = getResourceType("testServer2");
            server2 = em.find(ResourceType.class, server2.getId());

            assert server2.getChildSubCategories() != null;
            assert server2.getChildSubCategories().size() == 2 : "Unexpected number of subcategories ["
                + server2.getChildSubCategories().size() + "]";
            subCat = server2.getChildSubCategories().get(1);
            assert subCat.getName().equals("resource");

            List<ResourceSubCategory> childSubCats = subCat.getChildSubCategories();
            assert childSubCats.size() == 2;
            subCat = childSubCats.get(1);
            assert subCat.getName().equals("destinations");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSimpleSubCategoryCreate");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSimpleSubCategoryUpdate() throws Exception {
        try {
            registerPlugin("test-subcategories.xml", "1.0");
            // pretend to be an updated plugin
            registerPlugin("test-subcategories2.xml", "2.0");
            // now test how the subcategories got updated
            ResourceType server1 = getResourceType("testServer1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            server1 = em.find(ResourceType.class, server1.getId());

            ResourceSubCategory subCat = assertSubCategory(server1.getChildSubCategories(), 1, 0);
            assertApps2SubCategory(subCat);

            ResourceType server2 = getResourceType("testServer2");
            server2 = em.find(ResourceType.class, server2.getId());

            assert server2.getChildSubCategories() != null;

            assert server2.getChildSubCategories().size() == 2 : "Unexpected number of subcategories ["
                + server2.getChildSubCategories().size() + "]";
            int found = 0;
            ResourceSubCategory resourceSubCat = null;
            for (int i = 0; i <= 1; i++) {
                ResourceSubCategory subCategory = server2.getChildSubCategories().get(i);
                String name = subCategory.getName();
                if ("services2".equals(name) || "resource".equals(name))
                    found++;
                if ("resource".equals(name))
                    resourceSubCat = subCategory;
            }
            assert found == 2;
            assert resourceSubCat != null;

            List<ResourceSubCategory> childSubCats = resourceSubCat.getChildSubCategories();
            assert childSubCats.size() == 2;
            subCat = childSubCats.get(1);
            assert subCat.getName().equals("destinations");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSimpleSubCategoryUpdate");
            }
        }
    }

    /**
     * Test if illegal subcategories will make the deployer bail out as
     * expected.
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testAddIllegalSubcategory1() throws Exception {
        try {
            registerPlugin("illegal-subcat-1.xml");
            fail("Exception was not thrown.");
        } catch (Throwable t) {
            // expected
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testAddIllegalSubcategory1");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testReferenceToUndefinedChildSubCategory() throws Exception {
        System.out.println("= testReferenceToUndefinedChildSubCategory");
        try {
            try {
                registerPlugin("undefined-child-subcat-1.xml");
                fail("Exception was not thrown.");
            } catch (Exception ignored) {
                // expected
            }
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testReferenceToUndefinedChildSubCategory");
            }
        }
    }

    private ResourceSubCategory assertSubCategory(List<ResourceSubCategory> subCats, Integer size, Integer index) {
        assert subCats != null;
        assert subCats.size() == size : "Unexpected number of Sub categories. Expected [" + size + "] got ["
            + subCats.size() + "].";
        ResourceSubCategory subCat = null;
        if (index != null) {
            subCat = subCats.get(index);
        }

        return subCat;
    }

    private void assertServicesSubCategory(ResourceSubCategory subCat) {
        assert subCat != null;
        assert subCat.getName().equals("services");
        assert subCat.getDisplayName().equals("Services");
        assert subCat.getDescription().equals("Some services");
    }

    private void assertAppsSubCategory(ResourceSubCategory subCat) {
        assert subCat != null;
        assert subCat.getName().equals("applications");
        assert subCat.getDisplayName().equals("Apps");
        assert subCat.getDescription().equals("The apps.");
    }

    private void assertApps2SubCategory(ResourceSubCategory subCat) {
        assert subCat != null;
        assert subCat.getName().equals("applications");
        assert subCat.getDisplayName().equals("Apps2");
        assert subCat.getDescription().equals("The apps2.");
    }

}