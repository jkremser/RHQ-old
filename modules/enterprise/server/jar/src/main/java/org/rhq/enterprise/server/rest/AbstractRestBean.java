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
package org.rhq.enterprise.server.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.domain.GroupRest;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

/**
 * Abstract base class for EJB classes that implement REST methods.
 * For the cache and its eviction policies see standalone-full.xml (in
 * the RHQ Server's AS7/standalone/configuration directory, as modified
 * by the installer.)
 *
 * @author Heiko W. Rupp
 * @author Jay Shaughnessy
 */
@javax.annotation.Resource(name = "ISPN", mappedName = "java:jboss/infinispan/rhq")
@SuppressWarnings("unchecked")
public class AbstractRestBean {

    Log log = LogFactory.getLog(getClass().getName());

    static private final CacheKey META_KEY = new CacheKey("rhq.rest.resourceMeta", 0);

    @javax.annotation.Resource( name = "ISPN")
    protected CacheContainer container;
    protected Cache<CacheKey, Object> cache;

    /** Subject of the caller that gets injected via {@link SetCallerInterceptor} */
    protected Subject caller;

    @EJB
    ResourceManagerLocal resMgr;
    @EJB
    ResourceGroupManagerLocal resourceGroupManager;

    @PostConstruct
    public void start() {
        this.cache = this.container.getCache("REST-API");
    }

    /**
     * Renders the passed object with the help of a freemarker template into a string. Freemarket templates
     * are searched in the class path in a directory called "/rest_templates". In the usual Maven tree structure,
     * this is below src/main/resources/.
     *
     * @param templateName Template to use for rendering. If the template name does not end in .ftl, .ftl is appended.
     * @param objectToRender Object to render via template
     * @return Template filled with data from objectToRender
     */
    protected String renderTemplate(String templateName, Object objectToRender) {
        try {
            freemarker.template.Configuration config = new Configuration();

            // XXX fall-over to ClassTL after failure in FTL seems not to work
            // FileTemplateLoader ftl = new FileTemplateLoader(new File("src/main/resources"));
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/rest_templates/");
            TemplateLoader[] loaders = new TemplateLoader[] { ctl };
            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);

            config.setTemplateLoader(mtl);

            if (!templateName.endsWith(".ftl"))
                templateName = templateName + ".ftl";
            Template template = config.getTemplate(templateName);

            StringWriter out = new StringWriter();
            try {
                Map<String, Object> root = new HashMap<String, Object>();
                root.put("var", objectToRender);
                template.process(root, out);
                return out.toString();
            } finally {
                out.close();
            }
        } catch (IOException ioe) {
            log.error(ioe);
        } catch (TemplateException te) {
            log.error(te.getMessage());
        }
        return null;
    }

    /**
     * Retrieve an object from the cache. This is identified by its class and id
     * @param id Id of the object to load.
     * @param clazz Wanted return type
     * @return Object if found and the caller has access to it.
     * @see #getFromCache(int, Class)
     */
    protected <T> T getFromCache(int id, Class<T> clazz) {
        CacheKey key = new CacheKey(clazz, id);
        return getFromCache(key, clazz);
    }

    /**
     * Retrieve an object from the cache if present or null otherwise.
     * We need to be careful here as we must not return objects the current
     * caller has no access to. We do this by checking the "readers" attribute
     * of the selected node to see if the caller has put the object there
     * @param key FullyQualified name (=path in cache) of the object to retrieve
     * @param clazz Return type
     * @return The desired object if found and valid for the current caller. Null otherwise.
     * @see #putToCache(CacheKey, Object)
     */
    protected <T> T getFromCache(CacheKey key, Class<T> clazz) {
        Object o = null;

        CacheValue value = (CacheValue) cache.get(key);

        if (null != value) {
            if (log.isDebugEnabled()) {
                log.debug("Cache Hit for " + key);
            }

            if (value.getReaders().contains(caller.getId())) {
                o = value.getValue();

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cache Hit ignored, caller " + caller.toString() + " not found");
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Cache Miss for " + key);
            }
        }

        return (T) o;
    }

    /**
     * Put an object into the cache identified by its type and id
     * @param id Id of the object to put
     * @param clazz Type to put in
     * @param o Object to put
     * @return true if put was successful
     * @see #putToCache(CacheKey, Object)
     */
    protected <T> boolean putToCache(int id, Class<T> clazz, T o) {
        CacheKey key = new CacheKey(clazz, id);
        return putToCache(key, o);
    }

    /**
     * Put an object into the cache. We need to record the caller so that we can later
     * check if the caller can access that object or not.
     * @param key Fully qualified name (=path to object)
     * @param o Object to put
     * @return true if put was successful
     * @see #getFromCache(CacheKey, Class)
     */
    @SuppressWarnings("unchecked")
    protected <T> boolean putToCache(CacheKey key, T o) {
        boolean result = false;

        CacheValue value = (CacheValue) cache.get(key);
        if (null != value) {
            value.getReaders().add(caller.getId());
            value.setValue(o);
        } else {
            value = new CacheValue(o, caller.getId());
        }
        try {
            cache.put(key, value);

            if (log.isDebugEnabled()) {
                log.debug("Cache Put " + key);
            }

            result = true;
        }

        catch (Exception e) {
            log.warn(e.getMessage());
        }

        return result;
    }

    protected void putResourceToCache(Resource res) {
        putToCache(res.getId(), Resource.class, res);

        CacheKey callerKey = new CacheKey("rhq.rest.caller", caller.getId());

        try {
            Set<Integer> visibleResources = (Set<Integer>) cache.get(callerKey);

            if (null == visibleResources) {
                visibleResources = new HashSet<Integer>();
            }

            visibleResources.add(res.getId());
            cache.put(callerKey, visibleResources);

            Map<Integer, Integer> childParentMap = (Map<Integer, Integer>) cache.get(META_KEY);

            if (null == childParentMap) {
                childParentMap = new HashMap<Integer, Integer>();
            }
            int pid = res.getParentResource() == null ? 0 : res.getParentResource().getId();
            childParentMap.put(res.getId(), pid);
            cache.put(META_KEY, childParentMap);

        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    protected List<Resource> getResourcesFromCacheByParentId(int pid) {
        List<Integer> candidateIds = new ArrayList<Integer>();
        List<Resource> ret = new ArrayList<Resource>();

        // First determine candidate children
        Map<Integer, Integer> childParentMap = (Map<Integer, Integer>) cache.get(META_KEY);

        if (null != childParentMap) {
            try {
                for (Map.Entry<Integer, Integer> entry : childParentMap.entrySet()) {
                    if (entry.getValue() == pid)
                        candidateIds.add(entry.getKey());
                }
                // then see if the current user can see them
                CacheKey callerKey = new CacheKey("rhq.rest.caller", caller.getId());
                Set<Integer> visibleResources = (Set<Integer>) cache.get(callerKey);
                Iterator<Integer> iter = candidateIds.iterator();
                while (iter.hasNext()) {
                    Integer resId = iter.next();
                    if (!visibleResources.contains(resId)) {
                        iter.remove();
                    }
                }

                // Last but not least, get the resources and return them
                for (Integer resId : candidateIds) {
                    ret.add(getFromCache(resId, Resource.class));
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }

        }
        return ret;
    }

    protected Resource getResourceFromCache(int resourceid) {

        Resource res = null;
        // check if the current user can see the resource
        CacheKey callerKey = new CacheKey("rhq.rest.caller", caller.getId());
        Set<Integer> visibleResources = (Set<Integer>) cache.get(callerKey);
        if (null != visibleResources) {
            try {
                if (visibleResources.contains(resourceid)) {
                    res = getFromCache(resourceid, Resource.class);
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }

        return res;
    }

    /**
     * Remove an item from the cache
     * @param id Id of the item
     * @param clazz Type of object for that node
     * @return true if object is no longer in cache
     */
    protected <T> boolean removeFromCache(int id, Class<T> clazz) {
        CacheKey key = new CacheKey(clazz, id);
        Object cacheValue = cache.remove(key);
        if (null != cacheValue) {
            log.debug("Cache Remove " + key);
        }

        return true;
    }

    public ResourceWithType fillRWT(Resource res, UriInfo uriInfo) {
        ResourceType resourceType = res.getResourceType();
        ResourceWithType rwt = new ResourceWithType(res.getName(), res.getId());
        rwt.setTypeName(resourceType.getName());
        rwt.setTypeId(resourceType.getId());
        rwt.setPluginName(resourceType.getPlugin());
        Resource parent = res.getParentResource();
        if (parent != null) {
            rwt.setParentId(parent.getId());
        } else
            rwt.setParentId(0);

        rwt.setAncestry(res.getAncestry());

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/operation/definitions");
        uriBuilder.queryParam("resourceId", res.getId());
        URI uri = uriBuilder.build();
        Link link = new Link("operationDefinitions", uri.toString());
        rwt.addLink(link);

        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}");
        uri = uriBuilder.build(res.getId());
        link = new Link("self", uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/schedules");
        uri = uriBuilder.build(res.getId());
        link = new Link("schedules", uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/availability");
        uri = uriBuilder.build(res.getId());
        link = new Link("availability", uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/children");
        uri = uriBuilder.build(res.getId());
        link = new Link("children", uri.toString());
        rwt.addLink(link);
        if (parent != null) {
            uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}/");
            uri = uriBuilder.build(parent.getId());
            link = new Link("parent", uri.toString());
            rwt.addLink(link);
        }

        return rwt;
    }

    protected Resource fetchResource(int resourceId) {
        Resource res;
        res = resMgr.getResource(caller, resourceId);
        if (res == null)
            throw new StuffNotFoundException("Resource with id " + resourceId);
        /*
                res = getFromCache(resourceId, Resource.class);
                if (res == null) {
                    res = resMgr.getResource(caller, resourceId);
                    if (res != null)
                        putToCache(resourceId, Resource.class, res);
                    else
                        throw new StuffNotFoundException("Resource with id " + resourceId);
                }
        */
        return res;
    }

    /**
     * Fetch the group with the passed id
     *
     * @param groupId id of the resource group
     * @param requireCompatible Does the group have to be a compatible group?
     * @return the group object if found
     * @throws org.rhq.enterprise.server.rest.StuffNotFoundException if the group is not found (or not accessible by the caller)
     * @throws IllegalArgumentException if a compatible group is required, but the found one is not a compatible one
     */
    protected ResourceGroup fetchGroup(int groupId, boolean requireCompatible) {
        ResourceGroup resourceGroup;
        resourceGroup = resourceGroupManager.getResourceGroup(caller, groupId);
        if (resourceGroup == null)
            throw new StuffNotFoundException("Group with id " + groupId);
        if (requireCompatible) {
            if (resourceGroup.getGroupCategory() != GroupCategory.COMPATIBLE) {
                throw new IllegalArgumentException("Group with id " + groupId + " is no compatible group");
            }
        }
        return resourceGroup;
    }

    protected GroupRest fillGroup(ResourceGroup group, UriInfo uriInfo) {

        GroupRest gr = new GroupRest(group.getName());
        gr.setId(group.getId());
        gr.setCategory(group.getGroupCategory());
        gr.setRecursive(group.isRecursive());
        if (group.getGroupDefinition()!=null)
            gr.setDynaGroupDefinitionId(group.getGroupDefinition().getId());
        gr.setExplicitCount(group.getExplicitResources().size());
        gr.setImplicitCount(group.getImplicitResources().size());
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}");
        URI uri = uriBuilder.build(group.getId());
        Link link = new Link("edit",uri.toASCIIString());
        gr.getLinks().add(link);

        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}/metricDefinitions");
        uri = uriBuilder.build(group.getId());
        link = new Link("metricDefinitions",uri.toASCIIString());
        gr.getLinks().add(link);

        return gr;
    }

    private static class CacheKey {
        private String namespace;
        private int id;

        /**
         * @param clazz The class name will be used as the namespace for the id.
         * @param id
         */
        public CacheKey(Class<?> clazz, int id) {
            this(clazz.getName(), id);
        }

        public CacheKey(String namespace, int id) {
            this.namespace = namespace;
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            result = prime * result + id;

            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            if (namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            } else if (!namespace.equals(other.namespace)) {
                return false;
            }
            if (id != other.id) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "CacheKey [namespace=" + namespace + ", id=" + id + "]";
        }
    }

    private static class CacheValue {
        private Object value;
        private Set<Integer> readers;

        public CacheValue(Object value, int readerId) {
            this.readers = new HashSet<Integer>();
            this.readers.add(readerId);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Set<Integer> getReaders() {
            return readers;
        }
    }
}
