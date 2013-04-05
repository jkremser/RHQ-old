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

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.HasD3MetricJsniChart;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A D3 graph implementation for graphing Resource metrics.
 */
public class MetricD3Graph extends EnhancedVLayout {

    protected AbstractGraph graph;
    private HTMLFlow graphDiv = null;
    protected Timer refreshTimer;

    /**
     * This constructor is for the use case in the Dashboard where we dont actually
     * have a entity or measurement yet.
     */
    public MetricD3Graph() {
        super();
    }

    public MetricD3Graph(AbstractGraph graph) {
        super();
        this.graph = graph;
        setHeight100();
        setWidth100();
    }

    /**
     * Svg definitions for patterns and gradients to use on SVG shapes.
     * @return xml String
     */
    private static String getSvgDefs() {
        return " <defs>"
            + "               <linearGradient id=\"headerGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">"
            + "                   <stop offset=\"0%\" style=\"stop-color:#E6E6E6;stop-opacity:1\"/>"
            + "                   <stop offset=\"100%\" style=\"stop-color:#F0F0F0;stop-opacity:1\"/>"
            + "               </linearGradient>"
            + "               <pattern id=\"noDataStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\""
            + "                        width=\"6\" height=\"3\">"
            + "                   <path d=\"M 0 0 6 0\" style=\"stroke:#CCCCCC; fill:none;\"/>"
            + "               </pattern>"
            + "               <pattern id=\"unknownStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\""
            + "                        width=\"6\" height=\"3\">"
            + "                   <path d=\"M 0 0 6 0\" style=\"stroke:#2E9EC2; fill:none;\"/>"
            + "               </pattern>"
            + "               <pattern id=\"downStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\""
            + "                        width=\"6\" height=\"3\">"
            + "                   <path d=\"M 0 0 6 0\" style=\"stroke:#ff8a9a; fill:none;\"/>"
            + "               </pattern>" + "</defs>";
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        if (graph.getDefinition() != null) {
            drawGraph();
        }
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        drawGraph();
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniGraph() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     *
     */
    protected void drawGraph() {
        Log.debug("drawGraph marker in MetricD3Graph for: " + getFullChartId() + " " + graph.getChartTitle());

        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs
            .append("<div id=\""
                    + getFullChartId()
                    + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" style=\"height:"
                    + getChartHeight() + "px;\">");
        divAndSvgDefs.append(getSvgDefs());
        divAndSvgDefs.append("</svg></div>");

        if (null != graphDiv) {
            removeMember(graphDiv);
        }

        graphDiv = new HTMLFlow(divAndSvgDefs.toString());
        graphDiv.setWidth100();
        graphDiv.setHeight100();
        addMember(graphDiv);

        new Timer() {
            @Override
            public void run() {
                //@todo: this is a hack around timing issue of jsni not seeing the DOM
                drawJsniChart();
            }
        }.schedule(200);
    }

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

    public String getFullChartId() {
        return "rChart-" + graph.getMetricGraphData().getChartId();

    }

    public void setGraph(AbstractGraph graph) {
        this.graph = graph;
    }


    public Integer getChartHeight() {
        return graph.getMetricGraphData().getChartHeight();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void hide() {
        super.hide();
    }

}
