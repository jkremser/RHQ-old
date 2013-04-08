/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.bean.OperationSchedule;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.DurationItem;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.form.SortedSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.form.TimeUnit;
import org.rhq.enterprise.gui.coregui.client.components.form.UnitType;
import org.rhq.enterprise.gui.coregui.client.components.trigger.JobTriggerEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.TypeConversionUtility;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A view for viewing or editing an RHQ {@link org.rhq.core.domain.operation.bean.OperationSchedule operation schedule}.
 *
 * @author Ian Springer
 */
public abstract class AbstractOperationScheduleDetailsView extends
    AbstractRecordEditor<AbstractOperationScheduleDataSource<? extends OperationSchedule>> {

    private static final String FIELD_OPERATION_DESCRIPTION = "operationDescription";
    private static final String FIELD_OPERATION_PARAMETERS = "operationParameters";

    protected static final int FIRST_COLUMN_WIDTH = 140;

    private Map<Integer, String> operationIdToNameMap = new HashMap<Integer, String>();
    private Map<String, String> operationNameToDescriptionMap = new HashMap<String, String>();
    private Map<String, ConfigurationDefinition> operationNameToParametersDefinitionMap = new HashMap<String, ConfigurationDefinition>();
    private StaticTextItem operationDescriptionItem;
    private StaticTextItem operationParametersItem;
    private EnhancedHLayout operationParametersConfigurationHolder;
    private ConfigurationEditor operationParametersConfigurationEditor;
    private Configuration operationParameters;
    private JobTriggerEditor triggerEditor;
    private EnhancedDynamicForm notesForm;
    private Integer operationDefinitionId;
    private ViewPath viewPath;
    private boolean isImmediateExecution;

    public AbstractOperationScheduleDetailsView(AbstractOperationScheduleDataSource<? extends OperationSchedule> dataSource,
        ResourceType resourceType, int scheduleId) {
        super(dataSource, scheduleId, MSG.view_operationScheduleDetails_operationSchedule(), null);

        this.setMembersMargin(5);

        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        for (OperationDefinition operationDefinition : operationDefinitions) {
            this.operationIdToNameMap.put(operationDefinition.getId(), operationDefinition.getName());
            this.operationNameToDescriptionMap.put(operationDefinition.getName(), operationDefinition.getDescription());
            this.operationNameToParametersDefinitionMap.put(operationDefinition.getName(),
                operationDefinition.getParametersConfigurationDefinition());
        }
    }

    protected abstract boolean hasControlPermission();

    protected abstract int getResourceId();

    @Override
    public String getListViewPath() {
        // If the operation is scheduled for immediate execution, we will send the user
        // to the history page so that he can view the status/result of the operation;
        // otherwise, the user will stay on the schedules list view.
        if (isImmediateExecution) {
            // If the operation is scheduled from the context menu, the view path will have
            // another entry appended to the end, the operation definition id (viewPath.getCurrentIndex() == 6)
            // Similarly, auto groups have another chunk of view path present ("AutoGroup").
            // If the operation is scheduled for the auto group, the view path will include "Resource/AutoGroup"
            // This was causing BZ 823908
            boolean isAutogroup = viewPath.getParentViewPath().contains(ResourceGroupDetailView.AUTO_GROUP_VIEW);
            if ((!isAutogroup && viewPath.getCurrentIndex() == 6) || (isAutogroup && viewPath.getCurrentIndex() == 7)) {
                return viewPath.getPathToIndex(viewPath.getCurrentIndex() - 3) + "/History";
            }
            // common case (for resource, group not using context menu)
            return viewPath.getPathToIndex(viewPath.getCurrentIndex() - 2) + "/History";
        }
        return super.getListViewPath();
    }

    @Override
    public void renderView(ViewPath viewPath) {
        this.viewPath = viewPath;
        super.renderView(viewPath);

        // If the operationDefId has been seeded capture it now
        this.operationDefinitionId = 0;
        if (!viewPath.isEnd()) {
            this.operationDefinitionId = viewPath.getCurrentAsInt();
            viewPath.next();
        }

        // Existing schedules are not editable. This may change in the future.
        boolean isReadOnly = (!hasControlPermission() || (getRecordId() != 0));

        // Note: subclases may override this and perform async work prior to calling super, so
        // be careful adding any code after this point in this method.
        init(isReadOnly);
    }

    @Override
    protected void init(boolean isReadOnly) {
        super.init(isReadOnly);

        // If the operationDefId has been seeded then treat it as a user-initiated change to
        // update associated widgets and button enablement.
        if (this.operationDefinitionId != null) {
            FormItem nameField = this.getForm().getField(AbstractOperationScheduleDataSource.Field.OPERATION_NAME);
            nameField.setValue(this.operationIdToNameMap.get(this.operationDefinitionId));
            handleOperationNameChange();
            getForm().rememberValues();
        }
    }

    @Override
    protected EnhancedDynamicForm buildForm() {
        EnhancedDynamicForm form = super.buildForm();

        form.setNumCols(3);
        form.setColWidths(FIRST_COLUMN_WIDTH, "140", "*");

        return form;
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        if (!isNewRecord()) {
            StaticTextItem idItem = new StaticTextItem(AbstractOperationScheduleDataSource.Field.ID);
            items.add(idItem);
        }

        SelectItem operationNameItem = new SortedSelectItem(AbstractOperationScheduleDataSource.Field.OPERATION_NAME);
        operationNameItem.setShowTitle(true);
        items.add(operationNameItem);
        operationNameItem.addChangedHandler(new ChangedHandler() {

            @Override
            public void onChanged(ChangedEvent event) {
                handleOperationNameChange();
            }
        });

        this.operationDescriptionItem = new StaticTextItem(FIELD_OPERATION_DESCRIPTION,
            MSG.view_operationScheduleDetails_field_description());
        this.operationDescriptionItem.setShowTitle(false);
        items.add(this.operationDescriptionItem);

        this.operationParametersItem = new StaticTextItem(FIELD_OPERATION_PARAMETERS,
            MSG.view_operationScheduleDetails_field_parameters());
        this.operationParametersItem.setColSpan(2);
        items.add(this.operationParametersItem);

        return items;
    }

    // override reset because we can't just blindly reset the op def name and leave behind associated widgets
    @Override
    protected void reset() {
        super.reset();
        handleOperationNameChange();
    }

    // The same logic needs to get applied to a user-initiated change, direct navigation to an op def, and
    // a reset, which may reset to a selected op def, or to no op def.
    private void handleOperationNameChange() {
        refreshOperationDescriptionItem();
        refreshOperationParametersItem();
        if (null != getSelectedOperationName()) {
            onItemChanged();
        }
    }

    @Override
    protected EnhancedVLayout buildContentPane() {
        EnhancedVLayout contentPane = super.buildContentPane();

        this.operationParametersConfigurationHolder = new EnhancedHLayout();
        this.operationParametersConfigurationHolder.setVisible(false);
        contentPane.addMember(this.operationParametersConfigurationHolder);

        HTMLFlow hr = new HTMLFlow("<hr/>");
        contentPane.addMember(hr);

        this.triggerEditor = new JobTriggerEditor(isReadOnly());
        contentPane.addMember(this.triggerEditor);

        hr = new HTMLFlow("<hr/>");
        contentPane.addMember(hr);

        this.notesForm = new EnhancedDynamicForm(isReadOnly(), isNewRecord());
        this.notesForm.setColWidths(FIRST_COLUMN_WIDTH, "50%", "140", "50%");

        this.notesForm.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent event) {
                AbstractOperationScheduleDetailsView.this.onItemChanged();
            }
        });

        List<FormItem> notesFields = new ArrayList<FormItem>();

        TreeSet<TimeUnit> supportedUnits = new TreeSet<TimeUnit>();
        supportedUnits.add(TimeUnit.SECONDS);
        supportedUnits.add(TimeUnit.MINUTES);
        supportedUnits.add(TimeUnit.HOURS);
        DurationItem timeoutItem = new DurationItem(AbstractOperationScheduleDataSource.Field.TIMEOUT,
            MSG.view_operationScheduleDetails_field_timeout(), supportedUnits, false, isReadOnly());
        ProductInfo productInfo = CoreGUI.get().getProductInfo();
        timeoutItem.setContextualHelp(MSG.view_operationScheduleDetails_fieldHelp_timeout(productInfo.getShortName()));
        notesFields.add(timeoutItem);

        if (!isNewRecord()) {
            StaticTextItem nextFireTimeItem = new StaticTextItem(
                AbstractOperationScheduleDataSource.Field.NEXT_FIRE_TIME,
                MSG.dataSource_operationSchedule_field_nextFireTime());
            notesFields.add(nextFireTimeItem);
        }

        TextAreaItem notesItem = new TextAreaItem(ResourceOperationScheduleDataSource.Field.DESCRIPTION,
            MSG.dataSource_operationSchedule_field_description());
        notesItem.setWidth(450);
        notesItem.setHeight(60);
        notesItem.setShowTitle(true);
        FormUtility.addContextualHelp(notesItem, MSG.view_operationScheduleDetails_fieldHelp_description());
        notesFields.add(notesItem);

        this.notesForm.setFields(notesFields.toArray(new FormItem[notesFields.size()]));

        contentPane.addMember(this.notesForm);

        return contentPane;
    }

    @Override
    protected ButtonBar buildButtonBar() {
        ButtonBar buttonBar = super.buildButtonBar();

        if (null != buttonBar) {
            buttonBar.getSaveButton().setTitle(MSG.common_button_schedule());
        }

        return buttonBar;
    }

    @Override
    protected String getTitleFieldName() {
        return ResourceOperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME;
    }

    @Override
    protected Record createNewRecord() {
        Record record = super.createNewRecord();

        Subject sessionSubject = UserSessionManager.getSessionSubject();
        record.setAttribute(ResourceOperationScheduleDataSource.Field.SUBJECT, sessionSubject.getName());
        record.setAttribute(ResourceOperationScheduleDataSource.Field.SUBJECT_ID, sessionSubject.getId());

        return record;
    }

    @Override
    protected void editRecord(Record record) {
        refreshOperationDescriptionItem();
        refreshOperationParametersItem();

        super.editRecord(record);
    }

    @Override
    protected void editExistingRecord(Record record) {
        JavaScriptObject jobTriggerJavaScriptObject = (JavaScriptObject) getForm().getValue(
            AbstractOperationScheduleDataSource.Field.JOB_TRIGGER);
        Record jobTriggerRecord = new ListGridRecord(jobTriggerJavaScriptObject);
        JobTrigger jobTrigger = getDataSource().createJobTrigger(jobTriggerRecord);
        this.triggerEditor.setJobTrigger(jobTrigger);

        FormItem nextFireTimeItem = this.notesForm.getField(AbstractOperationScheduleDataSource.Field.NEXT_FIRE_TIME);
        nextFireTimeItem.setValue(getForm().getValue(AbstractOperationScheduleDataSource.Field.NEXT_FIRE_TIME));

        DurationItem timeoutItem = (DurationItem) this.notesForm
            .getField(AbstractOperationScheduleDataSource.Field.TIMEOUT);
        Object value = getForm().getValue(AbstractOperationScheduleDataSource.Field.TIMEOUT);
        Integer integerValue = TypeConversionUtility.toInteger(value);
        timeoutItem.setValue(integerValue, UnitType.TIME);

        StaticTextItem notesItem = (StaticTextItem) this.notesForm
            .getField(AbstractOperationScheduleDataSource.Field.DESCRIPTION);
        // Notes field is user-editable, so escape HTML to prevent an XSS attack. Unless empty, then don't to prevent
        // displaying &nbsp; as the value.
        String notesValue = getForm().getValueAsString(AbstractOperationScheduleDataSource.Field.DESCRIPTION);
        if (null != notesValue && !notesValue.isEmpty()) {
            notesItem.setEscapeHTML(true);
        }
        notesItem.setValue(notesValue);

        this.operationParameters = (Configuration) record
            .getAttributeAsObject(AbstractOperationScheduleDataSource.Field.PARAMETERS);
        super.editExistingRecord(record);
    }

    @Override
    protected void save(DSRequest requestProperties) {
        if ((null != this.operationParametersConfigurationEditor && !this.operationParametersConfigurationEditor
            .isValid()) || !this.triggerEditor.validate()) {
            {
                Message message = new Message(MSG.widget_recordEditor_warn_validation(this.getDataTypeName()),
                    Message.Severity.Warning, EnumSet.of(Message.Option.Transient));
                CoreGUI.getMessageCenter().notify(message);

                return;
            }
        }

        requestProperties.setAttribute(AbstractOperationScheduleDataSource.RequestProperty.PARAMETERS,
            this.operationParameters);

        EnhancedDynamicForm form = getForm();

        Record jobTriggerRecord = new ListGridRecord();

        Date startTime = this.triggerEditor.getStartTime();
        isImmediateExecution = startTime == null;
        jobTriggerRecord.setAttribute(AbstractOperationScheduleDataSource.Field.START_TIME, startTime);

        Date endTime = this.triggerEditor.getEndTime();
        jobTriggerRecord.setAttribute(AbstractOperationScheduleDataSource.Field.END_TIME, endTime);

        Integer repeatCount = this.triggerEditor.getRepeatCount();
        jobTriggerRecord.setAttribute(AbstractOperationScheduleDataSource.Field.REPEAT_COUNT, repeatCount);

        Long repeatInterval = this.triggerEditor.getRepeatInterval();
        jobTriggerRecord.setAttribute(AbstractOperationScheduleDataSource.Field.REPEAT_INTERVAL, repeatInterval);

        String cronExpression = this.triggerEditor.getCronExpression();
        jobTriggerRecord.setAttribute(AbstractOperationScheduleDataSource.Field.CRON_EXPRESSION, cronExpression);

        form.setValue(AbstractOperationScheduleDataSource.Field.JOB_TRIGGER, jobTriggerRecord);

        DurationItem timeoutItem = (DurationItem) this.notesForm
            .getItem(AbstractOperationScheduleDataSource.Field.TIMEOUT);
        Long timeout = timeoutItem.getValueAsLong();
        if (timeout != null) {
            form.setValue(AbstractOperationScheduleDataSource.Field.TIMEOUT, timeout);
        } else {
            form.setValue(AbstractOperationScheduleDataSource.Field.TIMEOUT, (String) null);
        }

        FormItem notesItem = this.notesForm.getField(AbstractOperationScheduleDataSource.Field.DESCRIPTION);
        form.setValue(AbstractOperationScheduleDataSource.Field.DESCRIPTION, (String) notesItem.getValue());

        super.save(requestProperties);
    }

    private void refreshOperationDescriptionItem() {
        String operationName = getSelectedOperationName();
        String value;
        if (operationName == null) {
            value = "<i>" + MSG.view_operationScheduleDetails_fieldDefault_description() + "</i>";
        } else {
            value = this.operationNameToDescriptionMap.get(operationName);
        }
        this.operationDescriptionItem.setValue(value);
    }

    private void refreshOperationParametersItem() {
        String operationName = getSelectedOperationName();
        String value;
        if (isNewRecord()) { // BZ 909157: do it only for new schedule
            operationParameters = null; // reset params between dropdown selects
            // make sure we wipe out anything left by the previous op def
            for (Canvas child : this.operationParametersConfigurationHolder.getChildren()) {
                child.destroy();
            }
        }

        if (operationName == null) {
            value = "<i>" + MSG.view_operationScheduleDetails_fieldDefault_parameters() + "</i>";
            this.operationParametersConfigurationHolder.hide();
        } else {
            final ConfigurationDefinition parametersDefinition = this.operationNameToParametersDefinitionMap
                .get(operationName);
            if (parametersDefinition == null || parametersDefinition.getPropertyDefinitions().isEmpty()) {
                value = "<i>" + MSG.view_operationScheduleDetails_noParameters() + "</i>";
                this.operationParametersConfigurationHolder.hide();

            } else {
                value = isNewRecord() ? "<i>" + MSG.view_operationScheduleDetails_enterParametersBelow() + "</i>" : "";

                // Add spacer so params are indented.
                VLayout horizontalSpacer = new VLayout();
                horizontalSpacer.setWidth(FIRST_COLUMN_WIDTH);
                this.operationParametersConfigurationHolder.addMember(horizontalSpacer);

                if (isNewRecord()) {
                    ConfigurationTemplate defaultTemplate = parametersDefinition.getDefaultTemplate();
                    this.operationParameters = (defaultTemplate != null) ? defaultTemplate.createConfiguration()
                        : new Configuration();
                }

                ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();
                configurationService.getOptionValuesForConfigDefinition(getResourceId(), parametersDefinition,
                    new AsyncCallback<ConfigurationDefinition>() {

                        @Override
                        public void onFailure(Throwable throwable) {
                            operationParametersConfigurationEditor = new ConfigurationEditor(parametersDefinition,
                                operationParameters);
                            operationParametersConfigurationEditor.setReadOnly(isReadOnly());
                            operationParametersConfigurationHolder.addMember(operationParametersConfigurationEditor);
                            operationParametersConfigurationHolder.show();

                        }

                        @Override
                        public void onSuccess(ConfigurationDefinition result) {
                            operationParametersConfigurationEditor = new ConfigurationEditor(result,
                                operationParameters);
                            operationParametersConfigurationEditor.setReadOnly(isReadOnly());
                            operationParametersConfigurationHolder.addMember(operationParametersConfigurationEditor);
                            operationParametersConfigurationHolder.show();

                        }
                    });
            }
        }
        this.operationParametersItem.setValue(value);
    }

    private String getSelectedOperationName() {
        FormItem operationNameItem = getForm().getField(AbstractOperationScheduleDataSource.Field.OPERATION_NAME);
        return (String) operationNameItem.getValue();
    }

}
