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
package org.rhq.plugins.jmx;

import org.rhq.core.domain.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to help in querying object names and utilizing parts of the object name for setting configuration
 * values and changing string messages. A template that you'd build this utility with could look like
 * "foo:name=%myName%,type=myType". This will be "translated" into a valid JMX objectName Query of the form
 * "foo:type=myType,*". It will also detect that we've got a variable defined as "myName" that we'd like to later match
 * to.
 *
 * <p/>We can then find beans with the query and apply their objectName properties to the object and use those found
 * values to rewrite strings or setup configuration properities. For example, we've got a detected object name for the
 * above template "foo:name=bar,type=myType". We set its detected keys against this utility and we now have a variable
 * defined as the key "myName" and the value of "bar". Then I can call formatMessage with "A foo called {myName}" which
 * will be translated into "A foo called bar". This can be useful for naming resources and descriptions using parts of a
 * mapped ObjectName.
 *
 * @author Greg Hinkle
 */
public class ObjectNameQueryUtility {
    private String queryTemplate;

    private Map<String, String> variableProperties = new HashMap<String, String>();

    private Map<String, String> variableValues = new HashMap<String, String>();

    private Set<String> nonVariableProperties = new HashSet<String>();

    private String translatedQuery;

    /**
     * Builds a mapped query utility object and finds the variables in the supplied object name query template.
     *
     * @param objectNameQueryTemplate string of form "a:b=%c%,d=e,f=%g%"
     */
    public ObjectNameQueryUtility(String objectNameQueryTemplate) {
        this.queryTemplate = objectNameQueryTemplate;
        buildMatchMap(queryTemplate);
    }

    /**
     * Builds a mapped query utility object and finds the variables in the supplied object name query template.
     * This version first translates the objectName template for defined values in provided configuration. This
     * is explicitly built for hierarchical objectName models to find the children of parents.
     *
     * @param objectNameQueryTemplate string of form "a:b=%c%,d=e,f=%g%,h={myParentsH}"
     * @param parentConfiguration the config holding the matched values for the object name key property variables
     */
    public ObjectNameQueryUtility(String objectNameQueryTemplate, Configuration parentConfiguration) {

        Pattern p = Pattern.compile("\\{([^\\{\\}]*)\\}");
        Matcher m = p.matcher(objectNameQueryTemplate);
        while (m.find()) {
            String objectNameKeyPropVariableName = m.group(1);
            String value = parentConfiguration.getSimple(objectNameKeyPropVariableName).getStringValue();
            objectNameQueryTemplate = objectNameQueryTemplate.replaceAll("\\{" + objectNameKeyPropVariableName + "\\}", value);
        }

        this.queryTemplate = objectNameQueryTemplate;
        buildMatchMap(this.queryTemplate);
    }

    /**
     * Set values for properties from an objectName. These are first translated into the "real" keys. e.g. foo:bar=%baz%
     * In this case, the property of the bar objectName property will be set into the properties keyed against "baz"
     * which is the real key.
     *
     * @param  keyProperties properties from the found objectName to apply
     *
     * @return true if the objectName properties contained all variable properties or false if some where missing (e.g.
     *         foo:A=%a%,B=%b% is the queryTemplate but objectName found is foo:A=alpha)
     */
    public boolean setMatchedKeyValues(Map<String, String> keyProperties) {
        for (String key : keyProperties.keySet()) {
            if (this.variableProperties.containsKey(key)) {
                String realKey = this.variableProperties.get(key);
                String value = keyProperties.get(key);

                this.variableValues.put(realKey, value);
            }
        }

        // Return true if there are key properties for every variable in the template and false otherwise
        return (keyProperties.keySet().containsAll(this.variableProperties.keySet()));
    }

    /**
     * Format a message with {<key>} formatted replacement keys.
     *
     * @param  message the message to format
     *
     * @return the formatted text with variables replaced
     */
    public String formatMessage(String message) {
        for (String key : variableValues.keySet()) {
            message = message.replaceAll("\\{" + key + "\\}", this.variableValues.get(key));
        }

        return message;
    }

    /**
     * Clears out variables so that a new found bean can be used against the same utility object again.
     */
    public void resetVariables() {
        this.variableValues.clear();
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }

    public Map<String, String> getVariableProperties() {
        return variableProperties;
    }

    public Map<String, String> getVariableValues() {
        return variableValues;
    }

    public String getTranslatedQuery() {
        return translatedQuery;
    }

    /**
     * Detects the mapped variable object name properties and the resulting object name query that can find matching
     * beans.
     *
     * @param objectNameQueryTemplate a template of the form foo:bar=%baz%
     */
    private void buildMatchMap(String objectNameQueryTemplate) {
        StringBuilder queryBuilder = new StringBuilder();

        Pattern p = Pattern.compile("^([^:]*\\:)(.*)$");
        Matcher m = p.matcher(objectNameQueryTemplate);
        if (!m.find()) {
            assert false : "ObjectName did not match expected regular expression: " + objectNameQueryTemplate;
        }

        queryBuilder.append(m.group(1));
        String keyProps = m.group(2);
        String[] keys = keyProps.split(",");

        boolean firstVar = true;
        boolean onlyVar = true;
        for (String key : keys) {
            Pattern p2 = Pattern.compile("^([^=]*)=\\%(.*)\\%$");
            Matcher m2 = p2.matcher(key);
            if (m2.find()) {
                variableProperties.put(m2.group(1), m2.group(2));
            } else {
                Pattern p3 = Pattern.compile("^([^=]*)=(.*)$");
                Matcher m3 = p3.matcher(key);
                if (m3.find()) {
                    nonVariableProperties.add(m3.group(1));
                }

                onlyVar = false;
                if (firstVar) {
                    firstVar = false;
                } else {
                    queryBuilder.append(",");
                }

                queryBuilder.append(key);
            }
        }

        if (variableProperties.size() > 0) {
            if (!onlyVar) {
                queryBuilder.append(",");
            }

            queryBuilder.append("*");
        }

        this.translatedQuery = queryBuilder.toString();
    }

    public boolean isContainsExtraKeyProperties(Set<String> strings) {
        for (String key : strings) {
            if (!nonVariableProperties.contains(key) && !variableProperties.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}