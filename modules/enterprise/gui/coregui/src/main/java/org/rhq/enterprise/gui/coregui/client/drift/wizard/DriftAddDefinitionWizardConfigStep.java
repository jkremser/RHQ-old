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
import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.common.EntityContext;
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
public class DriftAddDefinitionWizardConfigStep extends AbstractWizardStep {

    private LocatableVLayout vLayout;
    private ConfigurationEditor editor;
    AbstractDriftAddDefinitionWizard wizard;
    private Configuration startingConfig;

    public DriftAddDefinitionWizardConfigStep(AbstractDriftAddDefinitionWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        // This VLayout allows us to set overflow on it and be able to scroll the config editor but always
        // be able to see the wizard's next/cancel buttons. This vlayout also provides for easier expansion if we add more items.
        if (vLayout == null || !wizard.getNewStartingConfiguration().equals(startingConfig)) {

            String locatorId = (null == parent) ? "DriftDefConfig" : parent.extendLocatorId("DriftDefConfig");
            vLayout = new LocatableVLayout(locatorId);

            vLayout.setOverflow(Overflow.AUTO);

            // keep a reference to the startingConfig in case the user navs back and changes it
            startingConfig = wizard.getNewStartingConfiguration();
            ConfigurationDefinition def = getDriftConfigDef();
            editor = new ConfigurationEditor(vLayout.extendLocatorId("Editor"), def, startingConfig);
            vLayout.addMember(editor);
        }

        return vLayout;
    }

    private ConfigurationDefinition getDriftConfigDef() {
        if (wizard.getEntityContext().getType() == EntityContext.Type.ResourceTemplate) {
            return DriftConfigurationDefinition.getNewTemplateInstance();
        }

        if (wizard.getSelectedTemplate().isPinned()) {
            return DriftConfigurationDefinition.getNewResourceInstanceByPinnedTemplate();
        }
        return DriftConfigurationDefinition.getInstance();
    }

    public boolean nextPage() {
        if (editor != null && editor.validate()) {
            wizard.setNewConfiguration(editor.getConfiguration());
            wizard.execute();
            return true;
        }

        return false;
    }

    public String getName() {
        return "";
    }
}