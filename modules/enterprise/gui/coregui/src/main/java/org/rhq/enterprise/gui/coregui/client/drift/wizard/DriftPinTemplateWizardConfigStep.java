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

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 */
public class DriftPinTemplateWizardConfigStep extends AbstractWizardStep {

    private LocatableVLayout vLayout;
    private ConfigurationEditor editor;
    AbstractDriftPinTemplateWizard wizard;
    private boolean isConfirmed;

    public DriftPinTemplateWizardConfigStep(AbstractDriftPinTemplateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        // to skip a step just return null, the wizard framework will then just call nextPage()
        if (!wizard.isCreateTemplate()) {
            return null;
        }

        // This VLayout allows us to set overflow on it and be able to scroll the config editor but always
        // be able to see the wizard's next/cancel buttons. This vlayout also provides for easier expansion if we add more items.
        if (vLayout == null) {

            String locatorId = (null == parent) ? "DriftDefConfig" : parent.extendLocatorId("DriftDefConfig");
            vLayout = new LocatableVLayout(locatorId);

            vLayout.setOverflow(Overflow.AUTO);

            Configuration startingConfig = wizard.getSnapshotDriftDef().getConfiguration();
            ConfigurationDefinition def = DriftConfigurationDefinition.getInstance();
            editor = new ConfigurationEditor(vLayout.extendLocatorId("Editor"), def, startingConfig);
            vLayout.addMember(editor);
        }

        return vLayout;
    }

    public boolean nextPage() {
        if (wizard.isCreateTemplate() && (null == editor || !editor.validate())) {
            return false;
        }

        if (isConfirmed) {
            if (wizard.isCreateTemplate()) {
                wizard.setNewConfiguration(editor.getConfiguration());
            }
            wizard.execute();
            return true;
        }

        String message = (!wizard.isCreateTemplate() && wizard.getSelectedTemplate().isPinned()) ? MSG
            .view_drift_wizard_pinTemplate_confirmPinned() : MSG.view_drift_wizard_pinTemplate_confirmNotPinned();

        SC.ask(message, new BooleanCallback() {
            public void execute(Boolean confirmed) {
                if (confirmed) {
                    isConfirmed = true;
                    nextPage();

                } else {
                    wizard.getView().closeDialog();
                }
            }
        });

        return false;
    }

    public String getName() {
        return "";
    }
}