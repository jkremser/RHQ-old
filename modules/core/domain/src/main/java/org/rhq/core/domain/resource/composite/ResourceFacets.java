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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.EnumSet;

import org.rhq.core.domain.resource.ResourceTypeFacet;

/**
 * The set of facets a Resource supports - used to determine which quicknav icons and tabs to display in the UI.
 *
 * @author Ian Springer
 */
public class ResourceFacets implements Serializable {
    private static final long serialVersionUID = 1L;

    public static ResourceFacets NONE = new ResourceFacets(-1, false, false, false, false, false, false, false, false);
    public static ResourceFacets ALL = new ResourceFacets(-1, true, true, true, true, true, true, true, true);
    /*
     * immutable private member data makes this object safe to use in a concurrent environment, such as a 
     * concurrent-access cache of ResourceFacets objects
     */
    private final int resourceTypeId;
    private final boolean measurement;
    private final boolean event;
    private final boolean pluginConfiguration;
    private final boolean configuration;
    private final boolean operation;
    private final boolean content;
    private final boolean callTime;
    private final boolean support;
    private EnumSet<ResourceTypeFacet> facets;

    public ResourceFacets(int resourceTypeId, boolean measurement, boolean event, boolean pluginConfiguration,
        boolean configuration, boolean operation, boolean content, boolean callTime, boolean support) {
        this.resourceTypeId = resourceTypeId;
        this.measurement = measurement;
        this.event = event;
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configuration;
        this.operation = operation;
        this.content = content;
        this.callTime = callTime;
        this.support = support;
        initEnum();
    }

    public ResourceFacets(int resourceTypeId, Number measurement, Number event, Number pluginConfiguration,
        Number configuration, Number operation, Number content, Number callTime, Number support) {
        this.resourceTypeId = resourceTypeId;
        this.measurement = measurement.intValue() != 0;
        this.event = event.intValue() != 0;
        this.pluginConfiguration = pluginConfiguration.intValue() != 0;
        this.configuration = configuration.intValue() != 0;
        this.operation = operation.intValue() != 0;
        this.content = content.intValue() != 0;
        this.callTime = callTime.intValue() != 0;
        this.support = support.intValue() != 0;
        initEnum();
    }

    public int getResourceTypeId() {
        return resourceTypeId;
    }

    /**
     * Does this resource expose any metrics? (currently not used for anything in the GUI, since the Monitor and Alert
     * tabs are always displayed).
     *
     * @return true if the resource exposes any metrics, false otherwise
     */
    public boolean isMeasurement() {
        return measurement;
    }

    /**
     * Does this resource have any event definitions? 
     *
     * @return true if the resource has any event definitions
     */
    public boolean isEvent() {
        return event;
    }

    /**
     * Does this resource have a plugin configuration? If so, the Inventory>Connection subtab will be displayed in the 
     * GUI.
     *
     * @return true if the resource has a plugin configuration, false otherwise
     */
    public boolean isPluginConfiguration() {
        return pluginConfiguration;
    }

    /**
     * Does this resource expose its configuration? If so, the Configure tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its configuration, false otherwise
     */
    public boolean isConfiguration() {
        return configuration;
    }

    /**
     * Does this resource expose any operations? If so, the Operations tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its operations, false otherwise
     */
    public boolean isOperation() {
        return operation;
    }

    /**
     * Does this resource expose any content? If so, the Content tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its content, false otherwise
     */
    public boolean isContent() {
        return content;
    }

    /**
     * Does this resource expose any call-time metrics? If so, the Call Time sub-tab will be displayed in the GUI.
     *
     * @return true if the resource exposes any call-time metrics, false otherwise
     */
    public boolean isCallTime() {
        return callTime;
    }

    /**
     * Does this resource expose support snapshot capability? If so, the Support sub-tab will be displayed in the GUI.
     *
     * @return true if the resource allows support snapshots, false otherwise
     */
    public boolean isSupport() {
        return support;
    }

    /**
     * Returns an enum representation of the facets.
     *
     * @return an enum representation of the facets
     */
    public EnumSet<ResourceTypeFacet> getFacets() {
        return facets;
    }

    private void initEnum() {
        this.facets = EnumSet.noneOf(ResourceTypeFacet.class);
        if (measurement)
            this.facets.add(ResourceTypeFacet.MEASUREMENT);
        if (event)
            this.facets.add(ResourceTypeFacet.EVENT);
        if (pluginConfiguration)
            this.facets.add(ResourceTypeFacet.PLUGIN_CONFIGURATION);
        if (configuration)
            this.facets.add(ResourceTypeFacet.CONFIGURATION);
        if (operation)
            this.facets.add(ResourceTypeFacet.OPERATION);
        if (content)
            this.facets.add(ResourceTypeFacet.CONTENT);
        if (callTime)
            this.facets.add(ResourceTypeFacet.CALL_TIME);
        if (support)
            this.facets.add(ResourceTypeFacet.SUPPORT);
    }
}