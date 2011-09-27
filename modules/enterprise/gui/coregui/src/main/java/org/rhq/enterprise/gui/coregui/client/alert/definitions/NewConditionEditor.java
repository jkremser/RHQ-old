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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.form.NumberWithUnitsValidator;
import org.rhq.enterprise.gui.coregui.client.util.measurement.MeasurementParser;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class NewConditionEditor extends LocatableDynamicForm {

    // these aren't "real" calltime condition categories (not real AlertConditionCategory enums)
    // but we need these values for the drop down menu selections
    private static final String ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE = "calltime-change";
    private static final String ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD = "calltime-threshold";

    private static final String AVAILABILITY_ITEMNAME = "availability";
    private static final String THRESHOLD_METRIC_ITEMNAME = "thresholdMetric";
    private static final String THRESHOLD_COMPARATOR_ITEMNAME = "thresholdComparator";
    private static final String THRESHOLD_ABSVALUE_ITEMNAME = "metricAbsoluteValue";
    private static final String THRESHOLD_NO_METRICS_ITEMNAME = "thresholdNoMetrics";
    private static final String BASELINE_METRIC_ITEMNAME = "baselineMetric";
    private static final String BASELINE_COMPARATOR_ITEMNAME = "baselineComparator";
    private static final String BASELINE_PERCENTAGE_ITEMNAME = "baselinePercentage";
    private static final String BASELINE_SELECTION_ITEMNAME = "baselineSelection";
    private static final String BASELINE_NO_METRICS_ITEMNAME = "baselineNoMetrics";
    private static final String CHANGE_METRIC_ITEMNAME = "changeMetric";
    private static final String CHANGE_NO_METRICS_ITEMNAME = "changeNoMetrics";
    private static final String CALLTIME_THRESHOLD_METRIC_ITEMNAME = "calltimeThresholdMetric";
    private static final String CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME = "calltimeThresholdMinMaxAvgSelection";
    private static final String CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME = "calltimeThresholdComparator";
    private static final String CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME = "calltimeThresholdAbsoluteValue";
    private static final String CALLTIME_THRESHOLD_REGEX_ITEMNAME = "calltimeThresholdRegex";
    private static final String CALLTIME_CHANGE_METRIC_ITEMNAME = "calltimeChangeMetric";
    private static final String CALLTIME_CHANGE_MINMAXAVG_ITEMNAME = "calltimeChangeMinMaxAvgSelection";
    private static final String CALLTIME_CHANGE_COMPARATOR_ITEMNAME = "calltimeChangeComparator";
    private static final String CALLTIME_CHANGE_PERCENTAGE_ITEMNAME = "calltimeChangePercentageValue";
    private static final String CALLTIME_CHANGE_REGEX_ITEMNAME = "calltimeChangeRegex";
    private static final String TRAIT_METRIC_ITEMNAME = "trait";
    private static final String OPERATION_NAME_ITEMNAME = "operation";
    private static final String OPERATION_RESULTS_ITEMNAME = "operationResults";
    private static final String EVENT_SEVERITY_ITEMNAME = "eventSeverity";
    private static final String EVENT_REGEX_ITEMNAME = "eventRegex";
    private static final String DRIFT_CONFIGNAME_REGEX_ITEMNAME = "driftConfigNameRegex";
    private static final String DRIFT_PATHNAME_REGEX_ITEMNAME = "driftPathNameRegex";
    private static final String RANGE_METRIC_ITEMNAME = "rangeMetric";
    private static final String RANGE_COMPARATOR_ITEMNAME = "rangeComparator";
    private static final String RANGE_LO_ABSVALUE_ITEMNAME = "rangeMetricLoValue";
    private static final String RANGE_HI_ABSVALUE_ITEMNAME = "rangeMetricHiValue";
    private static final String RANGE_NO_METRICS_ITEMNAME = "rangeNoMetrics";

    private SelectItem conditionTypeSelectItem;
    private HashSet<AlertCondition> conditions; // the new condition we create goes into this set
    private final SelectItem conditionExpression; // this is the GWT menu where the user selects ALL or ANY conjunction
    private boolean supportsMetrics = false;
    private boolean supportsCalltimeMetrics = false;
    private boolean supportsTraits = false;
    private boolean supportsOperations = false;
    private boolean supportsEvents = false;
    private boolean supportsResourceConfig = false;
    private boolean supportsDrift = false;
    private Runnable closeFunction; // this is called after a button is pressed and the editor should close 
    private ResourceType resourceType;

    public NewConditionEditor(String locatorId, HashSet<AlertCondition> conditions, SelectItem conditionExpression,
        ResourceType rtype, Runnable closeFunc) {

        super(locatorId);
        this.conditions = conditions;
        this.conditionExpression = conditionExpression;
        this.closeFunction = closeFunc;
        this.resourceType = rtype;

        this.supportsEvents = (rtype.getEventDefinitions() != null && rtype.getEventDefinitions().size() > 0);
        this.supportsResourceConfig = (rtype.getResourceConfigurationDefinition() != null);
        this.supportsDrift = (rtype.getDriftConfigurationTemplates() != null && rtype.getDriftConfigurationTemplates()
            .size() > 0);

        Set<MeasurementDefinition> metricDefinitions = rtype.getMetricDefinitions();
        if (metricDefinitions != null && metricDefinitions.size() > 0) {
            for (MeasurementDefinition measurementDefinition : metricDefinitions) {
                switch (measurementDefinition.getDataType()) {
                case MEASUREMENT: {
                    this.supportsMetrics = true;
                    break;
                }
                case CALLTIME: {
                    this.supportsCalltimeMetrics = true;
                    break;
                }
                case TRAIT: {
                    this.supportsTraits = true;
                    break;
                }
                default: {
                    break;
                }
                }
            }
        }

        Set<OperationDefinition> operationDefinitions = rtype.getOperationDefinitions();
        if (operationDefinitions != null && operationDefinitions.size() > 0) {
            this.supportsOperations = true;
        }
    }

    @Override
    protected void onInit() {
        super.onInit();

        setMargin(20);

        conditionTypeSelectItem = new SelectItem("conditionType", MSG
            .view_alert_definition_condition_editor_option_label());
        LinkedHashMap<String, String> condTypes = new LinkedHashMap<String, String>(7);
        condTypes.put(AlertConditionCategory.AVAILABILITY.name(), MSG
            .view_alert_definition_condition_editor_option_availability());
        if (supportsMetrics) {
            condTypes.put(AlertConditionCategory.THRESHOLD.name(), MSG
                .view_alert_definition_condition_editor_option_metric_threshold());
            condTypes.put(AlertConditionCategory.BASELINE.name(), MSG
                .view_alert_definition_condition_editor_option_metric_baseline());
            condTypes.put(AlertConditionCategory.CHANGE.name(), MSG
                .view_alert_definition_condition_editor_option_metric_change());
            condTypes.put(AlertConditionCategory.RANGE.name(), MSG
                .view_alert_definition_condition_editor_option_metric_range());
        }
        if (supportsCalltimeMetrics) {
            condTypes.put(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD, MSG
                .view_alert_definition_condition_editor_option_metric_calltime_threshold());
            condTypes.put(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE, MSG
                .view_alert_definition_condition_editor_option_metric_calltime_change());
        }
        if (supportsTraits) {
            condTypes.put(AlertConditionCategory.TRAIT.name(), MSG
                .view_alert_definition_condition_editor_option_metric_trait_change());
        }
        if (supportsOperations) {
            condTypes.put(AlertConditionCategory.CONTROL.name(), MSG
                .view_alert_definition_condition_editor_option_operation());
        }
        if (supportsResourceConfig) {
            condTypes.put(AlertConditionCategory.RESOURCE_CONFIG.name(), MSG
                .view_alert_definition_condition_editor_option_resource_configuration());
        }
        if (supportsEvents) {
            condTypes.put(AlertConditionCategory.EVENT.name(), MSG
                .view_alert_definition_condition_editor_option_event());
        }
        if (supportsDrift) {
            condTypes.put(AlertConditionCategory.DRIFT.name(), MSG
                .view_alert_definition_condition_editor_option_drift());
        }
        conditionTypeSelectItem.setValueMap(condTypes);
        conditionTypeSelectItem.setDefaultValue(AlertConditionCategory.AVAILABILITY.name());
        conditionTypeSelectItem.setWrapTitle(false);
        conditionTypeSelectItem.setRedrawOnChange(true);
        conditionTypeSelectItem.setWidth("*");

        SpacerItem spacer1 = new SpacerItem();
        spacer1.setColSpan(2);
        spacer1.setHeight(5);

        SpacerItem spacer2 = new SpacerItem();
        spacer2.setColSpan(2);
        spacer2.setHeight(5);

        ButtonItem ok = new ButtonItem("okButtonItem", MSG.common_button_ok());
        ok.setEndRow(false);
        ok.setAlign(Alignment.RIGHT);
        ok.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (validate(false)) {
                    if (saveNewCondition()) {
                        closeFunction.run();
                    }
                }
            }
        });

        ButtonItem cancel = new ButtonItem("cancelButtonItem", MSG.common_button_cancel());
        cancel.setStartRow(false);
        cancel.setAlign(Alignment.LEFT);
        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                closeFunction.run();
            }
        });

        ArrayList<FormItem> formItems = new ArrayList<FormItem>();
        formItems.add(conditionTypeSelectItem);
        formItems.add(spacer1);
        formItems.addAll(buildAvailabilityChangeFormItems());
        if (supportsMetrics) {
            formItems.addAll(buildMetricThresholdFormItems());
            formItems.addAll(buildMetricRangeFormItems());
            formItems.addAll(buildMetricBaselineFormItems());
            formItems.addAll(buildMetricChangeFormItems());
        }
        if (supportsCalltimeMetrics) {
            formItems.addAll(buildCalltimeThresholdFormItems());
            formItems.addAll(buildCalltimeChangeFormItems());
        }
        if (supportsTraits) {
            formItems.addAll(buildTraitChangeFormItems());
        }
        if (supportsOperations) {
            formItems.addAll(buildOperationFormItems());
        }
        if (supportsEvents) {
            formItems.addAll(buildEventFormItems());
        }
        if (supportsResourceConfig) {
            formItems.addAll(buildResourceConfigChangeFormItems());
        }
        if (supportsDrift) {
            formItems.addAll(buildDriftFormItems());
        }
        formItems.add(spacer2);
        formItems.add(ok);
        formItems.add(cancel);

        setFields(formItems.toArray(new FormItem[formItems.size()]));
    };

    private boolean saveNewCondition() {

        try {
            // Find out if this is using the ALL conjunction - if it is, we can't have more than one conditional use the same metric.
            // If we do, immediately abort and warn the user. See BZ 737565
            if ((BooleanExpression.ALL.name().equals(this.conditionExpression.getValue().toString()))
                && (supportsMetrics && this.resourceType.getMetricDefinitions() != null)) {

                Map<Integer, String> metricIdsUsed = new HashMap<Integer, String>();
                for (AlertCondition condition : this.conditions) {
                    if (condition.getMeasurementDefinition() != null) {
                        Integer id = Integer.valueOf(condition.getMeasurementDefinition().getId());
                        if (metricIdsUsed.containsKey(id)) {
                            String msg = MSG.view_alert_definition_condition_editor_metricswarning(metricIdsUsed
                                .get(id));
                            Message warning = new Message(msg, Severity.Warning, EnumSet.of(Message.Option.Transient));
                            CoreGUI.getMessageCenter().notify(warning);
                            return false; // multiple conditions used the same metric with ALL conjunction, this doesn't work - abort (BZ 737565)
                        }
                        metricIdsUsed.put(id, condition.getMeasurementDefinition().getDisplayName());
                    }
                }
            }

            final boolean calltimeCategory;
            final AlertConditionCategory category;

            String selectedCategory = conditionTypeSelectItem.getValue().toString();
            if (selectedCategory.equals(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD)) {
                calltimeCategory = true;
                category = AlertConditionCategory.THRESHOLD;
            } else if (selectedCategory.equals(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE)) {
                calltimeCategory = true;
                category = AlertConditionCategory.CHANGE;
            } else {
                calltimeCategory = false;
                category = AlertConditionCategory.valueOf(selectedCategory);
            }

            AlertCondition newCondition = new AlertCondition();
            newCondition.setCategory(category);

            switch (category) {
            case AVAILABILITY: {
                newCondition.setName(null);
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(getValueAsString(AVAILABILITY_ITEMNAME));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case THRESHOLD: {
                if (!calltimeCategory) {
                    MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(THRESHOLD_METRIC_ITEMNAME));
                    newCondition.setName(measDef.getDisplayName());
                    newCondition.setThreshold(getMeasurementValue(measDef,
                        getValueAsString(THRESHOLD_ABSVALUE_ITEMNAME)));
                    newCondition.setComparator(getValueAsString(THRESHOLD_COMPARATOR_ITEMNAME));
                    newCondition.setOption(null);
                    newCondition.setMeasurementDefinition(measDef);
                } else {
                    MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(CALLTIME_THRESHOLD_METRIC_ITEMNAME));
                    newCondition.setName(getValueAsString(CALLTIME_THRESHOLD_REGEX_ITEMNAME));
                    newCondition.setThreshold(getMeasurementValue(measDef,
                        getValueAsString(CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME)));
                    newCondition.setComparator(getValueAsString(CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME));
                    newCondition.setOption(getValueAsString(CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME));
                    newCondition.setMeasurementDefinition(measDef);
                }
                break;
            }

            case BASELINE: {
                MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(BASELINE_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName());
                newCondition.setThreshold(getMeasurementValueByUnits(MeasurementUnits.PERCENTAGE,
                    getValueAsString(BASELINE_PERCENTAGE_ITEMNAME)));
                newCondition.setComparator(getValueAsString(BASELINE_COMPARATOR_ITEMNAME));
                newCondition.setOption(getValueAsString(BASELINE_SELECTION_ITEMNAME));
                newCondition.setMeasurementDefinition(measDef);
                break;
            }

            case CHANGE: {
                if (!calltimeCategory) {
                    MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(CHANGE_METRIC_ITEMNAME));
                    newCondition.setName(measDef.getDisplayName());
                    newCondition.setComparator(null);
                    newCondition.setThreshold(null);
                    newCondition.setOption(null);
                    newCondition.setMeasurementDefinition(measDef);
                } else {
                    MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(CALLTIME_CHANGE_METRIC_ITEMNAME));
                    newCondition.setName(getValueAsString(CALLTIME_CHANGE_REGEX_ITEMNAME));
                    newCondition.setThreshold(getMeasurementValueByUnits(MeasurementUnits.PERCENTAGE,
                        getValueAsString(CALLTIME_CHANGE_PERCENTAGE_ITEMNAME)));
                    newCondition.setComparator(getValueAsString(CALLTIME_CHANGE_COMPARATOR_ITEMNAME));
                    newCondition.setOption(getValueAsString(CALLTIME_CHANGE_MINMAXAVG_ITEMNAME));
                    newCondition.setMeasurementDefinition(measDef);
                }
                break;
            }

            case TRAIT: {
                MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(TRAIT_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName());
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(null);
                newCondition.setMeasurementDefinition(measDef);
                break;
            }

            case CONTROL: {
                newCondition.setName(getValueAsString(OPERATION_NAME_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(getValueAsString(OPERATION_RESULTS_ITEMNAME));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case EVENT: {
                newCondition.setName(getValueAsString(EVENT_SEVERITY_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(getValueAsString(EVENT_REGEX_ITEMNAME));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case RESOURCE_CONFIG: {
                newCondition.setName(null);
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(null);
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case DRIFT: {
                newCondition.setName(getValueAsString(DRIFT_CONFIGNAME_REGEX_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(getValueAsString(DRIFT_PATHNAME_REGEX_ITEMNAME));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case RANGE: {
                MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(RANGE_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName());
                newCondition.setThreshold(getMeasurementValue(measDef, getValueAsString(RANGE_LO_ABSVALUE_ITEMNAME)));
                newCondition.setComparator(getValueAsString(RANGE_COMPARATOR_ITEMNAME));
                newCondition.setOption(getMeasurementValue(measDef, getValueAsString(RANGE_HI_ABSVALUE_ITEMNAME))
                    .toString());
                newCondition.setMeasurementDefinition(measDef);
                break;
            }

            default: {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_alert_common_tab_invalid_condition_category(category.name())); // should never happen
                break;
            }
            }

            this.conditions.add(newCondition);

            return true;
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Problem creating condition", e);
            return false;
        }
    }

    private Double getMeasurementValue(MeasurementDefinition measDef, String userEnteredValue) {
        return getMeasurementValueByUnits(measDef.getUnits(), userEnteredValue);
    }

    private Double getMeasurementValueByUnits(MeasurementUnits units, String userEnteredValue) {
        return MeasurementParser.parse(userEnteredValue, units).getValue();
    }

    private ArrayList<FormItem> buildMetricThresholdFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.THRESHOLD);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_threshold_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("thresholdHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildMetricDropDownMenu(THRESHOLD_METRIC_ITEMNAME, false, ifFunc);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
            formItems.add(buildComparatorDropDownMenu(THRESHOLD_COMPARATOR_ITEMNAME, ifFunc));
            TextItem absoluteValue = new TextItem(THRESHOLD_ABSVALUE_ITEMNAME, MSG
                .view_alert_definition_condition_editor_metric_threshold_value());
            absoluteValue.setWrapTitle(false);
            absoluteValue.setRequired(true);
            absoluteValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_threshold_value_tooltip());
            absoluteValue.setHoverWidth(200);
            absoluteValue.setValidateOnChange(true);
            absoluteValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
                metricDropDownMenu));
            absoluteValue.setShowIfCondition(ifFunc);
            formItems.add(absoluteValue);
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(THRESHOLD_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildMetricRangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.RANGE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_range_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("rangeHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildMetricDropDownMenu(RANGE_METRIC_ITEMNAME, false, ifFunc);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
            formItems.add(buildRangeComparatorDropDownMenu(RANGE_COMPARATOR_ITEMNAME, ifFunc));
            TextItem absoluteLowValue = new TextItem(RANGE_LO_ABSVALUE_ITEMNAME, MSG
                .view_alert_definition_condition_editor_metric_range_lovalue());
            absoluteLowValue.setWrapTitle(false);
            absoluteLowValue.setRequired(true);
            absoluteLowValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_range_lovalue_tooltip());
            absoluteLowValue.setHoverWidth(200);
            absoluteLowValue.setValidateOnChange(true);
            absoluteLowValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
                metricDropDownMenu));
            absoluteLowValue.setShowIfCondition(ifFunc);
            formItems.add(absoluteLowValue);

            TextItem absoluteHighValue = new TextItem(RANGE_HI_ABSVALUE_ITEMNAME, MSG
                .view_alert_definition_condition_editor_metric_range_hivalue());
            absoluteHighValue.setWrapTitle(false);
            absoluteHighValue.setRequired(true);
            absoluteHighValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_range_hivalue_tooltip());
            absoluteHighValue.setHoverWidth(200);
            absoluteHighValue.setValidateOnChange(true);
            absoluteHighValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
                metricDropDownMenu));
            absoluteHighValue.setShowIfCondition(ifFunc);
            formItems.add(absoluteHighValue);
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(RANGE_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildMetricBaselineFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.BASELINE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_baseline_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("baselineHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        // if a metric is trending (up or down), it will never have baselines calculated for it so only show dynamic metrics
        SelectItem metricDropDownMenu = buildMetricDropDownMenu(BASELINE_METRIC_ITEMNAME, true, ifFunc);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
            formItems.add(buildComparatorDropDownMenu(BASELINE_COMPARATOR_ITEMNAME, ifFunc));

            TextItem baselinePercentage = new TextItem(BASELINE_PERCENTAGE_ITEMNAME, MSG
                .view_alert_definition_condition_editor_metric_baseline_percentage());
            baselinePercentage.setWrapTitle(false);
            baselinePercentage.setRequired(true);
            baselinePercentage.setTooltip(MSG
                .view_alert_definition_condition_editor_metric_baseline_percentage_tooltip());
            baselinePercentage.setHoverWidth(200);
            baselinePercentage.setShowIfCondition(ifFunc);
            baselinePercentage.setValidateOnChange(true);
            baselinePercentage.setValidators(new NumberWithUnitsValidator(MeasurementUnits.PERCENTAGE));
            formItems.add(baselinePercentage);

            SelectItem baselineSelection = new SelectItem(BASELINE_SELECTION_ITEMNAME, MSG
                .view_alert_definition_condition_editor_metric_baseline_value());
            LinkedHashMap<String, String> baselines = new LinkedHashMap<String, String>(3);
            baselines.put("min", MSG.view_alert_definition_condition_editor_common_min()); // TODO can we have the current value of the min baseline
            baselines.put("mean", MSG.view_alert_definition_condition_editor_common_avg()); // TODO can we have the current value of the avg baseline
            baselines.put("max", MSG.view_alert_definition_condition_editor_common_max()); // TODO can we have the current value of the max baseline
            baselineSelection.setValueMap(baselines);
            baselineSelection.setDefaultValue("mean");
            baselineSelection.setWrapTitle(false);
            baselineSelection.setWidth("*");
            baselineSelection.setRedrawOnChange(true);
            baselineSelection.setShowIfCondition(ifFunc);
            formItems.add(baselineSelection);
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(BASELINE_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildMetricChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CHANGE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_change_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("changeMetricHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildMetricDropDownMenu(CHANGE_METRIC_ITEMNAME, false, ifFunc);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(CHANGE_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildCalltimeThresholdFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_calltime_threshold_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("calltimeThresholdHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildCalltimeMetricDropDownMenu(CALLTIME_THRESHOLD_METRIC_ITEMNAME, ifFunc);
        formItems.add(metricDropDownMenu);

        SelectItem minMaxAvgSelection = new SelectItem(CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME, MSG
            .view_alert_definition_condition_editor_metric_calltime_common_limit());
        LinkedHashMap<String, String> limits = new LinkedHashMap<String, String>(3);
        limits.put("MIN", MSG.view_alert_definition_condition_editor_common_min());
        limits.put("AVG", MSG.view_alert_definition_condition_editor_common_avg());
        limits.put("MAX", MSG.view_alert_definition_condition_editor_common_max());
        minMaxAvgSelection
            .setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_common_limit_tooltip());
        minMaxAvgSelection.setHoverWidth(200);
        minMaxAvgSelection.setValueMap(limits);
        minMaxAvgSelection.setDefaultValue("AVG");
        minMaxAvgSelection.setWrapTitle(false);
        minMaxAvgSelection.setWidth("*");
        minMaxAvgSelection.setRedrawOnChange(true);
        minMaxAvgSelection.setShowIfCondition(ifFunc);
        formItems.add(minMaxAvgSelection);

        formItems.add(buildComparatorDropDownMenu(CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME, ifFunc));
        TextItem absoluteValue = new TextItem(CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME, MSG
            .view_alert_definition_condition_editor_metric_calltime_threshold_value());
        absoluteValue.setWrapTitle(false);
        absoluteValue.setRequired(true);
        absoluteValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_threshold_value_tooltip());
        absoluteValue.setHoverWidth(200);
        absoluteValue.setShowIfCondition(ifFunc);
        absoluteValue.setValidateOnChange(true);
        absoluteValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
            metricDropDownMenu));
        formItems.add(absoluteValue);

        TextItem regex = new TextItem(CALLTIME_THRESHOLD_REGEX_ITEMNAME, MSG
            .view_alert_definition_condition_editor_metric_calltime_common_regex());
        regex.setRequired(false);
        regex.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_common_regex_tooltip());
        regex.setHoverWidth(200);
        regex.setWrapTitle(false);
        regex.setShowIfCondition(ifFunc);
        formItems.add(regex);

        return formItems;
    }

    private ArrayList<FormItem> buildCalltimeChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_calltime_change_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("calltimeChangeHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildCalltimeMetricDropDownMenu(CALLTIME_CHANGE_METRIC_ITEMNAME, ifFunc));

        SelectItem minMaxAvgSelection = new SelectItem(CALLTIME_CHANGE_MINMAXAVG_ITEMNAME, MSG
            .view_alert_definition_condition_editor_metric_calltime_common_limit());
        LinkedHashMap<String, String> limits = new LinkedHashMap<String, String>(3);
        limits.put("MIN", MSG.view_alert_definition_condition_editor_common_min());
        limits.put("AVG", MSG.view_alert_definition_condition_editor_common_avg());
        limits.put("MAX", MSG.view_alert_definition_condition_editor_common_max());
        minMaxAvgSelection
            .setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_common_limit_tooltip());
        minMaxAvgSelection.setHoverWidth(200);
        minMaxAvgSelection.setValueMap(limits);
        minMaxAvgSelection.setDefaultValue("AVG");
        minMaxAvgSelection.setWrapTitle(false);
        minMaxAvgSelection.setWidth("*");
        minMaxAvgSelection.setRedrawOnChange(true);
        minMaxAvgSelection.setShowIfCondition(ifFunc);
        formItems.add(minMaxAvgSelection);

        formItems.add(buildCalltimeComparatorDropDownMenu(CALLTIME_CHANGE_COMPARATOR_ITEMNAME, ifFunc));

        TextItem percentage = new TextItem(CALLTIME_CHANGE_PERCENTAGE_ITEMNAME, MSG
            .view_alert_definition_condition_editor_metric_calltime_change_percentage());
        percentage.setWrapTitle(false);
        percentage.setRequired(true);
        percentage.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_change_percentage_tooltip());
        percentage.setHoverWidth(200);
        percentage.setShowIfCondition(ifFunc);
        percentage.setValidateOnChange(true);
        percentage.setValidators(new NumberWithUnitsValidator(MeasurementUnits.PERCENTAGE));
        formItems.add(percentage);

        TextItem regex = new TextItem(CALLTIME_CHANGE_REGEX_ITEMNAME, MSG
            .view_alert_definition_condition_editor_metric_calltime_common_regex());
        regex.setRequired(false);
        regex.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_common_regex_tooltip());
        regex.setHoverWidth(200);
        regex.setWrapTitle(false);
        regex.setShowIfCondition(ifFunc);
        formItems.add(regex);

        return formItems;
    }

    private ArrayList<FormItem> buildTraitChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.TRAIT);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_trait_change_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("traitHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> traitsMap = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.TRAIT) {
                traitsMap.put(String.valueOf(def.getId()), def.getDisplayName());
            }
        }

        SelectItem traitSelection = new SelectItem(TRAIT_METRIC_ITEMNAME, MSG
            .view_alert_definition_condition_editor_metric_trait_change_value());
        traitSelection.setValueMap(traitsMap);
        traitSelection.setDefaultValue(traitsMap.keySet().iterator().next()); // just use the first one
        traitSelection.setWidth("*");
        traitSelection.setRedrawOnChange(true);
        traitSelection.setShowIfCondition(ifFunc);
        formItems.add(traitSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildAvailabilityChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.AVAILABILITY);

        String helpStr = MSG.view_alert_definition_condition_editor_avilability_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("availabilityHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem selection = new SelectItem(AVAILABILITY_ITEMNAME, MSG
            .view_alert_definition_condition_editor_avilability_value());
        LinkedHashMap<String, String> avails = new LinkedHashMap<String, String>(2);
        avails.put(AvailabilityType.UP.name(), MSG.view_alert_definition_condition_editor_avilability_option_up());
        avails.put(AvailabilityType.DOWN.name(), MSG.view_alert_definition_condition_editor_avilability_option_down());
        selection.setValueMap(avails);
        selection.setDefaultValue(AvailabilityType.DOWN.name());
        selection.setShowIfCondition(ifFunc);

        formItems.add(selection);
        return formItems;
    }

    private ArrayList<FormItem> buildOperationFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CONTROL);

        String helpStr = MSG.view_alert_definition_condition_editor_operation_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("operationHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> ops = new LinkedHashMap<String, String>();
        for (OperationDefinition opDef : this.resourceType.getOperationDefinitions()) {
            ops.put(opDef.getName(), opDef.getDisplayName());
        }

        SelectItem opSelection = new SelectItem(OPERATION_NAME_ITEMNAME, MSG.common_title_value());
        opSelection.setValueMap(ops);
        opSelection.setDefaultValue(ops.keySet().iterator().next()); // just use the first one
        opSelection.setWidth("*");
        opSelection.setRedrawOnChange(true);
        opSelection.setShowIfCondition(ifFunc);
        formItems.add(opSelection);

        SelectItem opResultsSelection = new SelectItem(OPERATION_RESULTS_ITEMNAME, MSG.common_title_status());
        LinkedHashMap<String, String> operationStatuses = new LinkedHashMap<String, String>(4);
        operationStatuses.put(OperationRequestStatus.INPROGRESS.name(), MSG.common_status_inprogress());
        operationStatuses.put(OperationRequestStatus.SUCCESS.name(), MSG.common_status_success());
        operationStatuses.put(OperationRequestStatus.FAILURE.name(), MSG.common_status_failed());
        operationStatuses.put(OperationRequestStatus.CANCELED.name(), MSG.common_status_canceled());
        opResultsSelection.setValueMap(operationStatuses);
        opResultsSelection.setDefaultValue(OperationRequestStatus.FAILURE.name());
        opResultsSelection.setWrapTitle(false);
        opResultsSelection.setShowIfCondition(ifFunc);
        formItems.add(opResultsSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildEventFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.EVENT);

        String helpStr = MSG.view_alert_definition_condition_editor_event_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("eventHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem eventSeveritySelection = new SelectItem(EVENT_SEVERITY_ITEMNAME, MSG
            .view_alert_definition_condition_editor_event_severity());
        LinkedHashMap<String, String> severities = new LinkedHashMap<String, String>(5);
        severities.put(EventSeverity.DEBUG.name(), MSG.common_severity_debug());
        severities.put(EventSeverity.INFO.name(), MSG.common_severity_info());
        severities.put(EventSeverity.WARN.name(), MSG.common_severity_warn());
        severities.put(EventSeverity.ERROR.name(), MSG.common_severity_error());
        severities.put(EventSeverity.FATAL.name(), MSG.common_severity_fatal());
        eventSeveritySelection.setValueMap(severities);
        eventSeveritySelection.setDefaultValue(EventSeverity.ERROR.name());
        eventSeveritySelection.setWrapTitle(false);
        eventSeveritySelection.setShowIfCondition(ifFunc);
        formItems.add(eventSeveritySelection);

        TextItem eventRegex = new TextItem(EVENT_REGEX_ITEMNAME, MSG
            .view_alert_definition_condition_editor_event_regex());
        eventRegex.setRequired(false);
        eventRegex.setTooltip(MSG.view_alert_definition_condition_editor_event_regex_tooltip());
        eventRegex.setHoverWidth(200);
        eventRegex.setWrapTitle(false);
        eventRegex.setShowIfCondition(ifFunc);
        formItems.add(eventRegex);

        return formItems;
    }

    private ArrayList<FormItem> buildResourceConfigChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.RESOURCE_CONFIG);

        String helpStr = MSG.view_alert_definition_condition_editor_resource_configuration_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("changeConfigHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        return formItems;
    }

    private ArrayList<FormItem> buildDriftFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.DRIFT);

        String helpStr = MSG.view_alert_definition_condition_editor_drift_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("driftHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        TextItem driftConfigNameRegex = new TextItem(DRIFT_CONFIGNAME_REGEX_ITEMNAME, MSG
            .view_alert_definition_condition_editor_drift_configname_regex());
        driftConfigNameRegex.setRequired(false);
        driftConfigNameRegex.setTooltip(MSG.view_alert_definition_condition_editor_drift_configname_regex_tooltip());
        driftConfigNameRegex.setHoverWidth(200);
        driftConfigNameRegex.setWrapTitle(false);
        driftConfigNameRegex.setShowIfCondition(ifFunc);
        formItems.add(driftConfigNameRegex);

        TextItem driftPathNameRegex = new TextItem(DRIFT_PATHNAME_REGEX_ITEMNAME, MSG
            .view_alert_definition_condition_editor_drift_pathname_regex());
        driftPathNameRegex.setRequired(false);
        driftPathNameRegex.setTooltip(MSG.view_alert_definition_condition_editor_drift_pathname_regex_tooltip());
        driftPathNameRegex.setHoverWidth(200);
        driftPathNameRegex.setWrapTitle(false);
        driftPathNameRegex.setShowIfCondition(ifFunc);
        formItems.add(driftPathNameRegex);

        return formItems;
    }

    private SelectItem buildMetricDropDownMenu(String itemName, boolean dynamicOnly, FormItemIfFunction ifFunc) {

        // find out if this is the ALL - if it is, we can't have more than one conditional use the same metric (BZ 737565)
        Set<String> metricIdsToHide = new HashSet<String>();
        if (BooleanExpression.ALL.name().equals(this.conditionExpression.getValue().toString())) {
            for (AlertCondition condition : this.conditions) {
                if (condition.getMeasurementDefinition() != null) {
                    metricIdsToHide.add(String.valueOf(condition.getMeasurementDefinition().getId()));
                }
            }
        }

        LinkedHashMap<String, String> metricsMap = new LinkedHashMap<String, String>();
        TreeSet<MeasurementDefinition> sortedDefs = new TreeSet<MeasurementDefinition>(
            new Comparator<MeasurementDefinition>() {

                @Override
                public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                    return o1.getDisplayName().compareTo(o2.getDisplayName());
                }
            });
        sortedDefs.addAll(this.resourceType.getMetricDefinitions());

        for (MeasurementDefinition def : sortedDefs) {
            if (def.getDataType() == DataType.MEASUREMENT) {
                if (!dynamicOnly || def.getNumericType() == NumericType.DYNAMIC) {
                    String idString = String.valueOf(def.getId()); // use id, not name, for key; name is not unique when per-minute metric is also used
                    if (!metricIdsToHide.contains(idString)) {
                        metricsMap.put(idString, def.getDisplayName());
                    }
                }
            }
        }

        if (metricsMap.isEmpty()) {
            return null; // all metrics should be hidden
        }

        SelectItem metricSelection = new SelectItem(itemName, MSG
            .view_alert_definition_condition_editor_metric_threshold_name());
        metricSelection.setValueMap(metricsMap);
        metricSelection.setDefaultValue(metricsMap.keySet().iterator().next()); // just use the first one
        metricSelection.setWidth("*");
        metricSelection.setRedrawOnChange(true);
        metricSelection.setShowIfCondition(ifFunc);
        return metricSelection;
    }

    private SelectItem buildCalltimeMetricDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> metricsMap = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.CALLTIME) {
                metricsMap.put(String.valueOf(def.getId()), def.getDisplayName());
            }
        }

        SelectItem metricSelection = new SelectItem(itemName, MSG
            .view_alert_definition_condition_editor_metric_calltime_common_name());
        metricSelection.setValueMap(metricsMap);
        metricSelection.setDefaultValue(metricsMap.keySet().iterator().next()); // just use the first one
        metricSelection.setWidth("*");
        metricSelection.setRedrawOnChange(true);
        metricSelection.setShowIfCondition(ifFunc);
        return metricSelection;
    }

    private SelectItem buildComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(3);
        comparators.put("<", "< (" + MSG.view_alert_definition_condition_editor_metric_threshold_comparator_less()
            + ")");
        comparators.put("=", "= (" + MSG.view_alert_definition_condition_editor_metric_threshold_comparator_equal()
            + ")");
        comparators.put(">", "> (" + MSG.view_alert_definition_condition_editor_metric_threshold_comparator_greater()
            + ")");

        SelectItem comparatorSelection = new SelectItem(itemName, MSG
            .view_alert_definition_condition_editor_metric_threshold_comparator());
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue("<");
        comparatorSelection
            .setTooltip(MSG.view_alert_definition_condition_editor_metric_threshold_comparator_tooltip());
        comparatorSelection.setHoverWidth(200);
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private SelectItem buildCalltimeComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(3);
        comparators.put("LO", MSG.view_alert_definition_condition_editor_metric_calltime_common_comparator_shrinks());
        comparators.put("CH", MSG.view_alert_definition_condition_editor_metric_calltime_common_comparator_changes());
        comparators.put("HI", MSG.view_alert_definition_condition_editor_metric_calltime_common_comparator_grows());

        SelectItem comparatorSelection = new SelectItem(itemName, MSG
            .view_alert_definition_condition_editor_metric_calltime_common_comparator());
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue("CH");
        comparatorSelection.setTooltip(MSG
            .view_alert_definition_condition_editor_metric_calltime_common_comparator_tooltip());
        comparatorSelection.setHoverWidth(200);
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private SelectItem buildRangeComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(2);
        comparators.put("<", MSG.view_alert_definition_condition_editor_metric_range_comparator_inside_exclusive());
        comparators.put(">", MSG.view_alert_definition_condition_editor_metric_range_comparator_outside_exclusive());
        comparators.put("<=", MSG.view_alert_definition_condition_editor_metric_range_comparator_inside_inclusive());
        comparators.put(">=", MSG.view_alert_definition_condition_editor_metric_range_comparator_outside_inclusive());

        SelectItem comparatorSelection = new SelectItem(itemName, MSG
            .view_alert_definition_condition_editor_metric_range_comparator());
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue("<");
        comparatorSelection.setTooltip(MSG.view_alert_definition_condition_editor_metric_range_comparator_tooltip());
        comparatorSelection.setHoverWidth(200);
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private StaticTextItem buildHelpTextItem(String itemName, String helpText, FormItemIfFunction ifFunc) {
        StaticTextItem help = new StaticTextItem(itemName);
        help.setShowTitle(false);
        help.setColSpan(2);
        help.setRowSpan(2);
        help.setWrap(true);
        help.setValue(helpText);
        help.setShowIfCondition(ifFunc);
        return help;
    }

    private MeasurementDefinition getMeasurementDefinition(String metricId) {
        int id = Integer.valueOf(metricId).intValue();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (id == def.getId()) {
                return def;
            }
        }
        CoreGUI.getErrorHandler().handleError(
            MSG.view_alert_definition_condition_editor_metric_common_definition_not_found());
        return null;
    }

    private class ShowIfCategoryFunction implements FormItemIfFunction {
        private final AlertConditionCategory category;
        private final String calltimeCategory;

        public ShowIfCategoryFunction(AlertConditionCategory category) {
            this.category = category;
            this.calltimeCategory = null;
        }

        public ShowIfCategoryFunction(String calltimeCategory) {
            this.category = null;
            this.calltimeCategory = calltimeCategory;
        }

        public boolean execute(FormItem item, Object value, DynamicForm form) {
            String conditionTypeString = form.getValue("conditionType").toString();
            if (category != null) {
                return category.name().equals(conditionTypeString);
            } else {
                return calltimeCategory.equals(conditionTypeString);
            }
        }
    }
}
