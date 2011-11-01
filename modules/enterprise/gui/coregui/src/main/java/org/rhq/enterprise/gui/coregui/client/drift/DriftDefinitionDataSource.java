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
package org.rhq.enterprise.gui.coregui.client.drift;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComposite;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.DriftDefinition.BaseDirectory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class DriftDefinitionDataSource extends RPCDataSource<DriftDefinitionComposite, DriftDefinitionCriteria> {

    public static final String ATTR_ENTITY = "object";
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_INTERVAL = "interval";
    public static final String ATTR_DRIFT_HANDLING_MODE = "driftHandlingMode";
    public static final String ATTR_BASE_DIR_STRING = "baseDirString";
    public static final String ATTR_IS_ENABLED = "isEnabled";
    public static final String ATTR_EDIT = "edit";
    public static final String ATTR_IS_PINNED = "isPinned";
    public static final String ATTR_CHANGE_SET_CTIME = "changesetTime";
    public static final String ATTR_CHANGE_SET_VERSION = "changesetVersion";

    public static final String DRIFT_HANDLING_MODE_NORMAL = MSG.view_drift_table_driftHandlingMode_normal();
    public static final String DRIFT_HANDLING_MODE_PLANNED = MSG.view_drift_table_driftHandlingMode_plannedChanges();

    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();
    private EntityContext entityContext;

    public DriftDefinitionDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public DriftDefinitionDataSource(EntityContext context) {
        this.entityContext = context;
        addDataSourceFields();
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField nameField = new ListGridField(ATTR_NAME, MSG.common_title_name());
        fields.add(nameField);

        ListGridField pinnedField = new ListGridField(ATTR_IS_PINNED, MSG.view_drift_table_pinned());
        pinnedField.setType(ListGridFieldType.IMAGE);
        pinnedField.setAlign(Alignment.CENTER);
        pinnedField.addRecordClickHandler(new RecordClickHandler() {

            public void onRecordClick(RecordClickEvent event) {
                switch (entityContext.getType()) {
                case Resource:
                    CoreGUI.goToView(LinkManager.getDriftCarouselSnapshotLink(entityContext.getResourceId(), event
                        .getRecord().getAttributeAsInt(ATTR_ID), 0));
                    break;
                default:
                    throw new IllegalArgumentException("Entity Type not supported");
                }
            }
        });
        pinnedField.setShowHover(true);
        pinnedField.setHoverCustomizer(new HoverCustomizer() {

            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {

                return ImageManager.getPinnedIcon().equals(record.getAttribute(ATTR_IS_PINNED)) ? MSG
                    .view_drift_table_hover_defPinned() : MSG.view_drift_table_hover_defNotPinned();
            }
        });
        fields.add(pinnedField);

        ListGridField changeSetField = new ListGridField(ATTR_CHANGE_SET_VERSION, MSG.view_drift_table_snapshot());
        changeSetField.setCanSortClientOnly(true);
        fields.add(changeSetField);

        ListGridField changeSetTimeField = new ListGridField(ATTR_CHANGE_SET_CTIME, MSG.view_drift_table_snapshotTime());
        changeSetTimeField.setCellFormatter(new TimestampCellFormatter());
        changeSetTimeField.setShowHover(true);
        changeSetTimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(ATTR_CHANGE_SET_CTIME));
        changeSetTimeField.setCanSortClientOnly(true);
        fields.add(changeSetTimeField);

        ListGridField enabledField = new ListGridField(ATTR_IS_ENABLED, MSG.common_title_enabled());
        enabledField.setType(ListGridFieldType.IMAGE);
        enabledField.setAlign(Alignment.CENTER);
        fields.add(enabledField);

        ListGridField driftHandlingModeField = new ListGridField(ATTR_DRIFT_HANDLING_MODE, MSG
            .view_drift_table_driftHandlingMode());
        fields.add(driftHandlingModeField);

        ListGridField intervalField = new ListGridField(ATTR_INTERVAL, MSG.common_title_interval());
        fields.add(intervalField);

        ListGridField baseDirField = new ListGridField(ATTR_BASE_DIR_STRING, MSG.view_drift_table_baseDir());
        changeSetTimeField.setCanSortClientOnly(true);
        fields.add(baseDirField);

        ListGridField editField = new ListGridField(ATTR_EDIT, MSG.common_title_edit());
        editField.setType(ListGridFieldType.IMAGE);
        editField.setAlign(Alignment.CENTER);
        editField.setShowHover(true);
        editField.setCanSort(false);
        editField.addRecordClickHandler(new RecordClickHandler() {

            public void onRecordClick(RecordClickEvent event) {
                switch (entityContext.getType()) {
                case Resource:
                    CoreGUI.goToView(LinkManager.getDriftDefinitionEditLink(entityContext.getResourceId(), event
                        .getRecord().getAttributeAsInt(ATTR_ID)));
                    break;
                default:
                    throw new IllegalArgumentException("Entity Type not supported");
                }
            }
        });
        editField.setHoverCustomizer(new HoverCustomizer() {

            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {

                return MSG.view_drift_table_hover_edit();
            }
        });
        fields.add(editField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
            ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    Integer resourceId = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                    String url = LinkManager.getResourceLink(resourceId);
                    return SeleniumUtility.getLocatableHref(url, o.toString(), null);
                }
            });
            resourceNameField.setShowHover(true);
            resourceNameField.setHoverCustomizer(new HoverCustomizer() {
                public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                    return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                }
            });
            fields.add(resourceNameField);

            ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
            fields.add(ancestryField);

            nameField.setWidth("20%");
            enabledField.setWidth(60);
            driftHandlingModeField.setWidth("10%");
            intervalField.setWidth(100);
            baseDirField.setWidth("*");
            pinnedField.setWidth(70);
            changeSetField.setWidth(70);
            changeSetTimeField.setWidth(100);
            editField.setWidth(70);
            resourceNameField.setWidth("20%");
            ancestryField.setWidth("40%");
        } else {
            nameField.setWidth("20%");
            changeSetField.setWidth(70);
            changeSetTimeField.setWidth(100);
            enabledField.setWidth(60);
            driftHandlingModeField.setWidth("15%");
            intervalField.setWidth(70);
            baseDirField.setWidth("*");
            pinnedField.setWidth(70);
            editField.setWidth(70);
        }

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final DriftDefinitionCriteria criteria) {
        this.driftService.findDriftDefinitionCompositesByCriteria(criteria,
            new AsyncCallback<PageList<DriftDefinitionComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<DriftDefinitionComposite> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    protected void dataRetrieved(final PageList<DriftDefinitionComposite> result, final DSResponse response,
        final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate, the drift defs are for a single resource
        case Resource:
            response.setData(buildRecords(result));
            // for paging to work we have to specify size of full result set
            response.setTotalRows(getTotalRows(result, response, request));
            processResponse(request.getRequestId(), response);
            break;

        case ResourceGroup:
            //TODO

        default:
            throw new IllegalArgumentException("Unsupported Context Type: " + entityContext);
        }
    }

    /**
     * Sub-classes can override this to add fine-grained control over the result set size. By default the
     * total rows are set to the total result set for the query, allowing proper paging.  But some views (portlets)
     * may want to limit results to a small set (like most recent).  
     * @param result
     * @param response
     * @param request
     * 
     * @return should not exceed result.size(). 
     */
    protected int getTotalRows(final Collection<DriftDefinitionComposite> result, final DSResponse response,
        final DSRequest request) {
        return result.size();
    }

    @Override
    protected DriftDefinitionCriteria getFetchCriteria(DSRequest request) {

        DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
        switch (entityContext.getType()) {
        case Resource:
            criteria.addFilterResourceIds(entityContext.getResourceId());
            break;

        case ResourceGroup:
            //TODO

        default:
            // no filter
        }

        criteria.fetchConfiguration(true);

        // filter out unsortable fields (i.e. fields sorted client-side only)
        PageControl pageControl = getPageControl(request);
        pageControl.removeOrderingField(ATTR_BASE_DIR_STRING);
        pageControl.removeOrderingField(ATTR_CHANGE_SET_CTIME);
        pageControl.removeOrderingField(ATTR_CHANGE_SET_VERSION);
        criteria.setPageControl(pageControl);

        return criteria;
    }

    @Override
    public DriftDefinitionComposite copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(DriftDefinitionComposite from) {
        return convert(from);
    }

    public static ListGridRecord convert(DriftDefinitionComposite from) {
        DriftDefinition def = from.getDriftDefinition();
        DriftChangeSet<?> changeSet = from.getMostRecentChangeset();
        ListGridRecord record = new ListGridRecord();

        // We need this for Detect Now support
        record.setAttribute(ATTR_ENTITY, from.getDriftDefinition());

        record.setAttribute(ATTR_ID, def.getId());
        record.setAttribute(ATTR_NAME, def.getName());
        record.setAttribute(ATTR_DRIFT_HANDLING_MODE, getDriftHandlingModeDisplayName(def.getDriftHandlingMode()));
        record.setAttribute(ATTR_INTERVAL, String.valueOf(def.getInterval()));
        record.setAttribute(ATTR_BASE_DIR_STRING, getBaseDirString(def.getBasedir()));
        record.setAttribute(ATTR_IS_ENABLED, ImageManager.getAvailabilityIcon(def.isEnabled()));
        // fixed value, just the edit icon
        record.setAttribute(ATTR_EDIT, ImageManager.getEditIcon());
        record.setAttribute(ATTR_IS_PINNED, def.isPinned() ? ImageManager.getPinnedIcon() : ImageManager
            .getUnpinnedIcon());

        record.setAttribute(ATTR_CHANGE_SET_VERSION, (null != changeSet) ? String.valueOf(changeSet.getVersion()) : MSG
            .common_label_none());
        record.setAttribute(ATTR_CHANGE_SET_CTIME, (null != changeSet) ? new Date(changeSet.getCtime()) : null);

        return record;
    }

    public static String getDriftHandlingModeDisplayName(DriftHandlingMode driftHandlingMode) {
        switch (driftHandlingMode) {
        case plannedChanges:
            return DRIFT_HANDLING_MODE_PLANNED;

        default:
            return DRIFT_HANDLING_MODE_NORMAL;
        }
    }

    private static String getBaseDirString(BaseDirectory basedir) {
        return basedir.getValueContext() + ":" + basedir.getValueName();
    }

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    protected void setEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
    }

}