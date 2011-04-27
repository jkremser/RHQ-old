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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.criteria.InstalledPackageHistoryCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupPkgHistoryPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**This portlet allows the end user to customize the Package History display
 *
 * @author Simeon Pinder
 */
public class ResourcePkgHistoryPortlet extends GroupPkgHistoryPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourcePackageHistory";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_pkg_hisory();

    private int resourceId = -1;

    public ResourcePkgHistoryPortlet(String locatorId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        int currentResourceIdentifier = Integer.valueOf(elements[1]);
        this.resourceId = currentResourceIdentifier;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new ResourcePkgHistoryPortlet(locatorId);
        }
    }

    /** Fetches recent package history information and updates the DynamicForm instance with details.
     */
    @Override
    protected void getRecentPkgHistory() {
        final int resourceId = this.resourceId;
        InstalledPackageHistoryCriteria criteria = new InstalledPackageHistoryCriteria();

        int resultCount = 5;//default to
        //result count
        PropertySimple property = portletConfig.getSimple(Constant.RESULT_COUNT);
        if (property != null) {
            String currentSetting = property.getStringValue();
            if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase("5")) {
                resultCount = 5;
            } else {
                resultCount = Integer.valueOf(currentSetting);
            }
        }
        PageControl pageControl = new PageControl(0, resultCount);
        criteria.setPageControl(pageControl);
        criteria.addFilterResourceId(resourceId);

        criteria.addSortStatus(PageOrdering.DESC);

        GWTServiceLookup.getContentService().findInstalledPackageHistoryByCriteria(criteria,

        new AsyncCallback<PageList<InstalledPackageHistory>>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.debug("Error retrieving installed package history for group [" + resourceId + "]:"
                    + caught.getMessage());
            }

            @Override
            public void onSuccess(PageList<InstalledPackageHistory> result) {
                VLayout column = new VLayout();
                column.setHeight(10);
                if (!result.isEmpty()) {
                    for (InstalledPackageHistory history : result) {
                        LocatableDynamicForm row = new LocatableDynamicForm(recentPkgHistoryContent
                            .extendLocatorId(history.getPackageVersion().getFileName()
                                + history.getPackageVersion().getVersion()));
                        row.setNumCols(3);

                        StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(
                            "subsystems/content/Package_16.png", null);
                        String title = history.getPackageVersion().getFileName() + ":";
                        String destination = "/rhq/resource/content/audit-trail-item.xhtml?id=" + resourceId
                            + "&selectedHistoryId=" + history.getId();
                        //spinder 4/27/11: diabling links as they point into portal.war content pages
                        //                        LinkItem link = AbstractActivityView.newLinkItem(title, destination);
                        StaticTextItem link = AbstractActivityView.newTextItem(title);
                        StaticTextItem time = AbstractActivityView.newTextItem(GwtRelativeDurationConverter
                            .format(history.getTimestamp()));

                        row.setItems(iconItem, link, time);
                        column.addMember(row);
                    }
                    //                    //insert see more link
                    //                    LocatableDynamicForm row = new LocatableDynamicForm(recentPkgHistoryContent
                    //                        .extendLocatorId("PkgHistoryContentSeeMore"));
                    //                    String destination = "/rhq/resource/content/audit-trail-item.xhtml?id=" + groupId;
                    //                    addSeeMoreLink(row, destination, column);
                } else {
                    LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(recentPkgHistoryContent
                        .extendLocatorId("None"), MSG.view_portlet_results_empty());
                    column.addMember(row);
                }
                //cleanup
                for (Canvas child : recentPkgHistoryContent.getChildren()) {
                    child.destroy();
                }
                recentPkgHistoryContent.addChild(column);
                recentPkgHistoryContent.markForRedraw();
                markForRedraw();
            }
        });
    }
}