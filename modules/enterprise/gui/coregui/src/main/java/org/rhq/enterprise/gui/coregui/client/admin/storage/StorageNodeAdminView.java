/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.storage;

import java.util.EnumSet;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.components.tab.NamedTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.NamedTabSet;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration.GroupResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * The main view for managing storage nodes.
 *
 * @author Jirka Kremser
 */
public class StorageNodeAdminView extends EnhancedVLayout implements HasViewName, BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("StorageNodes", MSG.view_adminTopology_storageNodes(),
        IconEnum.STORAGE_NODE);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;
    
    private static final String GROUP_NAME = "RHQ Storage Nodes";
    
    private final NamedTabSet tabset;
    private TabInfo tableTabInfo = new TabInfo(0, new ViewName("Nodes"));
    private TabInfo settingsTabInfo = new TabInfo(1, new ViewName("Settings", "Cluster Settings"));
    private TabInfo alertsTabInfo = new TabInfo(2, new ViewName("Alerts", "Cluster Alerts"));
    private TabInfo backupTabInfo = new TabInfo(3, new ViewName("Backup"));
    private StorageNodeTableView table;

    private int[] resIds;

    public StorageNodeAdminView() {
        super();
        setHeight100();
        setWidth100();
        setLayoutTopMargin(8);
        tabset = new NamedTabSet();
        NamedTab table = new NamedTab(tableTabInfo.name);
        table.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent event) {
                CoreGUI.goToView(VIEW_PATH);
            }
        });

        NamedTab settings = new NamedTab(settingsTabInfo.name);
        settings.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent event) {
                CoreGUI.goToView(VIEW_PATH + "/" + settingsTabInfo.name);
            }
        });

        final NamedTab alerts = new NamedTab(alertsTabInfo.name);
        alerts.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent event) {
                CoreGUI.goToView(VIEW_PATH + "/" + alertsTabInfo.name);
            }
        });
        scheduleUnacknowledgedAlertsPollingJob(alerts); 
        
        final NamedTab backup = new NamedTab(backupTabInfo.name);
        backup.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent event) {
                CoreGUI.goToView(VIEW_PATH + "/" + backupTabInfo.name);
            }
        });

        tabset.setTabs(table, settings, alerts/*, backup*/);
        addMember(tabset);
    }

    private void showTab(final TabInfo tabInfo) {
        if (tabInfo.equals(tableTabInfo)) {
            table = new StorageNodeTableView();
            tabset.getTabByName(tabInfo.name.getName()).setPane(table);
            tabset.selectTab(tabInfo.index);
        } else if (tabInfo.equals(backupTabInfo)) {
            tabset.getTabByName(tabInfo.name.getName()).setPane(new Label("in progress.."));
        } else if (tabInfo.equals(alertsTabInfo)) {
            if (resIds != null) {
                tabset.getTabByName(tabInfo.name.getName()).setPane(new AlertHistoryView("storageNodesAlerts", resIds));
            } else {
                GWTServiceLookup.getStorageService().findResourcesWithAlertDefinitions(new AsyncCallback<Integer[]>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Message message = new Message("foobar",
                            Message.Severity.Warning);
                        CoreGUI.goToView(VIEW_ID.getName(), message);
                    }

                    @Override
                    public void onSuccess(Integer[] result) {
                        if (result == null || result.length == 0) {
                            onFailure(new Exception("foobaz"));
                        } else {
                            resIds = ArrayUtils.unwrapArray(result);
                            tabset.getTabByName(tabInfo.name.getName()).setPane(
                                new AlertHistoryView("storageNodesAlerts", resIds));
                            tabset.selectTab(tabInfo.index);
                        }
                    }
                });
            }
        } else if (tabInfo.equals(settingsTabInfo)) {
            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.addFilterName(GROUP_NAME);
            criteria.setStrict(true);
            GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
                new AsyncCallback<PageList<ResourceGroupComposite>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Message message = new Message(MSG.view_group_detail_failLoadComp(String.valueOf(GROUP_NAME)),
                            Message.Severity.Warning);
                        CoreGUI.goToView(VIEW_ID.getName(), message);
                    }

                    @Override
                    public void onSuccess(PageList<ResourceGroupComposite> result) {
                        if (result.isEmpty()) {
                            onFailure(new Exception("Group with name [" + GROUP_NAME + "] does not exist."));
                        } else {
                            ResourceGroupComposite groupComposite = result.get(0);
                            loadResourceType(groupComposite.getResourceGroup().getResourceType().getId());
                            tabset.getTabByName(tabInfo.name.getName()).setPane(
                                new GroupResourceConfigurationEditView(groupComposite));
                            tabset.selectTab(tabInfo.index);
                        }
                    }
                });
        }
    }
    
    private void loadResourceType(int resourceTypeId) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resourceTypeId,
            EnumSet.of(ResourceTypeRepository.MetadataType.content, ResourceTypeRepository.MetadataType.operations,
                ResourceTypeRepository.MetadataType.measurements, ResourceTypeRepository.MetadataType.events,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {

                }
            });
    }
    
    private void scheduleUnacknowledgedAlertsPollingJob(final NamedTab alerts) {
        new Timer() {
            public void run() {
                GWTServiceLookup.getStorageService().findNotAcknowledgedStorageNodeAlertsCount(new AsyncCallback<Integer>() {
                    @Override
                        public void onSuccess(Integer result) {
                            alerts.setTitle(alerts.getTitle()
                                + (result != 0 ? " <font color='#CC0000;'>(" + result + ")</font>" : " (" + result
                                    + ")"));
                            schedule(5 * 1000);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                        schedule(60 * 1000);
                    }
                });
            }
        }.run();
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }
    
    private static final class TabInfo {
        private int index;
        private ViewName name;

        private TabInfo(int index, ViewName name) {
            this.index = index;
            this.name = name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + index;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TabInfo other = (TabInfo) obj;
            if (index != other.index)
                return false;
            return true;
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (viewPath.getViewPath().size() == 3) {
            showTab(tableTabInfo);
        } else {
            String viewId = viewPath.getCurrent().getPath();
            if (settingsTabInfo.name.getName().equals(viewId)) {
                showTab(settingsTabInfo);
            } else if (alertsTabInfo.name.getName().equals(viewId)) {
                showTab(alertsTabInfo);
            } else if (backupTabInfo.name.getName().equals(viewId)) {
                showTab(backupTabInfo);
            } else {
                showTab(tableTabInfo);
                table.renderView(viewPath);
            }
        }
    }
}
