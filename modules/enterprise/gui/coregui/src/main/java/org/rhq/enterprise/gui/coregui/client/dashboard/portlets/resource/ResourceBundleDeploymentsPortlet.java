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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceBundleDeploymentCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupBundleDeploymentsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**This portlet allows the end user to customize the Bundle Deployment display
 *
 * @author Simeon Pinder
 */
public class ResourceBundleDeploymentsPortlet extends GroupBundleDeploymentsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceBundleDeployments";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_bundles();

    private int resourceId = -1;

    public ResourceBundleDeploymentsPortlet(String locatorId, int resourceId) {
        super(locatorId, -1);

        this.resourceId = resourceId;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId, EntityContext context) {

            if (EntityContext.Type.Resource != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new ResourceBundleDeploymentsPortlet(locatorId, context.getResourceId());
        }
    }

    /** Fetches recent bundle deployment information and updates the DynamicForm instance with details.
     */
    @Override
    protected void getRecentBundleDeployments() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();
        final int resourceId = this.resourceId;
        ResourceBundleDeploymentCriteria criteria = new ResourceBundleDeploymentCriteria();

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
        criteria.addFilterResourceIds(resourceId);
        criteria.addSortStatus(PageOrdering.DESC);
        criteria.fetchDestination(true);
        criteria.fetchBundleVersion(true);
        criteria.fetchResourceDeployments(true);

        GWTServiceLookup.getBundleService().findBundleDeploymentsByCriteria(criteria,
            new AsyncCallback<PageList<BundleDeployment>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving installed bundle deployments for resource [" + resourceId + "]:"
                        + caught.getMessage());
                    currentlyLoading = false;
                }

                @Override
                public void onSuccess(PageList<BundleDeployment> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (BundleDeployment deployment : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentBundleDeployContent
                                .extendLocatorId(deployment.getBundleVersion().getName()
                                    + deployment.getBundleVersion().getVersion()));
                            row.setNumCols(3);

                            StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(
                                "subsystems/content/Content_16.png", null);
                            String title = deployment.getBundleVersion().getName() + "["
                                + deployment.getBundleVersion().getVersion() + "]:";
                            String destination = LinkManager.getBundleDestinationLink(deployment.getBundleVersion()
                                .getBundle().getId(), deployment.getDestination().getId());
                            LinkItem link = AbstractActivityView.newLinkItem(title, destination);
                            StaticTextItem time = AbstractActivityView.newTextItem(GwtRelativeDurationConverter
                                .format(deployment.getCtime()));

                            row.setItems(iconItem, link, time);
                            column.addMember(row);
                        }
                        //insert see more link
                        //TODO: spinder:2/25/11 (add this later) no current view for seeing all bundle deployments
                        //                        LocatableDynamicForm row = new LocatableDynamicForm(recentBundleDeployContent.extendLocatorId("RecentBundleContentSeeMore"));
                        //                        addSeeMoreLink(row, LinkManager.getResourceGroupLink(groupId) + "/Events/History/", column);
                    } else {
                        LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(recentBundleDeployContent
                            .extendLocatorId("None"), MSG.view_portlet_results_empty());
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentBundleDeployContent.getChildren()) {
                        child.destroy();
                    }
                    column.markForRedraw();
                    recentBundleDeployContent.addChild(column);
                    recentBundleDeployContent.markForRedraw();
                    currentlyLoading = false;
                    markForRedraw();
                }
            });
    }
}