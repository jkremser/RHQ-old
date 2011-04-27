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

import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLFlow;

/**
 * A link.
 *
 * @author Ian Springer
 */
public class Link extends LocatableHTMLFlow {

    private static final String DEFAULT_MOUSE_OUT_STYLE_NAME = "viewLink";
    private static final String DEFAULT_MOUSE_OVER_STYLE_NAME = "viewLinkHover";

    private String mouseOverStyleName;
    private String mouseOutStyleName;

    public Link(String locatorId, String linkText, ClickHandler clickHandler) {
        super(locatorId);

        this.mouseOutStyleName = DEFAULT_MOUSE_OUT_STYLE_NAME;
        this.mouseOverStyleName = DEFAULT_MOUSE_OVER_STYLE_NAME;

	    setWidth100();
        setHeight(25);
        setContents(linkText);

        addClickHandler(clickHandler);

        addMouseOverHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                if (mouseOutStyleName != null && mouseOverStyleName != null) {
                    setStyleName(mouseOverStyleName);
                    markForRedraw();
                }
            }
        });

        addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                if (mouseOutStyleName != null && mouseOverStyleName != null) {
                    setStyleName(mouseOutStyleName);
                    markForRedraw();
                }
            }
        });
    }

    @Override
    protected void onInit() {
        super.onInit();

        setStyleName(getMouseOutStyleName());
    }

    public String getMouseOverStyleName() {
        return this.mouseOverStyleName;
    }

    public void setMouseOverStyleName(String mouseOverStyleName) {
        this.mouseOverStyleName = mouseOverStyleName;
    }

    public String getMouseOutStyleName() {
        return this.mouseOutStyleName;
    }

    public void setMouseOutStyleName(String mouseOutStyleName) {
        this.mouseOutStyleName = mouseOutStyleName;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + getContents() + " -> ?]";
    }

}
