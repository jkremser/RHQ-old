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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.MetadataType;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypeLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for editing a Resource's configuration.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceConfigurationEditView extends LocatableVLayout implements PropertyValueChangeListener,
    RefreshableView {

    private Resource resource;
    private ResourcePermission resourcePermission;
    private ConfigurationEditor editor;
    private ToolStrip buttonbar;
    private IButton saveButton;

    private boolean refreshing = false;

    public ResourceConfigurationEditView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);

        this.resource = resourceComposite.getResource();
        this.resourcePermission = resourceComposite.getResourcePermission();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        this.buttonbar = new ToolStrip();
        buttonbar.setWidth100();
        buttonbar.setExtraSpace(10);
        buttonbar.setMembersMargin(5);
        buttonbar.setLayoutMargin(5);

        this.saveButton = new LocatableIButton(this.extendLocatorId("Save"), MSG.common_button_save());
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        buttonbar.addMember(saveButton);
        // The button bar will remain hidden until the configuration has been successfully loaded.
        buttonbar.setVisible(false);
        addMember(buttonbar);

        refresh();

        if (!this.resourcePermission.isConfigureWrite()) {
            Message message = new Message(MSG.view_configurationDetails_noPermission(), Message.Severity.Info, EnumSet
                .of(Message.Option.Transient, Message.Option.Sticky));
            CoreGUI.getMessageCenter().notify(message);
        }
    }

    @Override
    public void refresh() {
        if (this.refreshing) {
            return; // we are already in the process of refreshing, don't do it again
        }

        this.refreshing = true;
        this.buttonbar.setVisible(false);

        if (editor != null) {
            editor.destroy();
            removeMember(editor);
        }
        
        GWTServiceLookup.getConfigurationService().getLatestResourceConfigurationUpdate(resource.getId(),
            new AsyncCallback<ResourceConfigurationUpdate>() {
                @Override
                public void onSuccess(final ResourceConfigurationUpdate result) {
                    if (result == null) {
                        
                        CoreGUI.getMessageCenter().notify(new Message(MSG.view_configurationDetails_noConfigurationFetched(), Message.Severity.Info));
                        
                        saveButton.disable();
                        refreshing = false;
                        return;
                    }
                    
                    ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
                        EnumSet.of(MetadataType.resourceConfigurationDefinition), new TypeLoadedCallback() {
                            @Override
                            public void onTypesLoaded(ResourceType type) {

                                ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();
                                configurationService.getOptionValuesForConfigDefinition(type.getResourceConfigurationDefinition(),new AsyncCallback<ConfigurationDefinition>(){
                                    @Override
                                    public void onFailure(Throwable throwable) {
                                        refreshing = false;
                                        CoreGUI.getErrorHandler().handleError("Failed to load configuration.", throwable);
                                    }

                                    @Override
                                    public void onSuccess(ConfigurationDefinition configurationDefinition) {

                                            editor = new ConfigurationEditor(extendLocatorId("Editor"), configurationDefinition, result.getConfiguration());
                                            editor.setOverflow(Overflow.AUTO);
                                            editor.addPropertyValueChangeListener(ResourceConfigurationEditView.this);
                                            editor.setReadOnly(!resourcePermission.isConfigureWrite());
                                            addMember(editor);

                                            saveButton.disable();
                                            buttonbar.setVisible(true);
                                            markForRedraw();
                                            refreshing = false;
                                    }
                                });
                            }
                    });
                }

                @Override
                public void onFailure(Throwable caught) {
                    refreshing = false;
                    CoreGUI.getErrorHandler().handleError("Failed to load configuration.", caught);
                }
            });
    }

    private void save() {
        Configuration updatedConfiguration = editor.getConfiguration();

        GWTServiceLookup.getConfigurationService().updateResourceConfiguration(resource.getId(), updatedConfiguration,
            new AsyncCallback<ResourceConfigurationUpdate>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_configurationDetails_error_updateFailure(), caught);
                }

                public void onSuccess(ResourceConfigurationUpdate result) {
                    Message message;
                    if (result != null) {
                        String version = String.valueOf(result.getId());
                        message = new Message(MSG.view_configurationDetails_messageConcise(version), MSG
                            .view_configurationDetails_messageDetailed(version, resource.getName()),
                            Message.Severity.Info);
                    } else {
                        // TODO: i18n
                        message = new Message(MSG.view_configurationDetails_configNotUpdatedDueToNoChange(),
                            Message.Severity.Warning);
                    }
                    String configHistoryUrl = LinkManager.getResourceTabLink(resource.getId(),
                        ResourceDetailView.Tab.CONFIGURATION, ResourceDetailView.ConfigurationSubTab.HISTORY);
                    String configHistoryView = configHistoryUrl.substring(1);  // chop off the leading '#'
                    CoreGUI.goToView(configHistoryView, message);
                }
            });
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        MessageCenter messageCenter = CoreGUI.getMessageCenter();
        Message message;
        if (event.isInvalidPropertySetChanged()) {
            Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();
                message = new Message(MSG.view_configurationDetails_allPropertiesValid(), Message.Severity.Info,
                    EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message(MSG.view_configurationDetails_somePropertiesInvalid(invalidPropertyNames.values()
                    .toString()), Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else {
            this.saveButton.enable();
        }
    }

}
