/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.report.tag.TagCloudView;

/**
 * @author Greg Hinkle
 */
public class TagCloudPortlet extends TagCloudView implements Portlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "TagCloud";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.common_title_tag_cloud();

    public TagCloudPortlet(String locatorId) {
        super(locatorId);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        // This portlet has no configuration settings
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_tagCloud());
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId, EntityContext context) {

            return new TagCloudPortlet(locatorId);
        }
    }

}
