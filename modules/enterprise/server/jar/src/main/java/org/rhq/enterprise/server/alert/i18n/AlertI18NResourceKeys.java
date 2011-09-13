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
package org.rhq.enterprise.server.alert.i18n;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * @author Joseph Marques
 */

@I18NResourceBundle(baseName = "alert-messages", defaultLocale = "en")
public interface AlertI18NResourceKeys {
    @I18NMessages( { @I18NMessage("value is between {0} and {1}, inclusive"),
        @I18NMessage(locale = "de", value = "Der Wert zwischen {0} und {1}, pauschal") })
    String ALERT_CONFIG_PROPS_CB_INSIDE_INCL_RANGE = "alert.config.props.CB.InsideRange.incl.";

    @I18NMessages( { @I18NMessage("between {0} and {1}, incl"),
        @I18NMessage(locale = "de", value = "zwischen {0} und {1}, paus") })
    String ALERT_CONFIG_PROPS_CB_INSIDE_INCL_RANGE_SHORT = "alert.config.props.CB.InsideRange.incl.short";

    @I18NMessages( { @I18NMessage("value is between {0} and {1}, exclusive"),
        @I18NMessage(locale = "de", value = "Der Wert zwischen {0} und {1}, exklusiv") })
    String ALERT_CONFIG_PROPS_CB_INSIDE_EXCL_RANGE = "alert.config.props.CB.InsideRange.excl";

    @I18NMessages( { @I18NMessage("between {0} and {1}, excl"),
        @I18NMessage(locale = "de", value = "zwischen {0} und {1}, exkl.") })
    String ALERT_CONFIG_PROPS_CB_INSIDE_EXCL_RANGE_SHORT = "alert.config.props.CB.InsideRange.excl.short";

    @I18NMessages( { @I18NMessage("value is outside {0} and {1}, inclusive"),
        @I18NMessage(locale = "de", value = "Der Wert außerhalb {0} und {1}, pauschal") })
    String ALERT_CONFIG_PROPS_CB_OUTSIDE_INCL_RANGE = "alert.config.props.CB.OutsideRange.incl";

    @I18NMessages( { @I18NMessage("outside {0} and {1}, incl"),
        @I18NMessage(locale = "de", value = "außerhalb {0} und {1}, paus") })
    String ALERT_CONFIG_PROPS_CB_OUTSIDE_INCL_RANGE_SHORT = "alert.config.props.CB.OutsideRange.incl.short";

    @I18NMessages( { @I18NMessage("value is outside {0} and {1}, exclusive"),
        @I18NMessage(locale = "de", value = "Der Wert außerhalb {0} und {1}, exklusiv") })
    String ALERT_CONFIG_PROPS_CB_OUTSIDE_EXCL_RANGE = "alert.config.props.CB.OutsideRange.excl";

    @I18NMessages( { @I18NMessage("outside {0} and {1}, excl"),
        @I18NMessage(locale = "de", value = "außerhalb {0} und {1}, exkl") })
    String ALERT_CONFIG_PROPS_CB_OUTSIDE_EXCL_RANGE_SHORT = "alert.config.props.CB.OutsideRange.excl.short";

    @I18NMessages( { @I18NMessage("Drift detected"), @I18NMessage(locale = "de", value = "Änderung erkannt") })
    String ALERT_CONFIG_PROPS_CB_DRIFT = "alert.config.props.CB.Drift";

    @I18NMessages( { @I18NMessage("Drift detected"), @I18NMessage(locale = "de", value = "Änderung erkannt") })
    String ALERT_CONFIG_PROPS_CB_DRIFT_SHORT = "alert.config.props.CB.Drift.short";

    @I18NMessages( { @I18NMessage("Availability goes {0}"),
        @I18NMessage(locale = "de", value = "Verf�gbarkeit wird {0}") })
    String ALERT_CONFIG_PROPS_CB_AVAILABILITY = "alert.config.props.CB.Availability";

    @I18NMessages( { @I18NMessage("Avail goes {0}"), @I18NMessage(locale = "de", value = "Verf. wird {0}") })
    String ALERT_CONFIG_PROPS_CB_AVAILABILITY_SHORT = "alert.config.props.CB.Availability.short";

    @I18NMessages( { @I18NMessage("Event Severity: {0}"),
        @I18NMessage(locale = "de", value = "Schwere des Ereignesses: {0}") })
    String ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY = "alert.config.props.CB.EventSeverity";

    @I18NMessages( { @I18NMessage("Sev: {0}"), @I18NMessage(locale = "de", value = "Schwere: {0}") })
    String ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY_SHORT = "alert.config.props.CB.EventSeverity.short";

    @I18NMessages( { @I18NMessage("Event Severity: {0} and matching expression \"{1}\""),
        @I18NMessage(locale = "de", value = "Schwere des Ereignesses: {0} und zugeh�riger Ausdruck  \"{1}\"") })
    String ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY_REGEX_MATCH = "alert.config.props.CB.EventSeverity.RegexMatch";

    @I18NMessages( { @I18NMessage("Sev: {0} & exp \"{1}\""),
        @I18NMessage(locale = "de", value = "Schwere: {0} & Ausdruck  \"{1}\"") })
    String ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY_REGEX_MATCH_SHORT = "alert.config.props.CB.EventSeverity.RegexMatch.short";

    @I18NMessages( { @I18NMessage("value changed"), @I18NMessage(locale = "de", value = "Der Wert hat sich ge�ndert") })
    String ALERT_CURRENT_LIST_VALUE_CHANGED = "alert.current.list.ValueChanged";

    @I18NMessages( { @I18NMessage("val chg"), @I18NMessage(locale = "de", value = "Wert�nd.") })
    String ALERT_CURRENT_LIST_VALUE_CHANGED_SHORT = "alert.current.list.ValueChanged.short";

    @I18NMessages( {
        @I18NMessage("\\  - Condition {0}: {1}\\n\\\n" + "\\  - Date/Time: {2}\\n\\\n" + "\\  - Details: {3}\\n\\\n"),
        @I18NMessage(locale = "de", value = "  - Bedingung {0}: {1}\\n\\\n  - Datum/Uhrzeit: {2}\\n\\\n"
            + "\\  - Details: {3}\\n\\\n") })
    String ALERT_EMAIL_CONDITION_LOG_FORMAT = "alert.email.condition.log.format";

    @I18NMessages( { @I18NMessage("\\  - Cond {0}: {1}\\n\\\n" + "\\  - Time: {2}\\n\\\n" + "\\  - Det: {3}\\n\\\n"),
        @I18NMessage(locale = "de", value = "  - Bed {0}: {1}\\n\\\n  - Zeit: {2}\\n\\\n" + "\\  - Det: {3}\\n\\\n") })
    String ALERT_EMAIL_CONDITION_LOG_FORMAT_SHORT = "alert.email.condition.log.format.short";
}