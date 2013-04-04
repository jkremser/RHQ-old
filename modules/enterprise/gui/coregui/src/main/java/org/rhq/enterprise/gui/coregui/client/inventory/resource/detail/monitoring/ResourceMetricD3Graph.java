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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.AutoRefresh;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.HasD3MetricJsniChart;

/**
 * A D3 graph implementation for graphing Resource metrics.
 */
public class ResourceMetricD3Graph extends AbstractMetricD3GraphView  {

    /**
     * This constructor is for the use case in the Dashboard where we dont actually
     * have a entity or measurement yet.
     */
    public ResourceMetricD3Graph() {
        super();
    }

    public ResourceMetricD3Graph(AbstractGraph graph) {

        super(graph);
        //startRefreshCycle();
    }



    @Override
    /**
     * Delegate the call to rendering the JSNI chart.
     * This way the chart type can be swapped out at any time.
     */
    public void drawJsniChart() {
        graph.drawJsniChart();
    }

    public HasD3MetricJsniChart getJsniChart() {
        return graph;
    }


//    @Override
//    public void startRefreshCycle() {
//        refreshTimer = AutoRefreshUtil.startRefreshCycle(this, this, refreshTimer);
//    }
//
//    @Override
//    protected void onDestroy() {
//        AutoRefreshUtil.onDestroy( refreshTimer);
//
//        super.onDestroy();
//    }
//
//    @Override
//    public boolean isRefreshing() {
//        return false;
//    }
//
//    //Custom refresh operation as we are not directly extending Table
//    @Override
//    public void refresh() {
//        if (isVisible() && !isRefreshing()) {
//            drawGraph();
//        }
//    }

}
