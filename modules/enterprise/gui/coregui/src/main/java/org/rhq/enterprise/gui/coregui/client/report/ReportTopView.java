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
package org.rhq.enterprise.gui.coregui.client.report;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.SubsystemResourceAlertView;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformSummaryPortlet;
import org.rhq.enterprise.gui.coregui.client.drift.DriftHistoryView;
import org.rhq.enterprise.gui.coregui.client.drift.SubsystemResourceDriftView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.operation.OperationHistoryView;
import org.rhq.enterprise.gui.coregui.client.report.measurement.MeasurementOOBView;
import org.rhq.enterprise.gui.coregui.client.report.operation.SubsystemOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TaggedView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The Reports top-level view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ReportTopView extends AbstractSectionedLeftNavigationView {

    public static final ViewName VIEW_ID = new ViewName("Reports", MSG.view_reportsTop_title());

    public static final ViewName SECTION_SUBSYSTEMS_VIEW_ID = new ViewName("Subsystems", MSG.view_reports_subsystems());
    public static final ViewName SECTION_INVENTORY_VIEW_ID = new ViewName("Inventory", MSG.common_title_inventory());

    public ReportTopView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID.getName());
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection subsystemsSection = buildSubsystemsSection();
        sections.add(subsystemsSection);

        NavigationSection inventorySection = buildInventorySection();
        sections.add(inventorySection);

        return sections;
    }

    @Override
    protected VLayout defaultView() {
        LocatableVLayout vLayout = new LocatableVLayout(this.extendLocatorId("Default"));
        vLayout.setWidth100();

        // TODO: Admin icon.
        TitleBar titleBar = new TitleBar(this, MSG.view_reportsTop_title());
        vLayout.addMember(titleBar);

        Label label = new Label(MSG.view_reportsTop_description());
        label.setPadding(10);
        vLayout.addMember(label);

        return vLayout;
    }

    private NavigationSection buildSubsystemsSection() {
        NavigationItem tagItem = new NavigationItem(TaggedView.VIEW_ID, "global/Tag_16.png", new ViewFactory() {
            public Canvas createView() {
                return new TaggedView(extendLocatorId(TaggedView.VIEW_ID.getName()));
            }
        });

        NavigationItem suspectMetricsItem = new NavigationItem(MeasurementOOBView.VIEW_ID,
            "subsystems/monitor/Monitor_failed_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new MeasurementOOBView(extendLocatorId(MeasurementOOBView.VIEW_ID.getName()));
                }
            });

        NavigationItem recentConfigurationChangesItem = new NavigationItem(ResourceConfigurationHistoryListView.VIEW_ID,
            "subsystems/configure/Configure_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new ResourceConfigurationHistoryListView(extendLocatorId(ResourceConfigurationHistoryListView.VIEW_ID.getName()),
                        getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));
                }
            });

        NavigationItem recentOperationsItem = new NavigationItem(OperationHistoryView.SUBSYSTEM_VIEW_ID,
            "subsystems/control/Operation_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new SubsystemOperationHistoryListView(extendLocatorId(OperationHistoryView.SUBSYSTEM_VIEW_ID
                        .getName()), getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));
                }
            });

        NavigationItem recentAlertsItem = new NavigationItem(AlertHistoryView.SUBSYSTEM_VIEW_ID,
            "subsystems/alert/Alert_LOW_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new SubsystemResourceAlertView(
                        extendLocatorId(AlertHistoryView.SUBSYSTEM_VIEW_ID.getName()), getGlobalPermissions().contains(
                            Permission.MANAGE_INVENTORY));
                }
            });

        NavigationItem alertDefinitionsItem = new NavigationItem(new ViewName("AlertDefinitions", MSG
            .view_reports_alertDefinitions()), "subsystems/alert/Alerts_16.png", new ViewFactory() {
            public Canvas createView() {
                return new AlertDefinitionReportView(extendLocatorId(AlertDefinitionReportView.VIEW_ID.getName()));
            }
        });

        NavigationItem recentDriftsItem = new NavigationItem(DriftHistoryView.SUBSYSTEM_VIEW_ID,
            "subsystems/drift/Drift_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new SubsystemResourceDriftView(
                        extendLocatorId(DriftHistoryView.SUBSYSTEM_VIEW_ID.getName()), getGlobalPermissions().contains(
                            Permission.MANAGE_INVENTORY));
                }
            });

        return new NavigationSection(SECTION_SUBSYSTEMS_VIEW_ID, tagItem, suspectMetricsItem,
            recentConfigurationChangesItem, recentOperationsItem, recentAlertsItem, alertDefinitionsItem,
            recentDriftsItem);
    }

    private NavigationSection buildInventorySection() {
        NavigationItem inventorySummaryItem = new NavigationItem(ResourceInstallReport.VIEW_ID,
            "subsystems/inventory/Inventory_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new ResourceInstallReport(extendLocatorId(ResourceInstallReport.VIEW_ID.getName()));
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        NavigationItem platformSystemInfoItem = new NavigationItem(PlatformSummaryPortlet.VIEW_ID, ImageManager
            .getResourceIcon(ResourceCategory.PLATFORM), new ViewFactory() {
            public Canvas createView() {
                return new PlatformSummaryPortlet(extendLocatorId(PlatformSummaryPortlet.VIEW_ID.getName()));
            }
        });

        return new NavigationSection(SECTION_INVENTORY_VIEW_ID, inventorySummaryItem, platformSystemInfoItem);
    }

}