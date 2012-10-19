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
package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.jboss.sasl.util.UsernamePasswordHashUtil;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.modules.plugins.jbossas7.helper.HostConfiguration;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Base component for functionality that is common to Standalone Servers and Host Controllers.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
public abstract class BaseServerComponent<T extends ResourceComponent<?>> extends BaseComponent<T>
        implements MeasurementFacet {

    private static final String SEPARATOR = "\n-----------------------\n";

    final Log log = LogFactory.getLog(BaseServerComponent.class);

    private ASConnection connection;
    private LogFileEventResourceComponentHelper logFileEventDelegate;
    private StartScriptConfiguration startScriptConfig;
    private ServerPluginConfiguration serverPluginConfig;
    private AvailabilityType lastAvail;

    @Override
    public void start(ResourceContext<T> resourceContext) throws InvalidPluginConfigurationException, Exception {
        super.start(resourceContext);

        serverPluginConfig = new ServerPluginConfiguration(pluginConfiguration);
        connection = new ASConnection(serverPluginConfig.getHostname(), serverPluginConfig.getPort(),
                serverPluginConfig.getUser(), serverPluginConfig.getPassword());
        @SuppressWarnings("UnusedDeclaration")
        AvailabilityType avail = getAvailability();
        logFileEventDelegate = new LogFileEventResourceComponentHelper(context);
        logFileEventDelegate.startLogFileEventPollers();
        startScriptConfig = new StartScriptConfiguration(pluginConfiguration);
    }

    @Override
    public void stop() {
        logFileEventDelegate.stopLogFileEventPollers();
        lastAvail = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType avail;
        try {
            readAttribute("launch-type");
            avail = AvailabilityType.UP;
        } catch (Exception e) {
            avail = AvailabilityType.DOWN;
        }

        try {
            if ((avail == AvailabilityType.UP) && (lastAvail != AvailabilityType.UP)) {
                validateServerAttributes();
                log.info(getResourceDescription() + " has just come UP.");
            }
        } finally {
            lastAvail = avail;
        }

        return avail;
    }

    private void validateServerAttributes() throws InvalidPluginConfigurationException {
        // Validate the base dir (e.g. /opt/jboss-as-7.1.1.Final/standalone).
        File runtimeBaseDir;
        File baseDir=null;
        try {
            String runtimeBaseDirString = readAttribute(getEnvironmentAddress(), getBaseDirAttributeName());
            // Canonicalize both paths before comparing them!
            runtimeBaseDir = new File(runtimeBaseDirString).getCanonicalFile();
            File baseDirTmp = serverPluginConfig.getBaseDir();
            if (baseDirTmp != null) { // may be null for manually added servers
                baseDir = baseDirTmp.getCanonicalFile();
            }
        } catch (Exception e) {
            runtimeBaseDir = null;
            baseDir = null;
            log.error("Failed to validate base dir for " + getResourceDescription() + ".", e);
        }
        if ((runtimeBaseDir != null) && (baseDir != null)) {
            if(!runtimeBaseDir.equals(baseDir)) {
                throw new InvalidPluginConfigurationException("The server listening on "
                        + serverPluginConfig.getHostname() + ":" + serverPluginConfig.getPort()
                        + " has base dir [" + runtimeBaseDir + "], but the base dir we expected was [" + baseDir
                        + "]. Perhaps the management hostname or port has been changed for the server with base dir ["
                        + baseDir + "].");
            }
        }

        // Validate the mode (e.g. STANDALONE or DOMAIN).
        String runtimeMode;
        try {
            runtimeMode = readAttribute("launch-type");
        } catch (Exception e) {
            runtimeMode = null;
            log.error("Failed to validate mode for " + getResourceDescription() + ".", e);
        }
        if (runtimeMode != null) {
            String mode = getMode().name();
            if(!runtimeMode.equals(mode)) {
                throw new InvalidPluginConfigurationException("The original mode discovered for this AS7 server was " +
                        getMode() + ", but the server is now reporting its mode is [" + runtimeMode + "].");
            }
        }

        // Validate the product type (e.g. AS or EAP).
        JBossProductType runtimeType;
        try {
            String runtimeTypeString = readAttribute("product-name");
            runtimeType = (runtimeTypeString != null && !runtimeTypeString.isEmpty()) ?
                    JBossProductType.getValueByProductName(runtimeTypeString) : JBossProductType.AS;
        } catch (Exception e) {
            runtimeType = null;
            log.error("Failed to validate product type for " + getResourceDescription() + ".", e);
        }
        if (runtimeType != null) {
            JBossProductType type = serverPluginConfig.getProductType();
            if (runtimeType != type) {
                throw new InvalidPluginConfigurationException("The original product type discovered for this AS7 server was "
                        + type + ", but the server is now reporting its product type is [" + runtimeType + "].");
            }
        }
    }

    public ServerPluginConfiguration getServerPluginConfiguration() {
        return serverPluginConfig;
    }

    public StartScriptConfiguration getStartScriptConfiguration() {
        return startScriptConfig;
    }

    @Override
    public ASConnection getASConnection() {
        return connection;
    }

    // TODO: Refactor this - we should be able to mock the ResourceContext passed to start() instead.
    @Override
    public void setConnection(ASConnection connection) {
        this.connection = connection;
    }

    @NotNull
    protected abstract AS7Mode getMode();

    /**
     * Restart the server by first executing a 'shutdown' operation via the management API and then calling
     * the {@link #startServer} method to start it again.
     *
     * @param parameters Parameters to pass to the (recursive) invocation of #invokeOperation
     * @return State of execution
     * @throws Exception If anything goes wrong
     */
    protected OperationResult restartServer(Configuration parameters) throws Exception {

        OperationResult operationResult = new OperationResult();
        if (isManuallyAddedServer(operationResult, "Restarting")) {
            return operationResult;
        }

        List<String> errors = validateStartScriptPluginConfigProps();
        if (!errors.isEmpty()) {
            OperationResult result  = new OperationResult();
            setErrorMessage(result, errors);
            return result;
        }

        OperationResult tmp = invokeOperation("shutdown", parameters);

        if (tmp.getErrorMessage() != null) {
            tmp.setErrorMessage("Restart failed while attempting to shut down: " + tmp.getErrorMessage());
            return tmp;
        }

        context.getAvailabilityContext().requestAvailabilityCheck();

        return startServer();
    }

    protected boolean waitUntilDown(OperationResult tmp) throws InterruptedException {
        boolean down=false;
        int count=0;
        while (!down) {
            Operation op = new ReadAttribute(new Address(),"release-version");
            Result res = getASConnection().execute(op);
            if (!res.isSuccess()) { // If op succeeds, server is not down
                down=true;
            } else if (count > 20) {
                tmp.setErrorMessage("Was not able to shut down the server");
                return true;
            }
            if (!down) {
                Thread.sleep(1000); // Wait 1s
            }
            count++;
        }
        log.debug("waitUntilDown: Used " + count + " delay round(s) to shut down");
        return false;
    }

    /**
     * Start the server by calling the start script defined in the plugin configuration.
     *
     * @return the result of the operation
     */
    protected OperationResult startServer() {
        OperationResult operationResult = new OperationResult();
        if (isManuallyAddedServer(operationResult, "Starting")) {
            return operationResult;
        }

        List<String> errors = validateStartScriptPluginConfigProps();
        if (!errors.isEmpty()) {
            setErrorMessage(operationResult, errors);
            return operationResult;
        }

        String startScriptPrefix = startScriptConfig.getStartScriptPrefix();
        File startScriptFile = getStartScriptFile();
        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(startScriptPrefix,
                startScriptFile);

        List<String> arguments = processExecution.getArguments();
        if (arguments == null) {
            arguments = new ArrayList<String>();
            processExecution.setArguments(arguments);
        }

        List<String> startScriptArgs = startScriptConfig.getStartScriptArgs();
        for (String startScriptArg : startScriptArgs) {
            startScriptArg = replacePropertyPatterns(startScriptArg);
            arguments.add(startScriptArg);
        }

        Map<String, String> startScriptEnv = startScriptConfig.getStartScriptEnv();
        for (String envVarName : startScriptEnv.keySet()) {
            String envVarValue = startScriptEnv.get(envVarName);
            envVarValue = replacePropertyPatterns(envVarValue);
            startScriptEnv.put(envVarName, envVarValue);
        }
        processExecution.setEnvironmentVariables(startScriptEnv);

        // When running on Windows 9x, standalone.bat and domain.bat need the cwd to be the AS bin dir in order to find
        // standalone.bat.conf and domain.bat.conf respectively.
        processExecution.setWorkingDirectory(startScriptFile.getParent());
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(15000L); // 15 seconds // TODO: Should we wait longer than 15 seconds?
        processExecution.setKillOnTimeout(false);

        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        SystemInfo systemInfo = context.getSystemInformation();
        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        logExecutionResults(results);
        if (results.getError() != null) {
            operationResult.setErrorMessage(results.getError().getMessage());
        } else if (results.getExitCode() != null) {
            operationResult.setErrorMessage("Start failed with error code " + results.getExitCode() + ":\n" + results.getCapturedOutput());
        } else {
            // Try to connect to the server - ping once per second, timing out after 20s.
            boolean up = waitForServerToStart();
            if (up) {
                operationResult.setSimpleResult("Success");
            } else {
                operationResult.setErrorMessage("Was not able to start the server");
            }
        }
        context.getAvailabilityContext().requestAvailabilityCheck();

        return operationResult;
    }

    private boolean isManuallyAddedServer(OperationResult operationResult, String operation) {
        if (pluginConfiguration.get("manuallyAdded")!=null) {
            operationResult.setErrorMessage(operation + " is not enabled for manually added servers");
            return true;
        }
        return false;
    }

    private void setErrorMessage(OperationResult operationResult, List<String> errors) {
        StringBuilder buffer = new StringBuilder("This Resource's connection properties contain errors: ");
        for (int i = 0, errorsSize = errors.size(); i < errorsSize; i++) {
            if (i != 0) {
                buffer.append(", ");
            }
            String error = errors.get(i);
            buffer.append('[').append(error).append(']');
        }
        operationResult.setErrorMessage(buffer.toString());
    }

    private List<String> validateStartScriptPluginConfigProps() {
        List<String> errors = new ArrayList<String>();

        File startScriptFile = getStartScriptFile();

        if (!startScriptFile.exists()) {
            errors.add("Start script '" + startScriptFile + "' does not exist.");
        } else {
            if (!startScriptFile.isFile()) {
                errors.add("Start script '" + startScriptFile + "' is not a regular file.");
            } else {
                if (!startScriptFile.canRead()) {
                    errors.add("Start script '" + startScriptFile + "' is not readable.");
                }
                if (!startScriptFile.canExecute()) {
                    errors.add("Start script '" + startScriptFile + "' is not executable.");
                }
            }
        }

        Map<String, String> startScriptEnv = startScriptConfig.getStartScriptEnv();
        if (startScriptEnv.isEmpty()) {
            errors.add("No start script environment variables are set. At a minimum, PATH should be set "
                     + "(on UNIX, it should contain at least /bin and /usr/bin). It is recommended that "
                     + "JAVA_HOME also be set, otherwise the PATH will be used to find java.");
        }

        return errors;
    }

    private File getStartScriptFile() {
        File startScriptFile = startScriptConfig.getStartScript();
        File homeDir = serverPluginConfig.getHomeDir();
        if (startScriptFile != null) {
            if (!startScriptFile.isAbsolute()) {
                startScriptFile = new File(homeDir, startScriptFile.getPath());
            }
        } else {
            // Use the default start script.
            String startScriptFileName = getMode().getStartScriptFileName();
            File binDir = new File(homeDir, "bin");
            startScriptFile = new File(binDir, startScriptFileName);
        }
        return startScriptFile;
    }

    private boolean waitForServerToStart() {
        boolean up = false;
        int count = 0;
        while (!up) {
            Operation op = new ReadAttribute(new Address(), "release-version");
            Result res = getASConnection().execute(op);
            if (res.isSuccess()) { // If op succeeds, server is not down
                up = true;
            } else if (count > 20) {
                break;
            }
            if (!up) {
                try {
                    Thread.sleep(1000); // Wait 1s
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            count++;
        }
        return up;
    }

    private void logExecutionResults(ProcessExecutionResults results) {
        // Always log the output at info level. On Unix we could switch depending on a exitCode being !=0, but ...
        log.info("Exit code from process execution: " + results.getExitCode());
        log.info("Output from process execution: " + SEPARATOR + results.getCapturedOutput() + SEPARATOR);
    }

    /**
     * Do some post processing of the Result - especially the 'shutdown' operation needs a special
     * treatment.
     * @param name Name of the operation
     * @param res Result of the operation vs. AS7
     * @return OperationResult filled in from values of res
     */
    protected OperationResult postProcessResult(String name, Result res) {
        OperationResult operationResult = new OperationResult();
        if (res==null) {
            operationResult.setErrorMessage("No result received from server");
            return operationResult;
        }

        if (name.equals("shutdown") || name.equals("restart")) {
            /*
             * Shutdown needs a special treatment, because after sending the operation, if shutdown succeeds,
             * the server connection is closed and we can't read from it. So if we get connection refused for
             * reading, this is a good sign.
             */
            if (!res.isSuccess()) {
                if (res.getRhqThrowable()!=null && (res.getRhqThrowable() instanceof ConnectException || res.getRhqThrowable().getMessage().equals("Connection refused"))) {
                    operationResult.setSimpleResult("Success");
                    log.debug("Got a ConnectionRefused for operation " + name + " this is considered ok, as the remote server sometimes closes the communications channel before sending a reply");
                }
                if (res.getFailureDescription().contains("Socket closed")) { // See https://issues.jboss.org/browse/AS7-4192
                    operationResult.setSimpleResult("Success");
                    log.debug("Got a 'Socket closed' result from AS for operation " + name );
                }
                else
                    operationResult.setErrorMessage(res.getFailureDescription());
            }
            else {
                operationResult.setSimpleResult("Success");
            }
        }
        else {
            if (res.isSuccess()) {
                if (res.getResult()!=null)
                    operationResult.setSimpleResult(res.getResult().toString());
                else
                    operationResult.setSimpleResult("-None provided by server-");
            }
            else
                operationResult.setErrorMessage(res.getFailureDescription());
        }
        return operationResult;
    }

    protected OperationResult installManagementUser(Configuration parameters, Configuration pluginConfig) {
        String user = parameters.getSimpleValue("user", "");
        String password = parameters.getSimpleValue("password", "");

        OperationResult result = new OperationResult();

        PropertySimple remoteProp = pluginConfig.getSimple("manuallyAdded");
        if (remoteProp!=null && remoteProp.getBooleanValue()!= null && remoteProp.getBooleanValue()) {
            result.setErrorMessage("This is a manually added server. This operation can not be used to install a management user. Use the server's 'bin/add-user.sh'");
            return result;
        }

        if (user.isEmpty() || password.isEmpty()) {
            result.setErrorMessage("User and Password must not be empty");
            return result;
        }

        File baseDir = serverPluginConfig.getBaseDir();
        if (baseDir == null) {
            result.setErrorMessage("'" + ServerPluginConfiguration.Property.BASE_DIR + "' plugin config prop is not set.");
            return result;
        }

        File configFile = getHostConfigFile();
        HostConfiguration hostConfig;
        try {
            hostConfig = new HostConfiguration(configFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse configuration file [" + configFile + "].", e);
        }

        String realm = pluginConfig.getSimpleValue("realm", "ManagementRealm");
        File propertiesFile = hostConfig.getSecurityPropertyFile(baseDir, getMode(), realm);
        if (!propertiesFile.canWrite()) {
            result.setErrorMessage("Management users properties file [" + propertiesFile + "] is not writable.");
            return result;
        }

        String encryptedPassword;
        try {
            UsernamePasswordHashUtil hashUtil = new UsernamePasswordHashUtil();
            encryptedPassword = hashUtil.generateHashedHexURP(user, realm, password.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password.", e);
        }

        boolean userAlreadyExisted;
        try {
            PropertiesFileUpdate propsFileUpdate = new PropertiesFileUpdate(propertiesFile.getPath());
            userAlreadyExisted = propsFileUpdate.update(user, encryptedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update management users properties file [" + propertiesFile + "].",
                    e);
        }

        String verb = (userAlreadyExisted) ? "updated" : "added";
        result.setSimpleResult("Management user [" + user + "] " + verb + ".");
        log.info("Management user [" + user + "] " + verb + " for " + context.getResourceType().getName()
                + " server with key [" + context.getResourceKey() + "].");

        context.getAvailabilityContext().requestAvailabilityCheck();

        return result;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> skmRequests = new HashSet<MeasurementScheduleRequest>(requests.size());
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request: requests) {
            String requestName = request.getName();
            if (requestName.equals("startTime")) {
                collectStartTimeTrait(report, request);
            } else if (requestName.startsWith("_skm:")) { // handled below
                skmRequests.add(request);
            } else {
                leftovers.add(request); // handled below
            }
        }

        // Now handle the server kind traits.
        if (skmRequests.size() > 0) {
            collectServerKindTraits(report, skmRequests);
        }

        // Finally let our superclass handle the leftovers.
        super.getValues(report, leftovers);
    }

    private void collectStartTimeTrait(MeasurementReport report, MeasurementScheduleRequest request) {
        Address address = new Address(getHostAddress());
        address.add("core-service", "platform-mbean");
        address.add("type", "runtime");
        Long startTime;
        try {
            startTime = readAttribute(address, "start-time", Long.class);
        } catch (Exception e) {
            startTime = null;
        }

        if (startTime != null) {
            MeasurementDataTrait data = new MeasurementDataTrait(request, new Date(startTime).toString());
            report.addData(data);
        }
    }

    @NotNull
    protected abstract Address getEnvironmentAddress();

    @NotNull
    protected abstract Address getHostAddress();

    @NotNull
    protected abstract String getBaseDirAttributeName();

    protected void collectConfigTrait(MeasurementReport report, MeasurementScheduleRequest request) {
        String config;
        try {
            config = readAttribute(getEnvironmentAddress(), request.getName(), String.class);
        } catch (Exception e) {
            log.error("Failed to read attribute [" + request.getName() + "]: " + e, e);
            config = null;
        }

        if (config != null) {
            MeasurementDataTrait data = new MeasurementDataTrait(request, new File(config).getName());
            report.addData(data);
        }
    }

    private File getHostConfigFile() {
        File configFile;
        try {
            String config = readAttribute(getEnvironmentAddress(), getMode().getHostConfigAttributeName());
            configFile = new File(config);
        } catch (Exception e) {
            // This probably means the server is not running and/or authentication is not set up. Fallback to the
            // host config file set in the plugin config during discovery.
            // TODO (ips, 05/05/12): This is not ideal, because the user could have restarted the server with a
            //                       different config file, since the time it was imported into inventory. The better
            //                       thing to do here is to find the current server process and parse its command line
            //                       to find the current config file name.
            configFile = serverPluginConfig.getHostConfigFile();
            if (configFile == null) {
                throw new RuntimeException("Failed to determine config file path.", e);
            }
        }
        return configFile;
    }

    private void collectServerKindTraits(MeasurementReport report, Set<MeasurementScheduleRequest> skmRequests) {
        Address address = new Address();
        ReadResource op = new ReadResource(address);
        op.includeRuntime(true);
        ComplexResult res = getASConnection().executeComplex(op);
        if (res.isSuccess()) {
            Map<String,Object> props = res.getResult();

            for (MeasurementScheduleRequest request: skmRequests) {
                String requestName = request.getName();
                String realName = requestName.substring(requestName.indexOf(':') + 1);
                String val=null;
                if (props.containsKey(realName)) {
                    val = getStringValue( props.get(realName) );
                }

                if ("null".equals(val)) {
                    if (realName.equals("product-name"))
                        val = "JBoss AS";
                    else if (realName.equals("product-version"))
                        val = getStringValue(props.get("release-version"));
                    else
                        log.debug("Value for " + realName + " was 'null' and no replacement found");
                }
                MeasurementDataTrait data = new MeasurementDataTrait(request,val);
                report.addData(data);
            }
        } else {
            log.debug("getSKMRequests failed: " + res.getFailureDescription());
        }
    }

    // Replace any "%xxx%" substrings with the values of plugin config props "xxx".
    private String replacePropertyPatterns(String value) {
        Pattern pattern = Pattern.compile("(%([^%]*)%)");
        Matcher matcher = pattern.matcher(value);
        Configuration pluginConfig = context.getPluginConfiguration();
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String propName = matcher.group(2);
            PropertySimple prop = pluginConfig.getSimple(propName);
            String propValue = ((prop != null) && (prop.getStringValue() != null)) ? prop.getStringValue() : "";
            String propPattern = matcher.group(1);
            String replacement = (prop != null) ? propValue : propPattern;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
