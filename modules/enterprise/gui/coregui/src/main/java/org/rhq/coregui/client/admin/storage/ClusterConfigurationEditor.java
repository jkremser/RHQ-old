/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * The component for editing the cluster wide configuration
 *
 * @author Jirka Kremser
 */
public class ClusterConfigurationEditor extends EnhancedVLayout implements RefreshableView {

    private EnhancedDynamicForm form;
    private EnhancedIButton saveButton;
    private StorageClusterSettings settings;
    private final boolean readOnly;

    private static String FIELD_CQL_PORT = "cql_port";
    private static String FIELD_GOSSIP_PORT = "gossip_port";
    private static String FIELD_AUTOMATIC_DEPLOYMENT = "automatic_deployment";
    private static String FIELD_PASSWORD = "password";
    private static String FIELD_PASSWORD_VERIFY = "verify_password";

    public ClusterConfigurationEditor(boolean readOnly) {
        super();
        this.readOnly = readOnly;
    }

    private void fetchClusterSettings() {
        GWTServiceLookup.getStorageService().retrieveClusterSettings(new AsyncCallback<StorageClusterSettings>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Unable to load common storage cluster configuration: " + caught.getMessage(), caught);
            }

            @Override
            public void onSuccess(StorageClusterSettings settings) {
                ClusterConfigurationEditor.this.settings = settings;
                prepareForm();
            }
        });
    }

    private void save() {
        updateSettings();
        GWTServiceLookup.getStorageService().updateClusterSettings(settings, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                Message msg = new Message("Storage cluster settings were successfully updated.", Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(msg);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Unable to update the storage node settings.", caught);
            }
        });
    }

    private List<FormItem> buildHeaderItems() {
        List<FormItem> fields = new ArrayList<FormItem>();
        fields.add(createHeaderTextItem(MSG.view_configEdit_property()));
        fields.add(createHeaderTextItem(MSG.common_title_value()));
        fields.add(createHeaderTextItem(MSG.common_title_description()));
        return fields;
    }

    private StaticTextItem createHeaderTextItem(String value) {
        StaticTextItem unsetHeader = new StaticTextItem();
        unsetHeader.setValue(value);
        unsetHeader.setShowTitle(false);
        unsetHeader.setCellStyle("configurationEditorHeaderCell");
        return unsetHeader;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        refresh();
    }

    private void prepareForm() {
        form = new EnhancedDynamicForm();
        form.setHiliteRequiredFields(true);
        form.setNumCols(3);
        form.setCellPadding(5);
        form.setColWidths(190, 220, "*");
        form.setIsGroup(true);
        form.setGroupTitle("Cluster Wide Settings");
        form.setBorder("1px solid #AAA");

        List<FormItem> items = buildHeaderItems();
        IsIntegerValidator validator = new IsIntegerValidator();
        
        // cql port field
        FormItemBuilder builder = new FormItemBuilder();
        List<FormItem> cqlPortItems = builder.withName(FIELD_CQL_PORT).withTitle("CQL Port")
            .withValue(String.valueOf(settings.getCqlPort()))
            .withDescription("The port on which the Storage Nodes listens for CQL client connections.")
            .withValidator(validator).build();
        items.addAll(cqlPortItems);

        // gossip port field
        builder = new FormItemBuilder();
        List<FormItem> gossipPortItems = builder.withName(FIELD_GOSSIP_PORT).withTitle("Gossip Port")
            .withValue(String.valueOf(settings.getGossipPort()))
            .withDescription("The port used for internode communication in the storage cluster.")
            .withValidator(validator).build();
        items.addAll(gossipPortItems);
        
        // password field
        builder = new FormItemBuilder();
        List<FormItem> passwordItems = builder.withName(FIELD_PASSWORD).withTitle("Password")
            .withValue(settings.getPassword()).withDescription("bla bla").withReadOnlySetTo(readOnly)
            .build((FormItem) GWT.create(PasswordItem.class));
        items.addAll(passwordItems);

        // password_verify field
        builder = new FormItemBuilder();
        MatchesFieldValidator passwordValidator = new MatchesFieldValidator();
        passwordValidator.setOtherField(FIELD_PASSWORD);
        List<FormItem> passwordVerifyItems = builder.withName(FIELD_PASSWORD_VERIFY).withTitle("Verify Password")
            .withValue(settings.getPassword()).withDescription("sldf sddd").withReadOnlySetTo(readOnly)
            .withValidator(passwordValidator).build((FormItem) GWT.create(PasswordItem.class));
        items.addAll(passwordVerifyItems);

        // automatic deployment field
        builder = new FormItemBuilder();
        List<FormItem> automaticDeploymentItems = builder.withName(FIELD_AUTOMATIC_DEPLOYMENT).withTitle("Automatic Deployment")
            .withValue(Boolean.toString(settings.getAutomaticDeployment())).withDescription("If this is set, the newly installed storage nodes will be automatically deployed to the storage cluster.")
            .build((FormItem) GWT.create(RadioGroupItem.class));
        RadioGroupItem autoDeployRadio = (RadioGroupItem) automaticDeploymentItems.get(1);
        autoDeployRadio.setVertical(false);
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>(2);
        values.put("true", "On");
        values.put("false", "Off");
        autoDeployRadio.setValueMap(values);
        autoDeployRadio.setValue(settings.getAutomaticDeployment());
        items.addAll(automaticDeploymentItems);
        
        form.setFields(items.toArray(new FormItem[items.size()]));
        form.setWidth100();
        form.setOverflow(Overflow.VISIBLE);
        setWidth100();

        LayoutSpacer spacer = new LayoutSpacer();
        spacer.setWidth100();

        ToolStrip toolStrip = buildToolStrip();
        setMembers(form, spacer, toolStrip);
        form.validate();
        markForRedraw();
    }

    @Override
    public void refresh() {
        fetchClusterSettings();
    }

    private EnhancedToolStrip buildToolStrip() {
        saveButton = new EnhancedIButton(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    SC.ask(
                        "Changing the cluster wide configuration will eventually affect all the storage nodes. Do you want to continue?",
                        new BooleanCallback() {
                            @Override
                            public void execute(Boolean value) {
                                if (value) {
                                    save();
                                }
                            }
                        });
                }
            }
        });
        saveButton.setDisabled(readOnly);
        EnhancedToolStrip toolStrip = new EnhancedToolStrip();
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);
        toolStrip.addMember(saveButton);

        return toolStrip;
    }

    private StorageClusterSettings updateSettings() {
        settings.setCqlPort(Integer.parseInt(form.getValueAsString(FIELD_CQL_PORT)));
        settings.setGossipPort(Integer.parseInt(form.getValueAsString(FIELD_GOSSIP_PORT)));
        settings.setAutomaticDeployment(Boolean.parseBoolean(form.getValueAsString(FIELD_AUTOMATIC_DEPLOYMENT)));
        settings.setPassword(form.getValueAsString(FIELD_PASSWORD));
        return settings;
    }
    
    private static class FormItemBuilder {
        private String name;
        private String title;
        private String value;
        private String description;
        private Validator validator;
        private boolean readOnly;
        
        private static boolean oddRow = true;
        
        public FormItemBuilder withName(String name) {
            this.name = name;
            return this;
        }
        
        public FormItemBuilder withTitle(String title) {
            this.title = title;
            return this;
        }
        
        public FormItemBuilder withValue(String value) {
            this.value = value;
            return this;
        }
        
        public FormItemBuilder withDescription(String description) {
            this.description = description;
            return this;
        }
        
        public FormItemBuilder withValidator(Validator validator) {
            this.validator = validator;
            return this;
        }
        
        public FormItemBuilder withReadOnlySetTo(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }
        
        public List<FormItem> build() {
            return build(new TextItem());
        }
        
        // GWT doesn't support reflection 
        public List<FormItem> build(FormItem valueItem) {
            List<FormItem> fields = new ArrayList<FormItem>();
            StaticTextItem nameItem = new StaticTextItem();
            nameItem.setStartRow(true);
            nameItem.setValue("<b>" + title + "</b>");
            nameItem.setShowTitle(false);
            nameItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(nameItem);

            valueItem.setName(name);
            valueItem.setValue(value);
            valueItem.setWidth(220);
            if (validator != null) {
                valueItem.setValidators(validator);
            }
            valueItem.setValidateOnChange(true);
            valueItem.setAlign(Alignment.CENTER);
            valueItem.setShowTitle(false);
            valueItem.setRequired(true);
            valueItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            valueItem.setDisabled(readOnly);
            fields.add(valueItem);

            StaticTextItem descriptionItem = new StaticTextItem();
            descriptionItem.setValue(description);
            descriptionItem.setShowTitle(false);
            descriptionItem.setEndRow(true);
            descriptionItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(descriptionItem);

            oddRow = !oddRow;
            return fields;
        }
    
    }
}
