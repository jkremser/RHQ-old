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
package org.rhq.enterprise.server.install.remote;

import javax.ejb.Stateless;

import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;

/**
 * @author Greg Hinkle
 */
@Stateless
public class RemoteInstallManagerBean implements RemoteInstallManagerLocal, RemoteInstallManagerRemote {


    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AgentInstallInfo agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo) {
        return null;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String path) {

    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        SSHInstallUtility ssh = new SSHInstallUtility(remoteAccessInfo);
        ssh.connect();
        return ssh.pathDiscovery(parentPath);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo) {

    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo) {

    }
}
