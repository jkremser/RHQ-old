/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.modcluster;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
@SuppressWarnings({ "rawtypes" })
public class ModclusterServerComponent extends MBeanResourceComponent {

    /* (non-Javadoc)
     * @see org.rhq.plugins.jmx.MBeanResourceComponent#getAvailability()
     */
    @Override
    public AvailabilityType getAvailability() {
        String rawProxyInfo = (String) getEmsBean().getAttribute("proxyInfo").refresh().toString();
        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);

        if (proxyInfo.getAvailableContexts().size() == 0) {
            return AvailabilityType.DOWN;
        }

        return super.getAvailability();
    }
}
