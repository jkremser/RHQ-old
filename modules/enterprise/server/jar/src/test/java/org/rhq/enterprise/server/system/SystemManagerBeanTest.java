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
package org.rhq.enterprise.server.system;

import java.util.Properties;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.drift.DriftServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class SystemManagerBeanTest extends AbstractEJB3Test {
    private Subject overlord;
    private SystemManagerLocal systemManager;
    private DriftServerPluginService driftServerPluginService;

    @BeforeClass
    public void setupServer() {
        systemManager = LookupUtil.getSystemManager();
        
        //we need this because the drift plugins are referenced from the system settings that we use in our tests
        driftServerPluginService = new DriftServerPluginService();
        prepareCustomServerPluginService(driftServerPluginService);
        driftServerPluginService.startMasterPluginContainer();        
    }

    @AfterClass
    public void tearDownServer() throws Exception {
        unprepareServerPluginService();
        driftServerPluginService.stopMasterPluginContainer();
    }
    
    @BeforeMethod
    public void beforeMethod() {
        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    public void testGetSystemConfiguration() {
        assert null != systemManager.getSystemConfiguration(overlord);
    }

    public void testAnalyze() {
        systemManager.analyze(overlord);
    }

    public void testEnableHibernateStatistics() {
        systemManager.enableHibernateStatistics();
    }

    public void testGetDatabaseType() {
        assert systemManager.getDatabaseType() instanceof DatabaseType;
    }

    public void testReindex() {
        systemManager.reindex(overlord);
    }

    public void testVacuum() {
        systemManager.vacuum(overlord);
    }

    public void testVacuumAppdef() {
        systemManager.vacuumAppdef(overlord);
    }
        
    @SuppressWarnings("deprecation")
    public void testLegacySystemSettingsInCorrectFormat() throws Exception {
        //some of the properties are represented differently
        //in the new style settings and the the old style
        //settings (and consequently database).
        //These two still co-exist together in the codebase
        //so let's make sure the values correspond to each other.
        
        SystemSettings settings = systemManager.getSystemSettings(overlord);
        Properties config = systemManager.getSystemConfiguration(overlord);
        
        SystemSettings origSettings = new SystemSettings(settings);
        
        try {
            //let's make sure the values are the same
            checkFormats(settings, config);
            
            boolean currentJaasProvider = Boolean.valueOf(settings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER));
            settings.put(SystemSetting.LDAP_BASED_JAAS_PROVIDER, Boolean.toString(!currentJaasProvider));
            
            boolean currentUseSslForLdap = Boolean.valueOf(settings.get(SystemSetting.USE_SSL_FOR_LDAP));
            settings.put(SystemSetting.USE_SSL_FOR_LDAP, Boolean.toString(!currentUseSslForLdap));
            
            systemManager.setSystemSettings(overlord, settings);
            
            settings = systemManager.getSystemSettings(overlord);
            config = systemManager.getSystemConfiguration(overlord);
            
            checkFormats(settings, config);
        } finally {
            systemManager.setSystemSettings(overlord, origSettings);
        }
    }
    
    private void checkFormats(SystemSettings settings, Properties config) {        
        assert settings.size() == config.size() : "The old and new style system settings differ in size";
        
        for(String name : config.stringPropertyNames()) {
            SystemSetting setting = SystemSetting.getByInternalName(name);
            
            String oldStyleValue = config.getProperty(name);            
            String newStyleValue = settings.get(setting);
            
            assert setting != null : "Could not find a system setting called '" + name + "'.";
            
            switch(setting) {
            case USE_SSL_FOR_LDAP:
                if (RHQConstants.LDAP_PROTOCOL_SECURED.equals(oldStyleValue)) {
                    assert Boolean.valueOf(newStyleValue) : "Secured LDAP protocol should be represented by a 'true' in new style settings.";
                } else if (RHQConstants.LDAP_PROTOCOL_UNSECURED.equals(oldStyleValue)) {
                    assert !Boolean.valueOf(newStyleValue) : "Unsecured LDAP protocol should be represented by a 'false' in the new style settings.";
                } else {
                    assert false : "Unknown value for system setting '" + setting + "': [" + oldStyleValue + "].";
                }
                break;
            case LDAP_BASED_JAAS_PROVIDER:
                if (RHQConstants.LDAPJAASProvider.equals(oldStyleValue)) {
                    assert Boolean.valueOf(newStyleValue) : "LDAP JAAS provider should be represented by a 'true' in new style settings.";
                } else if (RHQConstants.JDBCJAASProvider.equals(oldStyleValue)) {
                    assert !Boolean.valueOf(newStyleValue) : "JDBC JAAS provider should be represented by a 'false' in the new style settings.";
                } else {
                    assert false : "Unknown value for system setting '" + setting + "': [" + oldStyleValue + "].";
                }
                break;
            default:
                assert oldStyleValue != null && newStyleValue != null && oldStyleValue.equals(newStyleValue) : "Old and new style values unexpectedly differ for system setting '"
                    + setting + "': old=[" + oldStyleValue + "], new=[" + newStyleValue + "].";
            }
        }
    }
}