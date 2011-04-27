/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.components;

import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLFlow;

/**
 * A link to another view within coregui.war. Clicking on the link will navigate to the view via the GWT history API
 * to avoid a page reload.
 *
 * @author Ian Springer
 */
public class ViewLink extends Link {

    private String viewPath;

    public ViewLink(String locatorId, String linkText, final String viewPath) {
        super(locatorId, linkText, new ClickHandler() {
            public void onClick(ClickEvent event) {
                CoreGUI.goToView(viewPath);
            }
        });

        this.viewPath = viewPath;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + getContents() + " -> " + this.viewPath + "]";
    }

}
