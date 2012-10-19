/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.jbossas;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.plugins.jbossas.util.DeploymentUtility;
import org.rhq.plugins.jbossas.util.WarDeploymentInformation;
import org.rhq.plugins.jbossas.util.WarDiscoveryHelper;
import org.rhq.plugins.jmx.util.ObjectNameQueryUtility;

/**
* A resource component for managing a web application (WAR) deployed to a JBossAS server.
*
* @author Ian Springer
* @author Heiko W. Rupp
*/
public class WarComponent extends ApplicationComponent implements OperationFacet, DeleteResourceFacet {
    private static final String SERVLET_PREFIX = "Servlet.";
    public static final String CONTEXT_ROOT_CONFIG_PROP = "contextRoot";
    public static final String NAME_CONFIG_PROP = "name";
    public static final String FILE_NAME = "filename";
    public static final String JBOSS_WEB_NAME = "jbossWebName";
    public static final String RESPONSE_TIME_LOG_FILE_CONFIG_PROP = "responseTimeLogFile";
    public static final String RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP = "responseTimeUrlExcludes";
    public static final String RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP = "responseTimeUrlTransforms";

    private static final String RESPONSE_TIME_METRIC = "ResponseTime";
    private static final String CONTEXT_ROOT_METRIC = "ContextRoot";

    private static final String MAX_SERVLET_TIME = "Servlet.MaxResponseTime";
    private static final String MIN_SERVLET_TIME = "Servlet.MinResponseTime";
    private static final String AVG_SERVLET_TIME = "Servlet.AvgResponseTime";
    private static final String NUM_SERVLET_REQUESTS = "Servlet.NumRequests";
    private static final String NUM_SERVLET_ERRORS = "Servlet.NumErrors";
    private static final String TOTAL_TIME = "Servlet.TotalTime";
    private static final String SERVLET_NAME_BASE_TEMPLATE = "jboss.web:J2EEApplication=none,J2EEServer=none,j2eeType=Servlet,name=%name%";

    private static final String SESSION_NAME_BASE_TEMPLATE = "jboss.web:host=%HOST%,type=Manager,path=%PATH%";

    //WebModule=//localhost/test-simple,service=ClusterManager
    private static final String CLUSTER_SESSION_NAME_BASE_TEMPLATE = "jboss.web:service=ClusterManager,WebModule=//%HOST%%PATH%";
    private static final String SESSION_PREFIX = "Session.";
    private static final String VHOST_PREFIX = "Vhost";
    public static final String VHOST_CONFIG_PROP = "vHost";

    public static final String ROOT_WEBAPP_CONTEXT_ROOT = "/";

    private final Log log = LogFactory.getLog(this.getClass());

    //Mapping non-clustered names -> attribute name in the cluster manager
    private final String[] CLUSTER_SESSION_ATTRIBUTE_NAMES = { "maxInactiveInterval", "MaxInactiveInterval",
        "activeSessions", "ActiveSessionCount", "sessionCounter", "CreatedSessionCount", "sessionAverageAliveTime", "",
        "processingTime", "ProcessingTime", "maxActive", "MaxActiveSessionCount", "maxActiveSessions",
        "MaxActiveAllowed", "expiredSessions", "ExpiredSessionCount", "rejectedSessions", "RejectedSessionCount",
        "sessionIdLength", "SessionIdLength" };

    private EmsBean jbossWebMBean;
    private ResponseTimeLogParser logParser;
    private String vhost;
    private String contextRoot;

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType availability;
        if (this.jbossWebMBean != null) {
            int state = (Integer) this.jbossWebMBean.getAttribute("state").refresh();
            availability = (state == JBossWebMBeanState.STARTED) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } else {
            // The WAR has no jboss.web MBean associated with it - this means it
            // has no associated context root (i.e. it's not exposed via HTTP),
            // so consider it down.
            // @TODO In Embedded Console, we might want to call getJBossWebMBean() again to make sure. (See todo in that method for why)

            // Try to get the JBossWebMBean again
            // If you can't then set this to down.
            this.jbossWebMBean = getJBossWebMBean();
            if (this.jbossWebMBean == null) {
                availability = AvailabilityType.DOWN;
            } else {
                availability = getAvailability();
            }
        }

        return availability;
    }

    @Override
    public void start(ResourceContext resourceContext) {
        super.start(resourceContext);
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        this.jbossWebMBean = getJBossWebMBean();
        this.vhost = pluginConfig.getSimple(VHOST_CONFIG_PROP).getStringValue();
        this.contextRoot = pluginConfig.getSimple(CONTEXT_ROOT_CONFIG_PROP).getStringValue();
        ResponseTimeConfiguration responseTimeConfig = new ResponseTimeConfiguration(pluginConfig);

        File logFile = responseTimeConfig.getLogFile();
        if (logFile != null) {
            this.logParser = new ResponseTimeLogParser(logFile);
            this.logParser.setExcludes(responseTimeConfig.getExcludes());
            this.logParser.setTransforms(responseTimeConfig.getTransforms());
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) {
        Set<MeasurementScheduleRequest> remainingSchedules = new LinkedHashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest schedule : schedules) {
            String metricName = schedule.getName();
            if (metricName.equals(RESPONSE_TIME_METRIC)) {
                if (this.logParser != null) {
                    try {
                        CallTimeData callTimeData = new CallTimeData(schedule);
                        this.logParser.parseLog(callTimeData);
                        report.addData(callTimeData);
                    } catch (Exception e) {
                        log.error("Failed to retrieve HTTP call-time data.", e);
                    }
                } else {
                    log.error("The '" + RESPONSE_TIME_METRIC + "' metric is enabled for WAR resource '"
                        + getApplicationName() + "', but no value is defined for the '"
                        + RESPONSE_TIME_LOG_FILE_CONFIG_PROP + "' connection property.");
                    // TODO: Communicate this error back to the server for display in the GUI.
                }
            } else if (metricName.equals(CONTEXT_ROOT_METRIC)) {
                MeasurementDataTrait trait = new MeasurementDataTrait(schedule, contextRoot);
                report.addData(trait);
            } else if (metricName.startsWith(SERVLET_PREFIX)) {
                Double value = getServletMetric(metricName);
                MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, value);
                report.addData(metric);
            } else if (metricName.startsWith(SESSION_PREFIX)) {
                Double value = getSessionMetric(metricName);
                MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, value);
                report.addData(metric);
            } else if (metricName.startsWith(VHOST_PREFIX)) {
                if (metricName.equals("Vhost.name")) {
                    List<EmsBean> beans = getVHosts(contextRoot);
                    String value = "";
                    Iterator<EmsBean> iter = beans.iterator();
                    while (iter.hasNext()) {
                        EmsBean eBean = iter.next();
                        value += eBean.getBeanName().getKeyProperty("host");
                        if (iter.hasNext())
                            value += ",";
                    }
                    MeasurementDataTrait trait = new MeasurementDataTrait(schedule, value);
                    report.addData(trait);
                }
            } else {
                remainingSchedules.add(schedule);
            }
        }
        super.getValues(report, remainingSchedules);
    }

    private Double getSessionMetric(String metricName) {
        boolean isClustered = false;

        EmsConnection jmxConnection = getEmsConnection();
        String servletMBeanNames = SESSION_NAME_BASE_TEMPLATE.replace("%PATH%",
            WarDiscoveryHelper.getContextPath(this.contextRoot));
        servletMBeanNames = servletMBeanNames.replace("%HOST%", vhost);
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(servletMBeanNames);
        List<EmsBean> mBeans = jmxConnection.queryBeans(queryUtility.getTranslatedQuery());

        if (mBeans.size() == 0) {
            // retry with the cluster manager TODO select the local vs cluster mode on discovery
            servletMBeanNames = CLUSTER_SESSION_NAME_BASE_TEMPLATE.replace("%PATH%",
                WarDiscoveryHelper.getContextPath(this.contextRoot));
            servletMBeanNames = servletMBeanNames.replace("%HOST%", vhost);
            queryUtility = new ObjectNameQueryUtility(servletMBeanNames);
            mBeans = jmxConnection.queryBeans(queryUtility.getTranslatedQuery());
            if (mBeans.size() > 0)
                isClustered = true;
        }

        String property = metricName.substring(SESSION_PREFIX.length());

        Double ret = Double.NaN;

        if (mBeans.size() > 0) { // TODO flag error if != 1 ?
            EmsBean eBean = mBeans.get(0);
            eBean.refreshAttributes();

            if (isClustered) {
                property = lookupClusteredAttributeName(property);
            }

            EmsAttribute att = eBean.getAttribute(property);
            if (att != null) {
                Object o = att.getValue();
                if (o instanceof Long) {
                    Long l = (Long) o;
                    ret = Double.valueOf(l);
                } else {
                    Integer i = (Integer) o;
                    ret = Double.valueOf(i);
                }
            }

        }
        return ret;
    }

    private String lookupClusteredAttributeName(String property) {
        for (int i = 0; i < CLUSTER_SESSION_ATTRIBUTE_NAMES.length; i += 2) {
            if (CLUSTER_SESSION_ATTRIBUTE_NAMES[i].equals(property))
                return CLUSTER_SESSION_ATTRIBUTE_NAMES[i + 1];
        }
        return property;
    }

    private Double getServletMetric(String metricName) {
        String servletMBeanNames = SERVLET_NAME_BASE_TEMPLATE + ",WebModule=//" + this.vhost
            + WarDiscoveryHelper.getContextPath(this.contextRoot);
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(servletMBeanNames);
        List<EmsBean> mBeans = getEmsConnection().queryBeans(queryUtility.getTranslatedQuery());

        long min = Long.MAX_VALUE;
        long max = 0;
        long processingTime = 0;
        int requestCount = 0;
        int errorCount = 0;
        Double result;

        for (EmsBean mBean : mBeans) {
            mBean.refreshAttributes();
            if (metricName.equals(MIN_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("minTime");
                Long l = (Long) att.getValue();
                if (l < min)
                    min = l;
            } else if (metricName.equals(MAX_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("maxTime");
                Long l = (Long) att.getValue();
                if (l > max)
                    max = l;
            } else if (metricName.equals(AVG_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("processingTime");
                Long l = (Long) att.getValue();
                processingTime += l;
                att = mBean.getAttribute("requestCount");
                Integer i = (Integer) att.getValue();
                requestCount += i;
            } else if (metricName.equals(NUM_SERVLET_REQUESTS)) {
                EmsAttribute att = mBean.getAttribute("requestCount");
                Integer i = (Integer) att.getValue();
                requestCount += i;
            } else if (metricName.equals(NUM_SERVLET_ERRORS)) {
                EmsAttribute att = mBean.getAttribute("errorCount");
                Integer i = (Integer) att.getValue();
                errorCount += i;
            } else if (metricName.equals(TOTAL_TIME)) {
                EmsAttribute att = mBean.getAttribute("processingTime");
                Long l = (Long) att.getValue();
                processingTime += l;
            }
        }
        if (metricName.equals(AVG_SERVLET_TIME)) {
            result = (requestCount > 0) ? ((double) processingTime / (double) requestCount) : Double.NaN;
        } else if (metricName.equals(MIN_SERVLET_TIME)) {
            result = (min != Long.MAX_VALUE) ? (double) min : Double.NaN;
        } else if (metricName.equals(MAX_SERVLET_TIME)) {
            result = (max != 0) ? (double) max : Double.NaN;
        } else if (metricName.equals(NUM_SERVLET_ERRORS)) {
            result = (double) errorCount;
        } else if (metricName.equals(NUM_SERVLET_REQUESTS)) {
            result = (double) requestCount;
        } else if (metricName.equals(TOTAL_TIME)) {
            result = (double) processingTime;
        } else {
            // fallback
            result = Double.NaN;
        }

        return result;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        WarOperation operation = getOperation(name);
        if (this.jbossWebMBean == null) {
            throw new IllegalStateException("Could not find jboss.web MBean for WAR '" + getApplicationName() + "'.");
        }

        if (operation == WarOperation.REVERT) {
            try {
                revertFromBackupFile();
                return new OperationResult("Successfully reverted from backup");
            } catch (Exception e) {
                throw new RuntimeException("Error reverting from Backup: " + e.getMessage());
            }
        }

        // The following are MBean operations.
        EmsOperation mbeanOperation = this.jbossWebMBean.getOperation(name);
        if (mbeanOperation == null) {
            throw new IllegalStateException("Operation [" + name + "] not found on bean ["
                + this.jbossWebMBean.getBeanName() + "]");
        }

        // NOTE: None of the supported operations have any parameters or return values, which makes our job easier.
        Object[] paramValues = new Object[0];
        mbeanOperation.invoke(paramValues);
        int state = (Integer) this.jbossWebMBean.getAttribute("state").refresh();
        int expectedState = getExpectedPostExecutionState(operation);
        if (state != expectedState) {
            throw new Exception("Failed to " + name + " webapp (value of the 'state' attribute of MBean '"
                + this.jbossWebMBean.getBeanName() + "' is " + state + ", not " + expectedState + ").");
        }

        return new OperationResult();
    }

    private static int getExpectedPostExecutionState(WarOperation operation) {
        int expectedState;
        switch (operation) {
        case START:
        case RELOAD: {
            expectedState = JBossWebMBeanState.STARTED;
            break;
        }

        case STOP: {
            expectedState = JBossWebMBeanState.STOPPED;
            break;
        }

        default: {
            throw new IllegalStateException("Unsupported operation: " + operation); // will never happen
        }
        }

        return expectedState;
    }

    private WarOperation getOperation(String name) {
        try {
            return WarOperation.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation name: " + name);
        }
    }

    /**
     * Returns the the jboss.web MBean associated with this WAR (e.g.
     * jboss.web:J2EEApplication=none,J2EEServer=none,j2eeType=WebModule,name=//localhost/jmx-console), or null if the
     * WAR has no corresponding jboss.web MBean.
     *
     * <p/>This will return null only in the rare case that a deployed WAR has no context root associated with it. An
     * example of this is ROOT.war in the RHQ Server. rhq.ear maps rhq-portal.war to "/" and overrides ROOT.war's
     * association with "/".
     *
     * @return the jboss.web MBean associated with this WAR (e.g.
     *         jboss.web:J2EEApplication=none,J2EEServer=none,j2eeType=WebModule,name=//localhost/jmx-console), or null
     *         if the WAR has no corresponding jboss.web MBean
     */
    @Nullable
    private EmsBean getJBossWebMBean() {
        String jbossWebMBeanName = getJBossWebMBeanName();
        if (jbossWebMBeanName != null) {
            ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(jbossWebMBeanName);
            List<EmsBean> mBeans = getEmsConnection().queryBeans(queryUtility.getTranslatedQuery());
            // There should only be one mBean for this match.
            if (mBeans.size() == 1) {
                return mBeans.get(0);
            }
        } else {
            // This might be in Embedded in which during discovery it doesn't discover because they aren't deployed yet.
            // Might re-get, or let it get picked up the next time a discovery occurs.
        }
        return null;
    }

    @Nullable
    private String getJBossWebMBeanName() {
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        String jbossWebMBeanName = pluginConfig.getSimpleValue(WarComponent.JBOSS_WEB_NAME, null);
        if (jbossWebMBeanName == null) {
            WarDeploymentInformation deploymentInformation = getDeploymentInformation();
            if (deploymentInformation != null) {
                jbossWebMBeanName = deploymentInformation.getJbossWebModuleMBeanObjectName();
                WarDiscoveryHelper.setDeploymentInformation(pluginConfig, deploymentInformation);
            }
        }
        return jbossWebMBeanName;
    }

    /**
     * Virtual hosts for this app have the patten 'jboss.web:host=*,path=<ctx_root>,type=Manager'
     * @param contextRoot
     * @return
     */
    private List<EmsBean> getVHosts(String contextRoot) {

        return DeploymentUtility.getVHostsFromLocalManager(contextRoot, getEmsConnection());
    }

    private WarDeploymentInformation getDeploymentInformation() {
        WarDeploymentInformation deploymentInformation = null;
        EmsBean mBean = this.getEmsBean();
        if (mBean != null && mBean.getBeanName() != null) {
            String beanName = this.getEmsBean().getBeanName().getCanonicalName();
            List<String> beanNameList = new ArrayList<String>();
            beanNameList.add(beanName);
            //            deploymentInformation = DeploymentUtility.getWarDeploymentInformation(getEmsConnection(), beanNameList)
            //                .get(beanName); TODO fix this
        }
        return deploymentInformation;
    }

    private enum WarOperation {
        START, STOP, RELOAD, REVERT
    }

    private interface JBossWebMBeanState {
        int STOPPED = 0;
        int STARTED = 1;
    }
}
