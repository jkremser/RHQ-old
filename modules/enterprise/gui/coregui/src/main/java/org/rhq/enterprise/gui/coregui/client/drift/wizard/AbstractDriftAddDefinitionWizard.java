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

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;

/**
 * @author Jay Shaughnessy
 */
public abstract class AbstractDriftAddDefinitionWizard extends AbstractWizard {

    private EntityContext context;
    private ResourceType type;

    private DriftDefinitionTemplate selectedTemplate;
    private Configuration newStartingConfiguration;
    private DriftDefinition newDriftDefinition;

    private WizardView view;

    public AbstractDriftAddDefinitionWizard(final EntityContext context, ResourceType type) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }

        if (type == null) {
            throw new NullPointerException("type == null");
        }

        this.context = context;
        this.type = type;
    }

    public String getSubtitle() {
        return null;
    }

    abstract public void execute();

    public void display() {
        view = new WizardView(this);
        view.displayDialog();
    }

    public EntityContext getEntityContext() {
        return context;
    }

    public ResourceType getType() {
        return type;
    }

    public DriftDefinitionTemplate getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(DriftDefinitionTemplate template) {
        selectedTemplate = template;
    }

    public Configuration getNewStartingConfiguration() {
        return newStartingConfiguration;
    }

    public void setNewStartingConfiguration(Configuration newStartingConfiguration) {
        this.newStartingConfiguration = newStartingConfiguration;
    }

    public DriftDefinition getNewDriftDefinition() {
        return newDriftDefinition;
    }

    public void setNewConfiguration(Configuration newDriftDefinitionConfig) {
        newDriftDefinition = new DriftDefinition(newDriftDefinitionConfig);
        newDriftDefinition.setTemplate(selectedTemplate);
    }

    public void cancel() {
        // nothing to do
    }

}
