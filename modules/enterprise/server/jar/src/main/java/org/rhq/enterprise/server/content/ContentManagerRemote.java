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
package org.rhq.enterprise.server.content;

import java.util.List;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * @author Jay Shaughnessy
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface ContentManagerRemote {

    /**
     * Creates a new package version in the system. If the parent package (identified by the packageName parameter) does
     * not exist, it will be created. If a package version exists with the specified version ID, a new one will not be
     * created and the existing package version instance will be returned.
     *
     * @param subject        The logged in subject
     * @param packageName    parent package name; uniquely identifies the package under which this version goes
     * @param packageTypeId  identifies the type of package in case the general package needs to be created
     * @param version        identifies the version to be create
     * @param architectureId architecture of the newly created package version. If null then no architecture restriction.
     *
     * @return newly created package version if one did not exist; existing package version that matches these data if
     *         one was found
     */
    @WebMethod
    PackageVersion createPackageVersion( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "packageName") String packageName, //
        @WebParam(name = "packageTypeId") int packageTypeId, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architectureId") Integer architectureId, //
        @WebParam(name = "packageBytes") byte[] packageBytes);

    /**
     * Deletes the specified package from the resource.
     *
     * @param subject             The logged in subject
     * @param resourceId          identifies the resource from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    @WebMethod
    void deletePackages( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "installedPackages") int[] installedPackageIds, //
        @WebParam(name = "requestNotes") String requestNotes);

    /**
     * Deploys packages on the specified resources. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param subject           The logged in subject
     * @param resourceIds       identifies the resources against which the package will be deployed
     * @param packageVersionIds packageVersions we want to install
     */
    @WebMethod
    void deployPackages( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds, //
        @WebParam(name = "packageVersionIds") int[] packageVersionIds);

    /**
     * Returns all architectures known to the system.
     *
     * @param  subject The logged in subject
     * @return list of all architectures in the database
     */
    @WebMethod
    List<Architecture> findArchitectures( //
        @WebParam(name = "subject") Subject subject);

    /**
     * This gets the package types that can be deployed to the given resource. It is a function of the resource
     * type of the resource.
     *
     * @param subject          The logged in subject
     * @param resourceTypeName The resource type in question
     *
     * @return The requested list of package types. Can be empty.
     */
    @WebMethod
    List<PackageType> findPackageTypes( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceTypeName") String resourceTypeName, //
        @WebParam(name = "pluginName") String pluginName) throws ResourceTypeNotFoundException;

    /**
     * @param subject
     * @param criteria {@link InstalledPackageCriteria}
     * @return InstalledPackages for the criteria
     */
    @WebMethod
    PageList<InstalledPackage> findInstalledPackagesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") InstalledPackageCriteria criteria);

    /**
     * If a resourceId filter is not set via {@link PackageVersionCriteria.addFilterResourceId()} then
     * this method requires InventoryManager permissions. When set the user must have permission to view
     * the resource.
     * 
     * @param subject
     * @param criteria
     * @return Installed PackageVersions for the resource
     * @throws IllegalArgumentException for invalid resourceId filter
     */
    @WebMethod
    PageList<PackageVersion> findPackageVersionsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") PackageVersionCriteria criteria);

    /**
     * For a resource that is content-backed (aka package-backed), this call will return InstalledPackage information
     * for the backing content (package).
     *
     * @param resourceId a valid resource
     * @return The InstalledPackage object for the content-packed resource. Or null for non-existent or non-package backed resource.
     */
    @WebMethod
    InstalledPackage getBackingPackageForResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    @WebMethod
    byte[] getPackageBytes(@WebParam(name = "subject") Subject user, @WebParam(name = "resourceId") int resourceId,
        @WebParam(name = "installedPackageId") int installedPackageId);

    @WebMethod
    PackageVersion uploadPlatformPackageVersion(@WebParam(name = "subject") Subject user,
        @WebParam(name = "packageName") String packageName, //
        @WebParam(name = "packageTypeId") int packageTypeId, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architectureId") Integer architectureId, //
        @WebParam(name = "fileName") String fileName, //
        @WebParam(name = "MD5sum") String MD5sum, //
        @WebParam(name = "packageBytes") byte[] packageBytes, //
        @WebParam(name = "dbmode") boolean dbmode);

    @WebMethod
    void updatePackageVersionMetadata(@WebParam(name = "subject") Subject user, //
        @WebParam(name = "pvId") int pvId, //
        @WebParam(name = "metadata") byte[] metadata, //
        @WebParam(name = "fileSize") long fileSize);
}