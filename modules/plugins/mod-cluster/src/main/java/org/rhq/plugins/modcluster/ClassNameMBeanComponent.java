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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
@SuppressWarnings({ "rawtypes", "deprecation" })
public class ClassNameMBeanComponent extends MBeanResourceComponent<JMXComponent> {
    private static final Log log = LogFactory.getLog(WebappContextComponent.class);

    private static final String CLASS_NAME = "className";

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType beanAvailability = super.getAvailability();

        if (beanAvailability.equals(AvailabilityType.UP)) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getEmsBean().getClass().getClassLoader());

                String className = this.resourceContext.getPluginConfiguration().getSimple(CLASS_NAME).getStringValue();
                EmsBean emsBean = getEmsBean();

                if (className.equals(emsBean.getClassTypeName())) {
                    return AvailabilityType.UP;
                }
            } catch (Exception e) {
                log.info(e);
                return AvailabilityType.DOWN;
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        return AvailabilityType.DOWN;
    }
}
