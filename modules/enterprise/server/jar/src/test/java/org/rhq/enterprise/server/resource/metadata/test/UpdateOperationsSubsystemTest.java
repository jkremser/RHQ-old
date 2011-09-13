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
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Note, plugins are registered in new transactions. for tests, this means
 * you can't do everything in a trans and roll back at the end. You must clean up manually.
 */
public class UpdateOperationsSubsystemTest extends UpdatePluginMetadataTestBase {

    private static final boolean ENABLED = true;

    @Override
    protected String getSubsystemDirectory() {
        return "operation";
    }

    /**
     * Check updates of artifacts and operations
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testOperationAndArtifactUpdates() throws Exception {
        System.out.println("= testOperationAndArtifactUpdates");
        try {
            registerPlugin("update3-v1_0.xml");
            ResourceType platform1 = getResourceType("myPlatform3");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform1 = em.find(ResourceType.class, platform1.getId());

            Set<PackageType> packageTypes = platform1.getPackageTypes();
            assert packageTypes.size() == 3 : "Did not find the three expected package types in v1";
            Set<OperationDefinition> ops = platform1.getOperationDefinitions();
            assert ops.size() == 3 : "Did not find three expected operations in v1";
            getTransactionManager().rollback();

            /*
             * Now deploy the changed version of the plugin
             */
            registerPlugin("update3-v2_0.xml");
            ResourceType platform2 = getResourceType("myPlatform3");
            getTransactionManager().begin();
            em = getEntityManager();
            platform2 = em.find(ResourceType.class, platform2.getId());

            Set<PackageType> packageTypes2 = platform2.getPackageTypes();
            assert packageTypes2.size() == 3 : "Did not find the expected three package types in v2";
            Set<OperationDefinition> opDefs = platform2.getOperationDefinitions();
            assert opDefs.size() == 3 : "Did not find the three expected operations in v2";
            // now that the basics are tested, go for the details...

            boolean ubuFound = false;
            for (PackageType pt : packageTypes2) {
                //            System.out.println(at.getName());
                assert !(pt.getName().equals("rpm")) : "RPM should be gone in v2";
                if (pt.getName().equals("ubu")) {
                    ubuFound = true;
                }
            }

            assert ubuFound == true : "Ubu should be in v2";

            boolean startFound = false;
            for (OperationDefinition opDef : opDefs) {
                //            System.out.println(opDef.getName());
                assert !(opDef.getName().equals("restart")) : "Restart should be gone in v2";
                if (opDef.getName().equals("start")) {
                    startFound = true;
                }

                if (opDef.getName().equals("status")) {
                    assert opDef.getDescription().equals("Yadda!") : "Description for 'start' should be 'Yadda!', but was "
                        + opDef.getDescription();
                }
            }

            assert startFound == true : "Start should be in v2";
            getTransactionManager().rollback();

            /*
             * Now try the other way round
             */

            registerPlugin("update3-v1_0.xml", "3.0");
            ResourceType platform3 = getResourceType("myPlatform3");
            getTransactionManager().begin();
            em = getEntityManager();
            platform3 = em.find(ResourceType.class, platform3.getId());

            Set<PackageType> packageTypes3 = platform3.getPackageTypes();
            assert packageTypes3.size() == 3 : "Did not find the three package types in v3";
            Set<OperationDefinition> ops3 = platform3.getOperationDefinitions();
            assert ops3.size() == 3 : "Did not find three expected operations in v3";

            // we should have rpm, deb, mpkg. ubu from v2 should be gone again.
            boolean rpmFound = false;
            for (PackageType pt : packageTypes3) {
                System.out.println(pt.getName());
                assert !(pt.getName().equals("ubu")) : "ubu should be gone in v3";
                if (pt.getName().equals("rpm")) {
                    rpmFound = true;
                }
            }

            assert rpmFound == true : "rpm should be in v3";

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testOperationAndArtifactUpdates");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testAddRemoveOperationWithParams() throws Exception {
        System.out.println("= testAddRemoveOperationWithParams");
        try {
            registerPlugin("operation1-1.xml");
            ResourceType platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            Set<OperationDefinition> opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 1;
            OperationDefinition def = opDefs.iterator().next();
            assert def.getName().equals("sleep");
            assert def.getParametersConfigurationDefinition() == null;
            getTransactionManager().rollback();

            System.out.println("==> Done with v1");

            registerPlugin("operation1-2.xml");
            platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 2;

            for (OperationDefinition odef : opDefs) {
                if (odef.getName().equals("invokeSql")) {
                    assert odef.getDescription().startsWith("Execute");
                    ConfigurationDefinition conf = odef.getParametersConfigurationDefinition();
                    assert conf != null;
                    Map<String, PropertyDefinition> props = conf.getPropertyDefinitions();
                    assert props.size() == 2;
                    for (PropertyDefinition pd : props.values()) {
                        PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;
                        if (pds.getName().equals("sleep")) {
                            assert pds.getDescription() == null;
                        }
                        if (pds.getName().equals("invokeSql")) {
                            List<PropertyDefinitionEnumeration> pde = pds.getEnumeratedValues();
                            assert pde.size() == 2;
                        }
                    }

                    conf = odef.getResultsConfigurationDefinition();
                    assert conf != null;
                }
            }
            getTransactionManager().rollback();

            System.out.println("==> Done with v2");

            registerPlugin("operation1-1.xml", "3.0");
            platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 1;

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testAddRemoveOperationWithParams");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testAddRemoveOperationWithParams2() throws Exception {
        System.out.println("= testAddRemoveOperationWithParams2");
        try {
            registerPlugin("operation2-1.xml");
            ResourceType platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            Set<OperationDefinition> opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 3 : "Did not find the expected 3 defs, but " + opDefs.size();

            int found = 0;
            for (OperationDefinition def : opDefs) {
                if (containedIn(def.getName(), new String[] { "sleep", "wakeup", "getup" }))
                    found++;
            }
            assert found == 3 : "Did not find all 3 expected operations";
            getTransactionManager().rollback();

            System.out.println("==> Done with v1");

            registerPlugin("operation2-1.xml");
            System.out.println("==> Done with v1 (2)");

            registerPlugin("operation2-2.xml");
            platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 4 : "Did not find the expected 4 defs, but " + opDefs.size();
            found = 0;
            for (OperationDefinition def : opDefs) {
                if (containedIn(def.getName(), new String[] { "wakeup", "getup", "eat", "goToWork" }))
                    found++;
            }
            assert found == 4 : "Did not find all 4 expected operations";
            getTransactionManager().rollback();

            System.out.println("==> Done with v2");

            registerPlugin("operation2-1.xml", "3.0");
            platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 3 : "Did not find the expected 3 defs, but " + opDefs.size();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testAddRemoveOperationWithParams2");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testAddRemoveOperationWithGrouping() throws Exception {
        System.out.println("= testAddRemoveOperationWithGrouping");
        try {
            registerPlugin("operation3-1.xml");
            ResourceType platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            Set<OperationDefinition> opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 3 : "Did not find the expected 3 defs, but " + opDefs.size();

            int found = 0;
            for (OperationDefinition def : opDefs) {
                if (containedIn(def.getName(), new String[] { "sleep", "wakeup", "getup" }))
                    found++;
            }
            assert found == 3 : "Did not find all 3 expected operations";
            getTransactionManager().rollback();

            System.out.println("==> Done with v1");

            registerPlugin("operation3-1.xml");
            System.out.println("==> Done with v1 (2)");

            registerPlugin("operation3-2.xml");
            platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            opDefs = platform.getOperationDefinitions();
            assert opDefs != null;

            assert opDefs.size() == 4 : "Did not find the expected 4 defs, but " + opDefs.size();
            found = 0;
            for (OperationDefinition def : opDefs) {
                if (containedIn(def.getName(), new String[] { "wakeup", "getup", "eat", "goToWork" }))
                    found++;
            }
            assert found == 4 : "Did not find all 4 expected operations";
            getTransactionManager().rollback();

            System.out.println("==> Done with v2");

            registerPlugin("operation3-1.xml", "3.0");
            platform = getResourceType("ops");
            assert platform != null;
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            opDefs = platform.getOperationDefinitions();
            assert opDefs != null;
            assert opDefs.size() == 3 : "Did not find the expected 3 defs, but " + opDefs.size();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testAddRemoveOperationWithGrouping");
            }
        }
    }
}
