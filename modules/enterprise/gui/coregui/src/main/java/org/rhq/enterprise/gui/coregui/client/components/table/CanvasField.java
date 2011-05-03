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
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * A special LisGrid field, when used in conjunction with an {@link EnhancedListGrid}, whose values will be formatted as
 * custom SmartGWT canvases. The ListGrid equivalent of a DynamicForm CanvasItem.
 *
 * @author Ian Springer
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
    }

    /**
     * Take care when overriding this method. Generally, always call the super impl and don't mess with the
     * properties it has explicitly set (width, height, overflow. etc.).
     *
     * @param grid the grid containing this field
     * @param record the record containing the data for the row containing the cell being rendered
     * @param value the value of this field in the above record
     *
     * @return returns a canvas, which is the representation of the specified record value, to become the content
     *         of the corresponding grid cell
     */
    protected Canvas createCellComponent(ListGrid grid, ListGridRecord record, Object value) {
        VLayout vLayout = new VLayout();

        vLayout.setWidth100();
        vLayout.setHeight100();

        // Chop off stuff that doesn't fit, which is as ListGrid does for other cells.
        vLayout.setOverflow(Overflow.HIDDEN);

        // Use the grid-specified cell-padding as the margin (not 100% sure this is necessary).
        vLayout.setMargin(grid.getCellPadding());

        // We want center vertical alignment, since that's what ListGrid uses for other cells.
        vLayout.setAlign(VerticalAlignment.CENTER);

        // The below incantations are needed in order for this canvas to resize when the corresponding grid column is
        // resized. I figured these out by reading the Javadoc for ListGrid.addEmbeddedComponent().
        vLayout.setCanDragResize(true);
        vLayout.setSnapToGrid(true);

        // Call the abstract createCanvas() method which creates the Canvas containing the actual content.
        Canvas canvas = createCanvas(grid, record, value);
        if (canvas != null) {
            vLayout.addMember(canvas);
        }

        return vLayout;
    }

    /**
     * TODO
     *
     * @param grid the grid containing this field
     * @param record the record containing the data for the row containing the cell being rendered
     * @param value the value of this field in the above record
     *
     * @return the canvas containing content representing this field in the passed-in record, or null if the cell
     *         should be empty
     */
    protected abstract com.smartgwt.client.widgets.Canvas createCanvas(ListGrid grid, ListGridRecord record, Object value);

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ", title=" + getTitle() + "]";
    }

}
