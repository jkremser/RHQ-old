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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;
import org.rhq.core.server.MeasurementParser;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.SavedChartsPortletPreferences;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * View a chart for a metric.
 */
public class ViewChartAction extends MetricDisplayRangeAction {
    private final Log log = LogFactory.getLog(ViewChartAction.class.getName());

    MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
    MeasurementBaselineManagerLocal baselineManager = LookupUtil.getMeasurementBaselineManager();
    ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    /**
     * Modify the metric chart as specified in the given <code>{@link
     * ViewActionForm}</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        ViewChartForm chartForm = (ViewChartForm) form;
        Subject subject = SessionUtils.getWebUser(request.getSession()).getSubject();

        Integer[] resourceIds = chartForm.getResourceIds();

        Map<String, Object> forwardParams = new HashMap<String, Object>(3);
        // The autogroup metrics pages pass the ctype to us, and we
        // need to pass it back. If this happens, we don't need the
        // extra "mode" parameter. See bug #7501. (2003/06/24 -- JW)
        if (null != chartForm.getCtype() && chartForm.getCtype() != -1) {
            forwardParams.put(ParamConstants.CHILD_RESOURCE_TYPE_ID_PARAM, chartForm.getCtype());
        } else {
            forwardParams.put(ParamConstants.MODE_PARAM, chartForm.getMode());
        }

        if (chartForm.getSaveChart()) {
            ActionForward success = returnRedraw(request, mapping, forwardParams);

            // build the chart URL
            Map chartParams = new HashMap();
            chartParams.put("m", chartForm.getM());
            chartParams.put("showPeak", chartForm.getShowPeak());
            chartParams.put("showHighRange", chartForm.getShowHighRange());
            chartParams.put("showValues", chartForm.getShowValues());
            chartParams.put("showAverage", chartForm.getShowAverage());
            chartParams.put("showLowRange", chartForm.getShowLowRange());
            chartParams.put("showLow", chartForm.getShowLow());
            chartParams.put("showBaseline", chartForm.getShowBaseline());
            chartParams.put("threshold", chartForm.getThreshold());

            if (chartForm.getGroupId() > 0) { // comp group
                chartParams.put("groupId", chartForm.getGroupId());
                chartParams.put("mode", chartForm.getMode());
            } else if (chartForm.getParent() > 0 && chartForm.getCtype() > 0) { // autogroup
                chartParams.put("parent", chartForm.getParent());
                chartParams.put("type", chartForm.getCtype());
                chartParams.put("mode", chartForm.getMode());
            } else { // single resource 
                // TODO for a list of mostly independent resources that don't come from one of the above groups
                if (chartForm.getId() != null)
                    chartParams.put("id", chartForm.getId());
            }

            String url = ActionUtils.changeUrl(success.getPath(), chartParams);
            boolean changed = _saveUserChart(url, chartForm.getChartName(), request);

            if (log.isDebugEnabled()) {
                log.debug("Saving chart to dashboard ...\n\tchartName=" + chartForm.getChartName() + "\n\turl=" + url);
            }

            if (changed) {
                RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.confirm.ChartSaved");
            } else {
                RequestUtils.setConfirmation(request,
                    "resource.common.monitor.visibility.chart.confirm.ChartAlreadySaved");
            }

            return success;
        } else if (chartForm.isChangeBaselineClicked()) {
            request.setAttribute("editBaseline", Boolean.TRUE);
            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isSaveBaselineClicked()) {

            Boolean baselineWasNull = null;
            if (chartForm.getMode().equals(ParamConstants.MODE_MON_CHART_SMSR)) {
                // get the derived measurement in question
                MeasurementSchedule schedule = scheduleManager.getSchedule(subject, chartForm.getId(),
                    chartForm.getM()[0], true);
                baselineWasNull = (schedule.getBaseline() == null);

                baselineManager.calculateAutoBaseline(subject, schedule.getId(), chartForm.getStartDate().getTime(),
                    chartForm.getEndDate().getTime(), true /* save */);
            } else if (chartForm.getMode().equals(ParamConstants.MODE_MON_CHART_SMMR)) {
                MeasurementBaseline baselineIfEqual = baselineManager.getBaselineIfEqual(subject, chartForm
                    .getGroupId(), chartForm.getM()[0]);

                baselineWasNull = (baselineIfEqual == null);

                baselineManager.calculateAutoBaseline(subject, chartForm.getGroupId(), chartForm.getM()[0], chartForm
                    .getStartDate().getTime(), chartForm.getEndDate().getTime(), true /* save */);
            }
            request.setAttribute("editBaseline", Boolean.FALSE);
            request.setAttribute("justSavedBaseline", Boolean.TRUE);
            request.setAttribute("baselineWasNull", Boolean.valueOf(baselineWasNull));

            RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.confirm.BaselineSet");

            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isCancelBaselineClicked()) {
            request.setAttribute("editBaseline", Boolean.FALSE);
            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isChangeHighRangeClicked()) {
            request.setAttribute("editHighRange", Boolean.TRUE);
            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isSaveHighRangeClicked()) {
            // get the derived measurement in question
            if (chartForm.getMode().equals(ParamConstants.MODE_MON_CHART_SMSR)) {
                int definitionId = chartForm.getM()[0];
                int resourceId = chartForm.getId();
                boolean success = setBaselineMax(subject, request, resourceId, definitionId, chartForm.getHighRange());
                if (!success) {
                    return returnFailure(request, mapping, forwardParams);
                }
            } else if (chartForm.getMode().equals(ParamConstants.MODE_MON_CHART_SMMR)) {
                int groupId = chartForm.getGroupId();
                int definitionId = chartForm.getM()[0];
                List<Integer> groupMemberIds = resourceManager.findImplicitResourceIdsByResourceGroup(groupId);
                for (int resourceMemberId : groupMemberIds) {
                    boolean success = setBaselineMax(subject, request, resourceMemberId, definitionId, chartForm
                        .getHighRange());
                    if (!success) {
                        return returnFailure(request, mapping, forwardParams);
                    }
                }
            }
            request.setAttribute("editHighRange", Boolean.FALSE);
            request.setAttribute("justSavedHighRange", Boolean.TRUE);

            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isCancelHighRangeClicked()) {
            request.setAttribute("editHighRange", Boolean.FALSE);
            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isChangeLowRangeClicked()) {
            request.setAttribute("editLowRange", Boolean.TRUE);
            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isSaveLowRangeClicked()) {
            // get the derived measurement in question
            if (chartForm.getMode().equals(ParamConstants.MODE_MON_CHART_SMSR)) {
                int definitionId = chartForm.getM()[0];
                int resourceId = chartForm.getId();
                boolean success = setBaselineMin(subject, request, resourceId, definitionId, chartForm.getLowRange());
                if (!success) {
                    return returnFailure(request, mapping, forwardParams);
                }
            } else if (chartForm.getMode().equals(ParamConstants.MODE_MON_CHART_SMMR)) {
                int groupId = chartForm.getGroupId();
                int definitionId = chartForm.getM()[0];
                List<Integer> groupMemberIds = resourceManager.findImplicitResourceIdsByResourceGroup(groupId);
                for (int resourceMemberId : groupMemberIds) {
                    boolean success = setBaselineMax(subject, request, resourceMemberId, definitionId, chartForm
                        .getLowRange());
                    if (!success) {
                        return returnFailure(request, mapping, forwardParams);
                    }
                }
            }

            request.setAttribute("editLowRange", Boolean.FALSE);
            request.setAttribute("justSavedLowRange", Boolean.TRUE);

            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isCancelLowRangeClicked()) {
            request.setAttribute("editLowRange", Boolean.FALSE);
            return returnRedraw(request, mapping, forwardParams);
        } else if (chartForm.isPrevPageClicked()) {
            return returnSuccess(request, mapping, forwardParams);
        } else {
            // If prev or next buttons were clicked, the dates
            // caused by those clicks will override what's
            // actually in the form, so we must update the form as
            // appropriate.
            if (chartForm.isNextRangeClicked() || chartForm.isPrevRangeClicked()) {
                MetricRange range = new MetricRange();
                if (chartForm.isNextRangeClicked()) {
                    long newBegin = chartForm.getEndDate().getTime();
                    long diff = newBegin - chartForm.getStartDate().getTime();
                    long newEnd = newBegin + diff;

                    range.setBegin(newBegin);
                    range.setEnd(newEnd);
                } else if (chartForm.isPrevRangeClicked()) {
                    long newEnd = chartForm.getStartDate().getTime();
                    long diff = chartForm.getEndDate().getTime() - newEnd;
                    long newBegin = newEnd - diff;

                    range.setBegin(newBegin);
                    range.setEnd(newEnd);
                }
                chartForm.setA(MetricDisplayRangeForm.ACTION_DATE_RANGE);
                chartForm.populateStartDate(new Date(range.getBegin()), request.getLocale());
                chartForm.populateEndDate(new Date(range.getEnd()), request.getLocale());
                range.shiftNow();
                request.setAttribute(ParamConstants.METRIC_RANGE, range);
            }

            // Update metric display range.
            ActionForward retVal = super.execute(mapping, form, request, response);
            if (retVal.getName().equals(RetCodeConstants.SUCCESS_URL)) {
                return returnRedraw(request, mapping, forwardParams);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("returning " + retVal.getName());
                }
                return retVal;
            }
        }

    }

    private boolean setBaselineMax(Subject subject, HttpServletRequest request, int resourceId, int definitionId,
        String highRangeStr) throws Exception {
        MeasurementSchedule schedule = scheduleManager.getSchedule(subject, resourceId, definitionId, true);

        // validate here rather than in ViewChartForm.validate() because we don't want to parse the number twice
        if (highRangeStr.length() > 0) {
            try {
                double highRange = MeasurementParser.parse(highRangeStr, schedule).getValue();

                MeasurementBaseline baseline = schedule.getBaseline();

                if (baseline != null) {
                    if (null != baseline.getMin()) {
                        if (highRange <= baseline.getMin()) {
                            RequestUtils.setError(request, "resource.common.monitor.visibility.error.HighGreaterLow",
                                "highRange");
                            request.setAttribute("editHighRange", Boolean.TRUE);
                            return false;
                        }
                    }
                } else {
                    baseline = new MeasurementBaseline();
                    baseline.setSchedule(schedule); // add relationship both ways
                }

                baseline.setMax(highRange);
                baseline.setUserEntered(true);
                RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.confirm.HighRangeSet");
            } catch (MeasurementConversionException mce) {
                RequestUtils.setError(request, "resource.common.monitor.visibility.error.RangeParseException",
                    "highRange");
                request.setAttribute("editHighRange", Boolean.TRUE);
                return false;
            }
        } else {
            RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.confirm.HighRangeCleared");
        }

        scheduleManager.updateSchedule(subject, schedule);

        return true;
    }

    private boolean setBaselineMin(Subject subject, HttpServletRequest request, int resourceId, int definitionId,
        String lowRangeStr) throws Exception {
        MeasurementSchedule schedule = scheduleManager.getSchedule(subject, resourceId, definitionId, true);

        // validate here rather than in ViewChartForm.validate() because we don't want to parse the number twice
        if (lowRangeStr.length() > 0) {
            try {
                double lowRange = MeasurementParser.parse(lowRangeStr, schedule).getValue();

                MeasurementBaseline baseline = schedule.getBaseline();

                if (baseline != null) {
                    if (null != baseline.getMax()) {
                        if (lowRange >= baseline.getMax()) {
                            RequestUtils.setError(request, "resource.common.monitor.visibility.error.HighGreaterLow",
                                "lowRange");
                            request.setAttribute("editLowRange", Boolean.TRUE);
                            return false;
                        }
                    }
                } else {
                    baseline = new MeasurementBaseline();
                    baseline.setSchedule(schedule); // add relationship both ways
                }

                baseline.setMin(lowRange);
                baseline.setUserEntered(true);
                RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.confirm.LowRangeSet");
            } catch (MeasurementConversionException mce) {
                RequestUtils.setError(request, "resource.common.monitor.visibility.error.RangeParseException",
                    "lowRange");
                request.setAttribute("editLowRange", Boolean.TRUE);
                return false;
            }
        } else {
            RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.chart.confirm.LowRangeCleared");
        }

        scheduleManager.updateSchedule(subject, schedule);

        return true;
    }

    public ActionForward returnRedraw(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return constructForward(request, mapping, RetCodeConstants.REDRAW_URL, params, false);
    }

    // --------------------------------------------------------------------------------
    // -- private helpers
    // --------------------------------------------------------------------------------
    // forHTMLTag is copy-n-pasted from: http://www.javapractices.com/Topic96.cjp
    // used to be in our util.StringUtil, we should really use jakarta's
    // StringEscapeUtils.escapeHTML()
    /**
     * Replace characters having special meaning <em>inside</em> HTML tags with
     * their escaped equivalents, using character entities such as
     * <tt>'&amp;'</tt>.
     * <P>
     * The escaped characters are :
     * <ul>
     * <li><
     * <li>>
     * <li>"
     * <li>'
     * <li>\
     * <li>&
     * </ul>
     * <P>
     * This method ensures that arbitrary text appearing inside a tag does not
     * "confuse" the tag. For example, <tt>HREF='Blah.do?Page=1&Sort=ASC'</tt>
     * does not comply with strict HTML because of the ampersand, and should be
     * changed to <tt>HREF='Blah.do?Page=1&amp;Sort=ASC'</tt>. This is
     * commonly seen in building query strings. (In JSTL, the c:url tag performs
     * this task automatically.)
     * 
     * @param aTagFragment
     *           some HTML to be escaped
     * @return escaped HTML
     */
    private static String forHTMLTag(String aTagFragment) {
        final StringBuffer result = new StringBuffer();

        final StringCharacterIterator iterator = new StringCharacterIterator(aTagFragment);

        for (char character = iterator.current(); character != CharacterIterator.DONE; character = iterator.next()) {
            switch (character) {
            case '<':
                result.append("&lt;");
                break;
            case '>':
                result.append("&gt;");
                break;
            case '\"':
                result.append("&quot;");
                break;
            case '\'':
                result.append("&#039;");
                break;
            case '\\':
                result.append("&#092;");
                break;
            case '&':
                result.append("&amp;");
                break;
            case '|':
                result.append("&#124;");
                break;
            case ',':
                result.append("&#44;");
                break;
            default:
                // the char is not a special one add it to the result as is
                result.append(character);
                break;
            }
        }
        return result.toString();
    }

    private boolean _saveUserChart(String url, String name, HttpServletRequest request) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getWebPreferences();
        SavedChartsPortletPreferences savedCharts = preferences.getSavedChartsPortletPreferences();

        boolean changed = savedCharts.add(name, url);
        if (changed) {
            preferences.setSavedChartsPortletPreferences(savedCharts);
        }
        return changed;
    }

}
