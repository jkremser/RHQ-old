/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.plugin.pc.drift;

import java.io.File;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.util.PageList;

/**
 * All drift server plugins must implement this facet.  The methods here must all be defined in
 * DriftManagerLocal as well.  See DriftManagerLocal for jdoc of these methods.
 * 
 * Note that the Subject parameter is provided as a convenience to the plugin methods. It is not expected that
 * authz be performed by the plugin code. But, it may be useful to have the user name or session id for logging, or
 * for unknown need to interact with the RHQ SLSBs.
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public interface DriftServerPluginFacet {

    DriftSnapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria);

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The DriftChangeSets matching the criteria
     */
    PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria);

    PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria);

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The Drifts matching the criteria
     */
    PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria);

    /**
     * Simple get method for a DriftFile. Does not return the content.
     * @param subject
     * @param hashId
     * @return The DriftFile sans content.
     */
    DriftFile getDriftFile(Subject subject, String hashId) throws Exception;

    void saveChangeSet(Subject subject, int resourceId, File changeSetZip) throws Exception;

    void saveChangeSetFiles(Subject subject, File changeSetFilesZip) throws Exception;

    /**
     * When a user wants to completely remove all data related to a drift configuration,
     * this method will be called to give the plugin a chance to clean up any data related
     * to the drift configuration that is going to be deleted.
     * @param Subject
     * @param resourceId the resource whose drift configuration is being purged
     * @param driftConfigName identifies the data that is to be purged
     */
    void purgeByDriftConfigurationName(Subject subject, int resourceId, String driftConfigName) throws Exception;

    /**
     * This will remove all drift files that are no longer referenced by drift entries. This is a maintenance method
     * to help reclaim space on the backend.
     * 
     * @param subject
     * @param purgeMillis only those unused drift files that are older than this (in epoch millis) will be purged.
     * @return number of orphaned drife files that were removed
     */
    int purgeOrphanedDriftFiles(Subject subject, long purgeMillis);

    /**
     * Returns the content associated with the specified hash as a string
     *
     * @param hash The hash the uniquely identifies the requested content
     * @return The content as a string
     */
    String getDriftFileBits(String hash);
}
