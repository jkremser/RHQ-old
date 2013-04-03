/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricStackedBarGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceMetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3Graph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;

/**
 * Build the Group version of the View that shows the individual graph views.
 * @author Mike Thompson
 */
public final class D3GroupGraphListView extends AbstractD3GraphListView {

    private ResourceGroup resourceGroup;
    private VLayout graphsVLayout;

    public D3GroupGraphListView(ResourceGroup resourceGroup, boolean monitorDetailView) {
        super();
        this.resourceGroup = resourceGroup;
        this.showAvailabilityGraph = monitorDetailView;
        setOverflow(Overflow.HIDDEN);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        destroyMembers();

        addMember(measurementRangeEditor);
        if (showAvailabilityGraph) {
            availabilityGraph = new AvailabilityD3Graph(new AvailabilityLineGraphType(resourceGroup.getId()));
            // first step in 2 step to create d3 chart
            // create a placeholder for avail graph
            availabilityGraph.createGraphMarker();
            addMember(availabilityGraph);
        }
        graphsVLayout = new VLayout();
        graphsVLayout.setOverflow(Overflow.AUTO);
        graphsVLayout.setWidth100();

        if (resourceGroup != null) {
            redrawGraphs();
        }
        addMember(graphsVLayout);
    }

    /**
     * Build whatever graph metrics (MeasurementDefinitions) are defined for the resource.
     */
    public void redrawGraphs() {

        queryAvailability(EntityContext.forGroup(resourceGroup), measurementRangeEditor.getStartTime(),
            measurementRangeEditor.getEndTime(), null);

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceGroup.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(final ResourceType type) {

                    final ArrayList<MeasurementDefinition> measurementDefinitions = new ArrayList<MeasurementDefinition>();

                    for (MeasurementDefinition def : type.getMetricDefinitions()) {
                        if (def.getDataType() == DataType.MEASUREMENT && def.getDisplayType() == DisplayType.SUMMARY) {
                            measurementDefinitions.add(def);
                        }
                    }

                    Collections.sort(measurementDefinitions, new Comparator<MeasurementDefinition>() {
                        public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                            return new Integer(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
                        }
                    });

                    int[] measDefIdArray = new int[measurementDefinitions.size()];
                    for (int i = 0; i < measDefIdArray.length; i++) {
                        measDefIdArray[i] = measurementDefinitions.get(i).getId();
                    }

                    GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(resourceGroup.getId(),
                        measDefIdArray, measurementRangeEditor.getStartTime(), measurementRangeEditor.getEndTime(), 60,
                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(),
                                    caught);
                                loadingLabel.setContents(MSG.view_resource_monitor_graphs_loadFailed());
                            }

                            @Override
                            public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> result) {
                                if (result.isEmpty()) {
                                    loadingLabel.setContents(MSG.view_resource_monitor_graphs_noneAvailable());
                                } else {
                                    loadingLabel.hide();
                                    int i = 0;
                                    for (List<MeasurementDataNumericHighLowComposite> data : result) {
                                        buildIndividualGraph(measurementDefinitions.get(i++), data);
                                    }
                                    availabilityGraph.setGroupAvailabilityList(groupAvailabilityList);
                                    availabilityGraph.drawJsniChart();
                                }
                            }
                        });

                }
            });
    }

    protected void queryAvailability(final EntityContext groupContext, Long startTime, Long endTime,
        final CountDownLatch countDownLatch) {

        final long timerStart = System.currentTimeMillis();

        // now return the availability
        GWTServiceLookup.getAvailabilityService().getAvailabilitiesForResourceGroup(groupContext.getGroupId(),
            startTime, endTime, new AsyncCallback<List<ResourceGroupAvailability>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onSuccess(List<ResourceGroupAvailability> groupAvailList) {
                    Log.debug("\nSuccessfully queried group availability in: "
                        + (System.currentTimeMillis() - timerStart) + " ms.");
                    groupAvailabilityList = groupAvailList;
                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }
            });

    }

    private void buildIndividualGraph(MeasurementDefinition measurementDefinition,
        List<MeasurementDataNumericHighLowComposite> data) {

        MetricGraphData metricGraphData = MetricGraphData.createForResourceGroup(resourceGroup.getId(),
                resourceGroup.getName(), measurementDefinition, data, getWidth());
        MetricStackedBarGraph graph = new MetricStackedBarGraph(metricGraphData);
        ResourceMetricD3Graph graphView = new ResourceMetricD3Graph(graph);

        graphView.setWidth("95%");
        graphView.setHeight(225);

        if(graphsVLayout != null){
            graphsVLayout.addMember(graphView);
        }
    }
}
