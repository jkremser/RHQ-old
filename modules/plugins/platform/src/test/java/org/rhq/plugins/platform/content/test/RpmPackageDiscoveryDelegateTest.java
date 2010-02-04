/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.plugins.platform.content.test;

import java.util.Set;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.system.NativeSystemInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.platform.content.RpmPackageDiscoveryDelegate;

/**
 * @author Jason Dobies
 */
@Test(groups = "rpm")
public class RpmPackageDiscoveryDelegateTest {

    private static final boolean TESTS_ENABLED = true;

    private boolean rpmAvailable = true;

    @BeforeSuite
    public void init() {
        try {
            SystemInfo systemInfo = new NativeSystemInfo();
            RpmPackageDiscoveryDelegate.setSystemInfo(systemInfo);
            RpmPackageDiscoveryDelegate.checkExecutables();
        }
        catch (Exception e) {
            rpmAvailable = false;
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void discoverPackages() throws Exception {

        // Punch out early if RPMs aren't supported
        if (!rpmAvailable) {
            return;
        }

        Set<ResourcePackageDetails> packages = RpmPackageDiscoveryDelegate.discoverPackages(null);
        System.out.println("Loaded [" + packages.size() + "] packages");

        assert packages.size() > 0 : "Zero packages found from discovery";
    }

}
