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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for standalone AS7 servers.
 *
 * @author Heiko W. Rupp
 */
public class StandaloneASComponent<T extends ResourceComponent<?>> extends BaseServerComponent<T>
        implements MeasurementFacet, OperationFacet {

    private static final String SERVER_CONFIG_TRAIT = "config-file";

    private static final Address ENVIRONMENT_ADDRESS = new Address("core-service=server-environment");

    @Override
    protected AS7Mode getMode() {
        return AS7Mode.STANDALONE;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request: requests) {
            String requestName = request.getName();
            if (requestName.equals(SERVER_CONFIG_TRAIT)) {
                collectConfigTrait(report, request);
            } else if (requestName.equals("multicastAddress")) {
                collectMulticastAddressTrait(report, request);
            } else if (requestName.equals("deployDir")) {
                resolveDeployDir(report,request);
            } else {
                leftovers.add(request); // handled below
            }
        }

        super.getValues(report, leftovers);
    }

    /**
     * Try to determine the deployment directory (usually $as/standalone/deployments ).
     * For JDG we return fake data, as JDG does not have such a directory.
     * @param report Measurement report to tack the value on
     * @param request Measurement request with the schedule id to use
     */
    private void resolveDeployDir(MeasurementReport report, MeasurementScheduleRequest request) {

        if ("JDG".equals(pluginConfiguration.getSimpleValue("productType","AS7"))) {
            log.debug("This is a JDG server, so there is no deployDir");
            MeasurementDataTrait trait = new MeasurementDataTrait(request,"- not applicable to JDG -");
            report.addData(trait);
            return;
        }

        // So we have an AS7/EAP6
        Address scanner = new Address("subsystem=deployment-scanner,scanner=default");
        ReadResource op = new ReadResource(scanner);
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            Map<String,String> scannerMap = (Map<String, String>) res.getResult();
            String path = scannerMap.get("path");
            String relativeTo = scannerMap.get("relative-to");
            File basePath = resolveRelativePath(relativeTo);

            // It is safe to use File.separator, as the agent we are running in, will also lay down the plugins
            String deployDir = new File(basePath, path).getAbsolutePath();

            MeasurementDataTrait trait = new MeasurementDataTrait(request,deployDir);
            report.addData(trait);
        }
        else {
            log.error("No default deployment scanner was found, returning no value");
        }
    }

    private File resolveRelativePath(String relativeTo) {

        Address addr = new Address("path",relativeTo);
        ReadResource op = new ReadResource(addr);
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            Map<String,String> pathMap = (Map<String, String>) res.getResult();
            String path = pathMap.get("path");
            String relativeToProp = pathMap.get("relative-to");
            if (relativeToProp==null)
                return new File(path);
            else {
                File basePath = resolveRelativePath(relativeToProp);
                return new File(basePath, path);
            }
        }
        log.warn("The requested path property " + relativeTo + " is not registered in the server, so not resolving it.");
        return new File(relativeTo);
    }

    @Override
    protected Address getServerAddress() {
        return getAddress();
    }

    @Override
    protected String getSocketBindingGroup() {
        // TODO (ips): Can this ever be something other than "standard-sockets"?
        return "standard-sockets";
    }

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws Exception {
        if (name.equals("start")) {
            return startServer();
        } else if (name.equals("restart")) {
            return restartServer(parameters);
        } else if (name.equals("installRhqUser")) {
            return installManagementUser(parameters, pluginConfiguration);
        }

        // reload, shutdown go to the remote server
        Operation op = new Operation(name, new Address());
        Result res = getASConnection().execute(op);

        OperationResult operationResult = postProcessResult(name, res);

        if (name.equals("shutdown"))
            waitUntilDown(operationResult);

        if (name.equals("reload"))
            waitUntilReloaded(operationResult);

        context.getAvailabilityContext().requestAvailabilityCheck();

        return operationResult;
    }

    private void waitUntilReloaded(OperationResult operationResult) {
        boolean reloaded = false;
        int count = 0;
        while (!reloaded) {
            try {
                Thread.sleep(2000); // Wait 2s
            } catch (InterruptedException e) {
                // Ignore
            }

            Operation op = new ReadAttribute(new Address(), "release-version");
            Result res = getASConnection().execute(op);
            if (res.isSuccess() && !res.isReloadRequired()) {
                reloaded = true;
            } else if (count > 20) {
                operationResult.setErrorMessage("Was not able to reload the server");
                return;
            }
            count++;
        }
        log.debug("waitUntilReloaded: Used " + count + " delay round(s) to reload");
        return;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // We need to filter the path properties that are marked with the read-only flag
        // This is done by setting the logical removed flag on the map to signal
        // the write delegate to skip the map
        Configuration config = report.getConfiguration();
        PropertyList propertyList = config.getList("*3");
        for (Property property : propertyList.getList()) {
            PropertyMap map = (PropertyMap) property;
            String ro = map.getSimpleValue("read-only", "false");
            if (Boolean.parseBoolean(ro)) {
                map.setErrorMessage(ConfigurationWriteDelegate.LOGICAL_REMOVED);
            }
        }
        super.updateResourceConfiguration(report);
    }

    @NotNull
    @Override
    protected Address getEnvironmentAddress() {
        return ENVIRONMENT_ADDRESS;
    }

    @NotNull
    @Override
    protected Address getHostAddress() {
        // In standalone mode, the root address is the host address.
        return getAddress();
    }

    @NotNull
    @Override
    protected String getBaseDirAttributeName() {
        return "base-dir";
    }

}
