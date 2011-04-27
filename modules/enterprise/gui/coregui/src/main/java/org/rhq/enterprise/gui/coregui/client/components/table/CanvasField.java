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
package org.rhq.enterprise.gui.coregui.client.components.table;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Formats a cell value as a SmartGWT canvas.
 */
public abstract class CanvasField extends ListGridField {

    protected CanvasField() {
    }

    protected CanvasField(JavaScriptObject jsObj) {
        super(jsObj);
    }

    protected CanvasField(String name) {
        super(name);
    }

    protected CanvasField(String name, int width) {
        super(name, width);
    }

    protected CanvasField(String name, String title) {
        super(name, title);
    }

    protected CanvasField(String name, String title, int width) {
        super(name, title, width);
    }

    protected CanvasField(ListGridField field) {
        super(field.getName(), field.getTitle());

        setWidth(field.getWidth());
        // TODO: clone other commonly used fields
    }

    /**
     * TODO
     *
     * @param grid
     * @param record
     * @return
     */
    protected abstract Canvas createCanvas(ListGrid grid, ListGridRecord record);

    protected HLayout createHLayout(ListGrid grid) {
        HLayout component = new HLayout();
        component.setWidth100();
        // TODO: is this the best way to deal w/ height?
        component.setHeight(25);
        component.setMargin(grid.getCellPadding());
        component.setOverflow(Overflow.HIDDEN);
        return component;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ", title=" + getTitle() + "]";
    }

}
