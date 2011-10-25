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
package org.rhq.enterprise.gui.admin.config;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.enterprise.gui.legacy.StringConstants;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.util.NumberConstants;

public class SystemConfigForm extends BaseValidatorForm {
    private String baseUrl = "";
    private String agentMaxQuietTimeAllowed = "";
    private String agentMaxQuietTimeAllowedVal = "0";
    private boolean enableAgentAutoUpdate = false;
    private boolean enableDebugMode = false;
    private boolean enableExperimentalFeatures = false;
    private String helpUserId = "";
    private String helpPassword = "";
    private String maintIntervalVal = "0";
    private String maintInterval = "";
    private String rtPurgeVal = "0";
    private String rtPurge = "";
    private String alertPurgeVal = "0";
    private String alertPurge = "";
    private String eventPurgeVal = "0";
    private String eventPurge = "";
    private String traitPurgeVal = "0";
    private String traitPurge = "";
    private String availPurgeVal = "0";
    private String availPurge = "";
    private String baselineFrequencyVal = "0";
    private String baselineFrequency = "";
    private String baselineDataSetVal = "0";
    private String baselineDataSet = "";
    private String ldapUrl = "";
    private boolean ldapSsl = false;
    private String ldapLoginProperty = "";
    private String ldapSearchBase = "";
    private String ldapSearchFilter = "";
    private String ldapGroupFilter = "";
    private String ldapGroupMember = "";
    private String ldapUsername = "";
    private String ldapPassword = "";
    private Boolean ldapEnabled = null;
    private boolean reindex = false;
    private boolean allowResourceGenericPropertiesUpgrade;
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());

        buf.append(" baseUrl=").append(baseUrl);
        buf.append(" agentMaxQuietTimeAllowed=").append(agentMaxQuietTimeAllowed);
        buf.append(" enableAgentAutoUpdate=").append(enableAgentAutoUpdate);
        buf.append(" enableDebugMode=").append(enableDebugMode);
        buf.append(" enableExperimentalFeatures=").append(enableExperimentalFeatures);
        buf.append(" helpUserId=").append(helpUserId);
        buf.append(" helpPassword=").append(helpPassword);
        buf.append(" ldapEnabled=").append(ldapEnabled);
        buf.append(" ldapUrl=").append(ldapUrl);
        buf.append(" ldapSsl=").append(ldapSsl);
        buf.append(" ldapLoginProperty=").append(ldapLoginProperty);
        buf.append(" ldapSearchBase=").append(ldapSearchBase);
        buf.append(" ldapSearchFilter=").append(ldapSearchFilter);
        buf.append(" ldapUsername=").append(ldapUsername);
        buf.append(" ldapPassword=").append(ldapPassword);
        buf.append(" ldapGroupFilter=").append(ldapGroupFilter);
        buf.append(" ldapGroupMember=").append(ldapGroupMember);

        return buf.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.struts.action.ActionForm#reset(org.apache.struts.action.ActionMapping,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        agentMaxQuietTimeAllowed = "";
        agentMaxQuietTimeAllowedVal = null;
        enableAgentAutoUpdate = true;
        enableDebugMode = false;
        enableExperimentalFeatures = false;
        helpUserId = "";
        helpPassword = "";
        maintInterval = "";
        maintIntervalVal = null;
        rtPurge = "";
        rtPurgeVal = null;
        alertPurge = "";
        alertPurgeVal = null;
        eventPurge = "";
        eventPurgeVal = null;
        traitPurge = "";
        traitPurgeVal = null;
        availPurge = "";
        availPurgeVal = null;
        baselineFrequency = "";
        baselineFrequencyVal = null;
        baselineDataSet = "";
        baselineDataSetVal = null;
        ldapEnabled = null;
        ldapUrl = "";
        ldapSsl = false;
        ldapLoginProperty = "";
        ldapSearchBase = "";
        ldapSearchFilter = "";
        ldapUsername = "";
        ldapPassword = "";
        ldapGroupFilter = "";
        ldapGroupMember = "";

        super.reset(mapping, request);
    }

    public void loadConfigProperties(Properties prop) {
        baseUrl = prop.getProperty(RHQConstants.BaseURL);
        helpUserId = prop.getProperty(RHQConstants.HelpUser);
        helpPassword = prop.getProperty(RHQConstants.HelpUserPassword);

        String agentMaxQuietTimeAllowedValStr = prop.getProperty(RHQConstants.AgentMaxQuietTimeAllowed);
        Long agentMaxQuietTimeAllowedLong = new Long(agentMaxQuietTimeAllowedValStr);
        agentMaxQuietTimeAllowed = findTimeUnit(agentMaxQuietTimeAllowedLong.longValue());
        agentMaxQuietTimeAllowedVal = calcTimeUnit(agentMaxQuietTimeAllowedLong.longValue());

        String enableAgentAutoUpdateStr = prop.getProperty(RHQConstants.EnableAgentAutoUpdate);
        if (enableAgentAutoUpdateStr == null) {
            enableAgentAutoUpdateStr = Boolean.FALSE.toString();
        } else if (enableAgentAutoUpdateStr.equals("0")) {
            enableAgentAutoUpdateStr = Boolean.FALSE.toString();
        } else if (enableAgentAutoUpdateStr.equals("1")) {
            enableAgentAutoUpdateStr = Boolean.TRUE.toString();
        }
        enableAgentAutoUpdate = Boolean.valueOf(enableAgentAutoUpdateStr).booleanValue();

        String enableDebugModeStr = prop.getProperty(RHQConstants.EnableDebugMode);
        if (enableDebugModeStr == null) {
            enableDebugModeStr = Boolean.FALSE.toString();
        } else if (enableDebugModeStr.equals("0")) {
            enableDebugModeStr = Boolean.FALSE.toString();
        } else if (enableDebugModeStr.equals("1")) {
            enableDebugModeStr = Boolean.TRUE.toString();
        }
        enableDebugMode = Boolean.valueOf(enableDebugModeStr).booleanValue();

        String enableExperimentalFeaturesStr = prop.getProperty(RHQConstants.EnableExperimentalFeatures);
        if (enableExperimentalFeaturesStr == null) {
            enableExperimentalFeaturesStr = Boolean.FALSE.toString();
        } else if (enableExperimentalFeaturesStr.equals("0")) {
            enableExperimentalFeaturesStr = Boolean.FALSE.toString();
        } else if (enableExperimentalFeaturesStr.equals("1")) {
            enableExperimentalFeaturesStr = Boolean.TRUE.toString();
        }
        enableExperimentalFeatures = Boolean.valueOf(enableExperimentalFeaturesStr).booleanValue();

        String maintIntervalValStr = prop.getProperty(RHQConstants.DataMaintenance);
        Long maintIntervalLong = new Long(maintIntervalValStr);
        maintInterval = findTimeUnit(maintIntervalLong.longValue());
        maintIntervalVal = calcTimeUnit(maintIntervalLong.longValue());

        String nightlyReindexStr = prop.getProperty(RHQConstants.DataReindex);
        if (nightlyReindexStr == null) {
            nightlyReindexStr = Boolean.FALSE.toString();
        } else if (nightlyReindexStr.equals("0")) {
            nightlyReindexStr = Boolean.FALSE.toString();
        } else if (nightlyReindexStr.equals("1")) {
            nightlyReindexStr = Boolean.TRUE.toString();
        }
        reindex = Boolean.valueOf(nightlyReindexStr).booleanValue();

        String rtPurgeValStr = prop.getProperty(RHQConstants.RtDataPurge);
        Long rtPurgeLong = new Long(rtPurgeValStr);
        rtPurge = findTimeUnit(rtPurgeLong.longValue());
        rtPurgeVal = calcTimeUnit(rtPurgeLong.longValue());

        String alertPurgeValStr = prop.getProperty(RHQConstants.AlertPurge);
        Long alertPurgeLong = new Long(alertPurgeValStr);
        alertPurge = findTimeUnit(alertPurgeLong.longValue());
        alertPurgeVal = calcTimeUnit(alertPurgeLong.longValue());

        String eventPurgeValStr = prop.getProperty(RHQConstants.EventPurge);
        Long eventPurgeLong = new Long(eventPurgeValStr);
        eventPurge = findTimeUnit(eventPurgeLong.longValue());
        eventPurgeVal = calcTimeUnit(eventPurgeLong.longValue());

        String traitPurgeValStr = prop.getProperty(RHQConstants.TraitPurge);
        Long traitPurgeLong = new Long(traitPurgeValStr);
        traitPurge = findTimeUnit(traitPurgeLong.longValue());
        traitPurgeVal = calcTimeUnit(traitPurgeLong.longValue());

        String availPurgeValStr = prop.getProperty(RHQConstants.AvailabilityPurge);
        Long availPurgeLong = new Long(availPurgeValStr);
        availPurge = findTimeUnit(availPurgeLong.longValue());
        availPurgeVal = calcTimeUnit(availPurgeLong.longValue());

        String baselineFrequencyValStr = prop.getProperty(RHQConstants.BaselineFrequency);
        Long baselineFrequencyLong = new Long(baselineFrequencyValStr);
        baselineFrequency = findTimeUnit(baselineFrequencyLong.longValue());
        baselineFrequencyVal = calcTimeUnit(baselineFrequencyLong.longValue());

        String baselineDataSetValStr = prop.getProperty(RHQConstants.BaselineDataSet);
        Long baselineDataSetLong = new Long(baselineDataSetValStr);
        baselineDataSet = findTimeUnit(baselineDataSetLong.longValue());
        baselineDataSetVal = calcTimeUnit(baselineDataSetLong.longValue());

        ldapUrl = prop.getProperty(RHQConstants.LDAPUrl);
        ldapLoginProperty = prop.getProperty(RHQConstants.LDAPLoginProperty);
        ldapSearchBase = prop.getProperty(RHQConstants.LDAPBaseDN);
        ldapSearchFilter = prop.getProperty(RHQConstants.LDAPFilter);
        ldapGroupFilter = prop.getProperty(RHQConstants.LDAPGroupFilter);
        ldapGroupMember = prop.getProperty(RHQConstants.LDAPGroupMember);
        ldapUsername = prop.getProperty(RHQConstants.LDAPBindDN);
        ldapPassword = prop.getProperty(RHQConstants.LDAPBindPW);

        String value = prop.getProperty(SystemSetting.USE_SSL_FOR_LDAP.getInternalName());
        ldapSsl = Boolean.TRUE.toString().equals(value);

        value = prop.getProperty(SystemSetting.LDAP_BASED_JAAS_PROVIDER.getInternalName());
        ldapEnabled = (value != null) ? Boolean.valueOf(value) : false;

        String resourceGenericPropertiesUpgradeAllowed = prop.getProperty(RHQConstants.AllowResourceGenericPropertiesUpgrade);
        allowResourceGenericPropertiesUpgrade = Boolean.parseBoolean(resourceGenericPropertiesUpgradeAllowed);
    }

    /**
     * find the proper time unit associated with the timeUnit
     *
     * @return time unit label
     */
    private String findTimeUnit(long timeUnitInt) {
        if ((timeUnitInt % NumberConstants.DAYS) == 0) {
            return StringConstants.DAYS_LABEL;
        } else if ((timeUnitInt % NumberConstants.HOURS) == 0) {
            return StringConstants.HOURS_LABEL;
        } else {
            return StringConstants.MINUTES_LABEL;
        }
    }

    /**
     * find the proper time unit associated with the timeUnit
     *
     * @return time unit label
     */
    private String calcTimeUnit(long timeUnitInt) {
        if ((timeUnitInt % NumberConstants.DAYS) == 0) {
            return String.valueOf(timeUnitInt / NumberConstants.DAYS);
        } else if ((timeUnitInt % NumberConstants.HOURS) == 0) {
            return String.valueOf(timeUnitInt / NumberConstants.HOURS);
        } else {
            return String.valueOf(timeUnitInt / NumberConstants.MINUTES);
        }
    }

    /**
     * find the proper time unit associated with the timeUnit
     *
     * @return time unit label
     */
    private long convertToMillisecond(long val, String timeLabel) {
        if (timeLabel.equalsIgnoreCase(StringConstants.DAYS_LABEL)) {
            return val * NumberConstants.DAYS;
        } else if (timeLabel.equalsIgnoreCase(StringConstants.HOURS_LABEL)) {
            return val * NumberConstants.HOURS;
        } else {
            return val * NumberConstants.MINUTES;
        }
    }

    public Properties saveConfigProperties(Properties prop) {
        prop.setProperty(RHQConstants.BaseURL, baseUrl);
        prop.setProperty(RHQConstants.HelpUser, helpUserId);
        prop.setProperty(RHQConstants.HelpUserPassword, helpPassword);

        prop.setProperty(RHQConstants.DataReindex, String.valueOf(reindex));

        long agentMaxQuietTimeAllowedLong = convertToMillisecond(Integer.parseInt(agentMaxQuietTimeAllowedVal),
            agentMaxQuietTimeAllowed);
        prop.setProperty(RHQConstants.AgentMaxQuietTimeAllowed, String.valueOf(agentMaxQuietTimeAllowedLong));

        prop.setProperty(RHQConstants.EnableAgentAutoUpdate, String.valueOf(enableAgentAutoUpdate));

        prop.setProperty(RHQConstants.EnableDebugMode, String.valueOf(enableDebugMode));

        prop.setProperty(RHQConstants.EnableExperimentalFeatures, String.valueOf(enableExperimentalFeatures));

        long maintIntervalLong = convertToMillisecond(Integer.parseInt(maintIntervalVal), maintInterval);
        prop.setProperty(RHQConstants.DataMaintenance, String.valueOf(maintIntervalLong));

        long rtPurgeLong = convertToMillisecond(Long.parseLong(rtPurgeVal), rtPurge);
        prop.setProperty(RHQConstants.RtDataPurge, String.valueOf(rtPurgeLong));

        long alertPurgeLong = convertToMillisecond(Long.parseLong(alertPurgeVal), alertPurge);
        prop.setProperty(RHQConstants.AlertPurge, String.valueOf(alertPurgeLong));

        long eventPurgeLong = convertToMillisecond(Long.parseLong(eventPurgeVal), eventPurge);
        prop.setProperty(RHQConstants.EventPurge, String.valueOf(eventPurgeLong));

        long traitPurgeLong = convertToMillisecond(Long.parseLong(traitPurgeVal), traitPurge);
        prop.setProperty(RHQConstants.TraitPurge, String.valueOf(traitPurgeLong));

        long availPurgeLong = convertToMillisecond(Long.parseLong(availPurgeVal), availPurge);
        prop.setProperty(RHQConstants.AvailabilityPurge, String.valueOf(availPurgeLong));

        long baselineFrequencyLong = convertToMillisecond(Integer.parseInt(baselineFrequencyVal), baselineFrequency);
        prop.setProperty(RHQConstants.BaselineFrequency, String.valueOf(baselineFrequencyLong));

        long baselineDataSetLong = convertToMillisecond(Integer.parseInt(baselineDataSetVal), baselineDataSet);
        prop.setProperty(RHQConstants.BaselineDataSet, String.valueOf(baselineDataSetLong));

        prop.setProperty(RHQConstants.LDAPUrl, ldapUrl);
        prop.setProperty(RHQConstants.LDAPLoginProperty, ldapLoginProperty);
        prop.setProperty(RHQConstants.LDAPBaseDN, ldapSearchBase);
        prop.setProperty(RHQConstants.LDAPFilter, ldapSearchFilter);
        prop.setProperty(RHQConstants.LDAPGroupFilter, ldapGroupFilter);
        prop.setProperty(RHQConstants.LDAPGroupMember, ldapGroupMember);
        prop.setProperty(RHQConstants.LDAPBindDN, ldapUsername);
        prop.setProperty(RHQConstants.LDAPBindPW, ldapPassword);
        prop.setProperty(RHQConstants.LDAPProtocol, ldapSsl ? "ssl" : "");

        prop.setProperty(SystemSetting.LDAP_BASED_JAAS_PROVIDER.getInternalName(),
            (ldapEnabled != null) ? String.valueOf(ldapEnabled) : Boolean.FALSE.toString());

        prop.setProperty(RHQConstants.AllowResourceGenericPropertiesUpgrade, String.valueOf(allowResourceGenericPropertiesUpgrade));
        
        return prop;
    }

    public String getHelpPassword() {
        return helpPassword;
    }

    public String getHelpUserId() {
        return helpUserId;
    }

    public void setHelpPassword(String string) {
        helpPassword = string;
    }

    public void setHelpUserId(String string) {
        helpUserId = string;
    }

    public String getAgentMaxQuietTimeAllowed() {
        return agentMaxQuietTimeAllowed;
    }

    public void setAgentMaxQuietTimeAllowed(String string) {
        this.agentMaxQuietTimeAllowed = string;
    }

    public String getAgentMaxQuietTimeAllowedVal() {
        return agentMaxQuietTimeAllowedVal;
    }

    public void setAgentMaxQuietTimeAllowedVal(String string) {
        this.agentMaxQuietTimeAllowedVal = string;
    }

    public boolean getEnableAgentAutoUpdate() {
        return this.enableAgentAutoUpdate;
    }

    public void setEnableAgentAutoUpdate(boolean b) {
        this.enableAgentAutoUpdate = b;
    }

    public boolean getEnableDebugMode() {
        return this.enableDebugMode;
    }

    public void setEnableDebugMode(boolean b) {
        this.enableDebugMode = b;
    }

    public boolean getEnableExperimentalFeatures() {
        return this.enableExperimentalFeatures;
    }

    public void setEnableExperimentalFeatures(boolean b) {
        this.enableExperimentalFeatures = b;
    }

    public String getMaintIntervalVal() {
        return maintIntervalVal;
    }

    public void setMaintIntervalVal(String v) {
        maintIntervalVal = v;
    }

    public String getMaintInterval() {
        return maintInterval;
    }

    public void setMaintInterval(String s) {
        maintInterval = s;
    }

    public String getRtPurgeVal() {
        return rtPurgeVal;
    }

    public void setRtPurgeVal(String v) {
        rtPurgeVal = v;
    }

    public String getRtPurge() {
        return rtPurge;
    }

    public void setRtPurge(String s) {
        rtPurge = s;
    }

    public String getAlertPurgeVal() {
        return alertPurgeVal;
    }

    public void setAlertPurgeVal(String v) {
        alertPurgeVal = v;
    }

    public String getAlertPurge() {
        return alertPurge;
    }

    public void setAlertPurge(String s) {
        alertPurge = s;
    }

    public String getEventPurgeVal() {
        return eventPurgeVal;
    }

    public void setEventPurgeVal(String eventPurgeVal) {
        this.eventPurgeVal = eventPurgeVal;
    }

    public String getEventPurge() {
        return eventPurge;
    }

    public void setEventPurge(String eventPurge) {
        this.eventPurge = eventPurge;
    }

    public String getTraitPurgeVal() {
        return traitPurgeVal;
    }

    public void setTraitPurgeVal(String traitPurgeVal) {
        this.traitPurgeVal = traitPurgeVal;
    }

    public String getTraitPurge() {
        return traitPurge;
    }

    public void setTraitPurge(String traitPurge) {
        this.traitPurge = traitPurge;
    }

    public String getAvailPurgeVal() {
        return availPurgeVal;
    }

    public void setAvailPurgeVal(String availPurgeVal) {
        this.availPurgeVal = availPurgeVal;
    }

    public String getAvailPurge() {
        return availPurge;
    }

    public void setAvailPurge(String availPurge) {
        this.availPurge = availPurge;
    }

    public String getBaselineFrequencyVal() {
        return baselineFrequencyVal;
    }

    public void setBaselineFrequencyVal(String v) {
        baselineFrequencyVal = v;
    }

    public String getBaselineFrequency() {
        return baselineFrequency;
    }

    public void setBaselineFrequency(String s) {
        baselineFrequency = s;
    }

    public String getBaselineDataSetVal() {
        return baselineDataSetVal;
    }

    public void setBaselineDataSetVal(String v) {
        baselineDataSetVal = v;
    }

    public String getBaselineDataSet() {
        return baselineDataSet;
    }

    public void setBaselineDataSet(String s) {
        baselineDataSet = s;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String string) {
        baseUrl = string;
    }

    public Boolean getLdapEnabled() {
        return ldapEnabled;
    }

    public void setLdapEnabled(Boolean b) {
        if (b.booleanValue()) {
            ldapEnabled = Boolean.TRUE;
        }
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String s) {
        ldapUrl = s;
    }

    public boolean getLdapSsl() {
        return ldapSsl;
    }

    public void setLdapSsl(boolean b) {
        ldapSsl = b;
    }

    public String getLdapLoginProperty() {
        return ldapLoginProperty;
    }

    public void setLdapLoginProperty(String s) {
        ldapLoginProperty = s;
    }

    public String getLdapSearchBase() {
        return ldapSearchBase;
    }

    public void setLdapSearchBase(String s) {
        ldapSearchBase = s;
    }

    public String getLdapSearchFilter() {
        return ldapSearchFilter;
    }

    public void setLdapSearchFilter(String s) {
        ldapSearchFilter = s;
    }

    public String getLdapGroupFilter() {
        return ldapGroupFilter;
    }

    public void setLdapGroupFilter(String s) {
        ldapGroupFilter = s;
    }

    public String getLdapGroupMember() {
        return ldapGroupMember;
    }

    public void setLdapGroupMember(String s) {
        ldapGroupMember = s;
    }

    public String getLdapUsername() {
        return ldapUsername;
    }

    public void setLdapUsername(String s) {
        ldapUsername = s;
    }

    public String getLdapPassword() {
        return ldapPassword;
    }

    public void setLdapPassword(String s) {
        ldapPassword = s;
    }

    public boolean getReindex() {
        return reindex;
    }

    public void setReindex(boolean reindex) {
        this.reindex = reindex;
    }
    
    public boolean isAllowResourceGenericPropertiesUpgrade() {
        return allowResourceGenericPropertiesUpgrade;
    }

    public void setAllowResourceGenericPropertiesUpgrade(boolean allowResourceGenericPropertiesUpgrade) {
        this.allowResourceGenericPropertiesUpgrade = allowResourceGenericPropertiesUpgrade;
    }

    /* (non-Javadoc)
     * @see org.apache.struts.action.ActionForm#validate(org.apache.struts.action.ActionMapping,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if (errors == null) {
            errors = new ActionErrors();
        }

        if (!empty(baselineDataSetVal)) {
            try {
                long freq = Long.parseLong(baselineDataSetVal);
                // 1h table holds at most 14 days worth of data, make sure we don't set a dataset more than that
                if (freq > 14) {
                    ActionMessage errorMessage = new ActionMessage("admin.settings.BadBaselineDataSetVal");
                    errors.add("baselineDataSetVal", errorMessage);
                }
            } catch (Exception e) {
                ActionMessage errorMessage = new ActionMessage("admin.settings.BadBaselineDataSetVal");
                errors.add("baselineDataSetVal", errorMessage);
            }
        }

        if (!empty(agentMaxQuietTimeAllowedVal)) {
            try {
                long val = Long.parseLong(agentMaxQuietTimeAllowedVal);
                // we should never allow a quiet time threshold to be less than 2 minutes
                if (val < 2) {
                    ActionMessage errorMessage = new ActionMessage("admin.settings.BadAgentMaxQuietTimeAllowedVal");
                    errors.add("agentMaxQuietTimeAllowedVal", errorMessage);
                }
            } catch (Exception e) {
                ActionMessage errorMessage = new ActionMessage("admin.settings.BadAgentMaxQuietTimeAllowedVal");
                errors.add("agentMaxQuietTimeAllowedVal", errorMessage);
            }
        }

        checkForBadNumber(errors, this.maintIntervalVal, "maintIntervalVal");
        checkForBadNumber(errors, this.rtPurgeVal, "rtPurgeVal");
        checkForBadNumber(errors, this.alertPurgeVal, "alertPurgeVal");
        checkForBadNumber(errors, this.eventPurgeVal, "eventPurgeVal");
        checkForBadNumber(errors, this.traitPurgeVal, "traitPurgeVal");
        checkForBadNumber(errors, this.availPurgeVal, "availPurgeVal");
        checkForBadNumber(errors, this.baselineFrequencyVal, "baselineFrequencyVal");

        if (errors.isEmpty()) {
            return null;
        } else {
            return errors;
        }
    }

    private void checkForBadNumber(ActionErrors errors, String val, String valVariableName) {
        if (!empty(val)) {
            try {
                Long.parseLong(val);
            } catch (Exception e) {
                ActionMessage errorMessage = new ActionMessage("admin.settings.BadNumber");
                errors.add(valVariableName, errorMessage);
            }
        }
    }

    private boolean empty(String s) {
        return (s == null) || (s.length() == 0);
    }
}
