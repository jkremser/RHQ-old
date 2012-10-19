package org.rhq.modules.plugins.jbossas7;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.FileContentDelegate;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.Base64;
import org.rhq.core.util.ByteUtil;
import org.rhq.core.util.file.ContentFileInfo;
import org.rhq.core.util.file.JarContentFileInfo;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Deal with deployments
 * @author Heiko W. Rupp
 */
public class DeploymentComponent extends BaseComponent<ResourceComponent<?>> implements OperationFacet,  ContentFacet {

    private boolean verbose = ASConnection.verbose;
    private File deploymentFile;

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException, Exception {
        super.start(context);
        deploymentFile = determineDeploymentFile();
    }

    @Override
    public AvailabilityType getAvailability() {
        Operation op = new ReadAttribute(getAddress(),"enabled");
        Result res = getASConnection().execute(op);
        if (!res.isSuccess())
            return AvailabilityType.DOWN;

        if (res.getResult()== null || !(Boolean)(res.getResult()))
            return AvailabilityType.DOWN;

        return AvailabilityType.UP;
    }

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        if (name.equals("enable")) {
            return invokeSimpleOperation("deploy");
        } else if (name.equals("disable")) {
            return invokeSimpleOperation("undeploy");
        } else if (name.equals("restart")) {
            OperationResult result = invokeSimpleOperation("undeploy");

            if(result.getErrorMessage() == null){
                result = invokeSimpleOperation("deploy");
            }

            return result;
        } else {
            return super.invokeOperation(name, parameters);
        }
    }

    private OperationResult invokeSimpleOperation(String action) {
        Operation op = new Operation(action,getAddress());
        Result res = getASConnection().execute(op);
        OperationResult result = new OperationResult();
        if (res.isSuccess()) {
            result.setSimpleResult("Success");
            if ("enable".equals(action)) {
                context.getAvailabilityContext().enable();
            }
            if ("disable".equals(action)) {
                context.getAvailabilityContext().disable();
            }
        } else {
            result.setErrorMessage(res.getFailureDescription());
        }

        return result;
    }


    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return new ArrayList<DeployPackageStep>();
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages,
                                                 ContentServices contentServices) {
        log.debug("Starting deployment..");
        DeployPackagesResponse response = new DeployPackagesResponse();

        if (packages.size()!=1) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("Can only deploy one package at a time");
            log.warn("deployPackages can only deploy one package at a time");
        }

        ResourcePackageDetails detail = packages.iterator().next();

        ASUploadConnection uploadConnection = new ASUploadConnection(getASConnection());
        OutputStream out = uploadConnection.getOutputStream(detail.getFileName());
        ResourceType resourceType = context.getResourceType();

        log.info("Deploying " + resourceType.getName() + " Resource with key [" + detail.getKey() +"]...");

        contentServices.downloadPackageBits(context.getContentContext(),
                detail.getKey(), out, true);

        JsonNode uploadResult = uploadConnection.finishUpload();
        if (verbose) {
            log.info(uploadResult);
        }

        if (ASUploadConnection.isErrorReply(uploadResult)) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage(ASUploadConnection.getFailureDescription(uploadResult));

            return response;
        }
        JsonNode resultNode = uploadResult.get("result");
        String hash = resultNode.get("BYTES_VALUE").getTextValue();


        CreateResourceReport report1 = new CreateResourceReport("", resourceType, new Configuration(),
                new Configuration(), detail);
        //CreateResourceReport report = runDeploymentMagicOnServer(report1,detail.getKey().getName(),hash, hash);

        try {
            redeployOnServer(detail.getKey().getName(), hash);
            response.setOverallRequestResult(ContentResponseResult.SUCCESS);
            //we just deployed a different file on the AS7 server, so let's refresh ourselves
            deploymentFile = determineDeploymentFile();
            DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(detail.getKey(), ContentResponseResult.SUCCESS);
                    response.addPackageResponse(packageResponse);

        }
        catch (Exception e) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
        }

        log.info("Result of deployment of " + resourceType.getName() + " Resource with key [" + detail.getKey()
                + "]: " + response);

        return response;
    }

    private void redeployOnServer(String name, String hash) throws Exception {

        Operation op = new Operation("full-replace-deployment", new Address());
        op.addAdditionalProperty("name",name);
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",hash));
        content.add(contentValues);
        op.addAdditionalProperty("content",content);
        Result result = getASConnection().execute(op);
        if (result.isRolledBack())
            throw new Exception(result.getFailureDescription());
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        RemovePackagesResponse response = new RemovePackagesResponse(ContentResponseResult.NOT_PERFORMED);
        response.setOverallRequestErrorMessage("Removal of packages backing the deployments is not supported.");
        return response;
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        if (deploymentFile == null) {
            return Collections.emptySet();
        }

        String name = getDeploymentName();
        String sha256 = getSHA256(deploymentFile);
        String version = getVersion(sha256);

        PackageDetailsKey key = new PackageDetailsKey(name, version, type.getName(), "noarch");
        ResourcePackageDetails details = new ResourcePackageDetails(key);

        details.setDisplayVersion(getDisplayVersion(deploymentFile));
        details.setFileCreatedDate(null); //TODO figure this out from Sigar somehow?
        details.setFileName(name);
        details.setFileSize(deploymentFile.length());
        details.setInstallationTimestamp(deploymentFile.lastModified());
        details.setLocation(deploymentFile.getAbsolutePath());
        details.setSHA256(sha256);

        return Collections.singleton(details);
    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        try {
            return deploymentFile == null ? new ByteArrayInputStream(new byte[0]) : new FileInputStream(deploymentFile);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Deployment file seems to have disappeared");
        }
    }

    private File determineDeploymentFile() {
        Operation op = new ReadAttribute(getAddress(), "content");
        Result result = getASConnection().execute(op);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.getResult();
        if (content == null || content.isEmpty()) {
            log.warn("Could not determine the location of the deployment - the content descriptor wasn't found for deployment" + getAddress() + ".");
            return null;
        }

        Boolean archive = (Boolean) content.get(0).get("archive");
        if (archive != null && !archive) {
            log.debug("Exploded deployments not supported for retrieving the content.");
            return null;
        }

        File deploymentFile = null;
        if (content.get(0).containsKey("path")) {
            String path = (String) content.get(0).get("path");
            String relativeTo = (String) content.get(0).get("relative-to");
            deploymentFile = getDeploymentFileFromPath(relativeTo, path);
        } else if (content.get(0).containsKey("hash")) {
            @SuppressWarnings("unchecked")
            String base64Hash = ((Map<String, String>)content.get(0).get("hash")).get("BYTES_VALUE");
            byte[] hash = Base64.decode(base64Hash);
            Address contentPathAddress = new Address("core-service", "server-environment");
            op = new ReadAttribute(contentPathAddress, "content-dir");
            result = getASConnection().execute(op);

            String contentPath = (String) result.getResult();
            deploymentFile = getDeploymentFileFromHash(hash, contentPath);
        } else {
            log.warn("Failed to determine the deployment file of " + getAddress() + " deployment. Neither path nor hash attributes were available.");
        }

        return deploymentFile;
    }

    private File getDeploymentFileFromPath(String relativeTo, String path) {
        if (relativeTo == null || relativeTo.trim().isEmpty()) {
            return new File(path);
        } else {
            //Transform the property name into the name used in the server environment
            if (relativeTo.startsWith("jboss.server")) {
                relativeTo = relativeTo.substring("jboss.server.".length());
                relativeTo = relativeTo.replace('.', '-');

                //now look for the transformed relativeTo in the server environment
                Operation op = new ReadAttribute(new Address("core-service", "server-environment"), relativeTo);
                Result res = getASConnection().execute(op);

                relativeTo = (String) res.getResult();

                return new File(relativeTo, path);
            } else {
                log.warn("Unsupported property used as a base for deployment path specification: " + relativeTo);
                return null;
            }
        }
    }

    private File getDeploymentFileFromHash(byte[] hash, String contentPath) {
        String hashStr = ByteUtil.toHexString(hash);

        String head = hashStr.substring(0, 2);
        String tail = hashStr.substring(2);

        File hashPath = new File(new File(head, tail), "content");

        return new File(contentPath, hashPath.getPath());
    }

    /**
     * Retrieve SHA256 for a deployed app.
     *
     * Shamelessly copied from the AS5 plugin.
     *
     * @param file application file
     * @return SHA256 of the content
     */
    private String getSHA256(File file) {
        String sha256 = null;

        try {
            FileContentDelegate fileContentDelegate = new FileContentDelegate();
            sha256 = fileContentDelegate.retrieveDeploymentSHA(file, context.getResourceDataDirectory());
        } catch (Exception iex) {
            if (log.isDebugEnabled()) {
                log.debug("Problem calculating digest of package [" + file.getPath() + "]." + iex.getMessage());
            }
        }

        return sha256;
    }

    /**
     * Shamelessly copied from the AS5 plugin.
     *
     * @param sha256
     * @return
     */
    private static String getVersion(String sha256) {
        return "[sha256=" + sha256 + "]";
    }

    /**
     * Retrieve the display version for the component. The display version should be stored
     * in the manifest of the application (implementation and/or specification version).
     * It will attempt to retrieve the version for both archived or exploded deployments.
     *
     * Shamelessly copied from the AS5 plugin
     *
     * @param file component file
     * @return
     */
    private String getDisplayVersion(File file) {
        //JarContentFileInfo extracts the version from archived and exploded deployments
        ContentFileInfo contentFileInfo = new JarContentFileInfo(file);
        return contentFileInfo.getVersion(null);
    }

    private String getDeploymentName() {
        Operation op = new ReadAttribute(getAddress(), "name");
        Result res = getASConnection().execute(op);

        return (String) res.getResult();
    }
}
