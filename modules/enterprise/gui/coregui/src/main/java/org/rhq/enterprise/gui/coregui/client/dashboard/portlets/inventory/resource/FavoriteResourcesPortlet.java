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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource;

import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.events.FieldStateChangedEvent;
import com.smartgwt.client.widgets.grid.events.FieldStateChangedHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;

/**
 * @author Greg Hinkle
 */
public class FavoriteResourcesPortlet extends ResourceSearchView implements AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "FavoriteResources";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_favoriteResources();

    public static final String CFG_TABLE_PREFS = "tablePreferences";

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;

    private Timer refreshTimer;

    public FavoriteResourcesPortlet() {
        super(createInitialCriteria());
        setOverflow(Overflow.VISIBLE);

        setShowHeader(false);
        setShowFooter(false);
    }

    private static Criteria createInitialCriteria() {
        Set<Integer> favoriteIds = UserSessionManager.getUserPreferences().getFavoriteResources();

        Integer[] favArray = favoriteIds.toArray(new Integer[favoriteIds.size()]);

        Criteria criteria = new Criteria();
        if (favoriteIds.isEmpty()) {
            criteria.addCriteria("id", -1);
        } else {
            criteria.addCriteria(ResourceDatasource.FILTER_RESOURCE_IDS, favArray);
        }

        return criteria;
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        getListGrid().addFieldStateChangedHandler(new FieldStateChangedHandler() {
            public void onFieldStateChanged(FieldStateChangedEvent fieldStateChangedEvent) {
                String state = getListGrid().getViewState();

                portletWindow.getStoredPortlet().getConfiguration().put(new PropertySimple(CFG_TABLE_PREFS, state));
                portletWindow.save();
            }
        });

        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();
        if ((null != storedPortlet && null != storedPortlet.getConfiguration())) {

            PropertySimple tablePrefs = storedPortlet.getConfiguration().getSimple(CFG_TABLE_PREFS);
            ListGrid listGrid = getListGrid();
            if (null != tablePrefs && null != listGrid) {
                String state = tablePrefs.getStringValue();
                listGrid.setViewState(state);
            }
        }
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_favoriteResources());
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new FavoriteResourcesPortlet();
        }
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycle(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return false;
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            super.refresh();
        }
    }

}
