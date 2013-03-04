/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.criteria;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class Criteria implements Serializable, BaseCriteria {
    public enum Type {
        FILTER(new String[] { "filterId", "filterIds" }), FETCH(), SORT(new String[] { "sortId" });

        private List<String> globalFields;

        /**
         * Use this to get the global fields for this Criteria field type. Don't use inspection as the field names
         * for this abstract base class do not conform (for legacy reasons) to the prefix convention help by the
         * subclasses.  This is likely only relevant to {@link CriteriaQueryGenerator}.
         *  
         * @return The set of global fields for this Criteria field type. Meaning, usable by all subclasses.
         */
        public List<String> getGlobalFields() {
            return globalFields;
        }

        private Type() {
            this.globalFields = new ArrayList<String>(0);
        }

        private Type(String[] globalFields) {
            this.globalFields = Arrays.asList(globalFields);
        }
    }

    /**
     * This is the type of a filter value when the override for that filter does not
     * define any query parameter. ON means the filter is enabled and will take effect,
     * OFF means the filter will not be used in the query.
     * Example, from AlertDefinitionCriteria:
     *    private NonBindingOverrideFilter filterResourceOnly; // requires overrides - finds only those associated with a resource
     *    ...
     *    filterOverrides.put("resourceTypeOnly", "resourceType IS NOT NULL"); // notice no ? parameter
     * 
     * Note: Typically a null value is analogous to OFF.
     */
    public enum NonBindingOverrideFilter {
        ON, OFF;
    }

    /**
     * Apply a restriction to reduce the cost of the {@link Criteria}-based query generation and execution routines.
     */
    public enum Restriction {
        /** 
         * This returns an empty {@link PageList} result whose {@link PageList#getTotalSize()} method otherwise
         * contains the correct value.
         */
        COUNT_ONLY,
        /** 
         * This will return the {@link PageList} result whose {@link PageList#isUnbounded()} returned true, meaning
         * that the value contained within {@link PageList#getTotalSize()} is invalid / undefined.
         */
        COLLECTION_ONLY;
    }

    private static final long serialVersionUID = 1L;

    private Integer pageNumber;
    private Integer pageSize;

    private boolean filtersOptional;
    private boolean caseSensitive;
    private List<Permission> requiredPermissions;
    private boolean strict;
    private Restriction restriction = null;

    protected Map<String, String> filterOverrides;
    protected Map<String, String> sortOverrides;
    protected PageControl pageControlOverrides;

    private List<String> orderingFieldNames;
    private String alias;

    private String searchExpression;

    // All Criteria support sorting on ID
    private PageOrdering sortId;
    
    // All Criteria support filtering on ID
    private Integer filterId;
    
    // All Criteria support filtering on IDs
    private List<Integer> filterIds;

    /**
     * This default constructor will set default paging to avoid unintended fetch of huge results. The default is:
     * <pre>setPaging(0, 200);</pre>
     */
    public Criteria() {
        this.filterOverrides = new HashMap<String, String>();
        this.filterOverrides.put("ids", "id IN ( ? )");
        this.sortOverrides = new HashMap<String, String>();

        this.orderingFieldNames = new ArrayList<String>();

        /*
         * reasonably large default, but prevent accidentally returning 100K objects
         * unless you use the setPaging method to explicit denote you want that many
         */
        setPaging(0, 200);
    }

    public abstract Class<?> getPersistentClass();

    public Integer getPageNumber() {
        return pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public List<String> getOrderingFieldNames() {
        return orderingFieldNames;
    }

    public String getJPQLFilterOverride(String fieldName) {
        return filterOverrides.get(fieldName);
    }

    public String getJPQLSortOverride(String fieldName) {
        return sortOverrides.get(fieldName);
    }

    public PageControl getPageControlOverrides() {
        return pageControlOverrides;
    }

    public void addSortId(PageOrdering sortId) {
        addSortField("id");
        this.sortId = sortId;
    }
    
    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterIds(Integer... filterIds) {
        this.filterIds = CriteriaUtils.getListIgnoringNulls(filterIds);
    }

    protected void addSortField(String fieldName) {
        orderingFieldNames.add("sort" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
    }

    public void setPaging(int pageNumber, int pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    /**
     * If the pageControl is set, then this criteria object will completely ignore any
     * calls made to setPaging(pageNumber, pageSize) as well as addSortField(fieldName), 
     * which is useful from a server-side calling context where the PageControl object
     * will already have been created for you by the extensions at the JSF layer.
     */
    @Override
    public void setPageControl(PageControl pageControl) {
        this.pageControlOverrides = pageControl;
    }

    /**
     * By default, the ordering fields are automatically prepend with the alias of entity that this criteria object
     * wraps.  However, some authors of criteria objects want full control of this alias during sort operations.  if
     * this method returns true, then the alias will not be prepend to the generated "order by" clause, which makes
     * author responsible for constructing the fully-qualified ordering token for each sort override.
     */
    public boolean hasCustomizedSorting() {
        return false;
    }

    public void clearPaging() {
        PageControl unlimited = PageControl.getUnlimitedInstance();
        this.pageNumber = unlimited.getPageNumber();
        this.pageSize = unlimited.getPageSize();
        this.pageControlOverrides = null;
    }

    /**
     * If set to true, then results will come back if they match ANY filter;
     * Default is 'false', which means results must match all set filters.
     */
    public void setFiltersOptional(boolean filtersOptional) {
        this.filtersOptional = filtersOptional;
    }

    public boolean isFiltersOptional() {
        return filtersOptional;
    }

    /**
     * If set to true, string-based filters will use case-sensitive matching;
     * Default is 'false', which means results will match case-insensitively
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    /**
     * If set to true, string-based filters will use exact string matches;
     * Default is 'false', which means we'll fuzzy match 
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isStrict() {
        return this.strict;
    }

    /**
     * By default, two queries will be generated for this Criteria: one which fetches the requested page/subset of
     * entity results, and one which fetches the total cardinality of the result set.  If you wish to only retrieve one
     * of those pieces of data, you can do so by setting a restriction on the query generation and execution routines.
     * 
     * The restriction, once set, can be removed by passing NULL to this method.
     * 
     * @see Restriction
     */
    public void setRestriction(Restriction restriction) {
        this.restriction = restriction;
    }

    public Restriction getRestriction() {
        return this.restriction;
    }

    public void setSearchExpression(String searchExpression) {
        this.searchExpression = searchExpression;
    }

    public String getSearchExpression() {
        return this.searchExpression;
    }

    /** subclasses should override as necessary */
    public boolean isInventoryManagerRequired() {
        return false;
    }

    /** subclasses should override as necessary */
    public boolean isSecurityManagerRequired() {
        return false;
    }

    /**
     * @return the permissions required by the user on any applicable objects. Typically resource permissions
     * needed by the user on returned resources or resource related data.
     */
    public List<Permission> getRequiredPermissions() {
        return requiredPermissions;
    }

    /**
     * @param requiredPermissions the permissions required by the user on any applicable objects.
     * Typically resource permissions needed by the user on returned resources or resource related data.
     */
    // TODO (ips): This should really be renamed setRequiredPermissions()...
    public void addRequiredPermissions(Permission... requiredPermissions) {
        this.requiredPermissions = Arrays.asList(requiredPermissions);
    }

    public String getAlias() {
        if (this.alias == null) {
            // Base alias on persistent class's name: org.rhq.core.domain.ResourceType -> "resourcetype"
            // don't use getSimpleName - not available to GWT
            String className = getPersistentClass().getName();
            String classSimpleName = className.substring(className.lastIndexOf(".") + 1);
            this.alias = classSimpleName.toLowerCase();
        }
        return this.alias;
    }

    /**
     * Somewhat analogous to JPA's Query.getSingleResult. Wrap a CriteriaQuery result with this method when
     * expecting a single result from the fetch.  If the result set has only one entry it is returned. Otherwise
     * a RuntimeException is thrown, indicating whether no results, or multiple results were found.
     *    
     * @param result
     * @return
     * @throws RuntimeException In not exactly one result is found.  The message will include either the String
     * "NoResultException" or "NonUniqueResultException", appropriately.  The JPA exceptions are not used so that there
     * is no dependency on a JPA implementation jar for the caller.
     */
    public static <T> T getSingleResult(List<T> result) throws RuntimeException {
        if (null == result || result.isEmpty()) {
            throw new RuntimeException("NoResultException: Expected exactly one result but no result was found.");
        }

        if (1 != result.size()) {
            throw new RuntimeException(
                "NonUniqueResultException: Expected exactly one result but found multiple results: " + result);
        }

        return result.get(0);
    }
}
