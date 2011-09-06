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
package org.rhq.enterprise.gui.coregui.client;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.footer.FavoritesButton;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageBar;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenterView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class Footer extends LocatableHLayout {
    private static final String LOCATOR_ID = "CoreFooter";

    private MessageBar messageBar;
    private MessageCenterView messageCenter;

    public Footer() {
        super(LOCATOR_ID);
        setHeight(30);
        setAlign(Alignment.LEFT);
        setWidth100();
        setMembersMargin(5);
        setBackgroundColor("#F1F2F3");
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        messageCenter = new MessageCenterView(extendLocatorId(MessageCenterView.LOCATOR_ID));
        final FavoritesButton favoritesButton = new FavoritesButton(extendLocatorId("Favorites"));
        messageBar = new MessageBar();

        // leave space for the RPC Activity Spinner (I think this has been removed, giving back the space) 
        addMember(createHSpacer(16));

        addMember(messageBar);

        VLayout favoritesLayout = new VLayout();
        favoritesLayout.setHeight100();
        favoritesLayout.setAutoWidth();
        favoritesLayout.setAlign(Alignment.CENTER);
        favoritesLayout.addMember(favoritesButton);
        addMember(favoritesLayout);

        addMember(getMessageCenterButton());

        addMember(createHSpacer(0));
    }

    private LocatableVLayout getMessageCenterButton() {
        LocatableVLayout layout = new LocatableVLayout(extendLocatorId("layout"));
        layout.setMembersMargin(5);
        layout.setHeight100();
        layout.setAlign(Alignment.CENTER);
        layout.setAutoWidth();

        LocatableIButton button = new LocatableIButton(extendLocatorId("button"), MSG.view_messageCenter_messageTitle());
        button.setAlign(Alignment.CENTER);
        button.setAutoFit(true);
        button.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                messageCenter.showMessageCenterWindow();
            }
        });

        layout.addMember(button);
        return layout;
    }

    // Leaving this although it's unused. It used to be the subclass for the alert count mechanism, which is now
    // gone, but this may be useful in the future for something else.
    public abstract static class RefreshableLabel extends LocatableLabel {
        public RefreshableLabel(String locatorId) {
            super(locatorId);
        }

        // scheduling refreshes is sub-optimal, really need to move to a message bus architecture
        public void schedule(int millis) {
            new Timer() {
                public void run() {
                    refresh();
                }
            }.scheduleRepeating(millis);
        }

        @Override
        protected void onInit() {
            super.onInit();

            refresh();
        }

        public void refresh() {
            if (UserSessionManager.isLoggedIn()) {
                refreshLoggedIn();
            } else {
                refreshLoggedOut();
            }
        }

        public abstract void refreshLoggedIn();

        public void refreshLoggedOut() {
            setContents("");
            setIcon(null);
        }
    }

    public MessageBar getMessageBar() {
        return messageBar;
    }

    public MessageCenterView getMessageCenter() {
        return messageCenter;
    }

    public void reset() {
        messageBar.reset();
        messageCenter.reset();
    }

    private HLayout createHSpacer(int width) {
        HLayout spacer = new HLayout();
        spacer.setWidth(width);
        return spacer;
    }

}
