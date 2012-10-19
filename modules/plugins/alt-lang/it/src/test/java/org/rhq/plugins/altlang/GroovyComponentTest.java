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

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static org.rhq.plugins.altlang.TestGroups.DISCOVERY;
import static org.rhq.plugins.altlang.TestGroups.GROOVY;
import static org.rhq.plugins.altlang.TestGroups.OPERATIONS;
import static org.rhq.plugins.altlang.TestGroups.RESOURCE_COMPONENT;

public class GroovyComponentTest extends AltLangComponentTest {

    private static final String ALT_LANG_TEST_SERVER = "GroovyServer";

    @Override
    protected String getServerName() {
        return ALT_LANG_TEST_SERVER;
    }

    @Test(groups = {DISCOVERY, GROOVY})
    public void scriptShouldBeCalledForDiscovery() throws Exception {
        verifyDiscoveryScriptCalled();
    }

    @Test(groups = {RESOURCE_COMPONENT, GROOVY}, dependsOnMethods = {"scriptShouldBeCalledForDiscovery"})
    public void scriptShouldBeCalledToStartResourceComponent() throws Exception {
        verifyResourceComponentStarted();
    }

    @Test(groups = {RESOURCE_COMPONENT, GROOVY}, dependsOnMethods = {"scriptShouldBeCalledToStartResourceComponent"})
    public void scriptShouldBeCalledToCheckResourceAvailability() throws Exception {
        verifyResourceAvailability();
    }

    @Test(groups = {GROOVY, OPERATIONS}, dependsOnMethods = {"scriptShouldBeCalledToStartResourceComponent"})
    public void scriptShouldBeCalledToInvokeOperation() throws Exception {
        verifyOperationInvoked();
    }

    @AfterClass(groups = {GROOVY, RESOURCE_COMPONENT})
    public void scriptShouldBeCalledToStopResourceComponent() throws Exception {
        verifyResourceComponentStopped();
    }    

}