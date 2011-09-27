/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.drift.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Jay Shaughnessy
 */
public class DriftAddConfigWizardInfoStep extends AbstractWizardStep {

    private LocatableDynamicForm form;
    private AbstractDriftAddConfigWizard wizard;
    private Map<String, ConfigurationTemplate> templates;

    public DriftAddConfigWizardInfoStep(AbstractDriftAddConfigWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        if (form == null) {

            if (parent != null) {
                form = new LocatableDynamicForm(parent.extendLocatorId("DriftAddConfigInfo"));
            } else {
                form = new LocatableDynamicForm("DriftAddConfigInfo");
            }
            form.setNumCols(1);
            List<FormItem> formItems = new ArrayList<FormItem>(2);

            SelectItem templateSelect = new SelectItem("template", MSG.view_drift_wizard_addDef_templatePrompt());
            templateSelect.setTitleOrientation(TitleOrientation.TOP);
            templateSelect.setAlign(Alignment.LEFT);
            templateSelect.setWidth(300);
            templateSelect.setRequired(true);
            FormUtility.addContextualHelp(templateSelect, MSG.view_drift_wizard_addDef_templateHelp());

            Set<ConfigurationTemplate> templates = wizard.getType().getDriftConfigurationTemplates();
            final HashMap<String, ConfigurationTemplate> templatesMap = new HashMap<String, ConfigurationTemplate>(
                templates.size());
            if (!templates.isEmpty()) {
                for (ConfigurationTemplate template : templates) {
                    templatesMap.put(template.getName(), template);
                }
            } else {
                // there should be at least one template for any resource type that supports drift monitoring
                throw new IllegalStateException(
                    "At least one drift configuration template should exist for the resource type");
            }

            Set<String> templatesMapKeySet = templatesMap.keySet();
            String[] templatesMapKeySetArray = templatesMapKeySet.toArray(new String[templatesMap.size()]);
            templateSelect.setValueMap(templatesMapKeySetArray);
            if (templatesMapKeySetArray.length == 1) {
                // there is only one, select it for the user
                String theOne = templatesMapKeySetArray[0];
                templateSelect.setValue(theOne);
                wizard.setNewStartingConfiguration(templatesMap.get(theOne).createConfiguration());
            }
            templateSelect.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setNewStartingConfiguration(templatesMap.get(value).createConfiguration());
                }
            });

            formItems.add(templateSelect);

            form.setItems(formItems.toArray(new FormItem[formItems.size()]));
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        return MSG.view_drift_wizard_addDef_infoStepName();
    }

    public Configuration getStartingConfiguration() {
        String template = form.getValueAsString("template");
        return templates.get(template).createConfiguration();
    }
}
