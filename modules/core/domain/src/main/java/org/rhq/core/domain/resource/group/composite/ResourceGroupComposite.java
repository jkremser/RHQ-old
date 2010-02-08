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
package org.rhq.core.domain.resource.group.composite;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 * @author Joseph Marques
 */
public class ResourceGroupComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    ////JAXB Needs no args constructor and final fields make that difficult. 

    private Double implicitAvail;
    private Double explicitAvail;
    private ResourceGroup resourceGroup;

    private GroupCategory category;
    private long implicitUp;
    private long implicitDown;
    private long explicitUp;
    private long explicitDown;

    private ResourceFacets resourceFacets;

    private class GroupDefinitionMember extends ResourceGroup {
        public void setGroupCategory(GroupCategory category) {
            super.setGroupCategory(category);
        }
    }

    //def no args constructor for JAXB
    private ResourceGroupComposite() {
    }

    public ResourceGroupComposite(long explicitCount, double explicitAvailability, long implicitCount,
        double implicitAvailability, ResourceGroup resourceGroup) {
        this(explicitCount, explicitAvailability, implicitCount, implicitAvailability, resourceGroup, null);
    }

    public ResourceGroupComposite(long explicitCount, double explicitAvailability, long implicitCount,
        double implicitAvailability, ResourceGroup resourceGroup, ResourceFacets facets) {

        explicitUp = Math.round(explicitCount * explicitAvailability);
        explicitDown = explicitCount - explicitUp;
        if (explicitUp + explicitDown > 0) {
            // keep explicitAvail null if there are no explicit resources in the group
            explicitAvail = explicitAvailability;
        } else {
            explicitAvail = null;
        }

        implicitUp = Math.round(implicitCount * implicitAvailability);
        implicitDown = implicitCount - implicitUp;
        if (implicitUp + implicitDown > 0) {
            // keep implicitAvail null if there are no implicit resources in the group
            implicitAvail = implicitAvailability;
        } else {
            implicitAvail = null;
        }

        this.resourceGroup = resourceGroup;

        if (this.resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE) {
            this.category = GroupCategory.COMPATIBLE;
        } else if (this.resourceGroup.getGroupCategory() == GroupCategory.MIXED) {
            this.category = GroupCategory.MIXED;
        } else {
            throw new IllegalArgumentException("Unknown category " + this.resourceGroup.getGroupCategory()
                + " for ResourceGroup " + this.resourceGroup.getName());
        }

        this.resourceFacets = facets;
    }

    public Double getImplicitAvail() {
        return this.implicitAvail;
    }

    public Double getExplicitAvail() {
        return this.explicitAvail;
    }

    public ResourceGroup getResourceGroup() {
        return this.resourceGroup;
    }

    public GroupCategory getCategory() {
        return this.category;
    }

    public long getImplicitUp() {
        return this.implicitUp;
    }

    public long getImplicitDown() {
        return this.implicitDown;
    }

    public long getExplicitUp() {
        return this.explicitUp;
    }

    public long getExplicitDown() {
        return this.explicitDown;
    }

    public String getExplicitFormatted() {
        return getAlignedAvailabilityResults(getExplicitUp(), getExplicitDown());
    }

    public String getImplicitFormatted() {
        return getAlignedAvailabilityResults(getImplicitUp(), getImplicitDown());
    }

    @XmlTransient
    public void setResourceFacets(ResourceFacets facets) {
        this.resourceFacets = facets;
    }

    public ResourceFacets getResourceFacets() {
        return resourceFacets;
    }

    /**
     * Returns a query string snippet that can be passed to group URLs that reference this specific group.
     * Note that the returned string does not include the "?" itself.
     * 
     * @return query string snippet that can appear after the "?" in group URLs.
     */
    public String getGroupQueryString() {
        return "category=" + getCategory().getName() + "&amp;groupId=" + getResourceGroup().getId();
    }

    private String getAlignedAvailabilityResults(long up, long down) {
        StringBuilder results = new StringBuilder();
        results.append("<table width=\"120px\"><tr>");
        if (up == 0 && down == 0) {
            results.append(getColumn(false, "<img src=\"/images/icons/availability_grey_16.png\" /> 0"));
            results.append(getColumn(true));
            results.append(getColumn(false));
        } else {
            if (up > 0) {
                results.append(getColumn(false, " <img src=\"/images/icons/availability_green_16.png\" />", up));
            }

            if (up > 0 && down > 0) {
                results.append(getColumn(true)); // , " / ")); // use a vertical separator image if we want a separator
            }

            if (down > 0) {
                results.append(getColumn(false, " <img src=\"/images/icons/availability_red_16.png\" />", down));
            } else {
                results.append(getColumn(false,
                    "&nbsp;&nbsp;<img src=\"/images/blank.png\" width=\"16px\" height=\"16px\" />"));
            }
        }
        results.append("</tr></table>");
        return results.toString();
    }

    private String getColumn(boolean isSpacerColumn, Object... data) {
        StringBuilder results = new StringBuilder();
        if (isSpacerColumn) {
            results.append("<td nowrap=\"nowrap\" style=\"white-space:nowrap;\" width=\"10px\" align=\"left\" >");
        } else {
            results.append("<td nowrap=\"nowrap\" style=\"white-space:nowrap;\" width=\"55px\" align=\"left\" >");
        }
        if (data == null) {
            results.append("&nbsp;");
        } else {
            for (Object datum : data) {
                results.append(datum == null ? "&nbsp;" : datum);
            }
        }
        results.append("</td>");
        return results.toString();
    }

    @Override
    public String toString() {
        return "ResourceGroupComposite[name="
            + this.resourceGroup.getName() //
            + ", implicit[up/down/avail=," + this.implicitUp + "/" + this.implicitDown + "/" + this.implicitAvail + "]"
            + ", explicit[up/down/avail=," + this.explicitUp + "/" + this.explicitDown + "/" + this.explicitAvail + "]"
            + ", permission=" + "]";
    }
}