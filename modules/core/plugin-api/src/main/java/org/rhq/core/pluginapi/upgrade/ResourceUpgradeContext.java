/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.pluginapi.upgrade;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.InventoryContext;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext.Builder;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.system.SystemInfo;

/**
 * Represents a resource during the resource upgrade phase of discovery.
 * 
 * @see ResourceUpgradeFacet
 *
 * @since 3.0
 * @author Lukas Krejci
 * @author Jiri Kremser
 */
public class ResourceUpgradeContext<T extends ResourceComponent<?>> extends ResourceContext<T> {

    private final Configuration resourceConfiguration;
    private final String name;
    private final String description;

    /**
     * Don't call it directly, use {@link ResourceContext.Builder} instead.
     * @param builder the initialized builder instance
     * @see ResourceContext#ResourceContext(org.rhq.core.domain.resource.Resource, org.rhq.core.pluginapi.inventory.ResourceComponent, org.rhq.core.pluginapi.inventory.ResourceContext, org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent, org.rhq.core.system.SystemInfo, java.io.File, java.io.File, String, org.rhq.core.pluginapi.event.EventContext, org.rhq.core.pluginapi.operation.OperationContext, org.rhq.core.pluginapi.content.ContentContext, java.util.concurrent.Executor, org.rhq.core.pluginapi.inventory.PluginContainerDeployment)
     *
     * @since 4.0
     */
    private ResourceUpgradeContext(Builder<T> builder) {
        super(builder);
        this.resourceConfiguration = builder.resourceConfiguration;
        this.name = builder.name;
        this.description = builder.description;
    }

    /**
     * Returns the context of the Resource component's parent Resource component.
     *
     * @return the context of the Resource component's parent Resource component
     *
     * @since 4.0
     */
    @Override
    public ResourceContext<?> getParentResourceContext() {
        return super.getParentResourceContext();
    }

    public Configuration getResourceConfiguration() {
        return resourceConfiguration;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    
    /**
     * Creates a new {@link ResourceUpgradeContext} object.
     */
    public static class Builder<T extends ResourceComponent<?>> extends ResourceContext.Builder<T> {
        private Configuration resourceConfiguration;
        private String name;
        private String description;
        
        @Override
        public Builder<T> withAvailabilityContext(AvailabilityContext availabilityContext) {
            return (Builder<T>) super.withAvailabilityContext(availabilityContext);
        }
        
        @Override
        public Builder<T> withBinDirectory(File binDirectory) {
            return (Builder<T>) super.withBinDirectory(binDirectory);
        }
        
        @Override
        public Builder<T> withContentContext(ContentContext contentContext) {
            return (Builder<T>) super.withContentContext(contentContext);
        }
        
        @Override
        public Builder<T> withDataDirectory(File dataDirectory) {
            return (Builder<T>) super.withDataDirectory(dataDirectory);
        }
        
        @Override
        public Builder<T> withEventContext(EventContext eventContext) {
            return (Builder<T>) super.withEventContext(eventContext);
        }
        
        @Override
        public Builder<T> withInventoryContext(InventoryContext inventoryContext) {
            return (Builder<T>) super.withInventoryContext(inventoryContext);
        }
        
        @Override
        public Builder<T> withOperationContext(OperationContext operationContext) {
            return (Builder<T>) super.withOperationContext(operationContext);
        }
        
        @Override
        public Builder<T> withParentResourceComponent(T parentResourceComponent) {
            return (Builder<T>) super.withParentResourceComponent(parentResourceComponent);
        }
        
        @Override
        public Builder<T> withParentResourceContext(ResourceContext<?> parentResourceContext) {
            return (Builder<T>) super.withParentResourceContext(parentResourceContext);
        }
        
        @Override
        public Builder<T> withPluginContainerDeployment(PluginContainerDeployment pluginContainerDeployment) {
            return (Builder<T>) super.withPluginContainerDeployment(pluginContainerDeployment);
        }
        
        @Override
        public Builder<T> withPluginContainerName(String pluginContainerName) {
            return (Builder<T>) super.withPluginContainerName(pluginContainerName);
        }
        
        @Override
        public Builder<T> withResource(Resource resource) {
            return (Builder<T>) super.withResource(resource);
        }
        
        @Override
        public Builder<T> withResourceDiscoveryComponent(ResourceDiscoveryComponent<T> resourceDiscoveryComponent) {
            return (Builder<T>) super.withResourceDiscoveryComponent(resourceDiscoveryComponent);
        }
        
        @Override
        public Builder<T> withSystemInformation(SystemInfo systemInformation) {
            return (Builder<T>) super.withSystemInformation(systemInformation);
        }
        
        @Override
        public Builder<T> withTemporaryDirectory(File temporaryDirectory) {
            return (Builder<T>) super.withTemporaryDirectory(temporaryDirectory);
        }
        
        /**
         * Don't forget to initialize the state before calling this method.
         * 
         * @return the ResourceUpgradeContext instance
         */
        @Override
        public ResourceUpgradeContext<T> build() {
            this.resourceConfiguration = resource.getResourceConfiguration();
            this.name = resource.getName();
            this.description = resource.getDescription();
            return new ResourceUpgradeContext<T>(this);
        }
    }

}
