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

    private static final String DEFAULT_MOUSE_OVER_STYLE_NAME = "linkHover";
    private static final String DEFAULT_MOUSE_OUT_STYLE_NAME = "link";

    private String mouseOverStyleName;
    private String mouseOutStyleName;

    public Link(String linkText, ClickHandler clickHandler) {
        this(linkText, linkText, clickHandler);
    }

    public Link(String locatorId, String linkText, ClickHandler clickHandler) {
        super(locatorId);

        this.mouseOverStyleName = DEFAULT_MOUSE_OVER_STYLE_NAME;
        this.mouseOutStyleName = DEFAULT_MOUSE_OUT_STYLE_NAME;

        // TODO (ips, 04/28/11): This is hacky - try to find a better way.
        int width = calculateWidth(linkText);
        setWidth(width);
        setContents(linkText);
        addClickHandler(clickHandler);
    }

    private int calculateWidth(String string) {
        float width = 0;
        for (int i = 0; i < string.length(); i++) {
            float charWidth = (Character.isUpperCase(string.charAt(i))) ? 10.5F : 5.5F;
            width += charWidth;
        }
        width +=1;
        return (int)width;
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (this.mouseOverStyleName != null) {
            addMouseOverHandler(new MouseOverHandler() {
                public void onMouseOver(MouseOverEvent event) {
                    handleMouseOverEvent();
                }
            });
        }

        if (this.mouseOutStyleName != null) {
            setStyleName(this.mouseOutStyleName);
            addMouseOutHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent event) {
                    handleMouseOutEvent();
                }
            });
        }
    }

    protected void handleMouseOverEvent() {
        setStyleName(mouseOverStyleName);
        markForRedraw();
    }

    protected void handleMouseOutEvent() {
        setStyleName(mouseOutStyleName);
        markForRedraw();
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
