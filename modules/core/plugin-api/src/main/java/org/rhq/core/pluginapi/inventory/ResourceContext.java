/*
 * RHQ Management Platform
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
package org.rhq.core.pluginapi.inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;

/**
 * The context object that {@link ResourceComponent} objects will have access - it will have all the information that
 * the resource components needs during their lifetime.
 *
 * <p>This context class is currently designed to be an immutable object. Instances of this context object are to be
 * created by the plugin container only.</p>
 *
 * @param  <T> the parent resource component type for this component. This means you can nest a hierarchy of resource
 *             components that mimic the resource type hierarchy as defined in a plugin deployment descriptor.
 *
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class ResourceContext<T extends ResourceComponent<?>> {
    private final String resourceKey;
    private final ResourceType resourceType;
    private final String version;
    private final T parentResourceComponent;
    private final Configuration pluginConfiguration;
    private final SystemInfo systemInformation;
    private final ResourceDiscoveryComponent<T> resourceDiscoveryComponent;
    private final File temporaryDirectory;
    private final File dataDirectory;
    private final String pluginContainerName;
    private final EventContext eventContext;
    private final OperationContext operationContext;
    private final ContentContext contentContext;
    private final Executor availCollectionThreadPool;
    private final PluginContainerDeployment pluginContainerDeployment;

    private ProcessInfo processInfo;

    /**
     * Creates a new {@link ResourceContext} object. The plugin container is responsible for instantiating these
     * objects; plugin writers should never have to actually create context objects.
     *
     * @param resource                   the resource whose {@link org.rhq.core.pluginapi.inventory.ResourceComponent}
     *                                   will be given this context object of the plugin
     * @param parentResourceComponent    the parent component of the context's associated resource component
     * @param resourceDiscoveryComponent the discovery component that can be used to detect other resources of the same
     *                                   type as this resource (may be <code>null</code>)
     * @param systemInfo                 information about the system on which the plugin and its plugin container are
     *                                   running
     * @param temporaryDirectory         a temporary directory for plugin use that is destroyed at plugin container shutdown
     * @param dataDirectory              a directory where plugins can store persisted data that survives plugin container restarts
     * @param pluginContainerName        the name of the plugin container in which the discovery component is running.
     *                                   Components can be assured this name is unique across <b>all</b> plugin
     *                                   containers/agents running in the RHQ environment.
     * @param eventContext               an {@link EventContext}, if the resource supports one or more types of
     *                                   {@link org.rhq.core.domain.event.Event}s, or <code>null</code> otherwise
     * @param operationContext           an {@link OperationContext} the plugin can use to interoperate with the
     *                                   operation manager
     * @param contentContext             a {@link ContentContext} the plugin can use to interoperate with the content
     *                                   manager
     * @param availCollectorThreadPool   a thread pool that can be used by the plugin component should it wish
     *                                   or need to perform asynchronous availability checking. See the javadoc on
     *                                   {@link AvailabilityCollectorRunnable} for more information on this.
     * @param pluginContainerDeployment  indicates where the plugin container is running
     */
    public ResourceContext(Resource resource, T parentResourceComponent,
        ResourceDiscoveryComponent<T> resourceDiscoveryComponent, SystemInfo systemInfo, File temporaryDirectory,
        File dataDirectory, String pluginContainerName, EventContext eventContext, OperationContext operationContext,
        ContentContext contentContext, Executor availCollectorThreadPool,
        PluginContainerDeployment pluginContainerDeployment) {

        this.resourceKey = resource.getResourceKey();
        this.resourceType = resource.getResourceType();
        this.version = resource.getVersion();
        this.parentResourceComponent = parentResourceComponent;
        this.resourceDiscoveryComponent = resourceDiscoveryComponent;
        this.systemInformation = systemInfo;
        this.pluginConfiguration = resource.getPluginConfiguration();
        this.dataDirectory = dataDirectory;
        this.pluginContainerName = pluginContainerName;
        this.pluginContainerDeployment = pluginContainerDeployment;
        if (temporaryDirectory == null) {
            this.temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "AGENT_TMP");
            this.temporaryDirectory.mkdirs();
        } else {
            this.temporaryDirectory = temporaryDirectory;
        }

        this.eventContext = eventContext;
        this.operationContext = operationContext;
        this.contentContext = contentContext;
        this.availCollectionThreadPool = availCollectorThreadPool;
    }

    /**
     * The {@link Resource#getResourceKey() resource key} of the resource this context is associated with. This resource
     * key is unique across all of the resource's siblings. That is to say, this resource key is unique among all
     * children of the {@link #getParentResourceComponent() parent}.
     *
     * @return resource key of the associated resource
     */
    public String getResourceKey() {
        return this.resourceKey;
    }

    /**
     * The {@link Resource#getResourceType() resource type} of the resource this context is associated with.
     *
     * @return type of the associated resource
     */
    public ResourceType getResourceType() {
        return this.resourceType;
    }

    /**
     * The {@link Resource#getVersion() version} of the resource this context is associated with.
     *
     * @return the resource's version string
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * The parent of the resource component that is associated with this context.
     *
     * @return parent component of the associated resource component
     */
    public T getParentResourceComponent() {
        return this.parentResourceComponent;
    }

    /**
     * Returns a {@link SystemInfo} object that contains information about the platform/operating system that the
     * resource is running on. With this object, you can natively obtain things such as the operating system name, its
     * hostname,and other things. Please refer to the javadoc on {@link SystemInfo} for more details on the types of
     * information you can access.
     *
     * @return system information object
     */
    public SystemInfo getSystemInformation() {
        return this.systemInformation;
    }

    /**
     * Returns the resource's plugin configuration. This is used to configure the subsystem that is used to actually
     * talk to the managed resource. Do not confuse this with the <i>resource configuration</i>, which is the actual
     * configuration settings for the managed resource itself.
     *
     * @return plugin configuration
     */
    public Configuration getPluginConfiguration() {
        return this.pluginConfiguration.deepCopy();
    }

    /**
     * Returns the information on the native operating system process in which the managed resource is running. If
     * native support is not available or the process for some reason can no longer be found, this may return <code>
     * null</code>.
     *
     * @return information on the resource's process
     */
    public ProcessInfo getNativeProcess() {
        boolean rediscover = this.processInfo == null;

        if (!rediscover) {
            //if the process info thinks the process is running,
            //refresh it to check its facts again
            if (this.processInfo.isRunning()) {
                this.processInfo.refresh();
            }
            rediscover = !this.processInfo.isRunning();
        }

        if (rediscover) {
            //This method is documented to return null if the process can no longer be found.
            //Let's make sure that's the case and null it out now. The discovery might or might not
            //reassign it.
            this.processInfo = null;
            if (this.resourceDiscoveryComponent != null) {
                try {
                    Set<DiscoveredResourceDetails> details;
                    ResourceDiscoveryContext<T> context;

                    context = new ResourceDiscoveryContext<T>(this.resourceType, this.parentResourceComponent, this,
                        this.systemInformation, getNativeProcessesForType(), Collections.EMPTY_LIST,
                        getPluginContainerName(), getPluginContainerDeployment());

                    details = this.resourceDiscoveryComponent.discoverResources(context);

                    for (DiscoveredResourceDetails detail : details) {
                        if (detail.getResourceKey().equals(this.resourceKey)) {
                            this.processInfo = detail.getProcessInfo();
                        }
                    }
                } catch (Exception e) {
                    LogFactory.getLog(getClass()).warn(
                        "Cannot get native process for resource [" + this.resourceKey + "] - discovery failed", e);
                }
            }
        }

        return this.processInfo;
    }

    /**
     * Scans the current list of running processes and returns information on all processes that may contain resources
     * of the {@link #getResourceType() same type as this resource}. More specifically, this method will scan all the
     * processes and try to match them up with the {@link ResourceType#getProcessScans() PIQL queries} associated with
     * this resource's type.
     *
     * @return information on the processes that may be running this resource or other resources of the same type
     *
     * @see    ResourceType#getProcessScans()
     */
    public List<ProcessScanResult> getNativeProcessesForType() {
        // perform auto-discovery PIQL queries now to see if we can auto-detect resources that are running now of this type
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        try {
            Set<ProcessScan> processScans = this.resourceType.getProcessScans();
            if (processScans != null && !processScans.isEmpty()) {
                ProcessInfoQuery piq = new ProcessInfoQuery(systemInfo.getAllProcesses());
                for (ProcessScan processScan : processScans) {
                    List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
                    if ((queryResults != null) && (queryResults.size() > 0)) {
                        for (ProcessInfo autoDiscoveredProcess : queryResults) {
                            scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                        }
                    }
                }
            }
        } catch (UnsupportedOperationException uoe) {
        }

        return scanResults;
    }

    /**
     * A temporary directory for plugin use that is destroyed at plugin container shutdown. Plugins should use this if they need to
     * write temporary files that they do not expect to remain after the plugin container is restarted. This directory is shared
     * among all plugins - plugins must ensure they write unique files here, as other plugins may be using this same
     * directory. Typically, plugins will use the {@link File#createTempFile(String, String, File)} API when writing to
     * this directory.
     *
     * @return location for plugin temporary files
     */
    public File getTemporaryDirectory() {
        return temporaryDirectory;
    }

    /**
     * Directory where plugins can store persisted data that survives plugin container restarts. Each plugin will have their own
     * data directory. The returned directory may not yet exist - it is up to each individual plugin to manage this
     * directory as they see fit (this includes performing the initial creation when the directory is first needed).
     *
     * @return location for plugins to store persisted data
     */
    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * The name of the plugin container in which the resource component is running. Components
     * can be assured this name is unique across <b>all</b> plugin containers/agents running
     * in the RHQ environment.
     * 
     * @return the name of the plugin container
     */
    public String getPluginContainerName() {
        return pluginContainerName;
    }

    /**
     * Indicates where the plugin container (and therefore where the plugins) are deployed and running.
     * See {@link PluginContainerDeployment} for more information on what the return value means.
     * 
     * @return indicator of where the plugin container is deployed and running
     */
    public PluginContainerDeployment getPluginContainerDeployment() {
        return pluginContainerDeployment;
    }

    /**
     * Returns an {@link EventContext}, if the resource supports one or more types of
     * {@link org.rhq.core.domain.event.Event}s, or <code>null</code> otherwise.
     *
     * @return an <code>EventContext</code>, if the resource supports one or more types of
     *         {@link org.rhq.core.domain.event.Event}s, or <code>null</code> otherwise
     */
    public EventContext getEventContext() {
        return eventContext;
    }

    /**
     * Returns an {@link OperationContext} that allows the plugin to access the operation functionality provided by the
     * plugin container.
     *
     * @return operation context object
     */
    public OperationContext getOperationContext() {
        return operationContext;
    }

    /**
     * Returns a {@link ContentContext} that allows the plugin to access the content functionality provided by the
     * plugin container.
     *
     * @return content context object
     */
    public ContentContext getContentContext() {
        return contentContext;
    }

    /**
     * Under certain circumstances, a resource component may want to perform asynchronous availability checks, as
     * opposed to {@link AvailabilityFacet#getAvailability()} blocking waiting for the managed resource to return
     * its availability status. Using asynchronous availability checking frees the resource component from having
     * to guarantee that the managed resource will provide availability status in a timely fashion.
     * 
     * If the resource component needs to perform asynchronous availability checking, it should call this
     * method to create an instance of {@link AvailabilityCollectorRunnable} inside the {@link ResourceComponent#start} method.
     * It should then call the returned object's {@link AvailabilityCollectorRunnable#start()} method within the same resource
     * component {@link ResourceComponent#start(ResourceContext)} method. The resource component should call the
     * {@link AvailabilityCollectorRunnable#stop()} method when the resource component
     * {@link ResourceComponent#stop() stops}. The resource component's {@link AvailabilityFacet#getAvailability()} method
     * should simply return the value returned by {@link AvailabilityCollectorRunnable#getLastKnownAvailability()}. This
     * method will be extremely fast since it simply returns the last availability that was retrieved by the
     * given availability checker. Only when the availability checker finishes checking for availability of the managed resource
     * (however long it takes to do so) will the last known availability state change.
     * 
     * For more information, read the javadoc in {@link AvailabilityCollectorRunnable}.
     *
     * @param availChecker the object that will perform the actual check of the managed resource's availability
     * @param interval the interval, in milliseconds, between availability checks. The minimum value allowed
     *                 for this parameter is {@link AvailabilityCollectorRunnable#MIN_INTERVAL}.
     *
     * @return the availability collector runnable that will perform the asynchronous checking
     */
    public AvailabilityCollectorRunnable createAvailabilityCollectorRunnable(AvailabilityFacet availChecker,
        long interval) {
        // notice that we assume we are called with the same context classloader that will be need by the avail checker
        return new AvailabilityCollectorRunnable(availChecker, interval,
            Thread.currentThread().getContextClassLoader(), this.availCollectionThreadPool);
    }
}