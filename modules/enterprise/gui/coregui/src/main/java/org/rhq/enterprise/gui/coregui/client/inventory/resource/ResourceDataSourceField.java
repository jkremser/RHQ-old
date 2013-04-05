/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

public enum ResourceDataSourceField {

    NAME("name", CoreGUI.getMessages().common_title_name()),

    DESCRIPTION("description", CoreGUI.getMessages().common_title_description()),

    LOCATION("location", CoreGUI.getMessages().dataSource_resources_field_location()),

    KEY("resourceKey", CoreGUI.getMessages().dataSource_resources_field_key()),

    TYPE("resourceType.name", CoreGUI.getMessages().common_title_type()),
    
    TYPE_ID("resourceType.id", CoreGUI.getMessages().common_title_type() + CoreGUI.getMessages().common_title_id()),

    PLUGIN("pluginName", CoreGUI.getMessages().common_title_plugin()),

    VERSION("version", CoreGUI.getMessages().common_title_version()),

    CATEGORY("resourceType.category", CoreGUI.getMessages().common_title_category()),

    AVAILABILITY("currentAvailability", CoreGUI.getMessages().common_title_availability()),

    CTIME("ctime", CoreGUI.getMessages().dataSource_resources_field_discoveryTime()),

    ITIME("itime", CoreGUI.getMessages().dataSource_resources_field_importTime()),

    MTIME("mtime", CoreGUI.getMessages().dataSource_resources_field_lastModifiedTime()),

    MODIFIER("modifiedBy", CoreGUI.getMessages().dataSource_resources_field_lastModifier()),

    INVENTORY_STATUS("inventoryStatus", CoreGUI.getMessages().common_title_inventoryStatus()),

    PARENT_INVENTORY_STATUS("parentResource.inventoryStatus", "parent resource inventory status"); // this is not for display

    /**
     * Corresponds to a property name of Resource (e.g. resourceType.name).
     */
    private String propertyName;

    /**
     * The table header for the field or property (e.g. Type).
     */
    private String title;

    private ResourceDataSourceField(String propertyName, String title) {
        this.propertyName = propertyName;
        this.title = title;
    }

    public String propertyName() {
        return propertyName;
    }

    public String title() {
        return title;
    }

    public ListGridField getListGridField() {
        return new ListGridField(propertyName, title);
    }

    public ListGridField getListGridField(int width) {
        return new ListGridField(propertyName, title, width);
    }

}
