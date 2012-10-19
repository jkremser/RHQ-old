/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.itest.nonpc;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.HostControllerComponent;
import org.rhq.modules.plugins.jbossas7.json.Remove;

/**
 * Tests around server groups
 *
 * @author Heiko W. Rupp
 */
public class ServerGroupTest extends AbstractIntegrationTest {

    public void createServerGroupViaApi() throws Exception {
        ASConnection connection = getASConnection();
        HostControllerComponent hcc = new HostControllerComponent();
        hcc.setConnection(connection);

        Configuration rc = new Configuration();
        rc.put(new PropertySimple("profile","default"));
        rc.put(new PropertySimple("socket-binding-group","standard-sockets"));
        ResourceType rt = new ResourceType("ServerGroup", PLUGIN_NAME, ResourceCategory.SERVICE, null);

        String serverGroupName = "_test-sg";
        try {
            CreateResourceReport report = new CreateResourceReport(serverGroupName, rt, new Configuration(), rc, null);
            report = hcc.createResource(report);

            assert report != null : "Report was null.";
            assert report.getStatus() == CreateResourceStatus.SUCCESS : "Create was a failure: " + report.getErrorMessage();
        } finally {
            Remove r = new Remove("server-group", serverGroupName);
            connection.execute(r);
        }
    }

    public void badCreateServerGroupViaApi() throws Exception {
        ASConnection connection = getASConnection();
        HostControllerComponent hcc = new HostControllerComponent();
        hcc.setConnection(connection);

        Configuration rc = new Configuration();
        rc.put(new PropertySimple("profile","luzibumpf")); // Does not exist op should fail
        rc.put(new PropertySimple("socket-binding-group","standard-sockets"));
        ResourceType rt = new ResourceType("ServerGroup", PLUGIN_NAME,
                ResourceCategory.SERVICE, null);

        String serverGroupName = "_test-sg";
        try {
            CreateResourceReport report = new CreateResourceReport(serverGroupName, rt, new Configuration(), rc, null);
            report = hcc.createResource(report);

            assert report != null : "Report was null.";
            assert report.getStatus() == CreateResourceStatus.FAILURE : "Is AS7-1430 solved ?";
            assert report.getException() == null : report.getException();
        } finally {
            Remove r = new Remove("server-group", serverGroupName);
            connection.execute(r);
        }
    }

}
