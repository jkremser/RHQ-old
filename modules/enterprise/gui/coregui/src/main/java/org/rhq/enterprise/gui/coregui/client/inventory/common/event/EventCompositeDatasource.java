/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.common.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Joseph Marques
 */
public class EventCompositeDatasource extends RPCDataSource<EventComposite, EventCriteria> {

    public static final String FILTER_SEVERITIES = "severities";

    private EntityContext entityContext;

    public EventCompositeDatasource(EntityContext context) {
        super();
        this.entityContext = context;
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField timestampField = new ListGridField("timestamp", MSG.view_inventory_eventHistory_timestamp());
        TimestampCellFormatter.prepareDateField(timestampField);
        fields.add(timestampField);

        ListGridField severityField = new ListGridField("severity", MSG.view_inventory_eventHistory_severity());
        severityField.setAlign(Alignment.CENTER);
        severityField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String icon = ImageManager.getEventSeverityBadge(EventSeverity.valueOf(o.toString()));
                return Canvas.imgHTML(icon);
            }
        });
        severityField.setShowHover(true);
        severityField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                EventSeverity severity = EventSeverity.valueOf(record.getAttribute("severity"));
                switch (severity) {
                case DEBUG:
                    return MSG.common_severity_debug();
                case INFO:
                    return MSG.common_severity_info();
                case WARN:
                    return MSG.common_severity_warn();
                case ERROR:
                    return MSG.common_severity_error();
                case FATAL:
                    return MSG.common_severity_fatal();
                }
                return null;
            }
        });
        fields.add(severityField);

        ListGridField detailField = new ListGridField("detail", MSG.view_inventory_eventHistory_details());
        detailField.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (null == value) {
                    return "";
                } else if (((String) value).length() <= 200) {
                    return (String) value;
                } else {
                    return ((String) value).substring(0, 200); // first 200 chars
                }
            }
        });
        fields.add(detailField);

        ListGridField sourceField = new ListGridField("source", MSG.view_inventory_eventHistory_sourceLocation());
        sourceField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String sourceLocation = listGridRecord.getAttribute("source");
                int length = sourceLocation.length();
                if (length > 40) {
                    return "..." + sourceLocation.substring(length - 40); // the last 40 chars
                }
                return sourceLocation;
            }
        });
        sourceField.setShowHover(true);
        sourceField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String sourceLocation = record.getAttribute("source");
                return (sourceLocation.length() > 40) ? sourceLocation : null;
            }
        });
        fields.add(sourceField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
            ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    String url = LinkManager
                        .getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
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

            timestampField.setWidth(155);
            severityField.setWidth(55);
            detailField.setWidth("*");
            sourceField.setWidth(180);
            resourceNameField.setWidth("20%");
            ancestryField.setWidth("25%");
        } else {
            timestampField.setWidth(155);
            severityField.setWidth(55);
            detailField.setWidth("*");
            sourceField.setWidth(220);
        }

        return fields;
    }

    @Override
    public EventComposite copyValues(Record from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(EventComposite from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getEventId());
        record.setAttribute("timestamp", from.getTimestamp());
        record.setAttribute("detail", from.getEventDetail());
        record.setAttribute("severity", from.getSeverity().name());
        record.setAttribute("source", from.getSourceLocation());

        // for ancestry handling       
        record.setAttribute(AncestryUtil.RESOURCE_ID, from.getResourceId());
        record.setAttribute(AncestryUtil.RESOURCE_NAME, from.getResourceName());
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, from.getResourceAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, from.getResourceTypeId());

        return record;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final EventCriteria criteria) {
        if (criteria == null) {
            // the user selected no severities in the filter - it makes sense from the UI perspective to show 0 rows
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            return;
        }

        GWTServiceLookup.getEventService().findEventCompositesByCriteria(criteria,
            new AsyncCallback<PageList<EventComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_eventDetails_loadFailed(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                }

                public void onSuccess(final PageList<EventComposite> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overridden.
     */
    protected void dataRetrieved(final PageList<EventComposite> result, final DSResponse response,
        final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate, the alerts are for a singe resource
        case Resource:
            Record[] records = buildRecords(result);
            highlightFilterMatches(request, records);
            response.setData(records);
            // for paging to work we have to specify size of full result set
            response.setTotalRows(result.getTotalSize());
            processResponse(request.getRequestId(), response);

            break;

        // disambiguate as the results could be cross-resource
        default:
            Set<Integer> typesSet = new HashSet<Integer>();
            Set<String> ancestries = new HashSet<String>();
            for (EventComposite composite : result) {
                typesSet.add(composite.getResourceTypeId());
                ancestries.add(composite.getResourceAncestry());
            }

            // In addition to the types of the result resources, get the types of their ancestry
            typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

            ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
            typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
                @Override
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                    AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                    Record[] records = buildRecords(result);
                    highlightFilterMatches(request, records);

                    for (Record record : records) {
                        // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                        // Store the types map off the records so we can build a detailed hover string as needed.                      
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                        // Build the decoded ancestry Strings now for display
                        record
                            .setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                    }

                    response.setData(records);
                    // for paging to work we have to specify size of full result set
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected EventCriteria getFetchCriteria(final DSRequest request) {
        EventSeverity[] severities = getArrayFilter(request, FILTER_SEVERITIES, EventSeverity.class);
        if (severities == null || severities.length == 0) {
            return null; // user didn't select any severities - return null to indicate no data should be displayed
        }

        EventCriteria criteria = new EventCriteria();

        PageControl pageControl = getPageControl(request);
        if (pageControl.getOrderingFields().isEmpty()) {
            criteria.addSortTimestamp(PageOrdering.DESC); // default sort
        } else {
            criteria.setPageControl(pageControl);
        }

        // TODO: This call is broken in 2.2, http://code.google.com/p/smartgwt/issues/detail?id=490
        // when using AdvancedCriteria
        Map<String, Object> criteriaMap = request.getCriteria().getValues();

        criteria.addFilterSourceName((String) criteriaMap.get("source"));
        criteria.addFilterDetail((String) criteriaMap.get("detail"));
        // There's no need to add a severities filter to the criteria if the user specified all severities.
        if (severities.length != EventSeverity.values().length) {
            criteria.addFilterSeverities(severities);
        }

        criteria.addFilterEntityContext(entityContext);

        return criteria;
    }
}
