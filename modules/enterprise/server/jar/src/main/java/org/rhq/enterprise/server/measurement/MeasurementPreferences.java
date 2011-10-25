package org.rhq.enterprise.server.measurement;

import java.util.Arrays;
import java.util.List;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.auth.prefs.SubjectPreferencesBase;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

public class MeasurementPreferences extends SubjectPreferencesBase {

    public static final String PREF_FAV_RESOURCE_METRICS_PREFIX = ".resource.common.monitor.visibility.favoriteMetrics";
    public static final String PREF_METRIC_RANGE = ".resource.common.monitor.visibility.metricRange";
    public static final String PREF_METRIC_RANGE_LASTN = ".resource.common.monitor.visibility.metricRange.lastN";
    public static final String PREF_METRIC_RANGE_UNIT = ".resource.common.monitor.visibility.metricRange.unit";
    public static final String PREF_METRIC_RANGE_RO = ".resource.common.monitor.visibility.metricRange.ro";
    public static final String PREF_METRIC_THRESHOLD = ".resource.common.monitor.visibility.metricThreshold";

    public static final Boolean DEFAULT_VALUE_RANGE_RO = Boolean.FALSE;
    public static final Integer DEFAULT_VALUE_RANGE_LASTN = 8;
    public static final Integer DEFAULT_VALUE_RANGE_UNIT = 3;

    /**
     * key values for indicator views
     */
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX = "monitor.visibility.indicator.views.";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT = "resource.common.monitor.visibility.defaultview";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME = "Default";

    public MeasurementPreferences(Subject subject) {
        super(subject);
    }

    public static class MetricRangePreferences {
        /*
         * if readOnly is set to true, the beginning and ending range
         * dates are specified with explicit dates.  if readOnly is
         * false, then the time is relative to NOW and is specified as
         * <lastN> units of <unit> time
         */
        public boolean readOnly;

        // simple, when readOnly is false
        public int lastN;
        public int unit;

        // advanced, when readOnly is true
        public Long begin;
        public Long end;

        public String toString() {
            if (readOnly) {
                return getClass().getSimpleName() + "[begin=" + begin + end + ",end=" + end + "]";
            } else {
                return getClass().getSimpleName() + "[lastN=" + lastN + ",unit=" + unit + "]";
            }
        }
    }

    public MetricRangePreferences getMetricRangePreferences() {
        MetricRangePreferences prefs = new MetricRangePreferences();

        prefs.readOnly = getPreference(PREF_METRIC_RANGE_RO, DEFAULT_VALUE_RANGE_RO);
        if (prefs.readOnly == false) { // relative time range
            prefs.lastN = getPreference(PREF_METRIC_RANGE_LASTN, DEFAULT_VALUE_RANGE_LASTN);
            prefs.unit = getPreference(PREF_METRIC_RANGE_UNIT, DEFAULT_VALUE_RANGE_UNIT);

            List<Long> range = MeasurementUtils.calculateTimeFrame(prefs.lastN, prefs.unit);
            prefs.begin = range.get(0);
            prefs.end = range.get(1);
        } else { // explicit dates
            try {
                List<String> range = getPreferenceAsList(PREF_METRIC_RANGE);
                if (range.size() != 2) { // our gwt ui now uses |, let's be sure we can support it too
                    range = getPreferenceAsList(PREF_METRIC_RANGE, "|");
                }
                if (range.size() == 2) {
                    prefs.begin = new Long(range.get(0));
                    prefs.end = new Long(range.get(1));
                }
                else { // we still could not find a valid range BZ 743221
                    useDefaultAsFallback(prefs);
                }
            } catch (IllegalArgumentException iae) {
                // that's OK, range will remain null and we might use the lastN / unit
                useDefaultAsFallback(prefs);
            }
        }

        //log.debug("Getting Metric Range Preferences:" + prefs);

        return prefs;
    }

    /**
     * Set the prefs begin/end to a default as last resort.
     * @param prefs
     */
    private void useDefaultAsFallback(MetricRangePreferences prefs) {
        List<Long> range = MeasurementUtils.calculateTimeFrame(DEFAULT_VALUE_RANGE_LASTN,
                DEFAULT_VALUE_RANGE_UNIT);
        prefs.begin = range.get(0);
        prefs.end = range.get(1);
    }

    public void setMetricRangePreferences(MetricRangePreferences prefs) {
        setPreference(PREF_METRIC_RANGE_RO, prefs.readOnly);
        if (prefs.readOnly) {
            // persist advanced mode
            setPreference(PREF_METRIC_RANGE, Arrays.asList(prefs.begin, prefs.end));
            //unsetPreference(PREF_METRIC_RANGE_LASTN);
            //unsetPreference(PREF_METRIC_RANGE_UNIT);
        } else {
            setPreference(PREF_METRIC_RANGE_LASTN, prefs.lastN);
            setPreference(PREF_METRIC_RANGE_UNIT, prefs.unit);
            //unsetPreference(PREF_METRIC_RANGE);
        }

        //log.debug("Setting Metric Range Preferences:" + prefs);
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef type
     */
    public Integer getMetricThresholdPreference() throws IllegalArgumentException {
        return new Integer(getPreference(PREF_METRIC_THRESHOLD));
    }

    /**
     * Set a list of metric ids saved as favorites for a particular appdef type
     */
    public void setMetricThresholdPreference(Integer value) throws IllegalArgumentException {
        setPreference(PREF_METRIC_THRESHOLD, value);
    }

    public static class MetricViewsPreferences {
        public List<String> views;
    }

    public MetricViewsPreferences getMetricViews(String key) {
        MetricViewsPreferences prefs = new MetricViewsPreferences();
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        String value = getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + key,
            PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME);
        prefs.views = StringUtil.explode(value, PREF_ITEM_DELIM);
        return prefs;
    }

    public void setMetricViews(MetricViewsPreferences prefs, String key) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String viewName : prefs.views) {
            if (index != 0) {
                builder.append(PREF_ITEM_DELIM);
            }
            builder.append(viewName);
            index++;
        }
        setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + key, builder.toString());
    }

    public String getSelectedView(String key) {
        String value = getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + "selected." + key,
            PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME);
        return value;
    }

    public void setSelectedView(String key, String viewName) {
        setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + "selected." + key, viewName);
    }

    public static class MetricViewData {
        public List<String> charts;
    }

    public MetricViewData getMetricViewData(String context, String viewName) {
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        if (viewName == null || "".equals(viewName)) {
            viewName = "default";
        }
        MetricViewData chartPreferences = new MetricViewData();
        // important to let IllegalArgumentException bubble out of here, so the caller can persist the default set
        String data = getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName);
        chartPreferences.charts = StringUtil.explode(data, PREF_ITEM_DELIM);
        return chartPreferences;
    }

    public void setMetricViewData(String context, String viewName, MetricViewData prefs) {
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        if (viewName == null || "".equals(viewName)) {
            viewName = "default";
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String chart : prefs.charts) {
            if (index != 0) {
                builder.append(PREF_ITEM_DELIM);
            }
            builder.append(chart);
            index++;
        }
        setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName, builder.toString());
    }

    public void deleteMetricViewData(String context, String viewName) {
        unsetPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName);
    }

}
