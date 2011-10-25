/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;

import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementChartsGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class MeasurementChartsGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementChartsGWTService {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_VIEW_NAME = "Default";

    private MeasurementChartsManagerLocal chartsManager = LookupUtil.getMeasurementChartsManager();

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(int parent, int type, String viewName)
        throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForAutoGroup(getSessionSubject(), parent, type, viewName));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForAutoGroup1");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(int parent, int type, int[] schedIds,
        long begin, long end, boolean enabledOnly) throws RuntimeException {
        try {
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForAutoGroup(getSessionSubject(), parent, type, schedIds, begin, end,
                    enabledOnly));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForAutoGroup2");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(int groupId, String viewName)
        throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForCompatibleGroup(getSessionSubject(), groupId, viewName));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForCompatibleGroup1");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(int groupId, int[] defIds,
        long begin, long end, boolean enabledOnly) throws RuntimeException {
        try {
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForCompatibleGroup(getSessionSubject(), groupId, defIds, begin, end,
                    enabledOnly));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForCompatibleGroup2");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForResource(int resourceId, String viewName)
        throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForResource(getSessionSubject(), resourceId, viewName));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForResource1");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForResource(int resourceId, int[] schedIds,
        long begin, long end) throws RuntimeException {
        try {
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForResource(getSessionSubject(), resourceId, schedIds, begin, end));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForResource2");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}
