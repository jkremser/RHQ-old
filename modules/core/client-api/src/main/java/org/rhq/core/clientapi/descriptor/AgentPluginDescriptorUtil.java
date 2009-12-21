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
package org.rhq.core.clientapi.descriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph.PluginDependency;
import org.rhq.core.clientapi.descriptor.plugin.ParentResourceType;
import org.rhq.core.clientapi.descriptor.plugin.PlatformDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.RunsInsideType;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor.Relationships;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor.Relationships.Relationship;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor.Relationships.Relationship.Source;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Utilities for agent plugin descriptors.
 *
 * @author John Mazzitelli
 */
public abstract class AgentPluginDescriptorUtil {
    private static final Log LOG = LogFactory.getLog(AgentPluginDescriptorUtil.class);

    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";
    private static final String PLUGIN_SCHEMA_PATH = "rhq-plugin.xsd";

    /**
     * Determines which of the two plugins is obsolete - in other words, this determines which
     * plugin is older. Each plugin must have the same logical name, but
     * one of which will be determined to be obsolete and should not be deployed.
     * If they have the same MD5, they are identical, so <code>null</code> will be returned.
     * Otherwise, the versions are compared and the one with the oldest version is obsolete.
     * If they have the same versions, the one with the oldest timestamp is obsolete.
     * If they have the same timestamp too, we have no other way to determine obsolescence so plugin1
     * will be picked arbitrarily and a message will be logged when this occurs.
     *
     * @param plugin1
     * @param plugin2
     * @return a reference to the obsolete plugin (plugin1 or plugin2 reference will be returned)
     *         <code>null</code> is returned if they are the same (i.e. they have the same MD5)
     * @throws IllegalArgumentException if the two plugins have different logical names
     */
    public static Plugin determineObsoletePlugin(Plugin plugin1, Plugin plugin2) {
        if (!plugin1.getName().equals(plugin2.getName())) {
            throw new IllegalArgumentException("The two plugins don't have the same name:" + plugin1 + ":" + plugin2);
        }

        if (plugin1.getMd5().equals(plugin2.getMd5())) {
            return null;
        } else {
            String version1Str = plugin1.getVersion();
            String version2Str = plugin2.getVersion();
            ComparableVersion plugin1Version = new ComparableVersion((version1Str != null) ? version1Str : "0");
            ComparableVersion plugin2Version = new ComparableVersion((version2Str != null) ? version2Str : "0");
            if (plugin1Version.equals(plugin2Version)) {
                if (plugin1.getMtime() == plugin2.getMtime()) {
                    LOG.info("Plugins [" + plugin1 + ", " + plugin2
                        + "] are the same logical plugin but have different content. The plugin [" + plugin1
                        + "] will be considered obsolete.");
                    return plugin1;
                } else if (plugin1.getMtime() < plugin2.getMtime()) {
                    return plugin1;
                } else {
                    return plugin2;
                }
            } else if (plugin1Version.compareTo(plugin2Version) < 0) {
                return plugin1;
            } else {
                return plugin2;
            }
        }
    }

    /**
     * Returns the version for the plugin represented by the given descriptor/file.
     * If the descriptor defines a version, that is considered the version of the plugin.
     * However, if the plugin descriptor does not define a version, the plugin jar's manifest
     * is searched for an implementation version string and if one is found that is the version
     * of the plugin. If the manifest entry is also not found, the plugin does not have a version
     * associated with it, which causes this method to throw an exception.
     * 
     * @param pluginFile the plugin jar
     * @param descriptor the plugin descriptor as found in the plugin jar (if <code>null</code>,
     *                   the plugin file will be read and the descriptor parsed from it)
     * @return the version of the plugin
     * @throws Exception if the plugin is invalid, there is no version for the plugin or the version string is invalid
     */
    public static ComparableVersion getPluginVersion(File pluginFile, PluginDescriptor descriptor) throws Exception {

        if (descriptor == null) {
            descriptor = loadPluginDescriptorFromUrl(pluginFile.toURI().toURL());
        }

        String version = descriptor.getVersion();
        if (version == null) {
            Manifest manifest = getManifest(pluginFile);
            if (manifest != null) {
                version = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
        }

        if (version == null) {
            throw new Exception("No version is defined for plugin jar [" + pluginFile
                + "]. A version must be defined either via the MANIFEST.MF [" + Attributes.Name.IMPLEMENTATION_VERSION
                + "] attribute or via the plugin descriptor 'version' attribute.");
        }

        try {
            return new ComparableVersion(version);
        } catch (RuntimeException e) {
            throw new Exception("Version [" + version + "] for [" + pluginFile + "] did not parse", e);
        }
    }

    /**
     * Obtains the manifest of the plugin file represented by the given deployment info.
     * Use this method rather than calling deploymentInfo.getManifest()
     * (workaround for https://jira.jboss.org/jira/browse/JBAS-6266).
     * 
     * @param pluginFile the plugin file
     * @return the deployed plugin's manifest
     */
    private static Manifest getManifest(File pluginFile) {
        try {
            JarFile jarFile = new JarFile(pluginFile);
            try {
                Manifest manifest = jarFile.getManifest();
                return manifest;
            } finally {
                jarFile.close();
            }
        } catch (Exception ignored) {
            return null; // this is OK, it just means we do not have a manifest
        }
    }

    /**
     * Given an existing dependency graph and a plugin descriptor, this will add that plugin and its dependencies
     * to the dependency graph.
     * 
     * @param dependencyGraph
     * @param descriptor
     */
    public static void addPluginToDependencyGraph(PluginDependencyGraph dependencyGraph, PluginDescriptor descriptor) {
        String pluginName = descriptor.getName();
        List<PluginDependencyGraph.PluginDependency> dependencies = new ArrayList<PluginDependencyGraph.PluginDependency>();
        for (PluginDescriptor.Depends dependency : descriptor.getDepends()) {
            String dependencyName = dependency.getPlugin();
            boolean useClasses = dependency.isUseClasses(); // TODO this may not be used anymore
            boolean required = true; // all <depends> plugins are implicitly required
            dependencies.add(new PluginDependencyGraph.PluginDependency(dependencyName, useClasses, required));
        }

        List<PlatformDescriptor> platforms = descriptor.getPlatforms();
        List<ServerDescriptor> servers = descriptor.getServers();
        List<ServiceDescriptor> services = descriptor.getServices();

        for (PlatformDescriptor platform : platforms) {
            addOptionalDependency(platform, dependencies);
        }
        for (ServerDescriptor server : servers) {
            addOptionalDependency(server, dependencies);
        }
        for (ServiceDescriptor service : services) {
            addOptionalDependency(service, dependencies);
        }

        Relationships relationshipDescriptor = descriptor.getRelationshipDescriptor();
        if (relationshipDescriptor != null) {
            List<Relationship> relationships = relationshipDescriptor.getRelationships();
            if (relationships != null && !relationships.isEmpty()) {
                for (Relationship relationship : relationships) {
                    Source source = relationship.getSource();
                    if (source != null && source.getPlugin() != null) {
                        addOptionalDependency(source.getPlugin(), dependencies);
                    }
                }
            }
        }

        dependencyGraph.addPlugin(pluginName, dependencies);
        return;
    }

    private static void addOptionalDependency(PlatformDescriptor platform,
        List<PluginDependencyGraph.PluginDependency> dependencies) {
        for (ServerDescriptor childServer : platform.getServers()) {
            addOptionalDependency(childServer, dependencies);
        }
        for (ServiceDescriptor childService : platform.getServices()) {
            addOptionalDependency(childService, dependencies);
        }

        addOptionalDependency(platform.getRunsInside(), dependencies);
        return;
    }

    private static void addOptionalDependency(ServerDescriptor server,
        List<PluginDependencyGraph.PluginDependency> dependencies) {
        for (ServerDescriptor childServer : server.getServers()) {
            addOptionalDependency(childServer, dependencies);
        }
        for (ServiceDescriptor childService : server.getServices()) {
            addOptionalDependency(childService, dependencies);
        }

        addOptionalDependency(server.getRunsInside(), dependencies);
        addOptionalDependency(server.getSourcePlugin(), dependencies);
        return;
    }

    private static void addOptionalDependency(ServiceDescriptor service,
        List<PluginDependencyGraph.PluginDependency> dependencies) {
        for (ServiceDescriptor childService : service.getServices()) {
            addOptionalDependency(childService, dependencies);
        }

        addOptionalDependency(service.getRunsInside(), dependencies);
        addOptionalDependency(service.getSourcePlugin(), dependencies);
        return;
    }

    private static void addOptionalDependency(RunsInsideType runsInside,
        List<PluginDependencyGraph.PluginDependency> dependencies) {

        if (runsInside != null) {
            List<ParentResourceType> parents = runsInside.getParentResourceType();
            for (ParentResourceType parent : parents) {
                addOptionalDependency(parent.getPlugin(), dependencies);
            }
        }
        return;
    }

    private static void addOptionalDependency(String pluginName,
        List<PluginDependencyGraph.PluginDependency> dependencies) {

        if (pluginName != null) {
            boolean useClasses = false;
            boolean required = false;
            PluginDependency dep = new PluginDependencyGraph.PluginDependency(pluginName, useClasses, required);
            if (!dependencies.contains(dep)) {
                // only add it if it doesn't exist yet - this is so we don't override a required dep with an optional one
                dependencies.add(dep);
            }
        }
        return;
    }

    /**
     * Loads a plugin descriptor from the given plugin jar and returns it.
     * 
     * This is a static method to provide a convienence method for others to be able to use.
     *  
     * @param pluginJarFileUrl URL to a plugin jar file
     * @return the plugin descriptor found in the given plugin jar file
     * @throws PluginContainerException if failed to find or parse a descriptor file in the plugin jar
     */
    public static PluginDescriptor loadPluginDescriptorFromUrl(URL pluginJarFileUrl) throws PluginContainerException {

        final Log logger = LogFactory.getLog(AgentPluginDescriptorUtil.class);

        if (pluginJarFileUrl == null) {
            throw new PluginContainerException("A valid plugin JAR URL must be supplied.");
        }
        logger.debug("Loading plugin descriptor from plugin jar at [" + pluginJarFileUrl + "]...");

        testPluginJarIsReadable(pluginJarFileUrl);

        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        } catch (Exception e) {
            throw new PluginContainerException("Failed to create JAXB Context.", new WrappedRemotingException(e));
        }

        JarInputStream jis = null;
        JarEntry descriptorEntry = null;

        try {
            jis = new JarInputStream(pluginJarFileUrl.openStream());
            JarEntry nextEntry = jis.getNextJarEntry();
            while (nextEntry != null && descriptorEntry == null) {
                if (PLUGIN_DESCRIPTOR_PATH.equals(nextEntry.getName())) {
                    descriptorEntry = nextEntry;
                } else {
                    jis.closeEntry();
                    nextEntry = jis.getNextJarEntry();
                }
            }

            if (descriptorEntry == null) {
                throw new Exception("The plugin descriptor does not exist");
            }

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation
            URL pluginSchemaURL = AgentPluginDescriptorUtil.class.getClassLoader().getResource(PLUGIN_SCHEMA_PATH);
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                pluginSchemaURL);
            unmarshaller.setSchema(pluginSchema);

            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);

            PluginDescriptor pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(jis);

            for (ValidationEvent event : vec.getEvents()) {
                logger.debug("Plugin [" + pluginDescriptor.getName() + "] descriptor messages {Severity: "
                    + event.getSeverity() + ", Message: " + event.getMessage() + ", Exception: "
                    + event.getLinkedException() + "}");
            }

            return pluginDescriptor;
        } catch (Exception e) {
            throw new PluginContainerException("Could not successfully parse the plugin descriptor ["
                + PLUGIN_DESCRIPTOR_PATH + " found in plugin jar at [" + pluginJarFileUrl + "]",
                new WrappedRemotingException(e));
        } finally {
            if (jis != null) {
                try {
                    jis.close();
                } catch (Exception e) {
                    logger.warn("Cannot close jar stream [" + pluginJarFileUrl + "]. Cause: " + e);
                }
            }
        }
    }

    private static void testPluginJarIsReadable(URL pluginJarFileUrl) throws PluginContainerException {
        InputStream inputStream = null;
        try {
            inputStream = pluginJarFileUrl.openStream();
        } catch (IOException e) {
            throw new PluginContainerException("Unable to open plugin jar at [" + pluginJarFileUrl + "] for reading.");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
}
