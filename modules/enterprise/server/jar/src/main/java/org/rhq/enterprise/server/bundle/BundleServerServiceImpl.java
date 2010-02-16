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
package org.rhq.enterprise.server.bundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.bundle.BundleUpdateComplete;

/**
 * Server-side implementation of the <code>BundleServerService</code>. This implmentation simply forwards
 * the requests to the appropriate session bean.
 *
 * @author John Mazzitelli
 */
public class BundleServerServiceImpl implements BundleServerService {
    private final Log log = LogFactory.getLog(this.getClass());

    public void updateComplete(BundleUpdateComplete buc) {
        // TODO Auto-generated method stub
    }
}