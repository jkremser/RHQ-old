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
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Joseph Marques
 */
public class EventCompositeHistoryView extends TableSection<EventCompositeDatasource> {

    private static final SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier("timestamp", SortDirection.DESCENDING);
    private static final Criteria INITIAL_CRITERIA = new Criteria();
    private EntityContext context;
    private boolean hasWriteAccess;

    static {
        EventSeverity[] severityValues = EventSeverity.values();
        String[] severityNames = new String[severityValues.length];
        int i = 0;
        for (EventSeverity s : severityValues) {
            severityNames[i++] = s.name();
        }

        INITIAL_CRITERIA.addCriteria(EventCompositeDatasource.FILTER_SEVERITIES, severityNames);
    }

    public static EventCompositeHistoryView get(String locatorId, ResourceGroupComposite composite) {
        String tableTitle = MSG.view_inventory_eventHistory_groupEventHistory();
        EntityContext context = EntityContext.forGroup(composite.getResourceGroup().getId());
        boolean hasWriteAccess = composite.getResourcePermission().isEvent();
        return new EventCompositeHistoryView(locatorId, tableTitle, context, hasWriteAccess);
    }

    public static EventCompositeHistoryView get(String locatorId, ResourceComposite composite) {
        String tableTitle = MSG.view_inventory_eventHistory_resourceEventHistory();
        EntityContext context = EntityContext.forResource(composite.getResource().getId());
        boolean hasWriteAccess = composite.getResourcePermission().isEvent();
        return new EventCompositeHistoryView(locatorId, tableTitle, context, hasWriteAccess);
    }

    private EventCompositeHistoryView(String locatorId, String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(locatorId, tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setInitialCriteriaFixed(false);
        setDataSource(new EventCompositeDatasource(this.context));
    }

    @Override
    protected void onInit() {
        setFlexRowDisplay(false);

        super.onInit();
    }

    @Override
    protected void configureTableFilters() {
        final TextItem sourceFilter = new TextItem("source", MSG.view_inventory_eventHistory_sourceFilter());
        final TextItem detailsFilter = new TextItem("detail", MSG.view_inventory_eventHistory_detailsFilter());

        LinkedHashMap<String, String> severities = new LinkedHashMap<String, String>(5);
        severities.put(EventSeverity.DEBUG.name(), MSG.common_severity_debug());
        severities.put(EventSeverity.INFO.name(), MSG.common_severity_info());
        severities.put(EventSeverity.WARN.name(), MSG.common_severity_warn());
        severities.put(EventSeverity.ERROR.name(), MSG.common_severity_error());
        severities.put(EventSeverity.FATAL.name(), MSG.common_severity_fatal());
        LinkedHashMap<String, String> severityIcons = new LinkedHashMap<String, String>(5);
        severityIcons.put(EventSeverity.DEBUG.name(), ImageManager.getEventSeverityBadge(EventSeverity.DEBUG));
        severityIcons.put(EventSeverity.INFO.name(), ImageManager.getEventSeverityBadge(EventSeverity.INFO));
        severityIcons.put(EventSeverity.WARN.name(), ImageManager.getEventSeverityBadge(EventSeverity.WARN));
        severityIcons.put(EventSeverity.ERROR.name(), ImageManager.getEventSeverityBadge(EventSeverity.ERROR));
        severityIcons.put(EventSeverity.FATAL.name(), ImageManager.getEventSeverityBadge(EventSeverity.FATAL));
        final EnumSelectItem severityFilter = new EnumSelectItem(EventCompositeDatasource.FILTER_SEVERITIES, MSG
            .view_inventory_eventHistory_severityFilter(), EventSeverity.class, severities, severityIcons);

        setFilterFormItems(sourceFilter, detailsFilter, severityFilter);
    }

    @Override
    protected void configureTable() {
        ListGridField timestampField = new ListGridField("timestamp", MSG.view_inventory_eventHistory_timestamp());
        TimestampCellFormatter.prepareDateField(timestampField);

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

        timestampField.setWidth(155);
        severityField.setWidth(55);
        detailField.setWidth("*");
        sourceField.setWidth(220);

        setListGridFields(timestampField, severityField, detailField, sourceField);

        setupTableInteractions();

        super.configureTable();
    }

    protected String getDetailsLinkColumnName() {
        return "timestamp";
    }

    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                Integer recordId = getId(record);
                String detailsUrl = "#" + getBasePath() + "/" + recordId;
                return SeleniumUtility.getLocatableHref(detailsUrl, TimestampCellFormatter.format(value), null);
            }
        };
    }

    private void setupTableInteractions() {
        TableActionEnablement singleTargetEnablement = hasWriteAccess ? TableActionEnablement.ANY
            : TableActionEnablement.NEVER;
        addTableAction("deleteButton", MSG.common_button_delete(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(singleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteButtonPressed(selection);
                }
            });

        TableActionEnablement multipleTargetEnablement = hasWriteAccess ? TableActionEnablement.ALWAYS
            : TableActionEnablement.NEVER;
        addTableAction("purgeAllButton", MSG.common_button_purgeAll(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(multipleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    purgeButtonPressed();
                }
            });
    }

    private void deleteButtonPressed(ListGridRecord[] selection) {
        List<Integer> eventIds = new ArrayList<Integer>();
        for (ListGridRecord nextRecord : selection) {
            eventIds.add(nextRecord.getAttributeAsInt("id"));
        }
        GWTServiceLookup.getEventService().deleteEventsForContext(context, eventIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_inventory_eventHistory_deleteSuccessful(result.toString(), context
                        .toShortString()), Severity.Info));
                refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_inventory_eventHistory_deleteFailed(context.toShortString()), caught);
            }
        });
    }

    private void purgeButtonPressed() {
        GWTServiceLookup.getEventService().purgeEventsForContext(context, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_inventory_eventHistory_purgeSuccessful(result.toString(), context
                        .toShortString()), Severity.Info));
                refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_inventory_eventHistory_purgeFailed(context.toShortString()), caught);
            }
        });
    }

    @Override
    public Canvas getDetailsView(int eventId) {
        return EventCompositeDetailsView.getInstance();
    }

}
