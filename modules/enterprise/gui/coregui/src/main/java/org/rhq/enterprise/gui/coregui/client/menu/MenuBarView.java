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
package org.rhq.enterprise.gui.coregui.client.menu;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Hyperlink;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleTopView;
import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;
import org.rhq.enterprise.gui.coregui.client.components.ViewLink;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardsView;
import org.rhq.enterprise.gui.coregui.client.help.HelpView;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.report.ReportTopView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class MenuBarView extends LocatableVLayout {

    public static final ViewName[] SECTIONS = { DashboardsView.VIEW_ID, InventoryView.VIEW_ID, ReportTopView.VIEW_ID,
        BundleTopView.VIEW_ID, AdministrationView.VIEW_ID, HelpView.VIEW_ID };
    public static final ViewName LOGOUT_VIEW_ID = new ViewName("LogOut", MSG.view_menuBar_logout());

    private String currentlySelectedSection = DashboardsView.VIEW_ID.getName();
    private LocatableLabel userLabel;

    public MenuBarView(String locatorId) {
        super(locatorId);
    }

    protected void onDraw() {
        super.onDraw();

        ToolStrip topStrip = new ToolStrip();
        topStrip.setHeight(34);
        topStrip.setWidth100();
        topStrip.setBackgroundImage("header/header_bg.png");
        topStrip.setMembersMargin(20);

        topStrip.addMember(getLogoSection());
        topStrip.addMember(getLinksSection());
        topStrip.addMember(getActionsSection());

        addMember(topStrip);
        //addMember(new SearchBarPane(this.extendLocatorId("Search")));

        markForRedraw();
    }

    // When redrawing, ensure the correct session info is displayed
    @Override
    public void markForRedraw() {
        String currentDisplayName = userLabel.getContents();
        String currentUsername = UserSessionManager.getSessionSubject().getName();
        if (!currentUsername.equals(currentDisplayName)) {
            userLabel.setContents(currentUsername);
        }

        super.markForRedraw();
    }

    private Img getLogoSection() {
        final AboutModalWindow aboutModalWindow = new AboutModalWindow(extendLocatorId("AboutModalWindow"));
        Img logo = new Img("header/rhq_logo_28px.png", 80, 28);
        logo.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                aboutModalWindow.show();
            }
        });
        return logo;
    }

    private LinkBar getLinksSection() {
        LinkBar linksSection = new LinkBar();
        History.addValueChangeHandler(linksSection);
        return linksSection;
    }

    private Canvas getActionsSection() {
        HLayout layout = new HLayout();
        layout.setMargin(10);
        layout.setAlign(Alignment.RIGHT);

        userLabel = new LocatableLabel(this.extendLocatorId("User"), UserSessionManager.getSessionSubject().getName());
        userLabel.setAutoWidth();

        LocatableLabel lineLabel = new LocatableLabel(this.extendLocatorId("Line"), " | ");
        lineLabel.setWidth("10px");
        lineLabel.setAlign(Alignment.CENTER);

        Hyperlink logoutLink = SeleniumUtility.setHtmlId(new Hyperlink(LOGOUT_VIEW_ID.getTitle(), LOGOUT_VIEW_ID
            .getName()), LOGOUT_VIEW_ID.getName());
        logoutLink.setWidth("50px");

        layout.addMember(userLabel);
        layout.addMember(lineLabel);
        layout.addMember(logoutLink);

        return layout;
    }

    class LinkBar extends LocatableHStack implements ValueChangeHandler<String> {
        private final Map<String, LocatableVLayout> sectionNameToViewLinkMap = new HashMap<String, LocatableVLayout>();

        LinkBar() {
            super(MenuBarView.this.extendLocatorId("LinkBar"));

            setWidth100();
            setHeight(34);

            Img divider = new Img("images/header/header_bg_line.png");
            divider.setWidth(1);
            addMember(divider);

            for (ViewName sectionName : SECTIONS) {
                LocatableVLayout viewLinkContainer = new LocatableVLayout(extendLocatorId(sectionName.getName()));
                viewLinkContainer.setAlign(VerticalAlignment.CENTER);
                ViewLink viewLink = new ViewLink(extendLocatorId(sectionName.getName()), sectionName.getTitle(),
                        sectionName.getName());
                viewLink.setMouseOutStyleName("menuBarLink");
                viewLink.setMouseOverStyleName("menuBarLinkHover");
                this.sectionNameToViewLinkMap.put(sectionName.getName(), viewLinkContainer);
                viewLinkContainer.addMember(viewLink);
                updateViewLinkStyle(sectionName.getName());
                addMember(viewLinkContainer);

                divider = new Img("images/header/header_bg_line.png");
                divider.setWidth(1);
                addMember(divider);
            }
        }

        @Override
        public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
            String viewPath = stringValueChangeEvent.getValue();
            String topViewId = viewPath.split("/")[0];
            if ("Resource".equals(topViewId)) {
                topViewId = InventoryView.VIEW_ID.getName();
            }
            currentlySelectedSection = topViewId;

            for (String sectionName : this.sectionNameToViewLinkMap.keySet()) {
                updateViewLinkStyle(sectionName);
            }
        }

        private void updateViewLinkStyle(String sectionName) {
            String styleClass;
            if (sectionName.equals(currentlySelectedSection)) {
                styleClass = "TopSectionLinkSelected";
            } else {
                styleClass = "TopSectionLink";
            }
            LocatableVLayout viewLinkContainer = this.sectionNameToViewLinkMap.get(sectionName);
            viewLinkContainer.setStyleName(styleClass);
            viewLinkContainer.markForRedraw();
        }
    }

}
