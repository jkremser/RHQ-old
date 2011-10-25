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
package org.rhq.enterprise.server.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.jaas.JDBCLoginModule;
import org.rhq.enterprise.server.core.jaas.JDBCPrincipalCheckLoginModule;
import org.rhq.enterprise.server.core.jaas.LdapLoginModule;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/**
 * Deploy the JAAS login modules that are configured. The JDBC login module is always deployed, however, the LDAP login
 * module is only deployed if LDAP is enabled in the RHQ configuration.
 */
public class CustomJaasDeploymentService implements CustomJaasDeploymentServiceMBean, MBeanRegistration {
    private static final String AUTH_METHOD = "addAppConfig";
    private static final String AUTH_OBJECTNAME = "jboss.security:service=XMLLoginConfig";

    private Log log = LogFactory.getLog(CustomJaasDeploymentService.class.getName());
    private MBeanServer mbeanServer = null;

    /**
     * Constructor for {@link CustomJaasDeploymentService}.
     */
    public CustomJaasDeploymentService() {
    }

    /**
     * @see org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean#installJaasModules()
     */
    public void installJaasModules() {
        try {
            log.info("Installing RHQ Server's JAAS login modules");
            Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration(
                LookupUtil.getSubjectManager().getOverlord());
            registerJaasModules(systemConfig);
        } catch (Exception e) {
            log.fatal("Error deploying JAAS login modules", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer,javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.mbeanServer = server;
        return name;
    }

    /**
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean registrationDone) {
    }

    /**
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() {
    }

    /**
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
    }

    private void registerJaasModules(Properties systemConfig) throws Exception {
        List<AppConfigurationEntry> configEntries = new ArrayList<AppConfigurationEntry>();
        AppConfigurationEntry ace;
        Map<String, String> configOptions;

        try {
            configOptions = getJdbcOptions(systemConfig);
            ace = new AppConfigurationEntry(JDBCLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, configOptions);

            // We always add the JDBC provider to the auth config
            this.log.info("Enabling RHQ JDBC JAAS Provider...");
            configEntries.add(ace);

            String value = systemConfig.getProperty(SystemSetting.LDAP_BASED_JAAS_PROVIDER.getInternalName());
            boolean isLdapAuthenticationEnabled = (value != null) ? RHQConstants.LDAPJAASProvider.equals(value) : false;

            if (isLdapAuthenticationEnabled) {
                // this is a "gatekeeper" that only allows us to go to LDAP if there is no principal in the DB
                configOptions = getJdbcOptions(systemConfig);
                ace = new AppConfigurationEntry(JDBCPrincipalCheckLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, configOptions);
                this.log.info("Enabling RHQ JDBC-2 Principal Check JAAS Provider...");
                configEntries.add(ace);

                // this is the LDAP module that checks the LDAP for auth
                configOptions = getLdapOptions(systemConfig);
                try {
                    validateLdapOptions(configOptions);
                    ace = new AppConfigurationEntry(LdapLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, configOptions);
                    this.log.info("Enabling RHQ JDBC-2 LDAP JAAS Provider...");
                    configEntries.add(ace);
                } catch (NamingException e) {
                    this.log.info("Disabling RHQ JDBC-2 LDAP JAAS Provider: " + e, e);
                }
            }

            AppConfigurationEntry[] config = configEntries.toArray(new AppConfigurationEntry[0]);

            ObjectName objName = new ObjectName(AUTH_OBJECTNAME);
            Object obj = mbeanServer.invoke(objName, AUTH_METHOD, new Object[] { SECURITY_DOMAIN_NAME, config },
                new String[] { "java.lang.String", config.getClass().getName() });
        } catch (Exception e) {
            throw new Exception("Error registering RHQ JAAS modules", e);
        }
    }

    private Map<String, String> getJdbcOptions(Properties conf) {
        Map<String, String> configOptions = new HashMap<String, String>();

        // We always store passwords encoded.  Don't allow the end user to change this behavior.
        configOptions.put("hashAlgorithm", "MD5");
        configOptions.put("hashEncoding", "base64");

        return configOptions;
    }

    private Map<String, String> getLdapOptions(Properties conf) {
        Map<String, String> configOptions = new HashMap<String, String>();

        configOptions.put(Context.INITIAL_CONTEXT_FACTORY, conf.getProperty(RHQConstants.LDAPFactory));
        configOptions.put(Context.PROVIDER_URL, conf.getProperty(RHQConstants.LDAPUrl));
        String value = conf.getProperty(SystemSetting.USE_SSL_FOR_LDAP.getInternalName());
        boolean ldapSsl = Boolean.TRUE.toString().equals(value);
        configOptions.put(Context.SECURITY_PROTOCOL, (ldapSsl) ? "ssl" : null);
        configOptions.put("LoginProperty", conf.getProperty(RHQConstants.LDAPLoginProperty));
        configOptions.put("Filter", conf.getProperty(RHQConstants.LDAPFilter));
        configOptions.put("GroupFilter", conf.getProperty(RHQConstants.LDAPGroupFilter));
        configOptions.put("GroupMemberFilter", conf.getProperty(RHQConstants.LDAPGroupMember));
        configOptions.put("BaseDN", conf.getProperty(RHQConstants.LDAPBaseDN));
        configOptions.put("BindDN", conf.getProperty(RHQConstants.LDAPBindDN));
        configOptions.put("BindPW", conf.getProperty(RHQConstants.LDAPBindPW));

        return configOptions;
    }

    private void validateLdapOptions(Map<String, String> options) throws NamingException {
        Properties env = new Properties();

        String factory = options.get(Context.INITIAL_CONTEXT_FACTORY);
        if (factory == null) {
            throw new NamingException("No initial context factory");
        }

        String url = options.get(Context.PROVIDER_URL);
        if (url == null) {
            throw new NamingException("Naming provider url not set");
        }

        String protocol = options.get(Context.SECURITY_PROTOCOL);
        if ("ssl".equals(protocol)) {
            String ldapSocketFactory = env.getProperty("java.naming.ldap.factory.socket");
            if (ldapSocketFactory == null) {
                env.put("java.naming.ldap.factory.socket", UntrustedSSLSocketFactory.class.getName());
            }
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, factory);
        env.setProperty(Context.PROVIDER_URL, url);

        // Load any information we may need to bind
        String bindDN = options.get("BindDN");
        String bindPW = options.get("BindPW");
        if ((bindDN != null) && (bindDN.length() != 0) && (bindPW != null) && (bindPW.length() != 0)) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        log.debug("Validating LDAP with environment=" + env);
        new InitialLdapContext(env, null).close();

        return;
    }
}