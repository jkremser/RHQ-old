/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.importers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.NoSingleEntity;
import org.rhq.enterprise.server.system.SystemManagerLocal;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class SystemSettingsImporter implements Importer<NoSingleEntity, SystemSettings> {

    public static final String PROPERTIES_TO_IMPORT_PROPERTY = "propertiesToImport";
    public static final String DEFAULT_IMPORTED_PROPERTIES_LIST =
        "AGENT_MAX_QUIET_TIME_ALLOWED, ENABLE_AGENT_AUTO_UPDATE, ENABLE_DEBUG_MODE, ENABLE_EXPERIMENTAL_FEATURES, CAM_DATA_PURGE_1H, CAM_DATA_PURGE_6H, "
            + "CAM_DATA_PURGE_1D, CAM_DATA_MAINTENANCE, DATA_REINDEX_NIGHTLY, RT_DATA_PURGE, ALERT_PURGE, EVENT_PURGE, TRAIT_PURGE, AVAILABILITY_PURGE, CAM_BASELINE_FREQUENCY, CAM_BASELINE_DATASET";
    
    private Subject subject;
    private SystemManagerLocal systemManager;
    private Configuration importConfiguration;
    private Unmarshaller unmarshaller;

    public SystemSettingsImporter(Subject subject, SystemManagerLocal systemManager) {
        try {
            this.subject = subject;
            this.systemManager = systemManager;
            JAXBContext context = JAXBContext.newInstance(SystemSettings.class);
            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize JAXB marshaller for MetricTemplate.", e);
        }
    }

    @Override
    public ConfigurationDefinition getImportConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("SystemSettingsConfiguration", null);

        PropertyDefinitionSimple props =
            new PropertyDefinitionSimple(
                PROPERTIES_TO_IMPORT_PROPERTY,
                "The names of the properties that should be imported. Note that these are the INTERNAL names as used in the RHQ database",
                true, PropertySimpleType.STRING);
        props.setDefaultValue(DEFAULT_IMPORTED_PROPERTIES_LIST);
        def.put(props);

        ConfigurationUtility.initializeDefaultTemplate(def);

        return def;
    }

    @Override
    public void configure(Configuration importConfiguration) {
        this.importConfiguration = importConfiguration;
        if (importConfiguration == null) {
            this.importConfiguration = getImportConfigurationDefinition().getDefaultTemplate().getConfiguration();
        }
    }

    @Override
    public ExportedEntityMatcher<NoSingleEntity, SystemSettings> getExportedEntityMatcher() {
        return new NoSingleEntityMatcher<SystemSettings>();
    }

    @Override
    public void update(NoSingleEntity entity, SystemSettings exportedEntity) throws Exception {
        Properties props = exportedEntity.toProperties();

        Set<String> propsToImport = getSettingNamesConfiguredForImport(importConfiguration);

        Set<String> propsToRemove = new HashSet<String>();

        for (Object k : props.keySet()) {
            String key = (String) k;
            if (!propsToImport.contains(key)) {
                propsToRemove.add(key);
            }
        }

        for (String p : propsToRemove) {
            props.remove(p);
        }

        systemManager.setSystemConfiguration(subject, props, true);
    }

    @Override
    public SystemSettings unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
        try {
            return (SystemSettings) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to unmarshal system settings.", e);
        }
    }

    @Override
    public void finishImport() {
    }

    private Set<String> getSettingNamesConfiguredForImport(Configuration importConfiguration) {
        String settingsToImport = importConfiguration.getSimpleValue(PROPERTIES_TO_IMPORT_PROPERTY, null);

        if (settingsToImport == null) {
            return Collections.emptySet();
        } else {
            String[] vals = settingsToImport.split("\\s*,\\s*");

            return new HashSet<String>(Arrays.asList(vals));
        }
    }
}
