/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Remove;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * The base class for all AS7 resource components.
 *
 * @param <T> the type of the component's parent resource component
 */
public class BaseComponent<T extends ResourceComponent<?>> implements AS7Component<T>, MeasurementFacet,
    ConfigurationFacet, DeleteResourceFacet, CreateChildResourceFacet, OperationFacet  {

    private static final String INTERNAL = "_internal:";
    private static final int INTERNAL_SIZE = INTERNAL.length();
    public static final String MANAGED_SERVER = "Managed Server";

    final Log log = LogFactory.getLog(this.getClass());

    ResourceContext<T> context;
    Configuration pluginConfiguration;
    String myServerName;

    String path;
    Address address;
    String key;
    boolean includeRuntime;

    private boolean verbose = ASConnection.verbose;
    private BaseServerComponent serverComponent;
    protected ASConnection testConnection;

    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception {
        this.context = context;
        pluginConfiguration = context.getPluginConfiguration();
        serverComponent = findServerComponent();
        path = pluginConfiguration.getSimpleValue("path");
        address = new Address(path);
        key = context.getResourceKey();
        myServerName = context.getResourceKey().substring(context.getResourceKey().lastIndexOf("/") + 1);

        PropertySimple includeRuntimeProperty = pluginConfiguration.getSimple("includeRuntime");
        if (includeRuntimeProperty != null && includeRuntimeProperty.getBooleanValue() != null
            && includeRuntimeProperty.getBooleanValue()) {
            includeRuntime = true;
        } else {
            includeRuntime = false;
        }
    }

    @Override
    public void stop() {
        return;
    }

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        ReadResource op = new ReadResource(address);
        Result res = getASConnection().execute(op);
        return (res != null && res.isSuccess()) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    private BaseServerComponent findServerComponent() {
        BaseComponent<?> component = this;
        while ((component != null) && !(component instanceof BaseServerComponent)) {
            component = (BaseComponent<?>) component.context.getParentResourceComponent();
        }
        return (BaseServerComponent)component;
    }

    public BaseServerComponent getServerComponent() {
        return serverComponent;
    }

    /**
     * Gather measurement data
     * @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        for (MeasurementScheduleRequest req : metrics) {

            if (req.getName().startsWith(INTERNAL))
                processPluginStats(req, report);
            else {
                // Metrics from the application server

                String reqName = req.getName();

                ComplexRequest request = null;
                Operation op;
                if (reqName.contains(":")) {
                    request = ComplexRequest.create(reqName);
                    op = new ReadAttribute(address, request.getProp());
                } else {
                    op = new ReadAttribute(address, reqName); // TODO batching
                }

                Result res = getASConnection().execute(op);
                if (!res.isSuccess()) {
                    log.warn("Getting metric [" + req.getName() + "] at [ " + address + "] failed: "
                        + res.getFailureDescription());
                    continue;
                }

                Object val = res.getResult();
                if (val == null) // One of the AS7 ways of telling "This is not implemented" See also AS7-1454
                    continue;

                if (req.getDataType() == DataType.MEASUREMENT) {
                    if (val instanceof String && ((String) val).startsWith("JBAS018003")) // AS7 way of saying "no value available"
                        continue;
                    try {
                        if (request != null) {
                            HashMap<String, Number> myValues = (HashMap<String, Number>) val;
                            for (String key : myValues.keySet()) {
                                String sub = request.getSub();
                                if (key.equals(sub)) {
                                    addMetric2Report(report, req, myValues.get(key));
                                }
                            }
                        } else {
                            addMetric2Report(report, req, val);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Non numeric input for [" + req.getName() + "] : [" + val + "]");
                    }
                } else if (req.getDataType() == DataType.TRAIT) {

                    String realVal = getStringValue(val);

                    MeasurementDataTrait data = new MeasurementDataTrait(req, realVal);
                    report.addData(data);
                }
            }
        }
    }

    private void addMetric2Report(MeasurementReport report, MeasurementScheduleRequest req, Object val) {
        Double d = Double.parseDouble(getStringValue(val));
        MeasurementDataNumeric data = new MeasurementDataNumeric(req, d);
        report.addData(data);
    }

    protected String getStringValue(Object val) {
        String realVal = "";
        if (val instanceof String)
            realVal = (String) val;
        else
            realVal = String.valueOf(val);
        return realVal;
    }

    /**
     * Return internal statistics data
     * @param req Schedule for the requested data
     * @param report report to add th data to.
     */
    private void processPluginStats(MeasurementScheduleRequest req, MeasurementReport report) {

        String name = req.getName();
        if (!name.startsWith(INTERNAL))
            return;

        name = name.substring(INTERNAL_SIZE);

        PluginStats stats = PluginStats.getInstance();
        MeasurementDataNumeric data;
        Double val;
        if (name.equals("mgmtRequests")) {
            val = (double) stats.getRequestCount();
        } else if (name.equals("requestTime")) {
            val = (double) stats.getRequestTime();
        } else if (name.equals("maxTime")) {
            val = (double) stats.getMaxTime();
        } else
            val = Double.NaN;

        data = new MeasurementDataNumeric(req, val);
        report.addData(data);
    }

    public ASConnection getASConnection() {
        return (this.testConnection != null) ? this.testConnection : getServerComponent().getASConnection();
    }

    public String getPath() {
        return path;
    }

    public Configuration loadResourceConfiguration() throws Exception {

        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef, getASConnection(), address, includeRuntime);
        Configuration configuration = delegate.loadResourceConfiguration();

        // Read server state
        ReadAttribute op = new ReadAttribute(getAddress(), "name");
        Result res = getASConnection().execute(op);
        if (res.isReloadRequired()) {
            PropertySimple oobMessage = new PropertySimple("__OOB","The server needs a reload for the latest changes to come effective.");
            configuration.put(oobMessage);
        }
        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }

    @Override
    public void deleteResource() throws Exception {

        log.info("delete resource: " + path + " ...");
        if (context.getResourceType().getName().equals(MANAGED_SERVER)) {
            // We need to do two steps because of AS7-4032
            Operation stop = new Operation("stop", getAddress());
            Result res = getASConnection().execute(stop);
            if (!res.isSuccess()) {
                throw new IllegalStateException("Managed server @ " + path
                    + " is still running and can't be stopped. Can't remove it");
            }
        }
        Operation op = new Remove(address);
        Result res = getASConnection().execute(op);
        if (!res.isSuccess())
            throw new IllegalArgumentException("Delete for [" + path + "] failed: " + res.getFailureDescription());
        if (path.contains("server-group")) {
            // This was a server group level deployment - TODO do we also need to remove the entry in /deployments ?
            /*

                        for (PROPERTY_VALUE val : address) {
                            if (val.getKey().equals("deployment")) {
                                ComplexResult res2 = connection.executeComplex(new Operation("remove",val.getKey(),val.getValue()));
                                if (!res2.isSuccess())
                                    throw new IllegalArgumentException("Removal of [" + path + "] falied : " + res2.getFailureDescription());
                            }
                        }
            */
        }
        log.info("   ... done");

    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        if (report.getPackageDetails() != null) { // Content deployment
            return deployContent(report);
        } else {
            ASConnection connection = getASConnection();
            ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition();

            // Check for the Highlander principle
            boolean isSingleton = report.getResourceType().isSingleton();
            if (isSingleton) {
                // check if there is already a child with th desired type is present
                Configuration pluginConfig = report.getPluginConfiguration();
                PropertySimple pathProperty = pluginConfig.getSimple("path");
                if (path==null || path.isEmpty()) {
                    report.setErrorMessage("No path property found in plugin configuration");
                    report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);
                    return report;
                }

                ReadChildrenNames op = new ReadChildrenNames(address,pathProperty.getStringValue());
                Result res = connection.execute(op);
                if (res.isSuccess()) {
                    List<String> entries = (List<String>) res.getResult();
                    if (!entries.isEmpty()) {
                        report.setErrorMessage("Resource is a singleton, but there are already children " + entries + " please remove them and retry");
                        report.setStatus(CreateResourceStatus.FAILURE);
                        return report;
                    }
                }
            }


            CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, connection, address);
            return delegate.createResource(report);
        }
    }

    /**
     * Deploy content to the remote server - this is one half of #createResource
     * @param report Create resource report that tells us what to do
     * @return report that tells us what has been done.
     */
    protected CreateResourceReport deployContent(CreateResourceReport report) {
        ContentContext cctx = context.getContentContext();
        ResourcePackageDetails details = report.getPackageDetails();

        ContentServices contentServices = cctx.getContentServices();
        String resourceTypeName = report.getResourceType().getName();

        ServerPluginConfiguration serverPluginConfig = getServerComponent().getServerPluginConfiguration();
        ASUploadConnection uploadConnection = new ASUploadConnection(serverPluginConfig.getHostname(),
                serverPluginConfig.getPort(), serverPluginConfig.getUser(), serverPluginConfig.getPassword());
        OutputStream out = uploadConnection.getOutputStream(details.getFileName());
        contentServices.downloadPackageBitsForChildResource(cctx, resourceTypeName, details.getKey(), out);

        JsonNode uploadResult = uploadConnection.finishUpload();
        if (verbose)
            log.info(uploadResult);

        if (ASUploadConnection.isErrorReply(uploadResult)) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(ASUploadConnection.getFailureDescription(uploadResult));

            return report;
        }

        String fileName = details.getFileName();

        if (fileName.startsWith("C:\\fakepath\\")) { // TODO this is a hack as the server adds the fake path somehow
            fileName = fileName.substring("C:\\fakepath\\".length());
        }

        String runtimeName = report.getPackageDetails().getDeploymentTimeConfiguration().getSimpleValue("runtimeName");
        if (runtimeName == null || runtimeName.isEmpty()) {
            runtimeName = fileName;
        }

        JsonNode resultNode = uploadResult.get("result");
        String hash = resultNode.get("BYTES_VALUE").getTextValue();

        return runDeploymentMagicOnServer(report, runtimeName, fileName, hash);
    }

    /**
     * Do the actual fumbling with the domain api to deploy the uploaded content
     * @param report CreateResourceReport to report the result
     * @param runtimeName File name to use as runtime name
     * @param deploymentName Name of the deployment
     * @param hash Hash of the content bytes
     * @return the passed report with success or failure settings
     */
    public CreateResourceReport runDeploymentMagicOnServer(CreateResourceReport report, String runtimeName,
        String deploymentName, String hash) {

        boolean toServerGroup = context.getResourceKey().contains("server-group=");
        log.info("Deploying [" + runtimeName + "] to domain only= " + !toServerGroup + " ...");

        ASConnection connection = getASConnection();

        Operation step1 = new Operation("add", "deployment", runtimeName);
        //        step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        content.add(contentValues);
        step1.addAdditionalProperty("content", content);

        step1.addAdditionalProperty("name", deploymentName);
        step1.addAdditionalProperty("runtime-name", runtimeName);

        String resourceKey;
        Result result;

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);
        /*
         * We need to check here if this is an upload to /deployment only
         * or if this should be deployed to a server group too
         */

        if (!toServerGroup) {

            // if standalone, then :deploy the deployment anyway
            if (context.getResourceType().getName().contains("Standalone")) {
                Operation step2 = new Operation("deploy", step1.getAddress());
                cop.addStep(step2);
            }

            result = connection.execute(cop);
            resourceKey = step1.getAddress().getPath();

        } else {

            Address serverGroupAddress = new Address(context.getResourceKey());
            serverGroupAddress.add("deployment", deploymentName);
            Operation step2 = new Operation("add", serverGroupAddress);

            cop.addStep(step2);

            Operation step3 = new Operation("deploy", serverGroupAddress);
            cop.addStep(step3);

            resourceKey = serverGroupAddress.getPath();

            if (verbose)
                log.info("Deploy operation: " + cop);

            result = connection.execute(cop);
        }

        if ((!result.isSuccess())) {
            String failureDescription = result.getFailureDescription();
            report.setErrorMessage(failureDescription);
            report.setStatus(CreateResourceStatus.FAILURE);
            log.warn(" ... done with failure: " + failureDescription);
        } else {
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceName(runtimeName);
            report.setResourceKey(resourceKey);
            report.getPackageDetails().setSHA256(hash);
            report.getPackageDetails().setInstallationTimestamp(System.currentTimeMillis());
            log.info(" ... with success and key [" + resourceKey + "]");
        }

        return report;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        String what;
        String op;

        if (name.contains(":")) {
            int colonPos = name.indexOf(':');
            what = name.substring(0, colonPos);
            op = name.substring(colonPos + 1);
        }
        else {
            what=""; // dummy value
            op = name;
        }
        Operation operation = null;


        Address theAddress = new Address();

        if (what.equals("server-group")) {
            String groupName = parameters.getSimpleValue("name", "");
            String profile = parameters.getSimpleValue("profile", "default");

            theAddress.add("server-group", groupName);

            operation = new Operation(op, theAddress);
            operation.addAdditionalProperty("profile", profile);
        } else if (what.equals("destination")) {
            theAddress.add(address);
            String newName = parameters.getSimpleValue("name", "");
            String type = parameters.getSimpleValue("type", "jms-queue").toLowerCase();
            theAddress.add(type, newName);
            PropertyList jndiNamesProp = parameters.getList("entries");
            if (jndiNamesProp == null || jndiNamesProp.getList().isEmpty()) {
                OperationResult fail = new OperationResult();
                fail.setErrorMessage("No jndi bindings given");
                return fail;
            }
            List<String> jndiNames = new ArrayList<String>();
            for (Property p : jndiNamesProp.getList()) {
                PropertySimple ps = (PropertySimple) p;
                jndiNames.add(ps.getStringValue());
            }

            operation = new Operation(op, theAddress);
            operation.addAdditionalProperty("entries", jndiNames);
            if (type.equals("jms-queue")) {
                PropertySimple ps = (PropertySimple) parameters.get("durable");
                if (ps != null) {
                    boolean durable = ps.getBooleanValue();
                    operation.addAdditionalProperty("durable", durable);
                }
                String selector = parameters.getSimpleValue("selector", "");
                if (!selector.isEmpty())
                    operation.addAdditionalProperty("selector", selector);
            }

        } else if (what.equals("domain")) {
            operation = new Operation(op, new Address());
        } else if (what.equals("subsystem")) {
            operation = new Operation(op, new Address(this.path));
        } else {
            // We have a generic operation so we pass it literally
            // with the parameters it has.
            operation = new Operation(op, new Address((path)));
            for (Property prop : parameters.getProperties()) {
                if (prop instanceof PropertySimple) {
                    PropertySimple ps = (PropertySimple) prop;
                    if (ps.getStringValue() != null) {
                        Object val = getObjectForProperty(ps,op);
                        operation.addAdditionalProperty(ps.getName(),val);
                    }
                }
                else if (prop instanceof PropertyList) {
                    PropertyList pl = (PropertyList) prop;
                    List<Object> items = new ArrayList<Object>(pl.getList().size());
                    // Loop over the inner elements of the list
                    for (Property p2 : pl.getList()) {
                        if (p2 instanceof PropertySimple) {
                            PropertySimple ps = (PropertySimple) p2;
                            if (ps.getStringValue() != null) {
                                Object val = getObjectForPropertyList(ps,pl,op);
                                items.add(val);
                            }
                        }
                    }
                    operation.addAdditionalProperty(pl.getName(),items);
                }
                else {
                    log.error("PropertyMap for " + prop.getName() + " not yet supported");
                }
            }
        }

        OperationResult operationResult = new OperationResult();
        Result result = getASConnection().execute(operation);

        if (result == null) {
            operationResult.setErrorMessage("Connection was null - is the server running?");
            return operationResult;
        }

        if (!result.isSuccess()) {
            operationResult.setErrorMessage(result.getFailureDescription());
        } else {
            String tmp;
            if (result.getResult() == null)
                tmp = "-none provided by the server-";
            else
                tmp = result.getResult().toString();
            operationResult.setSimpleResult(tmp);
        }
        return operationResult;
    }

    /**
     * Return a value object for the passed property. The type is determined by looking at the operation definition
     * @param prop Property to evaluate
     * @param operationName Name of the operation to look at
     * @return Value or null on failure
     */
    Object getObjectForProperty(PropertySimple prop, String operationName) {
        ConfigurationDefinition parameterDefinitions = getParameterDefinitionsForOperation(operationName);
        if (parameterDefinitions==null)
            return null;

        PropertyDefinition pd = parameterDefinitions.get(prop.getName());
        if (pd instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;
            return getObjectForProperty(prop, pds);
        } else {
            log.warn("Property [" + prop.getName() + "] is not understood yet");
            return null;
        }
    }

    /**
     * Return a value object for the passed property, which is part of a list. The type is determined by
     * looking at the operation definition and PropertyList#getMemberDefinition
     * @param prop Property to evaluate
     * @param propertyList Outer list
     * @param operationName Name of the operation
     * @return Value or null on failure
     */
    Object getObjectForPropertyList(PropertySimple prop, PropertyList propertyList, String operationName) {
        ConfigurationDefinition parameterDefinitions = getParameterDefinitionsForOperation(operationName);
        if (parameterDefinitions==null)
            return null;

        PropertyDefinition def = parameterDefinitions.get(propertyList.getName());
        if (def instanceof  PropertyDefinitionList) {
            PropertyDefinitionList definitionList = (PropertyDefinitionList) def;
            PropertyDefinition tmp = definitionList.getMemberDefinition();
            if (tmp instanceof  PropertyDefinitionSimple) {
                return getObjectForProperty(prop, (PropertyDefinitionSimple) tmp);
            }
        }
        return null;
    }

    /**
     * Return the parameter definition for the operation with the name passed
     * @param operationName Name of the operation to look for
     * @return A configuration definition or null on failure
     */
    ConfigurationDefinition getParameterDefinitionsForOperation(String operationName) {
        ResourceType type = context.getResourceType();
        Set<OperationDefinition> operationDefinitions = type.getOperationDefinitions();

        for (OperationDefinition definition : operationDefinitions) {
            if (definition.getName().equals(operationName)) {
                return definition.getParametersConfigurationDefinition();
            }
        }
        return null;
    }

    /**
     * Return a value object for the passed property with the passed definition
     * @param prop Property to evaluate
     * @param propDef Definition to determine the type from
     * @return The value object
     */
    Object getObjectForProperty(PropertySimple prop, PropertyDefinitionSimple propDef) {
        PropertySimpleType type = propDef.getType();
        return getObjectForProperty(prop, type);
    }

    /**
     * Return the object representation of the passed PropertySimple for the passed type
     * @param prop Property to evaluate
     * @param type Type to convert into
     * @return Converted object -- if no valid type is found, a String-value is returned.
     */
    private Object getObjectForProperty(PropertySimple prop, PropertySimpleType type) {
        switch (type) {
        case STRING:
            return prop.getStringValue();
        case INTEGER:
            return prop.getIntegerValue();
        case BOOLEAN:
            return prop.getBooleanValue();
        case LONG:
            return prop.getLongValue();
        case FLOAT:
            return prop.getFloatValue();
        case DOUBLE:
            return prop.getDoubleValue();
        default:
            return prop.getStringValue();
        }
    }

    ///// These two are used to 'inject' the connection and the path from tests.
    // TODO: Refactor this - we should be able to mock the ResourceContext passed to start() instead.
    public void setConnection(ASConnection connection) {
        this.testConnection = connection;
    }

    public void setPath(String path) {
        this.path = path;
        this.address = new Address(path);
    }

    public Address getAddress() {
        return address;
    }

    protected String readAttribute(String name) throws Exception {
        return readAttribute(getAddress(), name);
    }

    protected String readAttribute(Address address, String name) throws Exception {
        return readAttribute(address, name, String.class);
    }

    protected <T> T readAttribute(Address address, String name, Class<T> resultType) throws Exception {
        Operation op = new ReadAttribute(address, name);
        Result res = getASConnection().execute(op);
        if (!res.isSuccess()) {
            throw new Exception("Failed to read attribute [" + name + "] of address [" + getAddress().getPath()
                    + "] - response: " + res);
        }
        return (T) res.getResult();
    }

    private static class ComplexRequest {
        private String prop;
        private String sub;

        private ComplexRequest(String prop, String sub) {
            this.prop = prop;
            this.sub = sub;
        }

        public String getProp() {
            return prop;
        }

        public String getSub() {
            return sub;
        }

        public static ComplexRequest create(String requestName) {
            StringTokenizer tokenizer = new StringTokenizer(requestName, ":");
            return new ComplexRequest(tokenizer.nextToken(), tokenizer.nextToken());
        }
    }

}
