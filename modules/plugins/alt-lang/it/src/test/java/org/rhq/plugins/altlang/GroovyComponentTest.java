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

package org.rhq.plugins.altlang;

import static org.testng.Assert.*;

import org.apache.commons.io.FileUtils;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.PluginFinder;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class GroovyComponentTest extends AltLangComponentTest {

    private static final String ALT_LANG_TEST_SERVER = "GroovyServer";

    private Resource testServer;

    @Test
    public void verifyDiscovery() throws Exception {
        executeServerScanImmediately();
        testServer = findResourceInInventory(ALT_LANG_TEST_SERVER);
        assertNotNull(testServer, "Failed to discover " + ALT_LANG_TEST_SERVER);
    }

    @Test(dependsOnMethods = {"verifyDiscovery"})
    public void verifyResourceComponentStarted() throws Exception {
        File testDir = getTestDir();
        File startFile = new File(testDir, ALT_LANG_TEST_SERVER + ".start");

        assertTrue(startFile.exists(), "Resource component script may not have been called. Failed to find " +
            startFile.getName() + " which should have been generated by the script.");
    }

    @Test(dependsOnMethods = {"verifyResourceComponentStarted"})
    public void scriptShouldBeCalledToCheckResourceAvailability() throws Exception {
        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        Availability availability = inventoryMgr.getCurrentAvailability(testServer);

        assertEquals(
            availability.getAvailabilityType(),
            AvailabilityType.UP,
            "Resource component script may not have been called. Expected resource to be available."
        );
    }

    @Test(dependsOnMethods = {"verifyResourceComponentStarted"})
    public void scriptShouldBeCalledToInvokeOperation() throws Exception {
        PropertySimple msg = new PropertySimple("msg", "hello world");
        Configuration params = new Configuration();
        params.put(msg);

        String operationName = "echo";

        OperationResult result = invokeOperation(testServer.getId(), operationName, params);

        assertEquals(
            result.getSimpleResult(),
            msg.getStringValue(),
            "Operations script may not have been called. Got back the wrong results"
        );
    }

    @AfterClass
    public void scriptShouldBeCalledToStopResourceComponent() throws Exception {
        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        inventoryMgr.removeResource(testServer.getId());

        File testDir = getTestDir();
        File stopFile = new File(testDir, ALT_LANG_TEST_SERVER + ".stop");

        assertTrue(stopFile.exists(), "Resource component script may not have been called. Failed to find " +
            stopFile.getName() + " which should have been generated by the script");
    }    

}
