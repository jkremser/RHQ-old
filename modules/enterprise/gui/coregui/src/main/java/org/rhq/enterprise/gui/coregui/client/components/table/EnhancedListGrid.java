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
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;

import java.util.HashMap;
import java.util.Map;

/**
 * A subclass of ListGrid that provides generic enhancements.
 *
 * @author Ian Springer
 */
public class EnhancedListGrid extends LocatableListGrid {

    private Map<String, ListGridField> originalFields;

    public EnhancedListGrid(String locatorId) {
        super(locatorId);
    }

    @Override
    public void setFields(ListGridField... fields) {
        super.setFields(fields);

        this.originalFields = new HashMap<String, ListGridField>();
        if (fields != null) {
            for (ListGridField field : fields) {
                this.originalFields.put(field.getName(), field);
                if (field instanceof CanvasField) {
                    setShowRecordComponents(true);
                    setShowRecordComponentsByCell(true);
                    // This is a workaround for a SmartGWT bug.
                    field.setCellFormatter(new CellFormatter() {
                        public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                            return "";
                        }
                    });
                }
            }
        }
    }

    /**
     * @see CanvasField
     */
    @Override
    protected Canvas createRecordComponent(ListGridRecord record, Integer colNum) {
        if (getShowRecordComponents() == null || !getShowRecordComponents()) {
            return null;
        }

        ListGridField field = getField(colNum);
        String fieldName = field.getName();
        ListGridField originalField = this.originalFields.get(fieldName);
        if (originalField instanceof CanvasField) {
            CanvasField canvasField = (CanvasField)originalField;
            Object value;
            try {
                Object rawValue = record.getAttributeAsObject(fieldName);
                value = (rawValue instanceof JavaScriptObject) ?
                        JSOHelper.convertToJava((JavaScriptObject) rawValue) : rawValue;
            } catch (RuntimeException e) {
                value = record.getAttribute(fieldName);
            }
            return canvasField.createCellComponent(this, record, value);
        } else {
            return null;
        }
    }

}
