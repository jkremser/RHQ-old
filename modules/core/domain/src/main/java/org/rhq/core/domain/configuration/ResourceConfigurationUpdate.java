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
package org.rhq.core.domain.configuration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.configuration.group.AbstractGroupConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;

@DiscriminatorValue("resource")
@Entity
@NamedQueries({
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.status = :status"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + "  JOIN cu.resource res " //
        + " WHERE ( res.id = :resourceId OR :resourceId IS NULL ) " //
        + "   AND ( cu.createdTime > :startTime OR :startTime IS NULL ) " //
        + "   AND ( cu.modifiedTime < :endTime OR :endTime IS NULL ) " //
        + "   AND ( :includeAll = 1 " //
        + "         OR ( :includeAll <> 1 " //
        + "              AND " //
        + "              cu.modifiedTime <> (SELECT MIN(icu.modifiedTime) " // 
        + "                                    FROM ResourceConfigurationUpdate icu " //
        + "                                   WHERE icu.resource.id = res.id) " //
        + "            ) " //
        + "       )"),
    // Note: Changes to this query should also be applied to QUERY_FIND_CURRENT_AND_IN_PROGRESS_CONFIGS, below. 
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG, query = "" //    
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.resource.id = :resourceId " //
        + "   AND cu.status = 'SUCCESS' " //
        + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) " //
        + "                             FROM ResourceConfigurationUpdate cu2 " //
        + "                            WHERE cu2.resource.id = :resourceId " //
        + "                              AND cu2.status = 'SUCCESS' ) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_CURRENT_AND_IN_PROGRESS_CONFIGS, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.resource.id = :resourceId " //
        + "   AND ( ( cu.status = 'INPROGRESS' ) " //
        + "    OR   ( cu.status = 'SUCCESS' " //        
        + "   AND     cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) " //
        + "                                 FROM ResourceConfigurationUpdate cu2 " //
        + "                                WHERE cu2.resource.id = :resourceId " //
        + "                                  AND cu2.status = 'SUCCESS' ) " //
        + "         ) ) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.resource.id = :resourceId " //
        + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) " // 
        + "                             FROM ResourceConfigurationUpdate cu2 " //
        + "                            WHERE cu2.resource.id = :resourceId ) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID_AND_STATUS, query = "" //
        + "SELECT cu.resource " //
        + "  FROM ResourceConfigurationUpdate cu JOIN cu.resource.explicitGroups rg " //
        + " WHERE rg.id = :groupId " //
        + "   AND cu.status = :status"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID_AND_STATUS, query = "" //
        + "SELECT cu.resource " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.groupConfigurationUpdate.id = :parentUpdateId " //
        + "   AND cu.status = :status"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID, query = "" //
        + "SELECT new org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite" //
        + "       ( cu.id, cu.status, cu.errorMessage, cu.subjectName, cu.createdTime, cu.modifiedTime, " // update w/o config
        + "         res.id, res.name ) " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + "  JOIN cu.resource res " //
        + " WHERE cu.groupConfigurationUpdate.id = :groupConfigurationUpdateId"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, query = "" //
        + "SELECT cu.id " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.groupConfigurationUpdate.id = :groupConfigurationUpdateId"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_STATUS_BY_PARENT_UPDATE_ID, query = "" //
        + "SELECT cu.status " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.groupConfigurationUpdate.id = :groupConfigurationUpdateId " //
        + " GROUP BY cu.status"), //
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_COMPOSITES_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite" //
        + "        ( cu.id, cu.status, cu.errorMessage, cu.subjectName, cu.createdTime, cu.modifiedTime, " // update w/o config
        + "          res.id, res.name, parent.id, parent.name ) " //
        + "     FROM ResourceConfigurationUpdate cu " //
        + "     JOIN cu.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE (cu.modifiedTime <> (SELECT MIN(icu.modifiedTime) " // 
        + "                                 FROM ResourceConfigurationUpdate icu " //
        + "                                WHERE icu.resource.id = res.id))" //
        + "      AND (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (cu.createdTime > :startTime OR :startTime IS NULL) " //
        + "      AND (cu.modifiedTime < :endTime OR :endTime IS NULL) " //
        + "      AND (cu.status LIKE :status OR :status IS NULL) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_COMPOSITES, query = "" //
        + "   SELECT new org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite" //
        + "        ( cu.id, cu.status, cu.errorMessage, cu.subjectName, cu.createdTime, cu.modifiedTime, " // update w/o config
        + "          res.id, res.name, parent.id, parent.name ) " //
        + "     FROM ResourceConfigurationUpdate cu " //
        + "     JOIN cu.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE res.id IN ( SELECT rr.id FROM Resource rr " //
        + "                        JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions perm " //
        + "                       WHERE s.id = :subjectId AND perm = 13 ) " //
        + "      AND (cu.modifiedTime <> (SELECT MIN(icu.modifiedTime) " // 
        + "                                 FROM ResourceConfigurationUpdate icu " //
        + "                                WHERE icu.resource.id = res.id))" //
        + "      AND (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (cu.createdTime > :startTime OR :startTime IS NULL) " //
        + "      AND (cu.modifiedTime < :endTime OR :endTime IS NULL) " //
        + "      AND (cu.status LIKE :status OR :status IS NULL) "),
    // This query is currently used by bulk delete only if the db vendor can't support
    // self-referential cascade. Instead, we have to orphan the properties that can't be deleted by
    // the foreign key reference.
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_0, query = "" //
        + "UPDATE Property p " //
        + "   SET p.parentMap = NULL, " //
        + "       p.parentList = NULL " //
        + " WHERE p.configuration IN ( SELECT rcu.configuration " //
        + "                              FROM ResourceConfigurationUpdate rcu " //
        + "                             WHERE rcu.resource.id IN ( :resourceIds ) " //
        + "                           AND NOT rcu.configuration = rcu.resource.resourceConfiguration )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_1, query = "" //
        + "DELETE FROM RawConfiguration rc " //
        + " WHERE rc.configuration IN ( SELECT rcu.configuration " //
        + "                FROM ResourceConfigurationUpdate rcu " //
        + "               WHERE rcu.resource.id IN ( :resourceIds ) " //
        + "                AND NOT rcu.configuration = rcu.resource.resourceConfiguration )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_2, query = "" //
        + "DELETE FROM Configuration c " //
        + " WHERE c IN ( SELECT rcu.configuration " //
        + "                FROM ResourceConfigurationUpdate rcu " //
        + "               WHERE rcu.resource.id IN ( :resourceIds ) " //
        + "                AND NOT rcu.configuration = rcu.resource.resourceConfiguration )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_3, query = ""
        + "DELETE FROM ResourceConfigurationUpdate rcu " //
        + " WHERE rcu.resource.id IN ( :resourceIds )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_GROUP_UPDATE, query = "" //
        + "UPDATE ResourceConfigurationUpdate rcu " //
        + "   SET rcu.groupConfigurationUpdate = NULL " //
        + " WHERE rcu.groupConfigurationUpdate IN ( SELECT arcu " //
        + "                                           FROM GroupResourceConfigurationUpdate arcu " //
        + "                                          WHERE arcu.id = :arcuId )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_GROUP_UPDATES_FOR_GROUP, query = "" //
        + "UPDATE ResourceConfigurationUpdate rcu " //
        + "   SET rcu.groupConfigurationUpdate = NULL " //
        + " WHERE rcu.groupConfigurationUpdate IN ( SELECT arcu " //
        + "                                           FROM GroupResourceConfigurationUpdate arcu " //
        + "                                          WHERE arcu.group.id = :groupId )") })
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceConfigurationUpdate extends AbstractResourceConfigurationUpdate {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_IN_STATUS = "ResourceConfigurationUpdate.findAllInStatus";
    public static final String QUERY_FIND_ALL_BY_RESOURCE_ID = "ResourceConfigurationUpdate.findAllByResourceId";
    public static final String QUERY_FIND_CURRENTLY_ACTIVE_CONFIG = "ResourceConfigurationUpdate.findCurrentlyActiveConfig";
    public static final String QUERY_FIND_CURRENT_AND_IN_PROGRESS_CONFIGS = "ResourceConfigurationUpdate.findCurrentAndInProgressConfigs";
    public static final String QUERY_FIND_LATEST_BY_RESOURCE_ID = "ResourceConfigurationUpdate.findByLatestByResourceId";
    public static final String QUERY_FIND_BY_GROUP_ID_AND_STATUS = "ResourceConfigurationUpdate.findByGroupIdAndStatus";
    public static final String QUERY_FIND_BY_PARENT_UPDATE_ID_AND_STATUS = "ResourceConfigurationUpdate.findByParentUpdateIdAndStatus";
    public static final String QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID = "ResourceConfigurationUpdate.findCompositeByParentUpdateId";
    public static final String QUERY_FIND_BY_PARENT_UPDATE_ID = "ResourceConfigurationUpdate.findByParentUpdateId";
    public static final String QUERY_FIND_STATUS_BY_PARENT_UPDATE_ID = "ResourceConfigurationUpdate.findStatusByParentUpdateId";

    // for subsystem views
    public static final String QUERY_FIND_ALL_COMPOSITES = "ResourceConfigurationUpdate.findAllComposites";
    public static final String QUERY_FIND_ALL_COMPOSITES_ADMIN = "ResourceConfigurationUpdate.findAllComposites_admin";

    // for efficient object cleanup/purge
    public static final String QUERY_DELETE_BY_RESOURCES_0 = "ResourceConfigurationUpdate.deleteByResources0";
    public static final String QUERY_DELETE_BY_RESOURCES_1 = "ResourceConfigurationUpdate.deleteByResources1";
    public static final String QUERY_DELETE_BY_RESOURCES_2 = "ResourceConfigurationUpdate.deleteByResources2";
    public static final String QUERY_DELETE_BY_RESOURCES_3 = "ResourceConfigurationUpdate.deleteByResources3";
    public static final String QUERY_DELETE_GROUP_UPDATE = "ResourceConfigurationUpdate.deleteGroupUpdate";
    public static final String QUERY_DELETE_GROUP_UPDATES_FOR_GROUP = "ResourceConfigurationUpdate.deleteGroupUpdatesForGroup";

    @JoinColumn(name = "CONFIG_RES_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    @XmlTransient
    private Resource resource;

    @JoinColumn(name = "AGG_RES_UPDATE_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private GroupResourceConfigurationUpdate groupConfigurationUpdate;

    public ResourceConfigurationUpdate() {
    }

    public ResourceConfigurationUpdate(Resource resource, Configuration config, String subjectName) {
        super(config, subjectName);
        this.resource = resource;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public AbstractGroupConfigurationUpdate getAbstractGroupConfigurationUpdate() {
        return getGroupConfigurationUpdate();
    }

    public GroupResourceConfigurationUpdate getGroupConfigurationUpdate() {
        return groupConfigurationUpdate;
    }

    public void setGroupConfigurationUpdate(GroupResourceConfigurationUpdate groupConfigurationUpdate) {
        this.groupConfigurationUpdate = groupConfigurationUpdate;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", resource=").append(this.resource);

        if (groupConfigurationUpdate != null) {
            // circular toString if you try to print the entire groupConfigurationUpdate object
            str.append(", groupResourceConfigurationUpdate=").append(groupConfigurationUpdate.getId());
        }
    }
}