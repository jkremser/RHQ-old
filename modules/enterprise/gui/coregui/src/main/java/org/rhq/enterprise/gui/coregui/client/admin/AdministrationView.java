/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.admin.agent.install.RemoteAgentInstallView;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;
import org.rhq.enterprise.gui.coregui.client.admin.templates.ResourceTypeTreeView;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The Administration top-level view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class AdministrationView extends AbstractSectionedLeftNavigationView {

    public static final ViewName VIEW_ID = new ViewName("Administration", MSG.view_admin_administration());

    public static final ViewName SECTION_SECURITY_VIEW_ID = new ViewName("Security", MSG.view_admin_security());
    public static final ViewName SECTION_TOPOLOGY_VIEW_ID = new ViewName("Topology", MSG.view_admin_topology());
    public static final ViewName SECTION_CONFIGURATION_VIEW_ID = new ViewName("Configuration", MSG
        .view_admin_configuration());
    public static final ViewName SECTION_CONTENT_VIEW_ID = new ViewName("Content", MSG.view_admin_content());

    // TODO these iframe page view ids should go away in favor of the gwt view page view_id, when available
    private static final ViewName PAGE_SERVERS_VIEW_ID = new ViewName("Servers", MSG.view_adminTopology_servers());
    private static final ViewName PAGE_AGENTS_VIEW_ID = new ViewName("Agents", MSG.view_adminTopology_agents());
    private static final ViewName PAGE_AFFINITY_GROUPS_VIEW_ID = new ViewName("AffinityGroups", MSG
        .view_adminTopology_affinityGroups());
    private static final ViewName PAGE_PARTITION_EVENTS_VIEW_ID = new ViewName("PartitionEvents", MSG
        .view_adminTopology_partitionEvents());

    // TODO this iframe page view id should go away in favor of the gwt view page view_id, when available
    private static final ViewName PAGE_PLUGINS_VIEW_ID = new ViewName("Plugins", MSG.view_adminConfig_plugins());

    // TODO these iframe page view ids should go away in favor of the gwt view page view_id, when available
    private static final ViewName PAGE_CONTENT_SOURCES_VIEW_ID = new ViewName("ContentSources", MSG
        .view_adminContent_contentSources());
    private static final ViewName PAGE_REPOS_VIEW_ID = new ViewName("Repositories", MSG.common_title_repositories());

    public AdministrationView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID.getName());
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection securitySection = buildSecuritySection();
        sections.add(securitySection);

        NavigationSection topologySection = buildTopologySection();
        sections.add(topologySection);

        NavigationSection configurationSection = buildConfigurationSection();
        sections.add(configurationSection);

        NavigationSection contentSection = buildContentSection();
        sections.add(contentSection);

        return sections;
    }

    protected VLayout defaultView() {
        LocatableVLayout vLayout = new LocatableVLayout(this.extendLocatorId("Default"));
        vLayout.setWidth100();

        // TODO: Admin icon.
        TitleBar titleBar = new TitleBar(this, MSG.view_admin_administration());
        vLayout.addMember(titleBar);

        Label label = new Label(MSG.view_admin_landing());
        label.setPadding(10);
        vLayout.addMember(label);

        return vLayout;
    }

    private NavigationSection buildSecuritySection() {
        NavigationItem usersItem = new NavigationItem(UsersView.VIEW_ID, "global/User_16.png", new ViewFactory() {
            public Canvas createView() {
                return new UsersView(extendLocatorId("Users"));
            }
        });

        NavigationItem rolesItem = new NavigationItem(RolesView.VIEW_ID, "global/Role_16.png", new ViewFactory() {
            public Canvas createView() {
                return new RolesView(extendLocatorId("Roles"));
            }
        });

        return new NavigationSection(SECTION_SECURITY_VIEW_ID, usersItem, rolesItem);
    }

    private NavigationSection buildTopologySection() {
        NavigationItem serversItem = new NavigationItem(PAGE_SERVERS_VIEW_ID, "types/Server_up_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId(PAGE_SERVERS_VIEW_ID.getName()),
                        "/rhq/ha/listServers-plain.xhtml?nomenu=true");
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        NavigationItem agentsItem = new NavigationItem(PAGE_AGENTS_VIEW_ID, "global/Agent_16.png", new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_AGENTS_VIEW_ID.getName()),
                    "/rhq/ha/listAgents-plain.xhtml?nomenu=true");
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        NavigationItem affinityGroupsItem = new NavigationItem(PAGE_AFFINITY_GROUPS_VIEW_ID, "types/Group_up_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId(PAGE_AFFINITY_GROUPS_VIEW_ID.getName()),
                        "/rhq/ha/listAffinityGroups-plain.xhtml?nomenu=true");
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        NavigationItem partitionEventsItem = new NavigationItem(PAGE_PARTITION_EVENTS_VIEW_ID,
            "subsystems/event/Events_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId(PAGE_PARTITION_EVENTS_VIEW_ID.getName()),
                        "/rhq/ha/listPartitionEvents-plain.xhtml?nomenu=true");
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        NavigationItem remoteAgentInstallItem = new NavigationItem(RemoteAgentInstallView.VIEW_ID,
            "global/Agent_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new RemoteAgentInstallView(extendLocatorId("RemoteAgentInstall"));
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        return new NavigationSection(SECTION_TOPOLOGY_VIEW_ID, serversItem, agentsItem, affinityGroupsItem,
            partitionEventsItem, remoteAgentInstallItem);
    }

    private NavigationSection buildConfigurationSection() {
        NavigationItem systemSettingsItem = new NavigationItem(SystemSettingsView.VIEW_ID,
            "subsystems/configure/Configure_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new SystemSettingsView(extendLocatorId(SystemSettingsView.VIEW_ID.getName()));
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));
        systemSettingsItem.setRefreshRequired(true); // refresh so it always reloads the latest settings

        NavigationItem templatesItem = new NavigationItem(ResourceTypeTreeView.VIEW_ID, ImageManager
            .getMetricEditIcon(), new ViewFactory() {
            public Canvas createView() {
                return new ResourceTypeTreeView(extendLocatorId(ResourceTypeTreeView.VIEW_ID.getName()));
            }
        });
        templatesItem.setRefreshRequired(true); // we always need a new page

        NavigationItem downloadsItem = new NavigationItem(DownloadsView.VIEW_ID, "global/Download_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new DownloadsView(extendLocatorId(DownloadsView.VIEW_ID.getName()));
                }
            });

        NavigationItem pluginsItem = new NavigationItem(PAGE_PLUGINS_VIEW_ID, "global/Plugin_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId(PAGE_PLUGINS_VIEW_ID.getName()),
                        "/rhq/admin/plugin/plugin-list-plain.xhtml?nomenu=true");
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        return new NavigationSection(SECTION_CONFIGURATION_VIEW_ID, systemSettingsItem, templatesItem, downloadsItem,
            pluginsItem);
    }

    private NavigationSection buildContentSection() {
        NavigationItem contentSourcesItem = new NavigationItem(PAGE_CONTENT_SOURCES_VIEW_ID,
            "subsystems/content/Content_16.png", new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId(PAGE_CONTENT_SOURCES_VIEW_ID.getName()),
                        "/rhq/content/listContentProviders-plain.xhtml");
                }
            }, getGlobalPermissions().contains(Permission.MANAGE_REPOSITORIES));

        NavigationItem reposItem = new NavigationItem(PAGE_REPOS_VIEW_ID, "subsystems/content/Content_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId(PAGE_REPOS_VIEW_ID.getName()),
                        "/rhq/content/listRepos-plain.xhtml");
                }
            });

        return new NavigationSection(SECTION_CONTENT_VIEW_ID, contentSourcesItem, reposItem);
    }
}
