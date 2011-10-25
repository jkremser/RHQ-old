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

package org.rhq.enterprise.gui.coregui.client.inventory.groups;

import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

public enum ResourceGroupDataSourceField {

    NAME("name", CoreGUI.getMessages().common_title_name()),

    DESCRIPTION("description", CoreGUI.getMessages().common_title_description()),

    TYPE("resourceTypeName", CoreGUI.getMessages().common_title_type()),

    PLUGIN("pluginName", CoreGUI.getMessages().common_title_plugin()),

    CATEGORY("groupCategory", CoreGUI.getMessages().common_title_category()),

    AVAIL_CHILDREN("availabilityChildren", CoreGUI.getMessages().view_inventory_groups_children()),

    AVAIL_DESCENDANTS("availabilityDescendants", CoreGUI.getMessages().view_inventory_groups_descendants());

    /**
     * Corresponds to a property name of Resource
     */
    private String propertyName;

    /**
     * The display name for the field or property
     */
    private String title;

    private ResourceGroupDataSourceField(String propertyName, String title) {
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
