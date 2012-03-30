/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.FavoriteResourcePortletPreferences;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A <code>BaseAction</code> that handles metrics display form submissions.
 */
public class MetricsDisplayAction extends MetricsControlAction {
    protected static final Log log = LogFactory.getLog(MetricsDisplayAction.class);

    // ---------------------------------------------------- Public
    // ---------------------------------------------------- Methods

    /**
     * Modify the metrics summary display as specified in the given <code>MetricsDisplayForm</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MetricsDisplayForm displayForm = (MetricsDisplayForm) form;

        Map forwardParams = displayForm.getForwardParams();

        Integer id = displayForm.getId();
        WebUser user = SessionUtils.getWebUser(request.getSession());
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        Subject subject = RequestUtils.getSubject(request);

        MeasurementBaselineManagerLocal baselineManager = LookupUtil.getMeasurementBaselineManager();

        if (displayForm.isCompareClicked()) {
            return returnCompare(request, mapping, forwardParams);
        } else if (displayForm.isChartClicked()) {
            forwardParams.put(ParamConstants.METRIC_ID_PARAM, displayForm.getM());
            return returnChart(request, mapping, forwardParams);
        } else if (displayForm.isThresholdClicked()) {
            Integer threshold = displayForm.getT();
            preferences.setMetricThresholdPreference(threshold);
            log.trace("saving threshold pref [" + threshold + "]");
            LogFactory.getLog("user.preferences").trace(
                "Invoking setUserPrefs" + " in MetricsDisplayAction " + " for " + user.getId() + " at "
                    + System.currentTimeMillis() + " user.prefs = " + user.getWebPreferences());
        } else if (displayForm.isUsersetClicked()) {
            Integer[] m = displayForm.getM();
            if ((m != null) && (m.length > 0)) {
                MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

                Long begin = rangePreferences.begin;
                Long end = rangePreferences.end;

                Integer[] resourceIds = displayForm.getR();
                Resource[] resources = null; // TODO hwr: fix this

                // TODO what do we really want here?
                // this looks like users want to see old baselines at display time
                //                    bb.saveBaselines(sessionId.intValue(), resources, m,
                //                                     begin.longValue(), end.longValue());
                log.trace("Set baselines in MetricsDisplayAction " + " for " + id + ": " + StringUtil.arrayToString(m));
            }

            RequestUtils.setConfirmation(request, Constants.CNF_METRICS_BASELINE_SET);
        } else if (displayForm.isEnableClicked()) {
            Integer[] m = displayForm.getM();
            if ((m != null) && (m.length > 0)) {
                Integer[] resources = displayForm.getR();
                baselineManager.enableAutoBaselineCalculation(subject, resources, m);
                log.trace("Enable auto baselines in MetricsDisplayAction " + " for " + id + ": "
                    + StringUtil.arrayToString(m));
            }

            RequestUtils.setConfirmation(request, Constants.CNF_AUTO_BASELINE_SET);
        } else if (displayForm.isAddClicked()) {
            Integer[] m = displayForm.getM();
            if ((m != null) && (m.length > 0)) {
                //addFavoriteMetrics(m, user, id.getTypeName());
                LogFactory.getLog("user.preferences").trace(
                    "Invoking setUserPrefs" + " in MetricsDisplayAction " + " for " + user.getId() + " at "
                        + System.currentTimeMillis() + " user.prefs = " + user.getWebPreferences());
            }

            RequestUtils.setConfirmation(request, Constants.CNF_FAVORITE_METRICS_ADDED);
        } else if (displayForm.isRemoveClicked()) {
            Integer[] m = displayForm.getM();
            if ((m != null) && (m.length > 0)) {
                //removeFavoriteMetrics(m, user, entityId.getTypeName());
                LogFactory.getLog("user.preferences").trace(
                    "Invoking setUserPrefs" + " in MetricsDisplayAction " + " for " + user.getId() + " at "
                        + System.currentTimeMillis() + " user.prefs = " + user.getWebPreferences());
            }

            RequestUtils.setConfirmation(request, Constants.CNF_FAVORITE_METRICS_REMOVED);
        }

        return super.execute(mapping, form, request, response);
    }

    // ---------------------------------------------------- Private Methods

    private ActionForward returnCompare(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        // set return path
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        SessionUtils.setReturnPath(request.getSession(), returnPath);

        return constructForward(request, mapping, Constants.COMPARE_URL, params, NO_RETURN_PATH);
    }

    private ActionForward returnChart(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        // set return path
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        SessionUtils.setReturnPath(request.getSession(), returnPath);

        return constructForward(request, mapping, Constants.CHART_URL, params, NO_RETURN_PATH);
    }

    private void addFavoriteMetrics(Integer[] selectedIds, WebUserPreferences preferences, String entityType)
        throws IllegalArgumentException {
        FavoriteResourcePortletPreferences prefs = preferences.getFavoriteResourcePortletPreferences();

        // add selected metrics, discarding any that are already favorites
        for (Integer resourceIdToAdd : selectedIds) {
            if (prefs.isFavorite(resourceIdToAdd)) {
                continue;
            }
            prefs.addFavorite(resourceIdToAdd);
        }

        preferences.setFavoriteResourcePortletPreferences(prefs);
    }

    private void removeFavoriteMetrics(Integer[] selectedIds, WebUserPreferences preferences, String entityType)
        throws IllegalArgumentException {
        FavoriteResourcePortletPreferences prefs = preferences.getFavoriteResourcePortletPreferences();

        // add selected metrics, discarding any that are already favorites
        for (Integer resourceIdToAdd : selectedIds) {
            prefs.removeFavorite(resourceIdToAdd);
        }

        preferences.setFavoriteResourcePortletPreferences(prefs);
    }
}