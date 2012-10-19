/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.serviceBinding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jboss.metatype.api.types.CollectionMetaType;
import org.jboss.metatype.api.types.CompositeMetaType;
import org.jboss.metatype.api.types.ImmutableCompositeMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.CollectionValue;
import org.jboss.metatype.api.values.CollectionValueSupport;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.MapCompositeValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Utility class for the SBM components.
 * 
 * @author Lukas Krejci
 */
class Util {

    public static final String STANDARD_BINDINGS_PROPERTY = "standardBindings";
    public static final String OVERRIDE_BINDINGS_PROPERTY = "overrideBindings";
    public static final String ACTIVE_BINDING_SET_NAME_PROPERTY = "activeBindingSetName";
    public static final String BINDING_SETS_PROPERTY = "bindingSets";
    
    public static final String NAME_PROPERTY = "name";
    public static final String DEFAULT_HOST_NAME_PROPERTY = "defaultHostName";
    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String FULLY_QUALIFIED_NAME_PROPERTY = "fullyQualifiedName";
    public static final String BINDING_NAME_PROPERTY = "bindingName";
    public static final String SERVICE_NAME_PROPERTY = "serviceName";
    public static final String PORT_OFFSET_PROPERTY = "portOffset";
    public static final String HOST_NAME_PROPERTY = "hostName";
    public static final String PORT_PROPERTY = "port";
    public static final String FIXED_HOST_NAME_PROPERTY = "fixedHostName";
    public static final String FIXED_PORT_PROPERTY = "fixedPort";
    
    public static class PropertyDefinition {
        public String propertyName;
        public Class<?> propertyType;

        public PropertyDefinition(String propertyName, Class<?> propertyType) {
            this.propertyName = propertyName;
            this.propertyType = propertyType;
        }
    }

    public static final PropertyDefinition[] BINDING_SET_SIMPLE_PROPERTIES = {
        new PropertyDefinition(NAME_PROPERTY, String.class), new PropertyDefinition(DEFAULT_HOST_NAME_PROPERTY, String.class),
        new PropertyDefinition(PORT_OFFSET_PROPERTY, Integer.class) };

    /**
     * All the properties of the override binding.
     */
    public static final PropertyDefinition[] BINDING_SET_OVERRIDE_PROPERTIES = {
        new PropertyDefinition(SERVICE_NAME_PROPERTY, String.class), new PropertyDefinition(BINDING_NAME_PROPERTY, String.class),
        new PropertyDefinition(FULLY_QUALIFIED_NAME_PROPERTY, String.class),
        new PropertyDefinition(DESCRIPTION_PROPERTY, String.class), new PropertyDefinition(HOST_NAME_PROPERTY, String.class),
        new PropertyDefinition(PORT_PROPERTY, Integer.class) };

    /**
     * Properties of a standard binding.
     */
    public static final PropertyDefinition[] STANDARD_BINDING_PROPERTIES = {
    new PropertyDefinition(SERVICE_NAME_PROPERTY, String.class), new PropertyDefinition(BINDING_NAME_PROPERTY, String.class),
    new PropertyDefinition(PORT_PROPERTY, Integer.class), new PropertyDefinition(HOST_NAME_PROPERTY, String.class),
    new PropertyDefinition(DESCRIPTION_PROPERTY, String.class),
    new PropertyDefinition(FULLY_QUALIFIED_NAME_PROPERTY, String.class),
    new PropertyDefinition(FIXED_HOST_NAME_PROPERTY, Boolean.class), new PropertyDefinition(FIXED_PORT_PROPERTY, Boolean.class) };

    private Util() {

    }

    public static <T> T getValue(SimpleValue value, Class<T> type) {
        if (value == null)
            return null;
        return type.cast(value.getValue());
    }

    public static <T> T getValue(CompositeValue compositeValue, String simpleValueName, Class<T> type) {
        SimpleValue val = (SimpleValue) compositeValue.get(simpleValueName);

        return getValue(val, type);
    }

    /**
     * Creates the binding set composite value from the supplied configuration.
     * 
     * @param bindingSetValueType the meta type of the returned composite value
     * @param configuration the configuration of the binding set to create
     * @return the binding set composite value ready to supply to Profile service
     * @throws Exception on error
     */
    public static CompositeValue getBindingSetFromConfiguration(MetaType bindingSetValueType,
        Configuration configuration) throws Exception {

        MapCompositeValueSupport currentBindingSet = new MapCompositeValueSupport(bindingSetValueType);

        //update the simple properties
        for (PropertyDefinition def : Arrays.asList(BINDING_SET_SIMPLE_PROPERTIES)) {
            currentBindingSet.put(def.propertyName, Util.wrap(configuration.getSimple(def.propertyName),
                def.propertyType));
        }

        CollectionMetaType overrideBindingsMetaType = (CollectionMetaType) ((ImmutableCompositeMetaType) bindingSetValueType)
            .getType(OVERRIDE_BINDINGS_PROPERTY);

        //update the override bindings
        List<MetaValue> updatedOverrideBindings = new ArrayList<MetaValue>();
        for (Property prop : configuration.getList(OVERRIDE_BINDINGS_PROPERTY).getList()) {
            PropertyMap updatedBinding = (PropertyMap) prop;

            MapCompositeValueSupport newBinding = new MapCompositeValueSupport(
                (CompositeMetaType) overrideBindingsMetaType.getElementType());

            for (PropertyDefinition def : Arrays.asList(BINDING_SET_OVERRIDE_PROPERTIES)) {
                newBinding.put(def.propertyName, Util
                    .wrap(updatedBinding.getSimple(def.propertyName), def.propertyType));
            }
            updatedOverrideBindings.add(newBinding);
        }

        //recreate the collection of override bindings
        CollectionValueSupport newOverrideBindings = new CollectionValueSupport(overrideBindingsMetaType);
        newOverrideBindings.setElements(updatedOverrideBindings.toArray(new MetaValue[updatedOverrideBindings.size()]));
        currentBindingSet.put(OVERRIDE_BINDINGS_PROPERTY, newOverrideBindings);

        return currentBindingSet;
    }

    /**
     * Creates a new collection of metavalues from the supplied collection.
     * The collection is supposed to contain composite values which must have a
     * "name" composite. If the name composite matches the replacedName parameter,
     * the newValue will be inserted into the resulting list instead of that composite value.
     * If newValue is null, it is not inserted into the result.
     * 
     * @param collection the collection to copy
     * @param replacedName the name of the composite value to replace
     * @param newValue a replacement for the composite value or null if the composite value should be removed from the collection
     * 
     * @return a copy of the collection with the appropriate element replaced or removed
     */
    public static List<MetaValue> replaceWithNew(CollectionValue collection, String replacedName, MetaValue newValue) {
        List<MetaValue> ret = new ArrayList<MetaValue>();

        Iterator<MetaValue> it = collection.iterator();
        while (it.hasNext()) {
            CompositeValue value = (CompositeValue) it.next();

            String currentName = getValue(value, "name", String.class);
            if (replacedName.equals(currentName)) {
                if (newValue != null) {
                    ret.add(newValue);
                }
            } else {
                ret.add(value);
            }
        }

        return ret;
    }

    /**
     * This can create an appropriate SimpleValue out of the property.
     * Obviously the method is not generic and supports only
     * the types that have corresponding conversion methods in the ProperySimple class.
     * 
     * @param value
     * @param type
     * @return the simple value
     */
    public static SimpleValue wrap(PropertySimple value, Class<?> type) {
        Serializable ret = null;

        if (value == null) {
            ret = null;
        } else if (value.getStringValue() == null) {
            ret = null;
        } else if (type == Integer.class) {
            ret = value.getIntegerValue();
        } else if (type == Long.class) {
            ret = value.getLongValue();
        } else if (type == Double.class) {
            ret = value.getDoubleValue();
        } else if (type == Boolean.class) {
            ret = value.getBooleanValue();
        } else if (type == Float.class) {
            ret = value.getFloatValue();
        } else if (type == String.class) {
            ret = value.getStringValue();
        }

        return ret == null ? null : SimpleValueSupport.wrap(ret);
    }

    public static List<PropertySimple> getProperties(List<PropertyDefinition> properties, CompositeValue value) {
        ArrayList<PropertySimple> ret = new ArrayList<PropertySimple>();

        for (PropertyDefinition def : properties) {
            ret.add(new PropertySimple(def.propertyName, getValue(value, def.propertyName, def.propertyType)));
        }

        return ret;
    }
}
