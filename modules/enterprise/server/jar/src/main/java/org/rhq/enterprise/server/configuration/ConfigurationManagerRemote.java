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
package org.rhq.enterprise.server.configuration;

import java.util.Map;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.jaxb.WebServiceTypeAdapter;
import org.rhq.enterprise.server.jaxb.adapter.ConfigurationAdapter;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * The configuration manager which allows you to request resource configuration changes, view current resource
 * configuration and previous update history and view/edit plugin configuration.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface ConfigurationManagerRemote {

    @WebMethod
    GroupPluginConfigurationUpdate getGroupPluginConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "configurationUpdateId") int configurationUpdateId);

    @WebMethod
    GroupResourceConfigurationUpdate getGroupResourceConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "configurationUpdateId") int configurationUpdateId);

    @WebMethod
    Configuration getConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "configurationId") int configurationId);

    /**
     * Get the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     * resource's plugin configuration is not yet initialized.
     *
     * @param  user             The logged in user's subject.
     * @param  resourceId       Resource Id
     *
     * @return the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     *         resource's configuration is not yet initialized
     * @throws FetchException
     */
    @Nullable
    @WebMethod
    @XmlJavaTypeAdapter(ConfigurationAdapter.class)
    Configuration getPluginConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    /**
     * Get the current Resource configuration.
     * @param  subject             The logged in user's subject.
     * @param resourceId        A resource id.
     * @return The specified configuration.
     * 
     * @throws FetchException In case where there was a problem fetching the resource configuration
     */
    @WebMethod
    @XmlJavaTypeAdapter(ConfigurationAdapter.class)
    Configuration getResourceConfiguration(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    @WebMethod
    PluginConfigurationUpdate getLatestPluginConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    @WebMethod
    ResourceConfigurationUpdate getLatestResourceConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    /**
     * Get whether the the specified resource is in the process of updating its configuration.
     * @param subject          The logged in user's subject.
     * @param resourceId       A resource id.
     * @return True if in progress, else False.
     * @throws FetchException
     */
    @WebMethod
    boolean isResourceConfigurationUpdateInProgress( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    @WebMethod
    boolean isGroupResourceConfigurationUpdateInProgress( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceGroupId") int resourceGroupId);

    /* this currently doesn't build because jaxws requires a default, no-arg constructor from all objects in the graph
     * in order to perform serialization correctly, and java.util.Map does not have one (because it's an interface)
     * */
    @WebMethod
    int scheduleGroupResourceConfigurationUpdate(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "compatibleGroupId") int compatibleGroupId, //
        @XmlJavaTypeAdapter(WebServiceTypeAdapter.class)//
        @WebParam(name = "newResourceConfigurationMap", targetNamespace = ServerVersion.namespace) Map<Integer, Configuration> newResourceConfigurationMap);

    /**
     * Updates the plugin configuration used to connect and communicate with the resource. The given <code>
     * newConfiguration</code> is usually a modified version of a configuration returned by
     * {@link #getPluginConfiguration(Subject, int)}.
     *
     * @param  subject          The logged in user's subject.
     * @param  resourceId       a {@link Resource} id
     * @param  newConfiguration the new plugin configuration
     *
     * @return the plugin configuration update item corresponding to this request
     */
    @WebMethod
    PluginConfigurationUpdate updatePluginConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "newConfiguration")//
        @XmlJavaTypeAdapter(ConfigurationAdapter.class) Configuration newConfiguration)
        throws ResourceNotFoundException;

    /**
     * This method is called when a user has requested to change the resource configuration for an existing resource. If
     * the user does not have the proper permissions to change the resource's configuration, an exception is thrown.
     *
     * <p>This will not wait for the agent to finish the configuration update. This will return after the request is
     * sent. Once the agent finishes with the request, it will send the completed request information to
     * {@link #completedResourceConfigurationUpdate(AbstractResourceConfigurationUpdate)}.</p>
     *
     * @param  subject          The logged in user's subject.
     * @param  resourceId       identifies the resource to be updated
     * @param  newConfiguration the resource's desired new configuration
     *
     * @return the resource configuration update item corresponding to this request
     */
    @WebMethod
    ResourceConfigurationUpdate updateResourceConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "newConfiguration")//
        @XmlJavaTypeAdapter(ConfigurationAdapter.class) Configuration newConfiguration)
        throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException;

    @WebMethod
    ResourceConfigurationUpdate updateStructuredOrRawConfiguration(
        @WebParam Subject subject,
        @WebParam int resourceId,
        @WebParam
        @XmlJavaTypeAdapter(ConfigurationAdapter.class) Configuration newConfiguration,
        @WebParam boolean fromStructured)
        throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException;

    /**
     * Get the currently live resource configuration for the {@link Resource} with the given id. This actually asks for
     * the up-to-date configuration directly from the agent. An exception will be thrown if communications with the
     * agent cannot be made.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId resourceId
     * @param pingAgentFirst
     * 
     * @return the live configuration
     *
     * @throws Exception if failed to get the configuration from the agent
     */
    @WebMethod
    Configuration getLiveResourceConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "pingAgentFirst") boolean pingAgentFirst) //
        throws Exception;

    /**
     * Return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     * specified id.
     *
     * @param  subject         the user who is requesting the resource configuration definition
     * @param  resourceTypeId identifies the resource type whose resource configuration definition is being requested
     *
     * @return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     *         specified id, or <code>null</code> if the ResourceType does not define a resource configuration
     */
    @WebMethod
    ConfigurationDefinition getResourceConfigurationDefinitionForResourceType(
        @WebParam(name = "subject") Subject subject, @WebParam(name = "resourceTypeId") int resourceTypeId);

    /**
     * Return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     * specified id. The templates will be loaded in the definition returned from this call.
     *
     * @param  subject         the user who is requesting the resource configuration definition
     * @param  resourceTypeId identifies the resource type whose resource configuration definition is being requested
     *
     * @return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     *         specified id, or <code>null</code> if the ResourceType does not define a resource configuration
     */
    @WebMethod
    ConfigurationDefinition getResourceConfigurationDefinitionWithTemplatesForResourceType(
        @WebParam(name = "subject") Subject subject, @WebParam(name = "resourceTypeId") int resourceTypeId);

    /**
     * Return the plugin configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     * specified id.
     *
     * @param  subject         the user who is requesting the plugin configuration definition
     * @param  resourceTypeId identifies the resource type whose plugin configuration definition is being requested
     *
     * @return the plugin configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     *         specified id, or <code>null</code> if the ResourceType does not define a plugin configuration
     */
    @WebMethod
    ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(
        @WebParam(name = "subject") Subject subject, @WebParam(name = "resourceTypeId") int resourceTypeId);

    /**
     * Return the deploy configuration definition for the {@link org.rhq.core.domain.content.PackageType} with the
     * specified id.
     *
     * @param  subject        the user who is requesting the plugin configuration definition
     * @param  packageTypeId  identifies the package type whose configuration definition is being requested
     *
     * @return the  the deploy configuration definition for the {@link org.rhq.core.domain.content.PackageType} with the
     * specified id.
     */
    @WebMethod
    ConfigurationDefinition getPackageTypeConfigurationDefinition( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "packageTypeId") int packageTypeId);

    Configuration translateResourceConfiguration(Subject subject, int resourceId, Configuration configuration,
        boolean fromStructured) throws ResourceNotFoundException;

}