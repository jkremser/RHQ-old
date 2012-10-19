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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for host- and domain controller
 * @author Heiko W. Rupp
 */
public class HostControllerComponent<T extends ResourceComponent<?>> extends BaseServerComponent<T>
        implements OperationFacet {

    private final Log log = LogFactory.getLog(HostControllerComponent.class);

    @Override
    public AvailabilityType getAvailability() {
        Operation op = new ReadAttribute(new Address(),"launch-type");
        Result res = getASConnection().execute(op);
        if (!res.isSuccess()) {
            return AvailabilityType.DOWN;
        }
        String mode = (String) res.getResult();
        if(!"DOMAIN".equals(mode)) {
            throw new InvalidPluginConfigurationException("Discovered Server is in domain mode, but runtime says " + mode);
        }
        // Now check product type
        op = new ReadAttribute(new Address(),"product-name");
        res = getASConnection().execute(op);
        String discoveredType = context.getPluginConfiguration().getSimpleValue("productType","AS");
        String runtimeType = (String) res.getResult();
        if (runtimeType==null || runtimeType.isEmpty()) {
            runtimeType = "AS";
        }
        if (!discoveredType.equals(runtimeType)) {
            throw new InvalidPluginConfigurationException("Discovered Servers is of " + discoveredType + " type, but runtime says " + runtimeType);
        }

        return AvailabilityType.UP;
    }


    public Configuration getHCConfig() {
        return pluginConfiguration;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        OperationResult operationResult;
        if (name.equals("start")) {
            operationResult = startServer(AS7Mode.DOMAIN);
        } else if (name.equals("restart")) {
            operationResult = restartServer(parameters, AS7Mode.DOMAIN);
        } else if (name.equals("shutdown")) {
            // This is a bit trickier, as it needs to be executed on the level on /host=xx
            String domainHost = pluginConfiguration.getSimpleValue("domainHost", "");
            if (domainHost.isEmpty()) {
                OperationResult result = new OperationResult();
                result.setErrorMessage("No domain host found - can not continue");
                operationResult = result;
            }
            Operation op = new Operation("shutdown", "host", domainHost);
            Result res = getASConnection().execute(op);
            operationResult = postProcessResult(name, res);

            waitUntilDown(operationResult);

        } else if (name.equals("installRhqUser")) {
            operationResult = installManagementUser(parameters, pluginConfiguration, AS7Mode.HOST);
        } else {

            // Defer other stuff to the base component for now
            operationResult = super.invokeOperation(name, parameters);
        }

//        context.getAvailabilityContext().requestAvailabilityCheck();

        return operationResult;
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {

        // If Content is to be deployed, call the deployContent method
        if (report.getPackageDetails() != null)
            return super.deployContent(report);

        String targetTypeName = report.getResourceType().getName();
        Operation op;

        String resourceName = report.getUserSpecifiedResourceName();
        Configuration rc = report.getResourceConfiguration();
        Address targetAddress ;

        // Dispatch according to child type
        if (targetTypeName.equals("ServerGroup")) {
            targetAddress = new Address(); // Server groups are at / level
            targetAddress.add("server-group", resourceName);
            op = new Operation("add", targetAddress);


            String profile = rc.getSimpleValue("profile", "");
            if (profile.isEmpty()) {
                report.setErrorMessage("No profile given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("profile", profile);
            String socketBindingGroup = rc.getSimpleValue("socket-binding-group", "");
            if (socketBindingGroup.isEmpty()) {
                report.setErrorMessage("No socket-binding-group given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("socket-binding-group", socketBindingGroup);
            PropertySimple offset = rc.getSimple("socket-binding-port-offset");
            if (offset != null && offset.getStringValue() != null)
                op.addAdditionalProperty("socket-binding-port-offset", offset.getIntegerValue());

            PropertySimple jvm = rc.getSimple("jvm");
            if (jvm!=null) {
                op.addAdditionalProperty("jvm",jvm.getStringValue());
            }
        }
        else if (targetTypeName.equals(BaseComponent.MANAGED_SERVER)) {

            String targetHost = rc.getSimpleValue("hostname",null);
            if (targetHost==null) {
                report.setErrorMessage("No domain host given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }

            targetAddress = new Address("host",targetHost);
            targetAddress.add("server-config", resourceName);
            op = new Operation("add", targetAddress);
            String socketBindingGroup = rc.getSimpleValue("socket-binding-group", "");
            if (socketBindingGroup.isEmpty()) {
                report.setErrorMessage("No socket-binding-group given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("socket-binding-group", socketBindingGroup);
            String autostartS = rc.getSimpleValue("auto-start","false");
            boolean autoStart = Boolean.valueOf(autostartS);
            op.addAdditionalProperty("auto-start",autoStart);

            String portS = rc.getSimpleValue("socket-binding-port-offset","0");
            int portOffset = Integer.parseInt(portS);
            op.addAdditionalProperty("socket-binding-port-offset",portOffset);

            String serverGroup = rc.getSimpleValue("group",null);
            if (serverGroup==null) {
                report.setErrorMessage("No server group given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("group",serverGroup);

        } else if (targetTypeName.equals("JVM-Definition")) {
            return super.createResource(report);

        }
        else {
            throw new IllegalArgumentException("Don't know yet how to create instances of " + targetTypeName);
        }
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            report.setResourceKey(targetAddress.getPath());
            report.setResourceName(resourceName);
            report.setStatus(CreateResourceStatus.SUCCESS);
        } else {
            report.setErrorMessage(res.getFailureDescription());
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setException(res.getRhqThrowable());
        }
        return report;
    }
}
