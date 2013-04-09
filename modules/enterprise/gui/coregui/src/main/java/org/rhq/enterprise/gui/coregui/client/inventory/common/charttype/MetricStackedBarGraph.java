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
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

/**
 * Contains the javascript chart definition for a d3 Stacked Bar graph chart.
 *
 * @author Mike Thompson
 */
public final class MetricStackedBarGraph extends AbstractGraph {

    /**
     * General constructor for stacked bar graph when you have all the data needed to produce the graph. (This is true
     * for all cases but the dashboard portlet).
     */
    public MetricStackedBarGraph(MetricGraphData metricGraphData) {
        setMetricGraphData(metricGraphData);
    }


    /**
     * The magic JSNI to draw the charts with $wnd.d3.js
     */
    @Override
    public native void drawJsniChart() /*-{

        console.log("Draw Stacked Bar jsni chart");
        var global = this,

        // create a chartContext object (from rhq.js) with the data required to render to a chart
        // this same data could be passed to different chart types
        // This way, we are decoupled from the dependency on globals and JSNI and kept all the java interaction right here.
                chartContext = new $wnd.ChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartId()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHeight()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getJsonMetrics()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getXAxisTitle()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTitle()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getYAxisUnits()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTitleMinLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTitleAvgLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTitlePeakLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartDateLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTimeLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartDownLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartUnknownLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartNoDataLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverStartLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverEndLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverPeriodLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverBarLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverTimeFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverDateFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::isPortalGraph()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getPortalId()()
                );


        // Define the Stacked Bar Graph function using the module pattern
        var metricStackedBarGraph = function () {
            "use strict";
            // privates
            var margin = {top: 10, right: 5, bottom: 5, left: 40},
                    width = 750 - margin.left - margin.right,
                    adjustedChartHeight = chartContext.chartHeight - 50,
                    height = adjustedChartHeight - margin.top - margin.bottom,
                    smallChartThresholdInPixels = 600,
                    titleHeight = 30, titleSpace = 10,
                    barOffset = 2,
                    chartData,
                    interpolation = "basis",
                    avg, min, peak,
                    oobMax,
                    determineLowBound,
                    lowBound,
                    highBound,
                    calcBarWidth,
                    yScale,
                    yAxis,
                    timeScale,
                    xAxis,
                    chart,
                    svg;

            function getChartWidth() {
                return $wnd.jQuery("#" + chartContext.chartHandle).width();
            }

            function useSmallCharts() {
                return  getChartWidth() <= smallChartThresholdInPixels;
            }

            function determineScale() {
                var xTicks, xTickSubDivide, numberOfBarsForSmallGraph = 20;
                if (chartContext.data.length > 0) {

                    // if window is too small server up small chart
                    if (useSmallCharts()) {
                        console.log("Using Small Charts Profile for width: "+getChartWidth());
                        width = 250;
                        xTicks = 3;
                        xTickSubDivide = 2;
                        chartData = chartContext.data.slice(chartContext.data.length - numberOfBarsForSmallGraph, chartContext.data.length - 1);
                    }
                    else {
                        console.log("Using Large Charts Profile");
                        //  we use the width already defined above
                        xTicks = 12;
                        xTickSubDivide = 5;
                        chartData = chartContext.data;
                    }

                    avg = $wnd.d3.mean(chartContext.data.map(function (d) {
                        return d.y;
                    }));
                    peak = $wnd.d3.max(chartContext.data.map(function (d) {
                        if (d.high != undefined) {
                            return d.high;
                        }
                        else {
                            return 0;
                        }
                    }));
                    min = $wnd.d3.min(chartContext.data.map(function (d) {
                        if (d.low != undefined) {
                            return d.low;
                        }
                        else {
                            return Number.MAX_VALUE;
                        }
                    }));
                    // adjust the min scale so blue low line is not in axis
                    determineLowBound = function (min, peak) {
                        var newLow = min - ((peak - min) * 0.1);
                        if (newLow < 0) {
                            return 0;
                        }
                        else {
                            return newLow;
                        }
                    };
                    lowBound = determineLowBound(min, peak);
                    highBound = peak + ((peak - min) * 0.1);
                    oobMax = $wnd.d3.max(chartContext.data.map(function (d) {
                        if (d.baselineMax == undefined) {
                            return 0;
                        }
                        else {
                            return +d.baselineMax;
                        }
                    }));
                    calcBarWidth = function () {
                        return (width / chartData.length - barOffset  )
                    };

                    yScale = $wnd.d3.scale.linear()
                            .clamp(true)
                            .rangeRound([height, 0])
                            .domain([lowBound, highBound]);
                    yAxis = $wnd.d3.svg.axis()
                            .scale(yScale)
                            .tickSubdivide(1)
                            .ticks(5)
                            .tickSize(4, 4, 0)
                            .orient("left");


                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(chartData, function (d) {
                                return d.x;
                            }));

                    xAxis = $wnd.d3.svg.axis()
                            .scale(timeScale)
                            .ticks(xTicks)
                            .tickSubdivide(xTickSubDivide)
                            .tickSize(4, 4, 0)
                            .orient("bottom");

                    // create the actual chart group
                    chart = $wnd.d3.select("#" + chartContext.chartSelection);

                    svg = chart.append("g")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top - titleHeight - titleSpace + margin.bottom)
                            .attr("transform", "translate(" + margin.left + "," + (+titleHeight + titleSpace + margin.top) + ")");

                    if (!useSmallCharts()) {
                        createMinAvgPeakSidePanel(chartContext.minChartTitle, min, chartContext.avgChartTitle, avg, chartContext.peakChartTitle, peak, chartContext.yAxisUnits);
                    }
                }

            }


            function createMinAvgPeakSidePanel(minLabel, minValue, avgLabel, avgValue, highLabel, highValue, uom) {
                var xLabel = 772,
                        xValue = 820,
                        yBase = 100,
                        yInc = 25,
                        decimalPlaces = 0;

                // title/header
                chart.append("g").append("rect")
                        .attr("class", "rightSidePanel")
                        .attr("x", xLabel - 10)
                        .attr("y", margin.top + 60)
                        .attr("height", 100)
                        .attr("width", 135)
                        .attr("opacity", "0.3")
                        .attr("fill", "#E8E8E8");

                // high
                chart.append("text")
                        .attr("class", "highLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase)
                        .text(highLabel + " - ");

                chart.append("text")
                        .attr("class", "highText")
                        .attr("x", xValue)
                        .attr("y", yBase)
                        .text(highValue.toFixed(decimalPlaces) + " " + uom);


                //avg
                chart.append("text")
                        .attr("class", "avgLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase + yInc)
                        .text(avgLabel + " - ");

                chart.append("text")
                        .attr("class", "avgText")
                        .attr("x", xValue)
                        .attr("y", yBase + yInc)
                        .text(avgValue.toFixed(decimalPlaces) + " " + uom);

                // min
                chart.append("text")
                        .attr("class", "minLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase + 2 * yInc)
                        .text(minLabel + " - ");

                chart.append("text")
                        .attr("class", "minText")
                        .attr("x", xValue)
                        .attr("y", yBase + 2 * yInc)
                        .text(minValue.toFixed(decimalPlaces) + " " + uom);


            }

            function createHeader(titleName) {
                var title = chart.append("g").append("rect")
                        .attr("class", "title")
                        .attr("x", 10)
                        .attr("y", margin.top)
                        .attr("height", titleHeight)
                        .attr("width", width + 30 + margin.left)
                        .attr("fill", "none");

                chart.append("text")
                        .attr("class", "titleName")
                        .attr("x", 40)
                        .attr("y", 37)
                        .attr("font-size", "12")
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(titleName)
                        .attr("fill", "#003168");

                return title;

            }

            function createStackedBars() {

                var pixelsOffHeight = 0;

                // The gray bars at the bottom leading up
                svg.selectAll("rect.leaderBar")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "leaderBar")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return yScale(highBound);
                            }
                            else {
                                return yScale(d.low);
                            }
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(highBound) - pixelsOffHeight;
                            }
                            else {
                                return height - yScale(d.low) - pixelsOffHeight;
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })

                        .attr("opacity", ".9")
                        .attr("fill", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return  "url(#noDataStripes)";
                            }
                            else {
                                return  "#d3d3d6";
                            }
                        });


                // upper portion representing avg to high
                svg.selectAll("rect.high")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "high")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.high) ? yScale(lowBound) : yScale(d.high);
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                return  yScale(d.y) - yScale(d.high);
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })
                        .attr("data-rhq-value", function (d) {
                            return d.y;
                        })
                        .attr("opacity", 0.9);


                // lower portion representing avg to low
                svg.selectAll("rect.low")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "low")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.y) ? height : yScale(d.y);
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                return  yScale(d.low) - yScale(d.y);
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })
                        .attr("opacity", 0.9);

                // if high == low put a "cap" on the bar to show non-aggregated bar
                svg.selectAll("rect.singleValue")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "singleValue")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.y) ? height : yScale(d.y) - 2;
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                if (d.low === d.high) {
                                    return  yScale(d.low) - yScale(d.y) + 2;
                                }
                                else {
                                    return  yScale(d.low) - yScale(d.y);
                                }
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", function (d) {
                            if (d.low === d.high) {
                                return  "#50505a";
                            }
                            else {
                                return  "#70c4e2";
                            }
                        });
            }

            function createYAxisGridLines() {
                // create the y axis grid lines
                svg.append("g").classed("grid y_grid", true)
                        .call($wnd.d3.svg.axis()
                                .scale(yScale)
                                .orient("left")
                                .ticks(10)
                                .tickSize(-width, 0, 0)
                                .tickFormat("")
                        );
            }

            function createXandYAxes() {

                // create x-axis
                svg.append("g")
                        .attr("class", "x axis")
                        .attr("transform", "translate(0," + height + ")")
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .call(xAxis);


                // create y-axis
                svg.append("g")
                        .attr("class", "y axis")
                        .call(yAxis)
                        .append("text")
                        .attr("transform", "rotate(-90),translate( -60,0)")
                        .attr("y", -30)
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .text(chartContext.yAxisUnits === "NONE" ? "" : chartContext.yAxisUnits);

            }

            function createAvgLines() {
                var showBarAvgTrendline =
                                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::showBarAvgTrendLine()(),
                        barAvgLine = $wnd.d3.svg.line()
                                .interpolate("linear")
                                .defined(function (d) {
                                    return !d.nodata;
                                })
                                .x(function (d) {
                                    return timeScale(d.x) + (calcBarWidth() / 2);
                                })
                                .y(function (d) {
                                    if (showBarAvgTrendline) {
                                        return yScale(d.y);
                                    }
                                    else {
                                        return yScale(0);
                                    }
                                });

                // Bar avg line
                svg.append("path")
                        .datum(chartData)
                        .attr("class", "barAvgLine")
                        .attr("fill", "none")
                        .attr("stroke", "#2e376a")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-opacity", ".7")
                        .attr("d", barAvgLine);

            }

            function createOOBLines() {

                var minBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMin);
                                }),
                        maxBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMax);
                                });

                // min baseline Line
                svg.append("path")
                        .datum(chartData)
                        .attr("class", "minBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "purple")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".9")
                        .attr("d", minBaselineLine);

                // max baseline Line
                svg.append("path")
                        .datum(chartData)
                        .attr("class", "maxBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "orange")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".7")
                        .attr("d", maxBaselineLine);

            }

            function formatHovers(chartContext, d) {
                var hoverString,
                        xValue = (d.x == undefined) ? 0 : +d.x,
                        date = new Date(+xValue),
                        barDuration = d.barDuration,
                        timeFormatter = $wnd.d3.time.format(chartContext.chartHoverTimeFormat),
                        dateFormatter = $wnd.d3.time.format(chartContext.chartHoverDateFormat),
                        highValue = d.high.toFixed(2),
                        lowValue = d.low.toFixed(2),
                        avgValue = d.y.toFixed(2);


                // our special condition to indicate no data because avg lines dont like discontinuous data
                if (d.y === 0 && d.high === 0 && d.low === 0) {
                    // no data
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel" >' + chartContext.timeLabel + ': </span>' + timeFormatter(date) +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ': </span>' + dateFormatter(date) + '</div>' +
                                    '<hr class="chartHoverDivider" ></hr>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">' + chartContext.noDataLabel + '</span></div>' +
                                    '</div>';


                }
                else {
                    // regular bar hover
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel">' + chartContext.timeLabel + ':  </span><span style="width:50px;">' + timeFormatter(date) + '</span>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ':  </span><span style="width:50px;">' + dateFormatter(date) + '</span></div>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverLabelSpan">' + chartContext.hoverBarLabel + ": " + barDuration + '</span></div>' +
                                    '<hr  class="chartHoverDivider"></hr>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverPeakValue" >' + chartContext.peakChartTitle + ': </span><span style="width:50px;">' + highValue + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverAvgValue" >' + chartContext.avgChartTitle + ':  </span><span style="width:50px;">' + avgValue + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverLowValue" >' + chartContext.minChartTitle + ': </span><span style="width:50px;">' + lowValue + '</span></div>' +
                                    '</div>';
                }
                return hoverString;

            }

            function createHovers(chartContext) {
                $wnd.jQuery('svg rect.leaderBar, svg rect.high, svg rect.low, svg rect.singleValue').tipsy({
                    gravity: 'w',
                    html: true,
                    trigger: 'hover',
                    title: function () {
                        var d = this.__data__;
                        return formatHovers(chartContext, d);
                    },
                    show: function (e, el) {
                        el.css({ 'z-index': '990000'})
                    }
                });
            }

            return {
                // Public API
                draw: function (chartContext) {
                    "use strict";
                    // Guard condition that can occur when a portlet has not been configured yet
                    if (chartContext.data.length > 0) {
                        console.info("Creating Chart: "+ chartContext.chartSelection + " --> "+ chartContext.chartTitle);
                        //console.time("chart");

                        determineScale();
                        createHeader(chartContext.chartTitle);

                        createYAxisGridLines();
                        createStackedBars();
                        createXandYAxes();
                        createAvgLines();
                        if (oobMax > 0) {
                            console.info("OOB Data Exists!");
                            createOOBLines();
                        }
                        createHovers(chartContext);
                        //console.timeEnd("chart");
                    }
                }
            }; // end public closure
        }();

        metricStackedBarGraph.draw(chartContext);

    }-*/;

}
