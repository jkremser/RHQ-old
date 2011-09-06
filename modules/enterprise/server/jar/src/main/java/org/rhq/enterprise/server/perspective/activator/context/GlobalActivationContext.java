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
package org.rhq.enterprise.server.perspective.activator.context;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.perspective.activator.LicenseFeature;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class GlobalActivationContext implements ActivationContext {
    private Subject subject;

    private transient EnumSet<LicenseFeature> licenseFeatures;
    private transient Boolean superuser;
    private transient EnumSet<Permission> globalPermissions;

    public GlobalActivationContext(Subject subject) {
        this.subject = subject;
    }

    public ActivationContextScope getScope() {
        return ActivationContextScope.GLOBAL;
    }

    @NotNull
    public EnumSet<LicenseFeature> getLicenseFeatures() {
        // lazy load
        if (this.licenseFeatures == null) {
            this.licenseFeatures = EnumSet.noneOf(LicenseFeature.class);
            this.licenseFeatures.add(LicenseFeature.MONITORING); // always give this permission
        }
        return this.licenseFeatures;
    }

    public Subject getSubject() {
        return this.subject;
    }

    public boolean isSuperuser() {
        // lazy load
        if (this.superuser == null) {
            AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
            this.superuser = authorizationManager.isSystemSuperuser(this.subject);
        }
        return this.superuser;
    }

    public boolean hasGlobalPermission(Permission permission) {
        return isSuperuser() || getGlobalPermissions().contains(permission);
    }

    protected EnumSet<Permission> getGlobalPermissions() {
        // lazy load
        if (this.globalPermissions == null) {
            AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
            Set<Permission> perms = authorizationManager.getExplicitGlobalPermissions(this.subject);
            this.globalPermissions = EnumSet.noneOf(Permission.class);
            this.globalPermissions.addAll(perms);
        }
        return this.globalPermissions;
    }
}