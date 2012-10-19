/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.helper;

/**
 * @author Heiko Rupp
 */
public class HostPort {

    public String host;
    public int port;
    public boolean isLocal = true;
    public boolean withOffset = false;

    public HostPort() {
        host = "localhost";
        port = HostConfiguration.DEFAULT_MGMT_PORT;
        isLocal = true;
    }

    public HostPort(boolean local) {
        this();
        isLocal = local;
    }

    @Override
    public String toString() {
        return "HostPort{" + "host='" + host + '\'' + ", port=" + port + ", isLocal=" + isLocal + '}';
    }

}
