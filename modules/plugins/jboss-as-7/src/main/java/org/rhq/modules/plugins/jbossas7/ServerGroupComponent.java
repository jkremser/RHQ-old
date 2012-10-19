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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component dealing with server group specific things
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class ServerGroupComponent extends DomainComponent implements ContentFacet, CreateChildResourceFacet {

    private static final String SUCCESS = "success";
    private static final String OUTCOME = "outcome";

    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;  // TODO: Customise this generated block
    }

    // TODO I think this package code is not used.
    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages,
                                                 ContentServices contentServices) {

        ContentContext cctx = context.getContentContext();

        DeployPackagesResponse response = new DeployPackagesResponse();

        for (ResourcePackageDetails details : packages) {

            ASUploadConnection uploadConnection = new ASUploadConnection(host,port);
            String fileName = details.getFileName();
            OutputStream out = uploadConnection.getOutputStream(fileName);
            contentServices.downloadPackageBits(cctx, details.getKey(), out, false);
            JsonNode uploadResult = uploadConnection.finishUpload();
            if (uploadResult.has(OUTCOME)) {
                String outcome = uploadResult.get(OUTCOME).getTextValue();
                if (outcome.equals(SUCCESS)) { // Upload was successful, so now add the file to the server group
                    JsonNode resultNode = uploadResult.get("result");
                    String hash = resultNode.get("BYTES_VALUE").getTextValue();
                    ASConnection connection = getASConnection();

                    List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
                    deploymentsAddress.add(new PROPERTY_VALUE("deployment", fileName));
                    Operation step1 = new Operation("add",deploymentsAddress);
//                    step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
                    List<Object> content = new ArrayList<Object>(1);
                    Map<String,Object> contentValues = new HashMap<String,Object>();
                    contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",hash));
                    content.add(contentValues);
                    step1.addAdditionalProperty("content",content);

                    step1.addAdditionalProperty("name", fileName);

                    List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
                    serverGroupAddress.addAll(pathToAddress(context.getResourceKey()));
                    serverGroupAddress.add(new PROPERTY_VALUE("deployment", fileName));
                    Operation step2 = new Operation("add",serverGroupAddress);
                    Operation step3 = new Operation("deploy",serverGroupAddress);

                    CompositeOperation cop = new CompositeOperation();
                    cop.addStep(step1);
                    cop.addStep(step2);
                    cop.addStep(step3);

                    Result result = connection.execute(cop);
                    if (!result.isSuccess()) // TODO get failure message into response
                        response.addPackageResponse(new DeployIndividualPackageResponse(details.getKey(),ContentResponseResult.FAILURE));
                    else {
                        DeployIndividualPackageResponse individualPackageResponse = new DeployIndividualPackageResponse(
                                details.getKey(), ContentResponseResult.SUCCESS);
                        response.addPackageResponse(individualPackageResponse);
                        response.setOverallRequestResult(ContentResponseResult.SUCCESS);
                    }
                }
                else
                    response.addPackageResponse(new DeployIndividualPackageResponse(details.getKey(),ContentResponseResult.FAILURE));
            }
            else {
                response.addPackageResponse(
                        new DeployIndividualPackageResponse(details.getKey(), ContentResponseResult.FAILURE));
            }
        }


        return response;
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {

        List<PROPERTY_VALUE> serverGroupAddress = pathToAddress(path);

        Operation op = new ReadChildrenNames(serverGroupAddress,"deployment"); // TODO read full packages not only names
        Result node = connection.execute(op);
        if (!node.isSuccess())
            return null;

        List<String> resultList = (List<String>) node.getResult(); // TODO needs checking
        Set<ResourcePackageDetails> details = new HashSet<ResourcePackageDetails>();
        for (String file : resultList) {
            String t;
            if (file.contains("."))
                t = file.substring(file.lastIndexOf(".")+1);
            else
                t = "-none-";

            ResourcePackageDetails detail = new ResourcePackageDetails(new PackageDetailsKey(file,"1.0",t,"all"));
            details.add(detail);
        }
        return details;

    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;  // TODO: Customise this generated block
    }

    private String serverGroupFromKey() {
        String key1 = context.getResourceKey();
        return key1.substring(key1.lastIndexOf("/")+1);
    }
}
