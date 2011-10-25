/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.drift;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.drift.wizard.DriftAddDefinitionWizard;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A list view that displays a paginated table of {@link org.rhq.core.domain.drift.DriftDefinition}s. It has offers various
 * options on the list like filtering (maybe) and sorting, add new/delete. Double-click drills down to the carousel view for
 * inspecting drift for the definition. Also, allows an edit view for the def's underlying Config. This view full respects 
 * the user's authorization, and will not allow actions on the drift defs unless the user is either the inventory 
 * manager or has MANAGE_DRIFT permission on every resource corresponding to the drift defs being operated on.
 *
 * @author Jay Shaughnessy
 */
public class DriftDefinitionsView extends TableSection<DriftDefinitionDataSource> {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("DriftDefs", MSG.common_title_definitions());

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(DriftDefinitionDataSource.ATTR_NAME,
        SortDirection.ASCENDING);

    private static final Criteria INITIAL_CRITERIA = new Criteria();

    private EntityContext context;
    private boolean hasWriteAccess;
    private DriftDefinitionDataSource dataSource;
    private boolean useEditDetailsView;

    static {
        DriftCategory[] categoryValues = DriftCategory.values();
        String[] categoryNames = new String[categoryValues.length];
        int i = 0;
        for (DriftCategory c : categoryValues) {
            categoryNames[i++] = c.name();
        }

        // Add any INITIAL_CRITERIA here (non currently)
    }

    // for subsystem views
    public DriftDefinitionsView(String locatorId) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), false);
    }

    public DriftDefinitionsView(String locatorId, EntityContext entityContext) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), entityContext, false);
    }

    public DriftDefinitionsView(String locatorId, String tableTitle, EntityContext entityContext) {
        this(locatorId, tableTitle, entityContext, false);
    }

    protected DriftDefinitionsView(String locatorId, String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(locatorId, tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setInitialCriteriaFixed(false);
        setDataSource(getDataSource());
    }

    @Override
    public DriftDefinitionDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new DriftDefinitionDataSource(context);
        }
        return this.dataSource;
    }

    @Override
    protected void configureTableFilters() {
        // currently no table filters
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
        setupTableInteractions(this.hasWriteAccess);

        super.configureTable();
    }

    private void setupTableInteractions(final boolean hasWriteAccess) {
        TableActionEnablement deleteEnablement = hasWriteAccess ? TableActionEnablement.ANY
            : TableActionEnablement.NEVER;
        TableActionEnablement detectNowEnablement = hasWriteAccess ? TableActionEnablement.SINGLE
            : TableActionEnablement.NEVER;

        addTableAction("New", MSG.common_button_new(), null, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return hasWriteAccess;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                add();
            }
        });

        addTableAction("Delete", MSG.common_button_delete(), MSG.view_drift_delete_defConfirm(),
            new AbstractTableAction(deleteEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    delete(selection);
                }
            });

        addTableAction("DeleteAll", MSG.common_button_delete_all(), MSG.view_drift_delete_defConfirmAll(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    ListGrid grid = getListGrid();
                    ResultSet resultSet = (null != grid) ? grid.getResultSet() : null;
                    return (hasWriteAccess && grid != null && resultSet != null && !resultSet.isEmpty());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteAll();
                }
            });

        addTableAction("DetectNow", MSG.view_drift_button_detectNow(), null, new AbstractTableAction(
            detectNowEnablement) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                detectDrift(selection); // will only ever be a single selection - see detectNowEnablement variable
            }
        });
    }

    private void add() {
        DriftAddDefinitionWizard.showWizard(context, this);
        // we can refresh the table buttons immediately since the wizard is a dialog, the
        // user can't access enabled buttons anyway.
        DriftDefinitionsView.this.refreshTableInfo();
    }

    private void delete(ListGridRecord[] records) {
        final String[] driftDefNames = new String[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            String driftDefName = record.getAttribute(DriftDefinitionDataSource.ATTR_NAME);
            driftDefNames[i] = driftDefName;
        }

        deleteDriftDefinitionsByName(driftDefNames);
    }

    private void deleteAll() {
        final RecordList records = getListGrid().getDataAsRecordList();
        final int numRecords = records.getLength();
        final String[] driftDefNames = new String[numRecords];
        for (int i = 0; i < numRecords; i++) {
            Record record = records.get(i);
            String driftDefName = record.getAttribute(DriftDefinitionDataSource.ATTR_NAME);
            driftDefNames[i] = driftDefName;
        }

        deleteDriftDefinitionsByName(driftDefNames);
    }

    private void deleteDriftDefinitionsByName(final String[] driftDefNames) {
        GWTServiceLookup.getDriftService().deleteDriftDefinitionsByContext(context, driftDefNames,
            new AsyncCallback<Integer>() {
                public void onSuccess(Integer resultCount) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_drift_success_deleteDefs(String.valueOf(resultCount)),
                            Message.Severity.Info));
                    refresh();
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_deleteDefs(), caught);
                }
            });
    }

    private void detectDrift(ListGridRecord[] records) {
        // we will only ever have a single record selected, hence why we can access the [0] item
        DriftDefinition driftDef = (DriftDefinition) records[0]
            .getAttributeAsObject(DriftDefinitionDataSource.ATTR_ENTITY);
        GWTServiceLookup.getDriftService().detectDrift(context, driftDef, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_detectNow(), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_detectNow(), caught);
            }
        });
    }

    @Override
    public void renderView(ViewPath viewPath) {
        // we have two detail views for drift defs, the config editor and the carousel. figure out which one we're
        // dealing with. The default is the carousel, anything further in the path we assume to be /Edit 
        if (!viewPath.isEnd()) {
            this.useEditDetailsView = !viewPath.isNextEnd() && "Edit".equals(viewPath.getNext().getPath());
        }

        super.renderView(viewPath);
    }

    @Override
    public Canvas getDetailsView(Integer driftDefId) {
        if (this.useEditDetailsView) {
            return new DriftDefinitionEditView(extendLocatorId("DefinitionEdit"), context, driftDefId, hasWriteAccess);
        }

        return new DriftCarouselView(extendLocatorId("Carousel"), context, driftDefId, hasWriteAccess);
    }

    public EntityContext getContext() {
        return context;
    }
}
