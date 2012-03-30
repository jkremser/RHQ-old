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
package org.rhq.enterprise.server.auth.prefs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.util.StringUtil;

public abstract class SubjectPreferencesBase {

    protected final Log log = LogFactory.getLog(SubjectPreferencesBase.class);

    /** delimiter for preferences that are multi-valued and stringified */
    protected static final String PREF_LIST_DELIM = ",";
    protected static final String PREF_ITEM_DELIM = "|";
    protected static final String PREF_ITEM_DELIM_REGEX = "\\|";

    private int subjectId;
    private Set<String> changed;

    public SubjectPreferencesBase(Subject subject) {
        this.subjectId = subject.getId();
        this.changed = new HashSet<String>();
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    protected boolean getBooleanPref(String key) {
        String val = getPreference(key);
        return Boolean.valueOf(val).booleanValue();
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    protected boolean getBooleanPref(String key, boolean ifNull) {
        String val;
        try {
            val = getPreference(key);
        } catch (IllegalArgumentException e) {
            return ifNull;
        }
        return Boolean.valueOf(val).booleanValue();
    }

    /**
     * Get the value of a preference as an int.
     * @param key the preference to get
     * @return the int value of 'key'
     */
    protected int getIntPref(String key) {
        String val = getPreference(key);
        return Integer.parseInt(val);
    }

    /**
     * Get the value of a preference as an int.
     * @param key the preference to get
     * @param ifNull if the pref is null, return this value instead
     * @return the int value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    protected int getIntPref(String key, int ifNull) {
        String val;
        try {
            val = getPreference(key);
            if ("".equals(val)) {
                return ifNull;
            }
        } catch (IllegalArgumentException e) {
            return ifNull;
        }
        return Integer.parseInt(val);
    }

    /**
     * Get the value of a preference as an long.
     * @param key the preference to get
     * @return the long value of 'key'
     */
    protected Long getLongPref(String key) {
        String val = getPreference(key);
        return Long.parseLong(val);
    }

    protected String getPreference(String key) throws IllegalArgumentException {
        PropertySimple prop = SubjectPreferencesCache.getInstance().getUserProperty(subjectId, key);

        if (prop == null) {
            if (this.subjectId == 0) {
                return ""; // this is probably an LDAP user that needs to register first
            }
            throw new IllegalArgumentException("preference '" + key + "' requested is not valid");
        }

        String value = prop.getStringValue();

        // null values are often the default for many props; let the caller determine whether this is an error
        if (value != null) {
            value = value.trim();
        }
        log.trace("Getting " + key + "[" + value + "]");

        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPreference(String key, T defaultValue) {
        T result;
        try {
            String preferenceValue = getPreference(key);

            Class<T> type = (Class<T>) String.class;
            if (defaultValue != null) {
                type = (Class<T>) defaultValue.getClass();
            }

            if (type == String.class) {
                result = (T) preferenceValue; // cast string to self-type
            } else {
                if (type == Boolean.class) {
                    if (preferenceValue.equalsIgnoreCase("on") || preferenceValue.equalsIgnoreCase("yes")
                        || preferenceValue.equalsIgnoreCase("true")) {
                        preferenceValue = "true"; // flexible support for boolean translations from forms
                    } else {
                        preferenceValue = "false";
                    }
                }

                try {
                    Method m = type.getMethod("valueOf", String.class);
                    result = (T) m.invoke(null, preferenceValue); // static method
                } catch (Exception e) {
                    throw new IllegalArgumentException("No support for automatic conversion of preferences of type "
                        + type);
                }
            }
        } catch (IllegalArgumentException iae) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Break the named preference into tokens delimited by <code>PREF_LIST_DELIM</code>.
     *
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    public List<String> getPreferenceAsList(String key) {
        return getPreferenceAsList(key, PREF_LIST_DELIM);
    }

    /**
     * Tokenize the named preference into a List of Strings. If no such preference exists, or the preference is null,
     * an empty List will be returned.
     *
     * @param delimiter the delimiter to break it up by
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    public List<String> getPreferenceAsList(String key, String delimiter) {
        String pref = null;
        try {
            pref = getPreference(key);
        } catch (IllegalArgumentException e) {
            log.debug("A user preference named '" + key + "' does not exist.");
        }
        return (pref != null) ? StringUtil.explode(pref, delimiter) : new ArrayList<String>();
    }

    protected List<Integer> getPreferenceAsIntegerList(String key, String delimiter) {
        try {
            List<String> values = getPreferenceAsList(key, delimiter);

            List<Integer> result = new ArrayList<Integer>(values.size());
            for (String value : values) {
                String trimmed = value.trim();
                if (trimmed.length() > 0) {
                    result.add(Integer.valueOf(trimmed));
                }
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<Integer>();
        }
    }

    protected void setPreference(String key, List<?> values) throws IllegalArgumentException {
        setPreference(key, values, PREF_LIST_DELIM);
    }

    protected void setPreference(String key, List<?> values, String delim) throws IllegalArgumentException {
        String stringified = StringUtil.listToString(values, delim);
        setPreference(key, stringified);
    }

    public void setPreference(String key, Object value) throws IllegalArgumentException {
        String val;
        if (value == null) {
            val = "";
        } else if (value instanceof String) {
            val = (String) value;
        } else {
            val = value.toString();
        }

        SubjectPreferencesCache.getInstance().setUserProperty(subjectId, key, val);
    }

    protected void unsetPreference(String key) {
        SubjectPreferencesCache.getInstance().unsetUserProperty(subjectId, key);
    }
}
