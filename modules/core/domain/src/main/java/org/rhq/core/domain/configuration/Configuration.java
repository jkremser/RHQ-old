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
package org.rhq.core.domain.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is the root object for the storage of a hierarchical value set of data. This data may represent configurations
 * of external systems or the components within ON. The data values supported are the basic primitive types in
 * containers of maps and lists. Containers may hold other containers creating the hierarchical data structure. This
 * content is loosely related to the definition entities that can provide a model for the possible values and validation
 * of them.
 *
 * <p>A <code>Configuration</code> has one or more named {@link Property} objects contained within it (similar to a
 * <code>Map</code>). Note that {@link Property} is an abstract class that actually represents either:</p>
 *
 * <ul>
 *   <li>a simple value ({@link PropertySimple})</li>
 *   <li>a list of other {@link Property} objects ({@link PropertyList})</li>
 *   <li>a map of other {@link Property} objects ({@link PropertyMap})</li>
 * </ul>
 *
 * <p>Because a Configuration can contain a list or map of properties, a Configuration can contain a hierarchy of
 * properties N-levels deep.</p>
 *
 * <p>Each Property within a Configuration has a name - this not only includes simple properties, but also lists and
 * maps of properties as well. For example, you can retrieve a list of properties via {@link #getList(String)} by
 * passing in the name of the list.</p>
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 * 
 * @see    Property
 * @see    PropertySimple
 * @see    PropertyList
 * @see    PropertyMap
 */
@Entity(name = "Configuration")
@NamedQueries( { //
@NamedQuery(name = Configuration.QUERY_GET_PLUGIN_CONFIG_BY_RESOURCE_ID, query = "" //
    + "select r.pluginConfiguration from Resource r where r.id = :resourceId"),
    @NamedQuery(name = Configuration.QUERY_GET_RESOURCE_CONFIG_BY_RESOURCE_ID, query = "" //
        + "select r.resourceConfiguration from Resource r where r.id = :resourceId"),
    @NamedQuery(name = Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID, query = "" //
        + "SELECT r.id, r.resourceConfiguration " //
        + "  FROM ResourceGroup rg " //
        + "  JOIN rg.explicitResources r " //
        + " WHERE rg.id = :resourceGroupId"),
    @NamedQuery(name = Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID, query = "" //
        + "SELECT r.id, r.pluginConfiguration " //
        + "  FROM ResourceGroup rg " //
        + "  JOIN rg.explicitResources r " //
        + " WHERE rg.id = :resourceGroupId"),
    @NamedQuery(name = Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_UPDATE_ID, query = "" //
        + "SELECT res.id, cu.configuration " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + "  JOIN cu.resource res " //
        + " WHERE cu.groupConfigurationUpdate.id = :groupConfigurationUpdateId"),
    @NamedQuery(name = Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_UPDATE_ID, query = "" //
        + "SELECT res.id, cu.configuration " //
        + "  FROM PluginConfigurationUpdate cu " //
        + "  JOIN cu.resource res " //
        + " WHERE cu.groupConfigurationUpdate.id = :groupConfigurationUpdateId"),
    @NamedQuery(name = Configuration.QUERY_BREAK_PROPERTY_RECURSION_BY_CONFIGURATION_IDS, query = "" //
        + "UPDATE Property p " //
        + "   SET p.parentMap = NULL, " //
        + "       p.parentList = NULL " //
        + " WHERE p.configuration.id IN ( :configurationIds )"),
    @NamedQuery(name = Configuration.QUERY_DELETE_RAW_CONFIGURATIONS_CONFIGURATION_IDS, query = "" //
        + "DELETE FROM RawConfiguration rc " //
        + " WHERE rc.configuration.id IN ( :configurationIds )"),
    @NamedQuery(name = Configuration.QUERY_DELETE_CONFIGURATIONS_BY_CONFIGURATION_IDs, query = "" //
        + "DELETE FROM Configuration c " //
        + " WHERE c.id IN ( :configurationIds )") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_ID_SEQ")
@Table(name = "RHQ_CONFIG")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Configuration implements Serializable, Cloneable, AbstractPropertyMap {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_GET_PLUGIN_CONFIG_BY_RESOURCE_ID = "Configuration.getPluginConfigByResourceId";
    public static final String QUERY_GET_RESOURCE_CONFIG_BY_RESOURCE_ID = "Configuration.getResourceConfigByResourceId";
    public static final String QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID = "Configuration.getResourceConfigMapByGroupId";
    public static final String QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID = "Configuration.getPluginConfigMapByGroupId";
    public static final String QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_UPDATE_ID = "Configuration.getResourceConfigMapByGroupUpdateId";
    public static final String QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_UPDATE_ID = "Configuration.getPluginConfigMapByGroupUpdateId";

    public static final String QUERY_BREAK_PROPERTY_RECURSION_BY_CONFIGURATION_IDS = "Property.breakPropertyRecursionByConfigurationIds";
    public static final String QUERY_DELETE_RAW_CONFIGURATIONS_CONFIGURATION_IDS = "Configuration.deleteRawByConfigurationIds";
    public static final String QUERY_DELETE_CONFIGURATIONS_BY_CONFIGURATION_IDs = "Configuration.deleteByConfigurationIdS";

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    // CascadeType.REMOVE has been omitted, the cascade delete has been moved to the data model for performance 
    @Cascade( { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DELETE_ORPHAN })
    @MapKey(name = "name")
    @OneToMany(mappedBy = "configuration", fetch = FetchType.EAGER)
    @XmlTransient
    private Map<String, Property> properties = new LinkedHashMap<String, Property>();

    private class PropertiesProxy implements Collection<Property> {

        public int size() {
            return properties.size();
        }

        public boolean isEmpty() {
            return properties.isEmpty();
        }

        public boolean contains(Object o) {
            return properties.containsValue(o);
        }

        public Iterator<Property> iterator() {
            return properties.values().iterator();
        }

        public Object[] toArray() {
            return properties.values().toArray();
        }

        public <T> T[] toArray(T[] a) {
            return properties.values().toArray(a);
        }

        public boolean add(Property e) {
            put(e);
            return true; //we always allow adding an element even if it is already present
        }

        public boolean remove(Object o) {
            return properties.values().remove(o);
        }

        public boolean containsAll(Collection<?> c) {
            return properties.values().containsAll(c);
        }

        public boolean addAll(Collection<? extends Property> c) {
            boolean ret = false;
            for(Property p : c) {
                ret = ret || add(p);
            }
            
            return ret;
        }

        public boolean removeAll(Collection<?> c) {
            boolean ret = false;
            for(Object o : c) {
                ret = ret || remove(o);
            }
            
            return ret;
        }

        public boolean retainAll(Collection<?> c) {
            boolean ret = false;
            ArrayList<Property> ps = new ArrayList<Property>(properties.values());
            for(Property p : ps) {
                if (!c.contains(p)) {
                    ret = ret || remove(p);
                }
            }
            
            return ret;
        }

        public void clear() {
            properties.clear();
        }
        
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            
            if (!(obj instanceof Collection)) {
                return false;
            }
            
            return properties.values().equals(obj);
        }
        
        public int hashCode() {
            return properties.values().hashCode();
        }
        
        public String toString() {
            return properties.values().toString();
        }
    }
    
    private transient PropertiesProxy propertiesProxy;
    
    @OneToMany(mappedBy = "configuration", fetch = FetchType.EAGER)
    @Cascade( { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DELETE_ORPHAN })
    private Set<RawConfiguration> rawConfigurations = new HashSet<RawConfiguration>();

    @Column(name = "NOTES")
    private String notes;

    @Column(name = "VERSION")
    private long version;

    @Column(name = "CTIME")
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME")
    private long mtime = System.currentTimeMillis();

    public Configuration() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getCreatedTime() {
        return this.ctime;
    }

    public long getModifiedTime() {
        return this.mtime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    /**
     * Adds the given property to this Configuration object. The property can be a
     * {@link PropertySimple simple property}, a {@link PropertyList list of properties} or a
     * {@link PropertyMap map of properties}.
     *
     * @param value the new property
     */
    public void put(Property value) {
        getMap().put(value.getName(), value);
        value.setConfiguration(this);
    }

    /**
     * Retrieves the given property from this Configuration object. The named property can be a
     * {@link PropertySimple simple property}, a {@link PropertyList list of properties} or a
     * {@link PropertyMap map of properties}.
     *
     * <p>Note that this only gets direct children of this Configuration. You cannot get a property from within a child
     * list or map via this method.</p>
     *
     * @param  name the name of the property to be retrieved from this configuration
     *
     * @return the named property or <code>null</code> if there was no direct child with the given name
     */
    public Property get(String name) {
        return getMap().get(name);
    }

    /**
     * Removes the given property from this Configuration object. The named property can be a
     * {@link PropertySimple simple property}, a {@link PropertyList list of properties} or a
     * {@link PropertyMap map of properties}.
     *
     * <p>Note that this only removes direct children of this Configuration. You cannot remove a property from within a
     * child list or map via this method.</p>
     *
     * @param  name the name of the property to be removed from this configuration
     *
     * @return the named property or <code>null</code> if there was no direct child with the given name
     */
    public Property remove(String name) {
        return getMap().remove(name);
    }

    /**
     * Same as {@link #get(String)} except that it returns the object as a {@link PropertySimple}.
     *
     * @param  name the name of the simple property to be retrieved
     *
     * @return the simple property with the given name, or <code>null</code> if there was no simple property with the
     *         given name
     *
     * @throws ClassCastException if there was a property in this Configuration with the given name, but it was not of
     *                            type {@link PropertySimple}
     */
    public PropertySimple getSimple(String name) {
        return (PropertySimple) getMap().get(name);
    }

    public String getSimpleValue(String name, @Nullable String defaultValue) {
        PropertySimple property = (PropertySimple) getMap().get(name);
        if ((property != null) && (property.getStringValue() != null)) {
            return property.getStringValue();
        } else {
            return defaultValue;
        }
    }

    /**
     * Same as {@link #get(String)} except that it returns the object as a {@link PropertyList}.
     *
     * @param  name the name of the list property to be retrieved
     *
     * @return the list property with the given name, or <code>null</code> if there was no list property with the given
     *         name
     *
     * @throws ClassCastException if there was a property in this Configuration with the given name, but it was not of
     *                            type {@link PropertyList}
     */
    public PropertyList getList(String name) {
        return (PropertyList) getMap().get(name);
    }

    /**
     * Same as {@link #get(String)} except that it returns the object as a {@link PropertyMap}.
     *
     * @param  name the name of the map property to be retrieved
     *
     * @return the map property with the given name, or <code>null</code> if there was no map property with the given
     *         name
     *
     * @throws ClassCastException if there was a property in this Configuration with the given name, but it was not of
     *                            type {@link PropertyMap}
     */
    public PropertyMap getMap(String name) {
        return (PropertyMap) getMap().get(name);
    }

    /**
     * Returns the contents of this Configuration as a map. The keys to the map are the member properties' names and the
     * values are the properties themselves.
     *
     * <p><b>Warning:</b> Caution should be used when accessing the returned map. Please see
     * {@link PropertyMap the javadoc for this class} for more information.</p>
     *
     * @return the actual map of the property objects - this is <b>not</b> a copy
     */
    @NotNull
    public Map<String, Property> getMap() {
        return this.properties;
    }

    /**
     * Returns the names of all properties that are <i>direct</i> children of this Configuration object.
     *
     * @return child property names
     */
    @NotNull
    public Collection<String> getNames() {
        return getMap().keySet();
    }

    /**
     * Returns all the <i>direct</i> children of this Configuration.
     * This collection is fully modifiable and can be added to and removed from.
     * <p>
     * When adding a property to the collection returned from this method, its
     * {@link Property#getConfiguration() configuration property} is set to this instance.
     * 
     * @return all child properties of this Configuration
     */
    @NotNull
    @XmlElementRefs( { @XmlElementRef(name = "PropertyList", type = PropertyList.class),
        @XmlElementRef(name = "PropertySimple", type = PropertySimple.class),
        @XmlElementRef(name = "PropertyMap", type = PropertyMap.class) })
    public Collection<Property> getProperties() {
        if (propertiesProxy == null) {
            propertiesProxy = new PropertiesProxy();
        }
        return propertiesProxy;
    }

    public void setProperties(Collection<Property> properties) {
        this.properties = new HashMap<String, Property>();
        for (Property p : properties) {
            this.properties.put(p.getName(), p);
        }
    }

    /**
     * Returns a map of all <i>direct</i> children of this Configuration that are of type {@link PropertyMap}. The
     * returned map is keyed on the {@link PropertyMap}'s names.
     *
     * @return map containing of all of the Configuration's direct {@link PropertyMap} children
     */
    @NotNull
    public Map<String, PropertyMap> getMapProperties() {
        Map<String, PropertyMap> map = new LinkedHashMap<String, PropertyMap>();
        for (Property prop : this.getProperties()) {
            if (prop instanceof PropertyMap) {
                map.put(prop.getName(), (PropertyMap) prop);
            }
        }

        return map;
    }

    /**
     * Returns a map of all <i>direct</i> children of this Configuration that are of type {@link PropertyList}. The
     * returned map is keyed on the {@link PropertyList}'s names.
     *
     * @return map containing of all of the Configuration's direct {@link PropertyList} children
     */
    @NotNull
    public Map<String, PropertyList> getListProperties() {
        Map<String, PropertyList> map = new LinkedHashMap<String, PropertyList>();
        for (Property prop : this.getProperties()) {
            if (prop instanceof PropertyList) {
                map.put(prop.getName(), (PropertyList) prop);
            }
        }

        return map;
    }

    /**
     * Returns a map of all <i>direct</i> children of this Configuration that are of type {@link PropertySimple}. The
     * returned map is keyed on the {@link PropertySimple}'s names.
     *
     * @return map containing of all of the Configuration's direct {@link PropertySimple} children
     */
    @NotNull
    public Map<String, PropertySimple> getSimpleProperties() {
        Map<String, PropertySimple> map = new LinkedHashMap<String, PropertySimple>();
        for (Property prop : this.getProperties()) {
            if (prop instanceof PropertySimple) {
                map.put(prop.getName(), (PropertySimple) prop);
            }
        }

        return map;
    }

    public Set<RawConfiguration> getRawConfigurations() {
        return rawConfigurations;
    }

    public void addRawConfiguration(RawConfiguration rawConfiguration) {
        rawConfiguration.setConfiguration(this);
        rawConfigurations.add(rawConfiguration);
    }

    public boolean removeRawConfiguration(RawConfiguration rawConfiguration) {
        boolean removed = rawConfigurations.remove(rawConfiguration);
        if (removed) {
            rawConfiguration.setConfiguration(null);
        }
        return removed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Clones this object in the same manner as {@link #deepCopy()}.
     *
     * @return a clone of this configuration
     *
     * @throws CloneNotSupportedException
     *
     * @see    #deepCopy()
     */
    public Configuration clone() {
        return deepCopy();
    }

    /**
     * Makes a fully independent copy of this object and returns it. This means all children N-levels deep in the
     * hierarchy of this Configuration object are copied.
     *
     * <p>This is the underlying implementation for the {@link #clone()} method.</p>
     *
     * @return a clone of this configuration
     */
    public Configuration deepCopy() {
        return deepCopy(true);
    }

    /**
     * Makes a fully independent copy of this object and returns it. This means all children N-levels deep in the
     * hierarchy of this Configuration object are copied.
     *
     * <p>If <code>keepIds</code> is <code>false</code>, then all IDs of all properties and the config object itself are
     * set to 0. Otherwise, they are kept the same and this method behaves the same as {@link #clone()}.
     *
     * @param  keepIds if <code>false</code>, zero out all IDs
     *
     * @return the new copy
     */
    public Configuration deepCopy(boolean keepIds) {
        Configuration copy = new Configuration();

        if (keepIds) {
            copy.id = this.id;
        }

        copy.notes = this.notes;
        copy.version = this.version;
        createDeepCopyOfProperties(copy, keepIds);
        createDeepCopyOfRawConfigs(copy, keepIds);

        return copy;
    }

    public Configuration deepCopyWithoutProxies() {
        Configuration copy = new Configuration();
        copy.notes = this.notes;
        copy.version = this.version;
        createDeepCopyOfProperties(copy, false);
        createDeepCopyOfRawConfigs(copy, false);

        return copy;
    }

    private void createDeepCopyOfRawConfigs(Configuration copy, boolean keepId) {
        for (RawConfiguration rawConfig : rawConfigurations) {
            copy.addRawConfiguration(rawConfig.deepCopy(keepId));
        }
    }

    private void createDeepCopyOfProperties(Configuration copy, boolean keepId) {
        for (Property property : this.properties.values()) {
            copy.put(property.deepCopy(keepId));
        }
    }

    /**
     * NOTE: A Configuration containing a null map is considered equal to a Configuration containing an empty map.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Configuration)) {
            return false;
        }

        Configuration that = (Configuration) obj;

        return (this.properties.equals(that.properties)) && (this.rawConfigurations.equals(that.rawConfigurations));
    }

    @Override
    public int hashCode() {
        return properties.hashCode() * rawConfigurations.hashCode() * 19;
    }

    @Override
    public String toString() {
        // Default to non-verbose (i.e. not printing the properties), since printing them makes toStrings extremely
        // verbose for large configs.
        final boolean verbose = false;
        return toString(verbose);
    }

    public String toString(boolean verbose) {
        StringBuilder builder = new StringBuilder("Configuration[id=").append(this.id);
        if (this.notes != null) {
            builder.append(", notes=").append(this.notes);
        }

        if (verbose) {
            builder.append(", properties[");
            boolean first = true;
            for (Property property : this.getMap().values()) {
                if (!first) {
                    builder.append(", ");
                } else {
                    first = false;
                }
                builder.append(property.getName());
                builder.append("=");
                if (property instanceof PropertySimple) {
                    builder.append(((PropertySimple) property).getStringValue());
                } else {
                    builder.append(property);
                }
            }
            builder.append("], rawConfigurations[");

            for (RawConfiguration rawConfig : rawConfigurations) {
                builder.append("[").append(rawConfig.getPath()).append(", ").append(rawConfig.getSha256()).append("]");
            }
            builder.append("]");
        }
        return builder.append("]").toString();
    }

    /**
     * This listener runs after jaxb unmarshalling and reconnects children properties to their parent configurations (as
     * we don't send them avoiding cyclic references).
     */
    public void afterUnmarshal(Object u, Object parent) {
        for (Property p : this.properties.values()) {
            p.setConfiguration(this);
        }
    }

    /** Getter for the properties reference. 
     * 
     * @return Map<String, Property> 
     */
    public Map<String, Property> getAllProperties() {
        return this.properties;
    }
}
