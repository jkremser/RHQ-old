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

package org.rhq.enterprise.gui.coregui.client.test;

import java.util.EnumSet;
import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.enterprise.gui.coregui.client.util.measurement.MeasurementParser;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class TestNumberFormatView extends LocatableVLayout {

    public TestNumberFormatView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        LinkedHashMap<String, String> unitsChoices = new LinkedHashMap<String, String>();
        for (MeasurementUnits unit : EnumSet.allOf(MeasurementUnits.class)) {
            unitsChoices.put(unit.name(), unit.name());
        }

        final SelectItem unitsMenu = new SelectItem("unitsItem", "Units");
        unitsMenu.setValueMap(unitsChoices);
        unitsMenu.setDefaultValue(MeasurementUnits.BYTES.name());

        final TextItem textItem = new TextItem("valueItem", "Value");

        final StaticTextItem resultsValueItem = new StaticTextItem("resultsValue", "Results Value");
        final StaticTextItem resultsUnitItem = new StaticTextItem("resultsUnits", "Results Units");

        ButtonItem button = new ButtonItem("convertItem", "Convert");
        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                MeasurementUnits units = MeasurementUnits.valueOf(unitsMenu.getValueAsString());
                String value = textItem.getValueAsString();
                MeasurementNumericValueAndUnits vu = MeasurementParser.parse(value, units);
                resultsValueItem.setValue(vu.getValue().toString());
                resultsUnitItem.setValue(vu.getUnits().name());
            }
        });

        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("form"));
        form.setItems(unitsMenu, textItem, button, resultsValueItem, resultsUnitItem);

        addMember(form);
    }
}
