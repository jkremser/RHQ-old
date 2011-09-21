/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

import java.io.Serializable;
import java.util.ArrayList;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;

/**
 * The drift subsystem has a fixed configuration definition. That is, its property definitions
 * are the same always. There is no metadata that needs to be read in from a descriptor - this definition
 * is fixed and the code requires all the property definitions to follow what is encoded in this POJO.
 * 
 * Note that this class must mimic the definition data as found in the database. The installer
 * will prepopulate the configuration definition tables that match the definitions encoded in this POJO.
 *  
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class DriftConfigurationDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PROP_NAME = "name";
    public static final String PROP_ENABLED = "enabled";
    public static final String PROP_BASEDIR = "basedir";
    public static final String PROP_BASEDIR_VALUECONTEXT = "valueContext";
    public static final String PROP_BASEDIR_VALUENAME = "valueName";
    public static final String PROP_INTERVAL = "interval";
    public static final String PROP_DRIFT_HANDLING_MODE = "driftHandlingMode";
    public static final String PROP_INCLUDES = "includes";
    public static final String PROP_INCLUDES_INCLUDE = "include";
    public static final String PROP_EXCLUDES = "excludes";
    public static final String PROP_EXCLUDES_EXCLUDE = "exclude";
    public static final String PROP_PATH = "path"; // for both include and exclude
    public static final String PROP_PATTERN = "pattern"; // for both include and exclude

    // because we know drift config names will actually be used by the agent's plugin container as directories names,
    // we must make sure they are restricted to only be characters valid for file system pathnames.
    // Thus, we only allow config names to only include spaces or "." or "-" or alphanumeric or "_" characters.
    public static final String PROP_NAME_REGEX_PATTERN = "[ \\.\\-\\w]+";

    public static final boolean DEFAULT_ENABLED = true;
    public static final long DEFAULT_INTERVAL = 1800L;
    public static final DriftHandlingMode DEFAULT_DRIFT_HANDLING_MODE = DriftHandlingMode.normal;

    /**
     * The basedir property is specified in two parts - a "context" and a "name". Taken together
     * the value of the basedir can be determined. The value name is just a simple name that
     * is used to look up the basedir value within the appropriate context. A context can be
     * one of four places - either the value is a named property in a resource's plugin configuration,
     * a named property in a resource's resource configuration, a named trait that is emitted by a
     * resource or an absolute path found on a file system.
     */
    public enum BaseDirValueContext {
        pluginConfiguration, resourceConfiguration, measurementTrait, fileSystem
    }

    /**
     * The enumerated values for drift handling mode property
     */
    public enum DriftHandlingMode {
        normal, plannedChanges
    }

    private static final ConfigurationDefinition INSTANCE = new ConfigurationDefinition("GLOBAL_DRIFT_CONFIG_DEF",
        "The drift detection definition");

    /**
     * For drift definitions that have already been created, this definition can be used for editing those existing configuration.
     * Existing drift definitions cannot have their name changed nor can their base directory or includes/excludes be altered.
     */
    private static final ConfigurationDefinition INSTANCE_FOR_EXISTING_CONFIGS = new ConfigurationDefinition(
        "GLOBAL_DRIFT_CONFIG_DEF", "The drift detection definition");

    /**
     * Returns a configuration definition suitable for showing a new configuration form - that is,
     * a configuration that has not yet been created.
     * This will allow all fields to be editable.
     * If you need a configuration definition to show an existing configuration, use the definition
     * returned by {@link #getInstanceForExistingConfiguration()}.
     * 
     * @return configuration definition
     */
    public static ConfigurationDefinition getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a configuration definition suitable for showing an existing drift configuration.
     * This will set certain fields as read-only - those fields which the user is not allowed to
     * edit on exiting drift configurations (which includes name, basedir and includes/excludes filters).
     * 
     * @return configuration definition
     */
    public static ConfigurationDefinition getInstanceForExistingConfiguration() {
        return INSTANCE_FOR_EXISTING_CONFIGS;
    }

    static {
        INSTANCE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        INSTANCE.put(createName(INSTANCE, false));
        INSTANCE.put(createEnabled(INSTANCE));
        INSTANCE.put(createDriftHandlingMode(INSTANCE));
        INSTANCE.put(createInterval(INSTANCE));
        INSTANCE.put(createBasedir(INSTANCE, false));
        INSTANCE.put(createIncludes(INSTANCE, false));
        INSTANCE.put(createExcludes(INSTANCE, false));

        INSTANCE_FOR_EXISTING_CONFIGS.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        INSTANCE_FOR_EXISTING_CONFIGS.put(createName(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createEnabled(INSTANCE_FOR_EXISTING_CONFIGS));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createDriftHandlingMode(INSTANCE_FOR_EXISTING_CONFIGS));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createInterval(INSTANCE_FOR_EXISTING_CONFIGS));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createBasedir(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createIncludes(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createExcludes(INSTANCE_FOR_EXISTING_CONFIGS, true));
    }

    private static PropertyDefinitionSimple createName(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_NAME;
        String description = "The drift detection definition name";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Drift Definition Name");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(configDef);

        RegexConstraint constrait = new RegexConstraint();
        constrait.setDetails(PROP_NAME_REGEX_PATTERN);
        pd.addConstraints(constrait);

        return pd;
    }

    private static PropertyDefinitionSimple createEnabled(ConfigurationDefinition configDef) {
        String name = PROP_ENABLED;
        String description = "Enables or disables the drift definition";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.BOOLEAN;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Enabled");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(configDef);
        pd.setDefaultValue(String.valueOf(DEFAULT_ENABLED));
        return pd;
    }

    private static PropertyDefinitionSimple createDriftHandlingMode(ConfigurationDefinition configDef) {
        String name = PROP_DRIFT_HANDLING_MODE;
        String description = "" //
            + "Specifies the way in which drift instances will be handled when reported. Normal " //
            + "handling implies the reported drift is unexpected and as such can trigger alerts, " //
            + "will be present in recent drift reports, etc.  Setting to 'Planned Changes' implies " //
            + "that the reported drift is happening at a time when drift is expected due to " //
            + "planned changes in the monitored environment, such as an application deployment, a " //
            + "configuration change, or something similar.  With this setting drift is only reported " //
            + " for inspection, in drift snapshot views.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Drift Handling Mode");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(2);
        pd.setConfigurationDefinition(configDef);

        PropertyDefinitionEnumeration normalEnum = new PropertyDefinitionEnumeration(DriftHandlingMode.normal.name(),
            DriftHandlingMode.normal.name());
        normalEnum.setOrderIndex(0);

        PropertyDefinitionEnumeration plannedEnum = new PropertyDefinitionEnumeration(DriftHandlingMode.plannedChanges
            .name(), DriftHandlingMode.plannedChanges.name());
        plannedEnum.setOrderIndex(1);

        ArrayList<PropertyDefinitionEnumeration> pdEnums = new ArrayList<PropertyDefinitionEnumeration>(2);
        pdEnums.add(normalEnum);
        pdEnums.add(plannedEnum);
        pd.setEnumeratedValues(pdEnums, false);
        pd.setDefaultValue(DEFAULT_DRIFT_HANDLING_MODE.name());

        return pd;
    }

    private static PropertyDefinitionSimple createInterval(ConfigurationDefinition configDef) {
        String name = PROP_INTERVAL;
        String description = "The frequency in seconds in which drift detection should run. Defaults to 1800 seconds (i.e. 30 minutes)";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.LONG;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Interval");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(3);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(configDef);
        pd.setDefaultValue(String.valueOf(DEFAULT_INTERVAL));
        return pd;
    }

    private static PropertyDefinitionMap createBasedir(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_BASEDIR;
        String description = "The root directory from which snapshots will be generated during drift monitoring.";
        boolean required = true;

        PropertyDefinitionSimple valueContext = createBasedirValueContext(readOnly);
        PropertyDefinitionSimple valueName = createBasedirValueName(readOnly);

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, valueContext, valueName);
        pd.setDisplayName("Base Directory");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(4);
        pd.setConfigurationDefinition(configDef);

        return pd;
    }

    private static PropertyDefinitionSimple createBasedirValueContext(boolean readOnly) {
        String name = PROP_BASEDIR_VALUECONTEXT;
        String description = "Identifies where the named value can be found.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Value Context");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);

        PropertyDefinitionEnumeration pcEnum = new PropertyDefinitionEnumeration(
            BaseDirValueContext.pluginConfiguration.name(), BaseDirValueContext.pluginConfiguration.name());
        pcEnum.setOrderIndex(0);

        PropertyDefinitionEnumeration rcEnum = new PropertyDefinitionEnumeration(
            BaseDirValueContext.resourceConfiguration.name(), BaseDirValueContext.resourceConfiguration.name());
        rcEnum.setOrderIndex(1);

        PropertyDefinitionEnumeration mtEnum = new PropertyDefinitionEnumeration(BaseDirValueContext.measurementTrait
            .name(), BaseDirValueContext.measurementTrait.name());
        mtEnum.setOrderIndex(2);

        PropertyDefinitionEnumeration fsEnum = new PropertyDefinitionEnumeration(BaseDirValueContext.fileSystem.name(),
            BaseDirValueContext.fileSystem.name());
        fsEnum.setOrderIndex(3);

        ArrayList<PropertyDefinitionEnumeration> pdEnums = new ArrayList<PropertyDefinitionEnumeration>(4);
        pdEnums.add(pcEnum);
        pdEnums.add(rcEnum);
        pdEnums.add(mtEnum);
        pdEnums.add(fsEnum);
        pd.setEnumeratedValues(pdEnums, false);

        return pd;
    }

    private static PropertyDefinitionSimple createBasedirValueName(boolean readOnly) {
        String name = PROP_BASEDIR_VALUENAME;
        String description = "The name of the value as found in the context.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Value Name");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionList createIncludes(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_INCLUDES;
        String description = "A set of patterns that specify files and/or directories to include.";
        boolean required = false;

        PropertyDefinitionMap map = createInclude(readOnly);

        PropertyDefinitionList pd = new PropertyDefinitionList(name, description, required, map);
        pd.setDisplayName("Includes");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(5);
        pd.setConfigurationDefinition(configDef);
        return pd;
    }

    private static PropertyDefinitionMap createInclude(boolean readOnly) {
        String name = PROP_INCLUDES_INCLUDE;
        String description = "A pattern that specifies a file or directory to include.";
        boolean required = true;

        PropertyDefinitionSimple path = createIncludePath(readOnly);
        PropertyDefinitionSimple pattern = createIncludePattern(readOnly);

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, path, pattern);
        pd.setDisplayName("Include");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        return pd;
    }

    private static PropertyDefinitionSimple createIncludePath(boolean readOnly) {
        String name = PROP_PATH;
        String description = "A file system path that can be a directory or a file. The path is assumed to be relative to the base directory of the drift definition.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Path");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionSimple createIncludePattern(boolean readOnly) {
        String name = PROP_PATTERN;
        String description = "Pathname pattern that must match for the items in the directory path to be included.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Pattern");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionList createExcludes(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_EXCLUDES;
        String description = "A set of patterns that specify files and/or directories to exclude.";
        boolean required = false;

        PropertyDefinitionMap map = createExclude(readOnly);

        PropertyDefinitionList pd = new PropertyDefinitionList(name, description, required, map);
        pd.setDisplayName("Excludes");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(6);
        pd.setConfigurationDefinition(configDef);
        return pd;
    }

    private static PropertyDefinitionMap createExclude(boolean readOnly) {
        String name = PROP_EXCLUDES_EXCLUDE;
        String description = "A pattern that specifies a file or directory to exclude.";
        boolean required = true;

        PropertyDefinitionSimple path = createExcludePath(readOnly);
        PropertyDefinitionSimple pattern = createExcludePattern(readOnly);

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, path, pattern);
        pd.setDisplayName("Exclude");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        return pd;
    }

    private static PropertyDefinitionSimple createExcludePath(boolean readOnly) {
        String name = PROP_PATH;
        String description = "A file system path that can be a directory or a file. The path is assumed to be relative to the base directory of the drift definition.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Path");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionSimple createExcludePattern(boolean readOnly) {
        String name = PROP_PATTERN;
        String description = "Pathname pattern that must match for the items in the directory path to be excluded.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Pattern");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

}
