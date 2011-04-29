package org.rhq.enterprise.gui.coregui.client.components.table;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.enterprise.gui.coregui.client.components.ViewLink;

/**
 * A list grid field that is displayed as a link to another CoreGUI view.
 *
 * @author Ian Springer
 */
public abstract class ViewLinkField extends CanvasField {

    protected ViewLinkField() {
        super();
    }

    protected ViewLinkField(JavaScriptObject jsObj) {
        super(jsObj);
    }

    protected ViewLinkField(String name) {
        super(name);
    }

    protected ViewLinkField(String name, int width) {
        super(name, width);
    }

    protected ViewLinkField(String name, String title) {
        super(name, title);
    }

    protected ViewLinkField(String name, String title, int width) {
        super(name, title, width);
    }

    @Override
    protected Canvas createCanvas(ListGrid grid, ListGridRecord record, Object value) {
        VLayout vLayout = createVLayout(grid);
        ViewLink viewLink = getViewLink(grid, record, value);
        vLayout.addMember(viewLink);
        return vLayout;
    }

    protected abstract ViewLink getViewLink(ListGrid grid, ListGridRecord record, Object value);

    protected ViewLinkField(ListGridField field) {
        super(field);
    }

}
