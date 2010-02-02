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
package org.rhq.enterprise.server.perspective.activator;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.perspective.activator.context.AbstractResourceOrGroupActivationContext;

/**
 * @author Ian Springer
 */
public class ResourceTypeActivator extends AbstractResourceOrGroupActivator {
    static final long serialVersionUID = 1L;

    private List<ResourceConditionSet> resourceConditionSets;

    public ResourceTypeActivator(List<ResourceConditionSet> resourceConditionSets) {
        this.resourceConditionSets = resourceConditionSets;
    }

    /**
     * Returns true if the current Resource or compatible Group matches at least one of our resource condition sets.
     *
     * @param context
     * @return
     */
    public boolean isActive(AbstractResourceOrGroupActivationContext context) {
        for (ResourceConditionSet resourceConditionSet : this.resourceConditionSets) {
            if (context.getResourceType().getPlugin().equals(resourceConditionSet.getPluginName())
                && context.getResourceType().getName().equals(resourceConditionSet.getResourceTypeName())) {
                if (hasResourcePermissions(context, resourceConditionSet.getPermissions())) {
                    if (hasTraits(context, resourceConditionSet.getTraitMatchers())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasResourcePermissions(AbstractResourceOrGroupActivationContext context,
        EnumSet<Permission> resourcePermissions) {
        for (Permission permission : resourcePermissions) {
            if (!context.hasResourcePermission(permission)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasTraits(AbstractResourceOrGroupActivationContext context, Map<String, Matcher> traitMatchers) {

        if (traitMatchers.isEmpty()) {
            return true;
        }

        Collection<Resource> resources = context.getResources();
        return ActivatorHelper.areTraitsSatisfied(context.getSubject(), traitMatchers, resources, true);
    }
}