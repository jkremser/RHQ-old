/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.alert;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.CustomUi;

/**
 * Plugin manager that takes care of loading the plug-ins and instantiating
 * of {@link AlertSender} etc.
 * @author Heiko W. Rupp
 */
public class AlertSenderPluginManager extends ServerPluginManager {

    private final Log log = getLog();
    private Map<String, String> pluginClassByName = new HashMap<String, String>();
    private Map<String, ServerPluginEnvironment> pluginEnvByName = new HashMap<String, ServerPluginEnvironment>();
    private Map<String, AlertSenderInfo> senderInfoByName = new HashMap<String, AlertSenderInfo>();
    private Map<String, String> backingBeanByName = new HashMap<String, String>();

    public AlertSenderPluginManager(AbstractTypeServerPluginContainer pc) {
        super(pc);
    }

    /**
     * Postprocess the loading of the plugin - the actual load is done
     * in the super class.
     * Here we verify that the passed &lt;plugin-class&gt; is valid and build the
     * list of plugins that can be queried by the UI etc.
     * 
     * @param env the environment of the plugin to be loaded
     * @param enabled if <code>true</code>, the plugin is to be enabled and will be started soon
     * 
     * @throws Exception if the alert plugin could not be loaded due to errors such as the alert class being invalid
     */
    @Override
    public void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {

        super.loadPlugin(env, enabled);

        if (enabled) {

            AlertPluginDescriptorType type = (AlertPluginDescriptorType) env.getPluginDescriptor();

            // make sure the alert sender class name is valid
            String className = type.getPluginClass();
            try {
                loadPluginClass(env, className, false);
            } catch (Exception e) {
                log.error("Alert sender class [" + className + "] defined in plugin ["
                    + env.getPluginKey().getPluginName() + "] is invalid and will be ignored. Cause: "
                    + ThrowableUtil.getAllMessages(e));
                try {
                    unloadPlugin(env.getPluginKey().getPluginName());
                } catch (Throwable t) {
                    log.warn("  +--> unload failed too. Cause: " + ThrowableUtil.getAllMessages(t));
                }
                throw e;
            }

            //
            // Ok, we have a valid plugin class, so we can look for other things and store the info
            //

            // The short name is basically the key into the plugin
            String shortName = type.getShortName();
            pluginClassByName.put(shortName, className);

            // UI snippet path allows the plugin to inject user interface fragments to the alert pages
            String uiSnippetPath;
            URL uiSnippetUrl = null;
            CustomUi customUI = type.getCustomUi();
            if (customUI != null) {
                uiSnippetPath = customUI.getUiSnippetName();

                try {
                    uiSnippetUrl = env.getPluginClassLoader().getResource(uiSnippetPath);
                    if (uiSnippetUrl == null) {
                        throw new Exception("plugin is missing alert ui snippet named [" + uiSnippetPath + "]");
                    }
                    log.debug("Alert plugin UI snippet for [" + shortName + "] is at: " + uiSnippetUrl);
                } catch (Exception e) {
                    log.error("Invalid alert UI snippet provided inside <custom-ui> for alert plugin [" + shortName
                        + "]. Plugin will be ignored. Cause: " + ThrowableUtil.getAllMessages(e));
                    throw e;
                }

                // Get the backing bean class
                className = customUI.getBackingBeanName();
                try {
                    loadPluginClass(env, className, true); // TODO how make this available to Seam and the Web-CL ?
                    backingBeanByName.put(shortName, className);
                } catch (Throwable t) {
                    String errMsg = "Backing bean [" + className + "] not found for plugin [" + shortName + ']';
                    log.error(errMsg);
                    throw new Exception(errMsg, t);
                }
            }

            AlertSenderInfo info = new AlertSenderInfo(shortName, type.getDescription(), env.getPluginKey());
            info.setUiSnippetUrl(uiSnippetUrl);
            senderInfoByName.put(shortName, info);

            pluginEnvByName.put(shortName, env);
        }
    }

    @Override
    protected void unloadPlugin(String pluginName) throws Exception {

        super.unloadPlugin(pluginName);

        String shortName = null;
        for (AlertSenderInfo info : senderInfoByName.values()) {
            if (info.getPluginName().equals(pluginName)) {
                shortName = info.getShortName();
                break;
            }
        }

        if (shortName != null) {
            pluginClassByName.remove(shortName);
            senderInfoByName.remove(shortName);
            pluginEnvByName.remove(shortName);
            backingBeanByName.remove(shortName);
        }
    }

    /**
     * Instantiate an AlertSender for the passed shortName, which is the name you have provided
     * in the plugin descriptor in the &lt;shortName&gt; element
     * @param notification The alert notification we need the sender for. Wanted sender is in notification.getSenderName()
     * @return a new AlertSender with preferences set
     * @see AlertSender
     */
    public AlertSender getAlertSenderForNotification(AlertNotification notification) {

        String senderName = notification.getSenderName();
        String className = pluginClassByName.get(senderName);
        if (className == null) {
            log.error("getAlertSenderForNotification: No pluginClass found for sender: " + senderName);
            return null;
        }
        ServerPluginEnvironment env = pluginEnvByName.get(senderName);
        AlertSender sender;
        try {
            sender = (AlertSender) instantiatePluginClass(env, className);
        } catch (Exception e) {
            log.error(e); // TODO
            return null;
        }

        // We have no entityManager lying around here, which means
        // Configuration is an uninitialized Proxy and we'd get a LazyInit
        // Exception later.
        // So lets get it from the SessionBeans
        ServerPluginContext ctx = getServerPluginContext(env);
        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();

        sender.alertParameters = mgr.getAlertPropertiesConfiguration(notification);
        if (sender.alertParameters == null) {
            sender.alertParameters = new Configuration(); // Safety measure
        }

        ServerPluginsLocal pluginsMgr = LookupUtil.getServerPlugins();

        PluginKey key = ctx.getPluginEnvironment().getPluginKey();
        ServerPlugin plugin = pluginsMgr.getServerPlugin(key);
        plugin = pluginsMgr.getServerPluginRelationships(plugin);

        sender.preferences = plugin.getPluginConfiguration();
        if (sender.preferences == null) {
            sender.preferences = new Configuration(); // Safety measure
        }

        sender.pluginComponent = getServerPluginComponent(key.getPluginName());

        return sender;
    }

    /**
     * Return the list of deployed alert sender plug-ins by their &lt;shortName&gt;
     * @return List of plugin names
     */
    public List<String> getPluginList() {
        return new ArrayList<String>(pluginClassByName.keySet());
    }

    public AlertSenderInfo getAlertSenderInfo(String shortName) {
        return senderInfoByName.get(shortName);
    }

    public AlertBackingBean getBackingBeanForSender(String shortName) {
        String className = backingBeanByName.get(shortName);
        ServerPluginEnvironment env = pluginEnvByName.get(shortName);
        AlertBackingBean bean = null;
        try {
            bean = (AlertBackingBean) instantiatePluginClass(env, className);
        } catch (Exception e) {
            log.error("Can't instantiate alert sender backing bean [" + className + "]. Cause: " + e.getMessage());
        }
        return bean;
    }
}
