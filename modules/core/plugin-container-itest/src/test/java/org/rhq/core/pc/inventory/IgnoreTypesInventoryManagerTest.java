/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.rhq.core.pc.inventory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.MergeInventoryReportResults;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.testplugin.ManualAddDiscoveryComponent;
import org.rhq.core.pc.inventory.testplugin.TestResourceComponent;
import org.rhq.core.pc.inventory.testplugin.TestResourceDiscoveryComponent;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * Test the server ignoring resource types and notifying the {@link InventoryManager} about it.
 *
 * @author John Mazzitelli
 */
@RunDiscovery
public class IgnoreTypesInventoryManagerTest extends Arquillian {

    @Deployment(name = "test")
    @TargetsContainer("pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        RhqAgentPluginArchive pluginJar = ShrinkWrap.create(RhqAgentPluginArchive.class, "test-plugin.jar");
        return pluginJar.setPluginDescriptor("test-great-grandchild-discovery-plugin.xml").addClasses(
            TestResourceDiscoveryComponent.class, TestResourceComponent.class, ManualAddDiscoveryComponent.class);
    }

    @ArquillianResource
    private MockingServerServices serverServices;

    @ArquillianResource
    private PluginContainerConfiguration pluginContainerConfiguration;

    @ArquillianResource
    private PluginContainer pluginContainer;

    private Resource platform;
    private HashMap<String, Resource> simulatedInventory; // key == UUID

    private Collection<ResourceType> ignoredTypes;

    private CountDownLatch gotIgnoredTypeFromAgent;

    @BeforeDiscovery
    public void resetServerServices() throws Exception {
        platform = null;
        simulatedInventory = new HashMap<String, Resource>();
        gotIgnoredTypeFromAgent = new CountDownLatch(1); // will be open once we know agent discovered types we want ignored

        ignoredTypes = new HashSet<ResourceType>();
        ignoredTypes.add(new ResourceType("Test Service GrandChild", "test", null, null));
        ignoredTypes.add(new ResourceType("Manual Add Server", "test", null, null));

        serverServices.resetMocks();

        when(serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            mergeInventoryReport());
    }

    @AfterDiscovery
    public void afterDiscovery() throws Exception {
        // wait for agent discovery to start getting ignored types
        // actually, I don't think this latch will ever count down - if I did this right,
        // the first inventory report will tell the agent about the ignored types - before the agent
        // even attempted to discover resources of that type. Therefore, the agent will probably never
        // send up resources of the ignored types. But we have to wait anyway for the few inventory reports
        // that have to be received (which includes the resources of the unignored types).
        gotIgnoredTypeFromAgent.await(20L, TimeUnit.SECONDS);
    }

    @Test
    public void testIgnoreTypes() throws Exception {
        // make sure the agent inventory does not have any resources of the ignored types
        validatePartiallyIgnoredInventory();

        // simulate the unignoring of all types (i.e. don't ignore any types anymore)
        ignoredTypes.clear();

        // Now execute a full discovery again, this time, we should see the full inventory come in
        System.out.println("Executing full discovery...");
        InventoryManager inventoryManager = pluginContainer.getInventoryManager();
        InventoryReport report = inventoryManager.executeServerScanImmediately();
        inventoryManager.handleReport(report);
        report = inventoryManager.executeServiceScanImmediately();
        inventoryManager.handleReport(report);
        waitForFullInventory();
        validateFullInventory();
    }

    private void waitForFullInventory() throws Exception {
        long start = System.currentTimeMillis();
        while (getInventoryDepth(platform) < 5) {
            Thread.sleep(1000);
            if (System.currentTimeMillis() - start > 30000L) {
                break; // this should never take longer than 30s
            }
        }
        return;
    }

    private int getInventoryDepth(Resource root) {
        if (root == null) {
            return 0;
        }
        int maxDepth = 0;
        for (Resource c : root.getChildResources()) {
            int childDepth = getInventoryDepth(c);
            if (maxDepth < childDepth) {
                maxDepth = childDepth;
            }
        }
        return maxDepth + 1;
    }

    private Answer<MergeInventoryReportResults> mergeInventoryReport() {
        return new Answer<MergeInventoryReportResults>() {
            @Override
            public MergeInventoryReportResults answer(InvocationOnMock invocation) throws Throwable {
                InventoryReport inventoryReport = (InventoryReport) invocation.getArguments()[0];
                return simulateInventoryReportServerProcessing(inventoryReport);
            }
        };
    }

    private MergeInventoryReportResults simulateInventoryReportServerProcessing(InventoryReport inventoryReport) {
        ResourceSyncInfo syncInfo = null;
        if (inventoryReport.getAddedRoots() != null && !inventoryReport.getAddedRoots().isEmpty()) {
            for (Resource res : inventoryReport.getAddedRoots()) {
                persistInSimulatedInventory(res);
            }
            syncInfo = ResourceSyncInfo.buildResourceSyncInfo(platform);
        }
        return new MergeInventoryReportResults(syncInfo, ignoredTypes);
    }

    private void persistInSimulatedInventory(Resource res) {
        if (!ignoredTypes.contains(res.getResourceType())) {
            if (!simulatedInventory.containsKey(res.getUuid())) {
                Resource persisted = new Resource(res.getResourceKey(), res.getName(), res.getResourceType());
                persisted.setUuid(res.getUuid());
                persisted.setInventoryStatus(InventoryStatus.COMMITTED);
                simulatedInventory.put(persisted.getUuid(), persisted);
                if (res.getParentResource() == Resource.ROOT) {
                    platform = persisted;
                } else {
                    Resource parent = simulatedInventory.get(res.getParentResource().getUuid());
                    parent.addChildResource(persisted);
                }
            }
            for (Resource child : res.getChildResources()) {
                persistInSimulatedInventory(child);
            }
        } else {
            gotIgnoredTypeFromAgent.countDown();
        }
        return;
    }

    private void validateFullInventory() {
        System.out.println("Validating full inventory...");

        // get platform
        Resource platform = pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        // get top server
        Assert.assertEquals(platform.getChildResources().size(), 1, "plat children: " + platform.getChildResources());
        Resource server = platform.getChildResources().iterator().next();
        Assert.assertNotNull(server);
        Assert.assertEquals(server.getInventoryStatus(), InventoryStatus.COMMITTED);

        // get top server's immediate child service
        Assert.assertEquals(server.getChildResources().size(), 1, "srv children: " + server.getChildResources());
        Resource service = server.getChildResources().iterator().next();
        Assert.assertNotNull(service);
        Assert.assertEquals(service.getInventoryStatus(), InventoryStatus.COMMITTED);

        // get top server's grandchild service
        Assert.assertEquals(service.getChildResources().size(), 1, "svc children: " + service.getChildResources());
        Resource grandchild = service.getChildResources().iterator().next();
        Assert.assertNotNull(grandchild);
        Assert.assertEquals(grandchild.getInventoryStatus(), InventoryStatus.COMMITTED);

        // get top server's great-grandchild service
        Assert.assertEquals(grandchild.getChildResources().size(), 1, "svc grandch: " + grandchild.getChildResources());
        Resource greatgrandchild = grandchild.getChildResources().iterator().next();
        Assert.assertNotNull(greatgrandchild);
        Assert.assertEquals(greatgrandchild.getInventoryStatus(), InventoryStatus.COMMITTED);

        assert greatgrandchild.getChildResources().isEmpty() : "great grandchild should have no children";
    }

    private void validatePartiallyIgnoredInventory() {
        System.out.println("Validating partially ignored inventory...");

        // get platform
        Resource platform = pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        // get top server
        Assert.assertEquals(platform.getChildResources().size(), 1, "plat children: " + platform.getChildResources());
        Resource server = platform.getChildResources().iterator().next();
        Assert.assertNotNull(server);
        Assert.assertEquals(server.getInventoryStatus(), InventoryStatus.COMMITTED);

        // get top server's immediate child service
        Assert.assertEquals(server.getChildResources().size(), 1, "srv children: " + server.getChildResources());
        Resource service = server.getChildResources().iterator().next();
        Assert.assertNotNull(service);
        Assert.assertEquals(service.getInventoryStatus(), InventoryStatus.COMMITTED);

        // the grandchild service type is ignored, we should have no children from here on down
        assert service.getChildResources().isEmpty() : "grandchild should have been ignored";
    }
}
