/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.components.form;

import java.util.EnumSet;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * An editor for a SmartGWT {@link Record} backed by an {@link RPCDataSource}.
 *
 * @author Ian Springer
 */
@SuppressWarnings("unchecked")
public abstract class AbstractRecordEditor<DS extends RPCDataSource> extends LocatableVLayout implements
    BookmarkableView, DetailsView {

    private static final Label LOADING_LABEL = new Label(MSG.widget_recordEditor_label_loading());

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";

    private static final int ID_NEW = 0;

    private int recordId;
    private TitleBar titleBar;
    private EnhancedDynamicForm form;
    private DS dataSource;
    private boolean isReadOnly;
    private String dataTypeName;
    private String listViewPath;
    private ButtonBar buttonBar;
    private LocatableVLayout contentPane;
    private boolean postFetchHandlerExecutedAlready;

    public AbstractRecordEditor(String locatorId, DS dataSource, int recordId, String dataTypeName, String headerIcon) {
        super(locatorId);
        this.dataSource = dataSource;
        this.recordId = recordId;
        this.dataTypeName = capitalize(dataTypeName);

        setLayoutMargin(0);
        setMembersMargin(16);

        // Display a "Loading..." label at the top of the view to keep the user informed.
        addMember(LOADING_LABEL);

        // Add title bar. We'll set the actual title later.
        this.titleBar = new TitleBar(this, null, headerIcon);
        this.titleBar.hide();
        addMember(this.titleBar);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        // TODO: The below line is temporary until TableSection.renderView() advances the view id pointer as it should.
        viewPath.next();

        String parentViewPath = viewPath.getParentViewPath();
        this.listViewPath = parentViewPath; // e.g. Administration/Security/Roles
    }

    /**
     * <b>IMPORTANT:</b> Subclasses are responsible for invoking this method after all asynchronous operations invoked
     * by {@link #renderView(ViewPath)} have completed.
     *
     * @param isReadOnly whether or not the record editor should be in read-only mode
     */
    protected void init(boolean isReadOnly) {
        if (this.recordId == ID_NEW && isReadOnly) {
            Message message = new Message(MSG.widget_recordEditor_error_permissionCreate(this.dataTypeName),
                Message.Severity.Error);
            CoreGUI.goToView(getListViewPath(), message);
        } else {
            this.isReadOnly = isReadOnly;

            this.contentPane = buildContentPane();
            this.contentPane.hide();
            addMember(this.contentPane);

            this.buttonBar = buildButtonBar();
            if (this.buttonBar != null) {
                this.buttonBar.hide();
                addMember(this.buttonBar);
            }

            if (this.recordId == ID_NEW) {
                editNewRecord();

                // Now that all the widgets have been created and initialized, make everything visible.
                displayForm();
            } else {
                fetchExistingRecord(this.recordId);
            }
        }
    }

    protected LocatableVLayout buildContentPane() {
        LocatableVLayout contentPane = new LocatableVLayout(extendLocatorId("Content"));
        contentPane.setWidth100();
        contentPane.setHeight100();
        contentPane.setOverflow(Overflow.AUTO);
        //contentPane.setPadding(7);

        this.form = buildForm();
        contentPane.addMember(this.form);

        return contentPane;
    }

    protected ButtonBar buildButtonBar() {
        if (this.isReadOnly) {
            return null;
        }

        return new ButtonBar();
    }

    protected EnhancedDynamicForm buildForm() {
        boolean isNewRecord = (this.recordId == ID_NEW);
        EnhancedDynamicForm form = new EnhancedDynamicForm(this.getLocatorId(), isFormReadOnly(), isNewRecord);
        form.setDataSource(this.dataSource);

        List<FormItem> items = createFormItems(form);
        form.setFields(items.toArray(new FormItem[items.size()]));

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent event) {
                AbstractRecordEditor.this.onItemChanged();
            }
        });

        return form;
    }

    protected boolean isFormReadOnly() {
        return this.isReadOnly;
    }

    public LocatableVLayout getContentPane() {
        return this.contentPane;
    }

    public void setForm(EnhancedDynamicForm form) {
        this.form = form;
    }

    public EnhancedDynamicForm getForm() {
        return this.form;
    }

    public DS getDataSource() {
        return this.dataSource;
    }

    public String getDataTypeName() {
        return dataTypeName;
    }

    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    public int getRecordId() {
        return this.recordId;
    }

    public boolean isNewRecord() {
        return (getRecordId() == ID_NEW);
    }

    public String getListViewPath() {
        return this.listViewPath;
    }

    protected abstract List<FormItem> createFormItems(EnhancedDynamicForm form);

    /**
     * This method should be called whenever any editable item on the page is changed. It will enable the Reset button
     * and update the Save button's enablement based on whether or not all items on the form are valid.
     */
    public void onItemChanged() {
        // If we're in editable mode, update the button enablement.
        if (!this.isReadOnly) {
            IButton saveButton = this.buttonBar.getSaveButton();
            if (saveButton.isDisabled()) {
                saveButton.setDisabled(false);
                // removing blink, I think it's distracting and it's not consistent with the rest of the gui (jshaughn)
                //CanvasUtility.blink(saveButton);
            }

            IButton resetButton = this.buttonBar.getResetButton();
            if (resetButton.isDisabled()) {
                resetButton.setDisabled(false);
                // removing blink, I think it's distracting and it's not consistent with the rest of the gui (jshaughn)                
                //CanvasUtility.blink(resetButton);
            }
        }
    }

    protected void reset() {
        this.form.resetValues();
    }

    protected void save(DSRequest requestProperties) {
        if (!this.form.validate()) {
            Message message = new Message(MSG.widget_recordEditor_warn_validation(this.dataTypeName),
                Message.Severity.Warning, EnumSet.of(Message.Option.Transient));
            CoreGUI.getMessageCenter().notify(message);
            return;
        }

        this.form.saveData(new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (response.getStatus() == RPCResponse.STATUS_SUCCESS) {
                    Record[] data = response.getData();
                    Record record = data[0];

                    String id = record.getAttribute(FIELD_ID);
                    String name = record.getAttribute(getTitleFieldName());

                    Message message;
                    String conciseMessage;
                    String detailedMessage;
                    DSOperationType operationType = request.getOperationType();
                    if (Log.isDebugEnabled()) {
                        Object dataObject = dataSource.copyValues(record);
                        if (operationType == DSOperationType.ADD) {
                            Log.debug("Created: " + dataObject);
                        } else {
                            Log.debug("Updated: " + dataObject);
                        }
                    }
                    switch (operationType) {
                    case ADD:
                        conciseMessage = MSG.widget_recordEditor_info_recordCreatedConcise(dataTypeName);
                        detailedMessage = MSG.widget_recordEditor_info_recordCreatedDetailed(dataTypeName, name);
                        if (CoreGUI.isDebugMode()) {
                            conciseMessage += " (" + FIELD_ID + "=" + id + ")";
                            detailedMessage += " (" + FIELD_ID + "=" + id + ")";
                        }
                        break;
                    case UPDATE:
                        conciseMessage = MSG.widget_recordEditor_info_recordUpdatedConcise(dataTypeName);
                        detailedMessage = MSG.widget_recordEditor_info_recordUpdatedDetailed(dataTypeName, name);
                        break;
                    default:
                        throw new IllegalStateException(MSG
                            .widget_recordEditor_error_unsupportedOperationType(operationType.name()));
                    }

                    message = new Message(conciseMessage, detailedMessage);
                    CoreGUI.goToView(getListViewPath(), message);
                } else if (response.getStatus() == RPCResponse.STATUS_VALIDATION_ERROR) {
                    Message message = new Message(MSG.widget_recordEditor_error_operationInvalidValues(),
                        Message.Severity.Error);
                    CoreGUI.getMessageCenter().notify(message);
                } else {
                    // assume failure                    
                    Message message = new Message(MSG.widget_recordEditor_error_operation(), Message.Severity.Error);
                    CoreGUI.getMessageCenter().notify(message);
                }
            }
        }, requestProperties);
    }

    protected void editNewRecord() {
        // Update the view title.
        this.titleBar.setTitle(MSG.widget_recordEditor_title_new(this.dataTypeName));

        // Create a new record.
        Record record = createNewRecord();

        // And populate the form with it.
        this.form.editRecord(record);
        this.form.setSaveOperationType(DSOperationType.ADD);

        // But make sure the value of the "id" field is set to "0", since a value of null could cause the dataSource's
        // copyValues(Record) impl to choke.
        FormItem idItem = this.form.getItem(FIELD_ID);
        if (idItem != null) {
            idItem.setDefaultValue(ID_NEW);
            idItem.hide();
        }

        editRecord(record);
    }

    protected void editExistingRecord(Record record) {
        // Update the view title.
        String recordName = record.getAttribute(getTitleFieldName());
        String title = (this.isReadOnly) ? MSG.widget_recordEditor_title_view(this.dataTypeName, recordName) : MSG
            .widget_recordEditor_title_edit(this.dataTypeName, recordName);
        this.titleBar.setTitle(title);

        // Load the data into the form.
        this.form.editRecord(record);

        // Perform up front validation for existing records.
        // NOTE: We do *not* do this for new records, since we expect most of the required fields to be blank.
        this.form.validate();

        editRecord(record);
    }

    /**
     * Initialize the editor with the data from the passed record. This method will be called for both new records
     * (after the record has been created by {@link #createNewRecord()}) and existing records (after the record has
     * been fetched by {@link #fetchExistingRecord(int)}.
     *
     * @param record the record
     */
    protected void editRecord(Record record) {
    }

    // Subclasses will generally want to override this.
    protected Record createNewRecord() {
        return new ListGridRecord();
    }

    private void displayForm() {
        removeMember(LOADING_LABEL);
        LOADING_LABEL.destroy();

        for (Canvas member : getMembers()) {
            member.show();
        }

        markForRedraw();
    }

    protected void fetchExistingRecord(final int recordId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(FIELD_ID, recordId);
        this.form.fetchData(criteria, new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                // The below check is a workaround for a SmartGWT bug, where it calls the execute() method on this
                // callback twice, rather than once.
                // TODO: Remove it once the SmartGWT bug has been fixed.
                if (!postFetchHandlerExecutedAlready) {
                    postFetchHandlerExecutedAlready = true;
                    if (response.getStatus() == DSResponse.STATUS_SUCCESS) {
                        Record[] records = response.getData();
                        if (records.length == 0) {
                            throw new IllegalStateException(MSG.widget_recordEditor_error_noRecords());
                        }
                        if (records.length > 1) {
                            throw new IllegalStateException(MSG.widget_recordEditor_error_multipleRecords());
                        }
                        Record record = records[0];
                        editExistingRecord(record);

                        // Now that all the widgets have been created and initialized, make everything visible.
                        displayForm();
                    }
                }
            }
        });
    }

    protected String getTitleFieldName() {
        return FIELD_NAME;
    }

    @Override
    public boolean isEditable() {
        return (!this.isReadOnly);
    }

    protected static ListGridRecord[] toListGridRecordArray(Record[] roleRecords) {
        ListGridRecord[] roleListGridRecords = new ListGridRecord[roleRecords.length];
        for (int i = ID_NEW, roleRecordsLength = roleRecords.length; i < roleRecordsLength; i++) {
            Record roleRecord = roleRecords[i];
            roleListGridRecords[i] = (ListGridRecord) roleRecord;
        }
        return roleListGridRecords;
    }

    private static String capitalize(String itemTitle) {
        return Character.toUpperCase(itemTitle.charAt(ID_NEW)) + itemTitle.substring(1);
    }

    protected class ButtonBar extends LocatableToolStrip {

        private IButton saveButton;
        private IButton resetButton;
        private IButton cancelButton;

        ButtonBar() {
            super(AbstractRecordEditor.this.extendLocatorId("ButtonBar"));

            setWidth100();
            setHeight(35);

            LocatableVLayout vLayout = new LocatableVLayout(extendLocatorId("VLayout"));
            vLayout.setAlign(VerticalAlignment.CENTER);
            vLayout.setLayoutMargin(4);

            LocatableHLayout hLayout = new LocatableHLayout(vLayout.extendLocatorId("HLayout"));
            hLayout.setMembersMargin(10);
            vLayout.addMember(hLayout);

            saveButton = new LocatableIButton(extendLocatorId("Save"), MSG.common_button_save());
            saveButton.setDisabled(true);
            saveButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    save(new DSRequest());
                }
            });
            hLayout.addMember(saveButton);

            resetButton = new LocatableIButton(extendLocatorId("Reset"), MSG.common_button_reset());
            resetButton.setDisabled(true);
            resetButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    resetButton.disable();
                    saveButton.disable();
                    reset();
                }
            });
            hLayout.addMember(resetButton);

            cancelButton = new LocatableIButton(extendLocatorId("Cancel"), MSG.common_button_cancel());
            cancelButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    CoreGUI.goToView(getListViewPath());
                }
            });
            hLayout.addMember(cancelButton);

            addMember(vLayout);
        }

        public IButton getCancelButton() {
            return cancelButton;
        }

        public IButton getResetButton() {
            return resetButton;
        }

        public IButton getSaveButton() {
            return saveButton;
        }
    }

}
