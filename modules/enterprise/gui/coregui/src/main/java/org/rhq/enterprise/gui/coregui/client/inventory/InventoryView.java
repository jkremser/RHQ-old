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
package org.rhq.enterprise.gui.coregui.client.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The Inventory top-level view.
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 * @author Ian Springer
 */
public class InventoryView extends AbstractSectionedLeftNavigationView {
    public static final ViewName VIEW_ID = new ViewName("Inventory", MSG.common_title_inventory());

    // view IDs for Resources section
    private static final ViewName RESOURCES_SECTION_VIEW_ID = new ViewName("Resources", MSG.common_title_resources());

    private static final ViewName PAGE_AUTODISCOVERY_QUEUE = new ViewName("AutodiscoveryQueue", MSG
        .view_inventory_adq());
    private static final ViewName PAGE_ALL_RESOURCES = new ViewName("AllResources", MSG.view_inventory_allResources());
    private static final ViewName PAGE_PLATFORMS = new ViewName("Platforms", MSG.view_inventory_platforms());
    private static final ViewName PAGE_SERVERS = new ViewName("Servers", MSG.view_inventory_servers());
    private static final ViewName PAGE_SERVICES = new ViewName("Services", MSG.view_inventory_services());
    private static final ViewName PAGE_UNAVAIL_SERVERS = new ViewName("UnavailableServers", MSG
        .view_inventory_unavailableServers());

    // view IDs for Groups section
    private static final ViewName GROUPS_SECTION_VIEW_ID = new ViewName("Groups", MSG.view_inventory_groups());

    private static final ViewName PAGE_DYNAGROUP_DEFINITIONS = new ViewName("DynagroupDefinitions", MSG
        .view_inventory_dynagroupDefs());
    private static final ViewName PAGE_ALL_GROUPS = new ViewName("AllGroups", MSG.view_inventory_allGroups());
    private static final ViewName PAGE_COMPATIBLE_GROUPS = new ViewName("CompatibleGroups", MSG
        .common_title_compatibleGroups());
    private static final ViewName PAGE_MIXED_GROUPS = new ViewName("MixedGroups", MSG.common_title_mixedGroups());
    private static final ViewName PAGE_PROBLEM_GROUPS = new ViewName("ProblemGroups", MSG
        .view_inventory_problemGroups());

    private Set<Permission> globalPermissions;

    public InventoryView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID.getName());
    }

    @Override
    protected void onInit() {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                globalPermissions = (permissions != null) ? permissions : EnumSet.noneOf(Permission.class);
                InventoryView.super.onInit();
            }
        });
    }

    protected Canvas defaultView() {
        LocatableVLayout vLayout = new LocatableVLayout(this.extendLocatorId("Default"));
        vLayout.setWidth100();

        // TODO: Admin icon.
        TitleBar titleBar = new TitleBar(this, MSG.common_title_inventory());
        vLayout.addMember(titleBar);

        Label label = new Label(MSG.view_inventory_sectionHelp());
        label.setPadding(10);
        vLayout.addMember(label);

        return vLayout;
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection resourcesSection = buildResourcesSection();
        sections.add(resourcesSection);

        NavigationSection groupsSection = buildGroupsSection();
        sections.add(groupsSection);

        return sections;
    }

    private NavigationSection buildResourcesSection() {
        NavigationItem autodiscoveryQueueItem = new NavigationItem(PAGE_AUTODISCOVERY_QUEUE,
            "global/AutoDiscovery_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new ResourceAutodiscoveryView(extendLocatorId(PAGE_AUTODISCOVERY_QUEUE.getName()));
                }
            }, this.globalPermissions.contains(Permission.MANAGE_INVENTORY));
        autodiscoveryQueueItem.setRefreshRequired(true);

        // TODO: Specify an icon for this item.
        NavigationItem allResourcesItem = new NavigationItem(PAGE_ALL_RESOURCES, null, new ViewFactory() {
            public Canvas createView() {
                return new ResourceSearchView(extendLocatorId(PAGE_ALL_RESOURCES.getName()), null, PAGE_ALL_RESOURCES
                    .getTitle(), ImageManager.getResourceLargeIcon(ResourceCategory.PLATFORM, Boolean.TRUE),
                    ImageManager.getResourceLargeIcon(ResourceCategory.SERVER, Boolean.TRUE), ImageManager
                        .getResourceLargeIcon(ResourceCategory.SERVICE, Boolean.TRUE));
            }
        });

        NavigationItem platformsItem = new NavigationItem(PAGE_PLATFORMS, ImageManager.getResourceIcon(
            ResourceCategory.PLATFORM, Boolean.TRUE), new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.PLATFORM.name());
                ResourceSearchView view = new ResourceSearchView(extendLocatorId(PAGE_PLATFORMS.getName()),
                    initialCriteria, PAGE_PLATFORMS.getTitle(), ImageManager.getResourceLargeIcon(ResourceCategory.PLATFORM,
                    Boolean.TRUE));
                view.setInitialSearchBarSearchText("category=" + ResourceCategory.PLATFORM.name().toLowerCase());
                return view;
            }
        });

        NavigationItem serversItem = new NavigationItem(PAGE_SERVERS, ImageManager.getResourceIcon(
            ResourceCategory.SERVER, Boolean.TRUE), new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name());
                ResourceSearchView view = new ResourceSearchView(extendLocatorId(PAGE_SERVERS.getName()),
                    initialCriteria, PAGE_SERVERS.getTitle(), ImageManager.getResourceLargeIcon(ResourceCategory.SERVER,
                    Boolean.TRUE));
                view.setInitialSearchBarSearchText("category=" + ResourceCategory.SERVER.name().toLowerCase());
                return view;
            }
        });

        NavigationItem servicesItem = new NavigationItem(PAGE_SERVICES, ImageManager.getResourceIcon(
            ResourceCategory.SERVICE, Boolean.TRUE), new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVICE.name());
                ResourceSearchView view = new ResourceSearchView(extendLocatorId(PAGE_SERVICES.getName()),
                    initialCriteria, PAGE_SERVICES.getTitle(), ImageManager.getResourceLargeIcon(ResourceCategory.SERVICE,
                    Boolean.TRUE));
                view.setInitialSearchBarSearchText("category=" + ResourceCategory.SERVICE.name().toLowerCase());
                return view;
            }
        });

        NavigationItem downServersItem = new NavigationItem(PAGE_UNAVAIL_SERVERS, ImageManager.getResourceIcon(
            ResourceCategory.SERVER, Boolean.FALSE), new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(ResourceDataSourceField.AVAILABILITY.propertyName(),
                    AvailabilityType.DOWN.name());
                initialCriteria.addCriteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name());
                ResourceSearchView view = new ResourceSearchView(extendLocatorId(PAGE_UNAVAIL_SERVERS.getName()),
                    initialCriteria, MSG.view_inventory_unavailableServers(), ImageManager.getResourceLargeIcon(
                        ResourceCategory.SERVER, Boolean.FALSE));
                // TODO (ips, 10/28/10): Should we include down platforms too?
                view.setInitialSearchBarSearchText("category=" + ResourceCategory.SERVER.name().toLowerCase()
                    + " availability=" + AvailabilityType.DOWN.name().toLowerCase());
                return view;
            }
        });

        return new NavigationSection(RESOURCES_SECTION_VIEW_ID, autodiscoveryQueueItem, allResourcesItem,
            platformsItem, serversItem, servicesItem, downServersItem);
    }

    private NavigationSection buildGroupsSection() {
        NavigationItem dynagroupDefinitionsItem = new NavigationItem(PAGE_DYNAGROUP_DEFINITIONS,
            "types/GroupDefinition_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new GroupDefinitionListView(extendLocatorId(PAGE_DYNAGROUP_DEFINITIONS.getName()),
                        "types/GroupDefinition_24.png");
                }
            }, this.globalPermissions.contains(Permission.MANAGE_INVENTORY));

        NavigationItem allGroupsItem = new NavigationItem(PAGE_ALL_GROUPS, ImageManager
            .getGroupIcon(GroupCategory.MIXED), new ViewFactory() {
            public Canvas createView() {
                return new ResourceGroupListView(extendLocatorId(PAGE_ALL_GROUPS.getName()), null, PAGE_ALL_GROUPS
                    .getTitle(), ImageManager.getGroupLargeIcon(GroupCategory.COMPATIBLE), ImageManager
                    .getGroupLargeIcon(GroupCategory.MIXED));
            }
        });

        NavigationItem compatibleGroupsItem = new NavigationItem(PAGE_COMPATIBLE_GROUPS, ImageManager
            .getGroupIcon(GroupCategory.COMPATIBLE), new ViewFactory() {
            public Canvas createView() {
                ResourceGroupListView view = new ResourceGroupListView(
                    extendLocatorId(PAGE_COMPATIBLE_GROUPS.getName()), new Criteria(
                        ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.COMPATIBLE.name()),
                    PAGE_COMPATIBLE_GROUPS.getTitle(), ImageManager.getGroupLargeIcon(GroupCategory.COMPATIBLE));
                view.setInitialSearchBarSearchText("groupCategory=compatible");
                return view;
            }
        });

        NavigationItem mixedGroupsItem = new NavigationItem(PAGE_MIXED_GROUPS, ImageManager
            .getGroupIcon(GroupCategory.MIXED), new ViewFactory() {
            public Canvas createView() {
                ResourceGroupListView view = new ResourceGroupListView(extendLocatorId(PAGE_MIXED_GROUPS.getName()),
                    new Criteria(ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.MIXED.name()),
                    PAGE_MIXED_GROUPS.getTitle(), ImageManager.getGroupLargeIcon(GroupCategory.MIXED));
                view.setInitialSearchBarSearchText("groupCategory=mixed");
                return view;
            }
        });

        NavigationItem problemGroupsItem = new NavigationItem(PAGE_PROBLEM_GROUPS, ImageManager.getGroupIcon(
            GroupCategory.MIXED, 0.0d), new ViewFactory() {
            public Canvas createView() {
                ResourceGroupListView view = new ResourceGroupListView(extendLocatorId(PAGE_PROBLEM_GROUPS.getName()),
                    new Criteria("downMemberCount", "1"), PAGE_PROBLEM_GROUPS.getTitle(), ImageManager
                        .getGroupLargeIcon(GroupCategory.MIXED, 0.0d));
                //view.setInitialSearchBarSearchText("availability=down"); I don't think this matches exactly what the criteria returns
                view.setShowNewButton(false);
                return view;
            }
        });

        return new NavigationSection(GROUPS_SECTION_VIEW_ID, dynagroupDefinitionsItem, allGroupsItem,
            compatibleGroupsItem, mixedGroupsItem, problemGroupsItem);
    }
}
