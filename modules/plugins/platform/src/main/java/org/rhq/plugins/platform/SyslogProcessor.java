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
package org.rhq.plugins.platform;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Processes syslog messages and prepares them as events.
 * Subclasses need to extend this in order to get the actual syslog messages from some source.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class SyslogProcessor {

    private final Log log = LogFactory.getLog(SyslogProcessor.class);

    private final EventContext eventContext;
    private final EventSeverity minimumEventSeverity;
    private final Pattern includesPattern;
    private final Pattern parserRegex;
    private final SimpleDateFormat dateTimeFormatter;
    private final String sourceLocation;

    protected static final String EVENT_LOG_TYPE = "Event Log"; // as defined in plugin descriptor

    public SyslogProcessor(ResourceContext resourceContext, PropertyMap logProperties, String sourceLocation) {

        this.sourceLocation = sourceLocation;
        this.eventContext = resourceContext.getEventContext();

        String minSev = logProperties.getSimpleValue(LinuxPlatformComponent.PLUGIN_CONFIG_EVENT_TRACKING_MIN_SEV, "");
        if (minSev.toLowerCase().startsWith("info")) {
            minimumEventSeverity = EventSeverity.INFO;
        } else if (minSev.toLowerCase().startsWith("warn")) {
            minimumEventSeverity = EventSeverity.WARN;
        } else if (minSev.toLowerCase().startsWith("err")) {
            minimumEventSeverity = EventSeverity.ERROR;
        } else {
            minimumEventSeverity = null;
        }

        String regexString = logProperties.getSimpleValue(
            LinuxPlatformComponent.PLUGIN_CONFIG_EVENT_TRACKING_INCLUDES_REGEX, null);
        Pattern regexPattern = null;
        if (regexString != null && !regexString.equals("")) {
            try {
                regexPattern = Pattern.compile(regexString);
            } catch (Exception e) {
                log.error("Invalid includes regex [" + regexString + "]. All events will be accepted. " + e);
            }
        }
        this.includesPattern = regexPattern;

        regexString = logProperties.getSimpleValue(LinuxPlatformComponent.PLUGIN_CONFIG_EVENT_TRACKING_PARSER_REGEX,
            null);
        regexPattern = null;
        if (regexString != null && !regexString.equals("")) {
            try {
                regexPattern = Pattern.compile(regexString);
            } catch (Exception e) {
                log.error("Invalid parser regex [" + regexString + "]. Will parse with a best guess. " + e);
            }
        }
        this.parserRegex = regexPattern;

        regexString = logProperties.getSimpleValue(LinuxPlatformComponent.PLUGIN_CONFIG_EVENT_TRACKING_DATETIME_FORMAT,
            null);
        SimpleDateFormat formatter = null;
        if (regexString != null && !regexString.equals("")) {
            try {
                formatter = new SimpleDateFormat(regexString);
            } catch (Exception e) {
                log.error("Invalid datetime format [" + regexString + "]. Will use current times. " + e);
            }
        }
        this.dateTimeFormatter = formatter;

        return;
    }

    protected String getSourceLocation() {
        return this.sourceLocation;
    }

    protected EventContext getEventContext() {
        return this.eventContext;
    }

    protected EventSeverity getMinimumEventSeverity() {
        return this.minimumEventSeverity;
    }

    protected Pattern getIncludesPattern() {
        return this.includesPattern;
    }

    protected Pattern getParserRegex() {
        return this.parserRegex;
    }

    /**
     * Converts the givem syslog message to an event.
     * If the parser regular expression was specified, it will be used to parse the message. If it was not
     * specified, this method will attempt to handle this format:
     * 
     * "%timegenerated:::date-rfc3339%,%syslogpriority-text%,%syslogfacility-text%:%msg%\n"
     *
     * If that fails, the entire line will be used as the message detail, with the timestamp being the current
     * time and the severity being INFO.
     *
     * @param syslogMessage the actual syslog message
     *
     * @return the event, or <code>null</code> if the event didn't match our regex, wasn't of the minimum severity
     *         or couldn't be parsed successfully
     */
    protected Event convertLine(String syslogMessage) {

        Event event = null;

        try {
            if (this.parserRegex != null) {
                event = convertLineParserRegEx(syslogMessage);
            } else {
                event = convertLineDefaultFormat(syslogMessage); // no parser regex, use best attempt with our default format
            }

            // if we could not parse the event, just use the entire log as the event detail
            if (event == null) {
                event = convertAnyLine(syslogMessage);
            }

            // filter the event if it doesn't meet our minimum severity requirement or it doesn't match the includes regex
            if (event != null) {
                if (this.minimumEventSeverity != null
                    && !event.getSeverity().isAtLeastAsSevereAs(this.minimumEventSeverity)) {
                    if (log.isTraceEnabled()) {
                        log.trace("event is not at minimum severity: " + event);
                    }
                    event = null;
                } else if (this.includesPattern != null && !this.includesPattern.matcher(event.getDetail()).matches()) {
                    if (log.isTraceEnabled()) {
                        log.trace("event does not match includes pattern [" + this.includesPattern + "]: " + event);
                    }
                    event = null;
                }
            }
        } catch (Exception e) {
            event = null;
            log.warn("Failed to convert syslog message [" + syslogMessage + "] to event: " + e);
        }

        return event;
    }

    protected Event convertLineParserRegEx(String syslogMessage) {
        Event event;
        try {
            Matcher matcher = this.parserRegex.matcher(syslogMessage);
            if (matcher.find() && (matcher.groupCount() == 3)) {
                String dateTimeString = matcher.group(1);
                String severityString = matcher.group(2);
                String detailsString = matcher.group(3);
                long timestamp = getTimestamp(dateTimeString);
                EventSeverity severity = getSeverity(severityString);
                event = new Event(EVENT_LOG_TYPE, this.sourceLocation, timestamp, severity, detailsString);
            } else {
                event = null;
                if (log.isTraceEnabled()) {
                    log.trace("Message [" + syslogMessage + "] did not match parser regex: "
                        + this.parserRegex.pattern());
                }
            }
        } catch (Exception e) {
            event = null;
            if (log.isTraceEnabled()) {
                log.trace("Failed to parse [" + syslogMessage + "] with [" + this.parserRegex.pattern() + "]", e);
            }
        }
        return event;
    }

    protected Event convertLineDefaultFormat(String syslogMessage) {
        // "%timegenerated:::date-rfc3339%,%syslogpriority-text%,%syslogfacility-text%:%msg%\n"

        Event event;
        try {
            String[] messageParts = syslogMessage.split("\\,", 3);
            if (messageParts == null || messageParts.length < 3) {
                return null; // message doesn't seem to match the syntax of our default format
            }

            String dateTimeString = messageParts[0];
            String severityString = messageParts[1];
            String detailsString = messageParts[2];

            long timestamp = getTimestamp(dateTimeString);
            EventSeverity severity = getSeverity(severityString);
            event = new Event(EVENT_LOG_TYPE, this.sourceLocation, timestamp, severity, detailsString);
        } catch (Exception e) {
            event = null;
            if (log.isTraceEnabled()) {
                log.trace("defaultFormat: Failed to convert syslog message [" + syslogMessage + "] to event: " + e);
            }
        }
        return event;
    }

    protected Event convertAnyLine(String syslogMessage) {
        Event event;
        try {
            long timestamp = System.currentTimeMillis();
            EventSeverity severity = EventSeverity.INFO;
            event = new Event(EVENT_LOG_TYPE, this.sourceLocation, timestamp, severity, syslogMessage);
        } catch (Exception e) {
            event = null;
            if (log.isTraceEnabled()) {
                log.trace("anyLine: Failed to convert syslog message [" + syslogMessage + "] to event: " + e);
            }
        }
        return event;
    }

    /**
     * Given a date/time stamp, this will parse it using the configured date/time format.
     * If no format is specified or an error occurs, the current time will be returned.
     * However, if no format is specified but the date string is specified in RFC3339 format,
     * this method will parse it as such.
     * RFC3339 format is like "YYYY-MM-DDTHH:mm:ss-00:00" where 'T' separates the date and time.
     * 
     * @param dateTimeString the date-time string to parse and return its epoch millis representation
     * @return the epoch millis that corresponds to the given time or current time otherwise
     */
    protected long getTimestamp(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.length() == 0) {
            return System.currentTimeMillis();
        }

        if (this.dateTimeFormatter == null) {
            if (dateTimeString.length() < 19 || dateTimeString.charAt(10) != 'T') {
                return System.currentTimeMillis();
            } else {
                try {
                    return parseRFC3339Date(dateTimeString).getTime();
                } catch (Exception e) {
                    return System.currentTimeMillis(); // we gave it a shot, but it still didn't parse properly
                }
            }
        }

        try {
            Date date = this.dateTimeFormatter.parse(dateTimeString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            if (cal.get(Calendar.YEAR) < 2000) {
                // the log message probably didn't have the year in it, set it to our current year
                cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
            }
            return cal.getTimeInMillis();
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to parse date/time [" + dateTimeString + "] with format ["
                    + this.dateTimeFormatter.toPattern() + "]. " + e);
            }
            return System.currentTimeMillis();
        }
    }

    /**
     * Given a severity string, returns the severity enum.
     * 
     * @param severityString
     * 
     * @return enum
     */
    protected EventSeverity getSeverity(String severityString) {
        EventSeverity severity = EventSeverity.INFO; // if we don't know it, flag it as INFO
        if (severityString != null) {
            severityString = severityString.toUpperCase();
            if (severityString.startsWith("EMERG") || severityString.startsWith("CRIT")) {
                severity = EventSeverity.FATAL;
            } else if (severityString.startsWith("ERR")) {
                severity = EventSeverity.ERROR;
            } else if (severityString.startsWith("WARN")) {
                severity = EventSeverity.WARN;
            } else if (severityString.startsWith("NOTICE") || severityString.startsWith("INFO")) {
                severity = EventSeverity.INFO;
            } else if (severityString.startsWith("DEBUG")) {
                severity = EventSeverity.DEBUG;
            }
        }
        return severity;
    }

    /**
     * Parses RFC3339 dates, but assumes the given RFC3339 represents the same timezone
     * as our default (which it should be, the syslog entry is on the same box as we are).
     * This makes the parsing easier and quicker. If the string cannot be parsed,
     * the current time is returned.
     */
    private static SimpleDateFormat RFC3339_FORMAT_ZULU;
    private static SimpleDateFormat RFC3339_FORMAT;
    static {
        try {
            RFC3339_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            RFC3339_FORMAT_ZULU = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            RFC3339_FORMAT_ZULU.setTimeZone(TimeZone.getTimeZone("UTC"));
        } catch (Throwable t) {
            LogFactory.getLog(SyslogProcessor.class).warn("Cannot setup rfc3999 formats", t);
        }
    }

    protected Date parseRFC3339Date(String rfc3339String) throws Exception {
        // we will truncate the millis since there seems to be a VM bug or at least odd behavior when parsing millis.
        // also need to remove the : from the timezone offset if it exists, SimpleDateFormat doesn't like it
        // end up with the format: yyyy-MM-ddTHH:mm:ss(+|-)####
        rfc3339String = rfc3339String.replaceFirst("\\.\\d*", ""); // remove millis

        // if there is no timezone, parse it as if in zulu timezone
        if (rfc3339String.endsWith("Z")) {
            Date date = RFC3339_FORMAT_ZULU.parse(rfc3339String);
            return date;
        }

        // remove any colon found in timezone and parse it
        if (rfc3339String.matches(".*[\\+\\-]\\d\\d:\\d\\d.*")) {
            int length = rfc3339String.length();
            rfc3339String = rfc3339String.substring(0, length - 3) + rfc3339String.substring(length - 2);
        }
        Date date = RFC3339_FORMAT.parse(rfc3339String);
        return date;
    }
}
