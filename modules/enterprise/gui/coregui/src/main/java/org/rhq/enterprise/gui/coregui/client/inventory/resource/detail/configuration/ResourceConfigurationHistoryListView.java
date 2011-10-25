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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * The main view that lists all resource configuration history items.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ResourceConfigurationHistoryListView extends AbstractConfigurationHistoryListView<ResourceConfigurationHistoryDataSource> {
    public static final ViewName VIEW_ID = new ViewName("ConfigurationHistoryView", MSG
        .view_configurationHistoryList_title());

    /**
     * Use this constructor to view config histories for all viewable Resources.
     */
    public ResourceConfigurationHistoryListView(String locatorId, boolean hasWritePerm) {
        super(locatorId, VIEW_ID.getTitle(), hasWritePerm);
        ResourceConfigurationHistoryDataSource datasource = new ResourceConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    /**
     * Use this constructor to view the config history for the Resource with the specified ID.
     *
     * @param resourceId a Resource ID
     */
    public ResourceConfigurationHistoryListView(String locatorId, boolean hasWritePerm, int resourceId) {
        super(locatorId, VIEW_ID.getTitle(), hasWritePerm, resourceId);
        ResourceConfigurationHistoryDataSource datasource = new ResourceConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        ConfigurationHistoryDetailView detailView = new ConfigurationHistoryDetailView(this.getLocatorId());
        return detailView;
    }

    @Override
    protected void rollback(int configHistoryIdToRollbackTo) {
        GWTServiceLookup.getConfigurationService().rollbackResourceConfiguration(getResourceId().intValue(),
            configHistoryIdToRollbackTo, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_configurationHistoryList_rollback_success(), Severity.Info));
                    refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_configurationHistoryList_rollback_failure(), caught);
                }
            });
    }

    @Override
    protected void delete(int[] doomedIds) {
        GWTServiceLookup.getConfigurationService().purgeResourceConfigurationUpdates(doomedIds, true,
            new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_configurationHistoryList_delete_success(), Severity.Info));
                    refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_configurationHistoryList_delete_failure(), caught);
                }
            });
    }
}
