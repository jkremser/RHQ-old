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
package org.rhq.core.domain.resource.group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.group.AbstractGroupConfigurationUpdate;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A {@link Group} that contains {@link Resource}s. It cannot contain other groups.
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT, query = "SELECT count(DISTINCT g) "
        + "FROM ResourceGroup g JOIN g.roles r JOIN r.subjects s " //
        + "LEFT JOIN g.resourceType type " //
        + "LEFT JOIN g.implicitResources res " // used for inventory>overview "member in groups" section, authz-related
        + "WHERE s = :subject " //
        + " AND g.visible = true "
        + " AND ( res.id = :resourceId OR :resourceId is null ) "
        + " AND ( g.id = :groupId OR :groupId is null ) "
        + " AND ( g.groupCategory = :groupCategory OR :groupCategory is null ) " //
        + " AND (UPPER(g.name) LIKE :search " //
        + " OR UPPER(g.description) LIKE :search " //
        + "OR :search is null) " //
        + "AND ( type is null " //
        + "      OR ( (type.name = :resourceTypeName OR :resourceTypeName is null) "
        + "           AND (type.plugin = :pluginName OR :pluginName is null) "
        + "           AND (type.category = :category OR :category is null) ) ) "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT_ADMIN, query = "SELECT count(DISTINCT g) FROM ResourceGroup g "
        + "LEFT JOIN g.resourceType type "
        + "LEFT JOIN g.implicitResources res " // used for inventory>overview "member in groups" section, authz-related
        + "WHERE ( g.groupCategory = :groupCategory OR :groupCategory is null ) "
        + " AND g.visible = true "
        + " AND ( res.id = :resourceId OR :resourceId is null ) "
        + " AND ( g.id = :groupId OR :groupId is null ) "
        + " AND (UPPER(g.name) LIKE :search "
        + " OR UPPER(g.description) LIKE :search "
        + " OR :search is null) "
        + "AND ( type is null " //
        + "      OR ( (type.name = :resourceTypeName OR :resourceTypeName is null) "
        + "           AND (type.plugin = :pluginName OR :pluginName is null) "
        + "           AND (type.category = :category OR :category is null) ) ) "),

    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY_COUNT, query = "SELECT COUNT(DISTINCT rg) "
        + "  FROM ResourceGroup AS rg JOIN rg.roles r JOIN r.subjects s " + " WHERE s = :subject "
        + " AND rg.visible = true " + "   AND rg.groupCategory = :category "),

    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY_COUNT_admin, query = "SELECT COUNT(rg) "
        + "  FROM ResourceGroup AS rg " + " WHERE rg.groupCategory = :category " + " AND rg.visible = true "),

    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_NAME, query = "SELECT rg FROM ResourceGroup AS rg WHERE LOWER(rg.name) = LOWER(:name)"),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_CLUSTER_KEY, query = "SELECT rg FROM ResourceGroup AS rg WHERE rg.clusterKey = :clusterKey"),

    @NamedQuery(name = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE, query = "SELECT DISTINCT rg "
        + "FROM ResourceGroup AS rg LEFT JOIN rg.roles AS r " + "WHERE rg.id NOT IN "
        + "  ( SELECT irg.id FROM Role ir JOIN ir.resourceGroups AS irg " + "    WHERE ir.id = :roleId )"
        + "  AND rg.clusterResourceGroup is NULL"),

    @NamedQuery(name = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE_WITH_EXCLUDES, query = "SELECT DISTINCT rg "
        + "FROM ResourceGroup AS rg LEFT JOIN rg.roles AS r "
        + "WHERE rg.id NOT IN "
        + "  ( SELECT irg.id "
        + "    FROM Role ir JOIN ir.resourceGroups AS irg "
        + "    WHERE ir.id = :roleId ) "
        + "  AND rg.id NOT IN ( :excludeIds )" + "  AND rg.clusterResourceGroup is NULL"),

    @NamedQuery(name = ResourceGroup.QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE_admin, query = "" //
        + "SELECT rg " //
        + "  FROM ResourceGroup AS rg " //
        + "  JOIN rg.roles AS r " //
        + " WHERE r.id = :id"), //
    @NamedQuery(name = ResourceGroup.QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE, query = "" //
        + "SELECT rg " //
        + "  FROM ResourceGroup AS rg " //
        + "  JOIN rg.roles AS r " //
        + " WHERE r.id = :id " //
        + "   AND r.id IN ( SELECT role.id " //
        + "                   FROM Role role " //
        + "                   JOIN role.subjects s " // 
        + "                  WHERE s.id = :subjectId ) "),

    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_IDS_admin, query = "" //
        + "          SELECT rg " //
        + "            FROM ResourceGroup AS rg " //
        + " LEFT JOIN FETCH rg.groupDefinition " //
        + " LEFT JOIN FETCH rg.resourceType " //
        + "           WHERE rg.id IN ( :ids )"), //
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_IDS, query = "" //
        + "          SELECT rg " //
        + "            FROM ResourceGroup AS rg " //
        + " LEFT JOIN FETCH rg.groupDefinition " //
        + " LEFT JOIN FETCH rg.resourceType " //
        + "            JOIN rg.roles roles " //
        + "            JOIN roles.subjects s " //
        + "           WHERE rg.id IN ( :ids ) " //
        + "             AND s = :subject"), //

    @NamedQuery(name = ResourceGroup.QUERY_FIND_IMPLICIT_RECURSIVE_GROUP_IDS_BY_RESOURCE_ID, query = "" //
        + "SELECT rg.id " //
        + "  FROM Resource AS res " //
        + "  JOIN res.implicitGroups rg " //
        + " WHERE res.id = :id " //
        + "   AND rg.recursive = true "),

    /* the following two are for auto-groups summary */
    @NamedQuery(name = ResourceGroup.QUERY_FIND_AUTOGROUP_BY_ID, query = "SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(AVG(a.availabilityType), res.parentResource, res.resourceType, COUNT(res)) "
        + "FROM Resource res JOIN res.implicitGroups irg JOIN irg.roles r JOIN r.subjects s JOIN res.currentAvailability a "
        + "WHERE s = :subject " + "AND res.id = :resourceId " + "GROUP BY res.resourceType "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_AUTOGROUP_BY_ID_ADMIN, query = "SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(AVG(a.availabilityType), res.parentResource, res.resourceType, COUNT(res)) "
        + "FROM Resource res JOIN res.currentAvailability a "
        + "WHERE res.id = :resourceId "
        + "GROUP BY res.resourceType "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_RESOURCE_NAMES_BY_GROUP_ID, query = "SELECT new org.rhq.core.domain.common.composite.IntegerOptionItem(res.id, res.name) "
        + "  FROM ResourceGroup g " + "  JOIN g.explicitResources res " + " WHERE g.id = :groupId "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_GROUP_DEFINITION_AND_EXPRESSION, query = "SELECT g "
        + "  FROM ResourceGroup g " + " WHERE (g.groupByClause = :groupByClause OR :groupByClause IS NULL) "
        + "   AND g.groupDefinition.id = :groupDefinitionId "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_RESOURCE_IDS_NOT_IN_GROUP_EXPLICIT, query = "" //
        + " SELECT res.id " //
        + "   FROM Resource res " //
        + "  WHERE res.id IN ( :resourceIds ) " //
        + "    AND res.id NOT IN ( SELECT explicitRes.id " //
        + "                          FROM ResourceGroup rg " //
        + "                          JOIN rg.explicitResources explicitRes " //
        + "                         WHERE rg.id = :groupId ) ") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_RESOURCE_GROUP_ID_SEQ")
@Table(name = "RHQ_RESOURCE_GROUP")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceGroup extends Group {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_BY_CATEGORY_COUNT = "ResourceGroup.findAllByCategory_Count";
    public static final String QUERY_FIND_ALL_BY_CATEGORY_COUNT_admin = "ResourceGroup.findAllByCategory_Count_admin";

    public static final String QUERY_FIND_BY_NAME = "ResourceGroup.findByName";
    public static final String QUERY_FIND_BY_CLUSTER_KEY = "ResourceGroup.findByClusterKey";
    public static final String QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE_WITH_EXCLUDES = "ResourceGroup.getAvailableResourceGroupsForRoleWithExcludes";
    public static final String QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE = "ResourceGroup.getAvailableResourceGroupsForRole";
    public static final String QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE_admin = "ResourceGroup.getResourceGroupsAssignedToRole_admin";
    public static final String QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE = "ResourceGroup.getResourceGroupsAssignedToRole";
    public static final String QUERY_FIND_BY_IDS_admin = "ResourceGroup.findByIds_admin";
    public static final String QUERY_FIND_BY_IDS = "ResourceGroup.findByIds";
    public static final String QUERY_FIND_IMPLICIT_RECURSIVE_GROUP_IDS_BY_RESOURCE_ID = "ResourceGroup.findImplicitRecursiveGroupIdsByResourceId";

    public static final String QUERY_FIND_AUTOGROUP_BY_ID = "ResourceGroup.findAutoGroupById";
    public static final String QUERY_FIND_AUTOGROUP_BY_ID_ADMIN = "ResourceGroup.findAutoGroupById_admin";

    public static final String QUERY_FIND_RESOURCE_NAMES_BY_GROUP_ID = "ResourceGroup.findResourceNamesByGroupId";
    public static final String QUERY_FIND_BY_GROUP_DEFINITION_AND_EXPRESSION = "ResourceGroup.findByGroupDefinitionAndExpression";
    public static final String QUERY_DELETE_EXPLICIT_BY_RESOURCE_IDS = "DELETE FROM RHQ_RESOURCE_GROUP_RES_EXP_MAP WHERE RESOURCE_ID IN ( :resourceIds )";
    public static final String QUERY_DELETE_IMPLICIT_BY_RESOURCE_IDS = "DELETE FROM RHQ_RESOURCE_GROUP_RES_IMP_MAP WHERE RESOURCE_ID IN ( :resourceIds )";

    public static final String QUERY_UPDATE_REMOVE_IMPLICIT = "" //
        + "DELETE FROM rhq_resource_group_res_imp_map implicitMap " //
        + "      WHERE implicitMap.resource_group_id = ?";
    public static final String QUERY_UPDATE_IMPLICIT_MIRROR_EXPLICIT = "" //
        + "INSERT INTO rhq_resource_group_res_imp_map (resource_id, resource_group_id) " //
        + "     SELECT explicitMap.resource_id, explicitMap.resource_group_id " //
        + "       FROM rhq_resource_group_res_exp_map explicitMap " //
        + "      WHERE explicitMap.resource_group_id = ?";

    public static final String QUERY_FIND_ALL_FILTERED_COUNT = "ResourceGroup.findAllFiltered_Count";
    public static final String QUERY_FIND_ALL_FILTERED_COUNT_ADMIN = "ResourceGroup.findAllFiltered_Count_Admin";
    public static final String QUERY_NATIVE_FIND_FILTERED_MEMBER = "" //
        + "         SELECT "
        + "              (     SELECT COUNT(eresAvail.ID) "
        + "                      FROM rhq_resource_avail eresAvail "
        + "                INNER JOIN rhq_resource eres "
        + "                        ON eresAvail.resource_id = eres.id "
        + "                INNER JOIN rhq_resource_group_res_exp_map expMap "
        + "                        ON eres.id = expMap.resource_id "
        + "                     WHERE expMap.resource_group_id = rg.id "
        + "              ) as explicitCount, "
        + "" //
        + "              (     SELECT AVG(eresAvail.availability_type) "
        + "                      FROM rhq_resource_avail eresAvail "
        + "                INNER JOIN rhq_resource eres "
        + "                        ON eresAvail.resource_id = eres.id "
        + "                INNER JOIN rhq_resource_group_res_exp_map expMap "
        + "                        ON eres.id = expMap.resource_id "
        + "                     WHERE expMap.resource_group_id = rg.id "
        + "              ) as explicitAvail, "
        + "" //
        + "              (     SELECT COUNT(iresAvail.ID) "
        + "                      FROM rhq_resource_avail iresAvail "
        + "                INNER JOIN rhq_resource ires "
        + "                        ON iresAvail.resource_id = ires.id "
        + "                INNER JOIN rhq_resource_group_res_imp_map impMap "
        + "                        ON ires.id = impMap.resource_id "
        + "                     WHERE impMap.resource_group_id = rg.id "
        + "              ) as implicitCount, "
        + "" //
        + "              (     SELECT AVG(iresAvail.availability_type) "
        + "                      FROM rhq_resource_avail iresAvail "
        + "                INNER JOIN rhq_resource ires "
        + "                        ON iresAvail.resource_id = ires.id "
        + "                INNER JOIN rhq_resource_group_res_imp_map impMap "
        + "                        ON ires.id = impMap.resource_id "
        + "                     WHERE impMap.resource_group_id = rg.id "
        + "              ) as implicitAvail, "
        + "" //
        + "                rg.id as groupId, "
        + "                rg.name as groupName, "
        + "                rg.description as groupDescription, "
        + "                resType.name as resourceTypeName "
        + "" //
        + "           FROM rhq_resource_group rg "
        + "LEFT OUTER JOIN rhq_resource_type resType "
        + "             ON rg.resource_type_id = resType.id "
        + "LEFT OUTER JOIN rhq_resource_group_res_imp_map memberMap "
        + "             ON rg.id = memberMap.resource_group_id "
        + "LEFT OUTER JOIN rhq_resource res "
        + "             ON memberMap.resource_id = res.id "
        + "                %SECURITY_FRAGMENT_JOIN%"
        + "LEFT OUTER JOIN rhq_resource_avail resAvail "
        + "             ON res.id = resAvail.resource_id "
        + "          WHERE %GROUP_AND_VISIBILITY_FRAGMENT_WHERE% " // postgres uses true/false, oracle uses 1/0
        + "                %RESOURCE_FRAGMENT_WHERE% " //
        + "            AND ( ? IS NULL " // :search
        + "                  OR UPPER(rg.name) LIKE ? ESCAPE ?" // :search :escapeChar
        + "                  OR UPPER(rg.description) LIKE ? ESCAPE ?) " // :search escapeChar
        + "            AND ( rg.resource_type_id IS NULL " //
        + "                  OR ( ( resType.name = ? OR ? IS NULL ) " // :resourceTypeName x2
        + "                      AND ( resType.plugin = ? OR ? IS NULL ) " // :pluginName x2
        + "                      AND ( resType.category = ? OR ? IS NULL ) ) ) " // :resourceCategory x2
        + "            AND ( rg.category = ? OR ? IS NULL ) " // :groupCategory x2
        + "                %SECURITY_FRAGMENT_WHERE%" //
        + "       GROUP BY rg.id, rg.category, rg.name, rg.group_by, rg.description, resType.name, resType.plugin ";

    public static final String QUERY_NATIVE_FIND_FILTERED_MEMBER_SECURITY_FRAGMENT_JOIN = ""
        + " INNER JOIN rhq_role_resource_group_map roleMap " //
        + "         ON roleMap.resource_group_id = rg.id " //
        + " INNER JOIN rhq_subject_role_map subjectMap " //
        + "         ON subjectMap.role_id = roleMap.role_id ";

    public static final String QUERY_NATIVE_FIND_FILTERED_MEMBER_SECURITY_FRAGMENT_WHERE = ""
        + " AND ( subjectMap.subject_id = ? ) "; // :subjectId

    public static final String QUERY_NATIVE_FIND_FILTERED_MEMBER_RESOURCE_FRAGMENT_WHERE = "" //
        + " AND ( res.id = ? ) "; // resourceId

    public static final String QUERY_FIND_RESOURCE_IDS_NOT_IN_GROUP_EXPLICIT = "ResourceGroup.findResourceIdsNotInGroupExplicit";
    public static final String QUERY_NATIVE_ADD_RESOURCES_TO_GROUP_EXPLICIT = "" //
        + "    insert into RHQ_RESOURCE_GROUP_RES_EXP_MAP ( RESOURCE_ID, RESOURCE_GROUP_ID ) " //
        + "         select res.ID, ? " // groupId
        + "           from RHQ_RESOURCE res " //
        + "          where res.ID in ( @@RESOURCE_IDS@@ ) ";
    public static final String QUERY_NATIVE_ADD_RESOURCES_TO_GROUP_IMPLICIT = "" //
        + "    insert into RHQ_RESOURCE_GROUP_RES_IMP_MAP ( RESOURCE_ID, RESOURCE_GROUP_ID ) " //
        + "         select res.ID, ? " // groupId
        + "           from RHQ_RESOURCE res " //
        + "          where res.ID in ( @@RESOURCE_IDS@@ ) ";
    public static final String QUERY_NATIVE_ADD_RESOURCES_TO_GROUP_IMPLICIT_RECURSIVE = "" //
        + "    insert into RHQ_RESOURCE_GROUP_RES_IMP_MAP ( RESOURCE_ID, RESOURCE_GROUP_ID ) " //
        + "         select res.ID, ? " // groupId
        + "           from RHQ_RESOURCE res " //
        + "left outer join RHQ_RESOURCE g1parent on res.PARENT_RESOURCE_ID = g1parent.ID " //
        + "left outer join RHQ_RESOURCE g2parent on g1parent.PARENT_RESOURCE_ID = g2parent.ID " //
        + "left outer join RHQ_RESOURCE g3parent on g2parent.PARENT_RESOURCE_ID = g3parent.ID " //
        + "left outer join RHQ_RESOURCE g4parent on g3parent.PARENT_RESOURCE_ID = g4parent.ID " //
        + "left outer join RHQ_RESOURCE g5parent on g4parent.PARENT_RESOURCE_ID = g5parent.ID " //
        + "left outer join RHQ_RESOURCE g6parent on g5parent.PARENT_RESOURCE_ID = g6parent.ID " //
        + "          where ( res.ID = ? or " // resourceId
        + "                  g1parent.ID = ? or " // resourceId
        + "                  g2parent.ID = ? or " // resourceId
        + "                  g3parent.ID = ? or " // resourceId
        + "                  g4parent.ID = ? or " // resourceId
        + "                  g5parent.ID = ? or " // resourceId
        + "                  g6parent.ID = ? ) " // resourceId
        + "            and ( res.ID not in ( select impRes.ID " //
        + "                                    from RHQ_RESOURCE_GROUP rg " //
        + "                              inner join RHQ_RESOURCE_GROUP_RES_IMP_MAP implicitMap on rg.ID = implicitMap.RESOURCE_GROUP_ID " //
        + "                              inner join RHQ_RESOURCE impRes on implicitMap.RESOURCE_ID = impRes.ID " //
        + "                                   where rg.ID = ? ) ) "; // groupId
    public static final String QUERY_NATIVE_REMOVE_RESOURCES_FROM_GROUP_EXPLICIT = "" //
        + "    delete from RHQ_RESOURCE_GROUP_RES_EXP_MAP " //
        + "          where RESOURCE_GROUP_ID = ? " // groupId
        + "            and RESOURCE_ID in ( @@RESOURCE_IDS@@ ) ";
    public static final String QUERY_NATIVE_REMOVE_RESOURCES_FROM_GROUP_IMPLICIT = "" //
        + "    delete from RHQ_RESOURCE_GROUP_RES_IMP_MAP " //
        + "          where RESOURCE_GROUP_ID = ? " // groupId
        + "            and RESOURCE_ID in ( @@RESOURCE_IDS@@ ) ";
    public static final String QUERY_NATIVE_REMOVE_RESOURCES_FROM_GROUP_IMPLICIT_RECURSIVE = "" //
        + "   delete from RHQ_RESOURCE_GROUP_RES_IMP_MAP " // delete mappings
        + "         where RESOURCE_GROUP_ID = ? " // groupId
        + "           and RESOURCE_ID in " // from any descendant of resourceId, including itself
        + "               ( select res.id " //
        + "                   from RHQ_RESOURCE res " //
        + "        left outer join RHQ_RESOURCE g1parent on res.PARENT_RESOURCE_ID = g1parent.ID " //
        + "        left outer join RHQ_RESOURCE g2parent on g1parent.PARENT_RESOURCE_ID = g2parent.ID " //
        + "        left outer join RHQ_RESOURCE g3parent on g2parent.PARENT_RESOURCE_ID = g3parent.ID " //
        + "        left outer join RHQ_RESOURCE g4parent on g3parent.PARENT_RESOURCE_ID = g4parent.ID " //
        + "        left outer join RHQ_RESOURCE g5parent on g4parent.PARENT_RESOURCE_ID = g5parent.ID " //
        + "        left outer join RHQ_RESOURCE g6parent on g5parent.PARENT_RESOURCE_ID = g6parent.ID " //
        + "                  where ( res.ID = ? or " // resourceId
        + "                          g1parent.ID = ? or " // resourceId
        + "                          g2parent.ID = ? or " // resourceId
        + "                          g3parent.ID = ? or " // resourceId
        + "                          g4parent.ID = ? or " // resourceId
        + "                          g5parent.ID = ? or " // resourceId
        + "                          g6parent.ID = ? ) ) " // resourceId
        + "           and RESOURCE_ID not in " // which aren't already descendants of members in the explicit set
        + "               ( select res.id " //
        + "                   from RHQ_RESOURCE_GROUP_RES_EXP_MAP alreadyMember, RHQ_RESOURCE res " //
        + "        left outer join RHQ_RESOURCE g1parent on res.PARENT_RESOURCE_ID = g1parent.ID " //
        + "        left outer join RHQ_RESOURCE g2parent on g1parent.PARENT_RESOURCE_ID = g2parent.ID " //
        + "        left outer join RHQ_RESOURCE g3parent on g2parent.PARENT_RESOURCE_ID = g3parent.ID " //
        + "        left outer join RHQ_RESOURCE g4parent on g3parent.PARENT_RESOURCE_ID = g4parent.ID " //
        + "        left outer join RHQ_RESOURCE g5parent on g4parent.PARENT_RESOURCE_ID = g5parent.ID " //
        + "        left outer join RHQ_RESOURCE g6parent on g5parent.PARENT_RESOURCE_ID = g6parent.ID " //
        + "                  where alreadyMember.RESOURCE_GROUP_ID = ? " // groupId
        + "                    and alreadyMember.RESOURCE_ID <> ? " // resourceId
        + "                    and ( res.ID = alreadyMember.RESOURCE_ID or " //
        + "                          g1parent.ID = alreadyMember.RESOURCE_ID or " //
        + "                          g2parent.ID = alreadyMember.RESOURCE_ID or " //
        + "                          g3parent.ID = alreadyMember.RESOURCE_ID or " //
        + "                          g4parent.ID = alreadyMember.RESOURCE_ID or " //
        + "                          g5parent.ID = alreadyMember.RESOURCE_ID or " //
        + "                          g6parent.ID = alreadyMember.RESOURCE_ID ) ) ";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id")
    @Id
    private int id;

    @JoinTable(name = "RHQ_RESOURCE_GROUP_RES_EXP_MAP", joinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_ID") })
    @ManyToMany
    private Set<Resource> explicitResources;

    @JoinTable(name = "RHQ_RESOURCE_GROUP_RES_IMP_MAP", joinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_ID") })
    @ManyToMany
    private Set<Resource> implicitResources;

    @JoinTable(name = "RHQ_ROLE_RESOURCE_GROUP_MAP", joinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") }, inverseJoinColumns = { @JoinColumn(name = "ROLE_ID") })
    @ManyToMany
    private Set<Role> roles = new HashSet<Role>();

    @OneToMany(mappedBy = "group", cascade = { CascadeType.ALL })
    @OrderBy
    // by primary key which will also put the operation histories in chronological order
    private List<GroupOperationHistory> operationHistories = new ArrayList<GroupOperationHistory>();

    @OneToMany(mappedBy = "group", cascade = { CascadeType.ALL })
    @OrderBy
    // by primary key which will also put the configuration updates in chronological order
    private List<AbstractGroupConfigurationUpdate> configurationUpdates = new ArrayList<AbstractGroupConfigurationUpdate>();

    @JoinColumn(name = "GROUP_DEFINITION_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private GroupDefinition groupDefinition;

    @Column(name = "GROUP_BY", nullable = true)
    private String groupByClause;

    @Column(name = "RECURSIVE")
    private boolean recursive;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupCategory groupCategory;

    @JoinColumn(name = "RESOURCE_TYPE_ID", nullable = true)
    @ManyToOne
    private ResourceType resourceType; // if non-null, it implies a compatible group

    @Column(name = "CLUSTER_KEY", nullable = true)
    private String clusterKey;

    // The compatible group for which this is an auto-cluster backing group
    @JoinColumn(name = "CLUSTER_RESOURCE_GROUP_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private ResourceGroup clusterResourceGroup = null;

    // When false hide this group from the UI. For example, for a Resource Cluster backing group. 
    private boolean visible = true;

    // When a compatible group is removed any referring backing groups should also be removed
    @OneToMany(mappedBy = "clusterResourceGroup")
    private List<ResourceGroup> clusterBackingGroups = null;

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.ALL })
    @OneToMany(mappedBy = "resourceGroup", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<AlertDefinition> alertDefinitions = new LinkedHashSet<AlertDefinition>();

    /* no-arg constructor required by EJB spec */
    protected ResourceGroup() {
    }

    public ResourceGroup(@NotNull String name) {
        super(name);
        setResourceType(null);
    }

    public ResourceGroup(@NotNull String name, ResourceType type) {
        super(name);
        setResourceType(type);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addExplicitResource(@NotNull Resource resource) {
        getExplicitResources().add(resource);
        resource.addExplicitGroup(this);
    }

    public void setExplicitResources(Set<Resource> resources) {
        this.explicitResources = resources;
    }

    @NotNull
    public Set<Resource> getExplicitResources() {
        if (this.explicitResources == null) {
            this.explicitResources = new HashSet<Resource>();
        }

        return this.explicitResources;
    }

    public boolean removeExplicitResource(@NotNull Resource resource) {
        boolean removed = getExplicitResources().remove(resource);
        resource.removeExplicitGroup(this);
        return removed;
    }

    public void addImplicitResource(@NotNull Resource resource) {
        getImplicitResources().add(resource);
        resource.addImplicitGroup(this);
    }

    public void setImplicitResources(Set<Resource> resources) {
        this.implicitResources = resources;
    }

    public GroupCategory getGroupCategory() {
        return groupCategory;
    }

    protected void setGroupCategory(GroupCategory groupCategory) {
        this.groupCategory = groupCategory;
    }

    @NotNull
    public Set<Resource> getImplicitResources() {
        if (this.implicitResources == null) {
            this.implicitResources = new HashSet<Resource>();
        }

        return this.implicitResources;
    }

    public boolean removeImplicitResource(@NotNull Resource resource) {
        boolean removed = getImplicitResources().remove(resource);
        resource.removeImplicitGroup(this);
        return removed;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    @NotNull
    public List<GroupOperationHistory> getOperationHistories() {
        return operationHistories;
    }

    public void setOperationHistories(@NotNull List<GroupOperationHistory> operationHistories) {
        this.operationHistories = operationHistories;
    }

    @NotNull
    public List<AbstractGroupConfigurationUpdate> getConfigurationUpdates() {
        return configurationUpdates;
    }

    public void setConfigurationUpdates(@NotNull List<AbstractGroupConfigurationUpdate> configurationUpdates) {
        this.configurationUpdates = configurationUpdates;
    }

    public GroupDefinition getGroupDefinition() {
        return groupDefinition;
    }

    public void setGroupDefinition(GroupDefinition groupDefinition) {
        this.groupDefinition = groupDefinition;
    }

    public String getGroupByClause() {
        return groupByClause;
    }

    public void setGroupByClause(String groupByClause) {
        this.groupByClause = groupByClause;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
        if (resourceType == null) {
            setGroupCategory(GroupCategory.MIXED);
        } else {
            setGroupCategory(GroupCategory.COMPATIBLE);
        }
    }

    public String getClusterKey() {
        return clusterKey;
    }

    public void setClusterKey(String clusterKey) {
        this.clusterKey = clusterKey;
    }

    public ResourceGroup getClusterResourceGroup() {
        return clusterResourceGroup;
    }

    public void setClusterResourceGroup(ResourceGroup clusterResourceGroup) {
        this.clusterResourceGroup = clusterResourceGroup;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<ResourceGroup> getClusterBackingGroups() {
        return clusterBackingGroups;
    }

    public void setClusterBackingGroups(List<ResourceGroup> clusterBackingGroups) {
        this.clusterBackingGroups = clusterBackingGroups;
    }

    public Set<AlertDefinition> getAlertDefinitions() {
        if (this.alertDefinitions == null) {
            this.alertDefinitions = new LinkedHashSet<AlertDefinition>();
        }

        return alertDefinitions;
    }

    public void setAlertDefinitions(Set<AlertDefinition> alertDefinitions) {
        this.alertDefinitions = alertDefinitions;
    }

    @PrePersist
    @PreUpdate
    void onPersist() {
        // always normalize empty string descriptions to NULL, which will give consistent sorting 
        // between databases that treat empty string and null as distinct entities (e.g. postgres) 
        // and those that interpret empty string and null as being equivalent (oracle)
        String description = getDescription();
        if (description != null && description.trim().equals("")) {
            setDescription(null);
        }
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ResourceGroup.class.getSimpleName()).append("[");
        buffer.append("id=").append(this.id);
        buffer.append(", name=").append(this.getName());
        buffer.append(", category=").append(groupCategory.name());
        String typeName = (this.resourceType != null) ? this.resourceType.getName() : "<mixed>";
        buffer.append(", type=").append(typeName);
        boolean isDynaGroup = (this.groupDefinition != null);
        buffer.append(", isDynaGroup=").append(isDynaGroup);
        boolean isClusterGroup = (this.clusterKey != null);
        buffer.append(", isClusterGroup=").append(isClusterGroup);
        buffer.append("]");
        return buffer.toString();
    }
}