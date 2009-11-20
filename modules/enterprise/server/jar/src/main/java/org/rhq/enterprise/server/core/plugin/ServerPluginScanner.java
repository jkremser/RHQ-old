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
package org.rhq.enterprise.server.core.plugin;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * This looks at both the file system and the database for new server plugins.
 *
 * @author John Mazzitelli
 */
public class ServerPluginScanner {
    private Log log = LogFactory.getLog(ServerPluginScanner.class);

    private DatabaseType dbType = null;

    /** a list of server plugins found on previous scans that have not yet been processed */
    private List<File> scanned = new ArrayList<File>();

    /** Maintains a cache of what we had on the filesystem during the last scan */
    private Map<File, PluginWithDescriptor> serverPluginsOnFilesystem = new HashMap<File, PluginWithDescriptor>();

    private File serverPluginDir;

    public ServerPluginScanner() {
    }

    public File getServerPluginDir() {
        return this.serverPluginDir;
    }

    public void setServerPluginDir(File dir) {
        this.serverPluginDir = dir;
    }

    /**
     * This should be called after a call to {@link #serverPluginScan()} to register
     * plugins that were found in the scan.
     * 
     * @throws Exception
     */
    void registerServerPlugins() throws Exception {
        for (File file : this.scanned) {
            log.debug("Deploying server plugin [" + file + "]...");
            registerServerPlugin(file);
        }
        this.scanned.clear();
        return;
    }

    /**
     * This method just scans the filesystem and DB for server plugin changes but makes
     * no attempt to register the plugins.
     * 
     * @throws Exception
     */
    void serverPluginScan() throws Exception {
        log.debug("Scanning for server plugins");

        if (this.getServerPluginDir() == null || !this.getServerPluginDir().isDirectory()) {
            // nothing to do since there is no plugin directory configured
            return;
        }

        // ensure that the filesystem and database are in a consistent state
        List<File> updatedFiles1 = serverPluginScanFilesystem();
        List<File> updatedFiles2 = serverPluginScanDatabase();

        // process any newly detected plugins
        List<File> allUpdatedFiles = new ArrayList<File>();
        allUpdatedFiles.addAll(updatedFiles1);
        allUpdatedFiles.addAll(updatedFiles2);

        for (File updatedFile : allUpdatedFiles) {
            log.debug("Scan detected server plugin [" + updatedFile + "]...");
            this.scanned.add(updatedFile);
        }
        return;
    }

    /**
     * This is called when a server plugin jar has been found on the filesystem that hasn't been seen yet
     * during this particular lifetime of the scanner. This does not necessarily mean its a new plugin jar,
     * it only means this is the first time we've seen it since this object has been instantiated.
     * This method will check to see if the database record matches the new plugin file and if so, does nothing.
     * 
     * @param file the new server plugin file
     */
    private void registerServerPlugin(File pluginFile) {
        try {
            ServerPluginDescriptorType descriptor;
            descriptor = this.serverPluginsOnFilesystem.get(pluginFile).descriptor;

            String pluginName = descriptor.getName();
            String displayName = descriptor.getDisplayName();

            ComparableVersion version; // this must be non-null, the next line ensures this
            version = ServerPluginDescriptorUtil.getPluginVersion(pluginFile, descriptor);

            log.info("Registering RHQ server plugin [" + pluginName + "], version " + version);

            Plugin plugin = new Plugin(pluginName, pluginFile.getName());
            plugin.setDeployment(PluginDeploymentType.SERVER);
            plugin.setDisplayName((displayName != null) ? displayName : pluginName);
            plugin.setEnabled(!descriptor.isDisabledOnDiscovery());
            plugin.setDescription(descriptor.getDescription());
            plugin.setMtime(pluginFile.lastModified());
            plugin.setVersion(version.toString());
            plugin.setAmpsVersion(descriptor.getApiVersion());
            plugin.setMD5(MessageDigestGenerator.getDigestString(pluginFile));
            plugin.setPluginConfiguration(getDefaultPluginConfiguration(descriptor));
            plugin.setScheduledJobsConfiguration(getDefaultScheduledJobsConfiguration(descriptor));

            if (descriptor.getHelp() != null && !descriptor.getHelp().getContent().isEmpty()) {
                plugin.setHelp(String.valueOf(descriptor.getHelp().getContent().get(0)));
            }

            ServerPluginsLocal serverPluginsManager = LookupUtil.getServerPlugins();
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            serverPluginsManager.registerPlugin(subjectManager.getOverlord(), plugin, descriptor, pluginFile);
        } catch (Exception e) {
            log.error("Failed to register RHQ plugin file [" + pluginFile + "]", e);
        }
        return;
    }

    private Configuration getDefaultPluginConfiguration(ServerPluginDescriptorType descriptor) throws Exception {
        Configuration defaults = null;
        ConfigurationDefinition def = ServerPluginDescriptorMetadataParser.getPluginConfigurationDefinition(descriptor);
        if (def != null) {
            defaults = def.getDefaultTemplate().createConfiguration();
        }
        return defaults;
    }

    private Configuration getDefaultScheduledJobsConfiguration(ServerPluginDescriptorType descriptor) throws Exception {
        Configuration defaults = null;
        ConfigurationDefinition def = ServerPluginDescriptorMetadataParser.getScheduledJobsDefinition(descriptor);
        if (def != null) {
            defaults = def.getDefaultTemplate().createConfiguration();
        }
        return defaults;
    }

    /**
     * Scans the plugin directory and updates our cache of known plugin files.
     * This will purge any old plugins that are deemed obsolete.
     * 
     * @return a list of files that appear to be new or updated and should be deployed
     */
    private List<File> serverPluginScanFilesystem() {
        List<File> updated = new ArrayList<File>();

        // get the current list of plugins deployed on the filesystem
        File[] pluginJars = this.getServerPluginDir().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        // refresh our cache so it reflects what is currently on the filesystem
        // first we remove any jar files in our cache that we no longer have on the filesystem
        ArrayList<File> doomedPluginFiles = new ArrayList<File>();
        for (File cachedPluginFile : this.serverPluginsOnFilesystem.keySet()) {
            boolean existsOnFileSystem = false;
            for (File filesystemPluginFile : pluginJars) {
                if (cachedPluginFile.equals(filesystemPluginFile)) {
                    existsOnFileSystem = true;
                    continue; // our cached jar still exists on the file system
                }
            }
            if (!existsOnFileSystem) {
                doomedPluginFiles.add(cachedPluginFile); // this plugin file has been deleted from the filesystem, remove it from the cache
            }
        }
        for (File deletedPluginFile : doomedPluginFiles) {
            this.serverPluginsOnFilesystem.remove(deletedPluginFile);
        }

        // now insert new cache items representing new jar files and update existing ones as appropriate
        for (File pluginJar : pluginJars) {
            String md5 = null;

            PluginWithDescriptor pluginWithDescriptor = this.serverPluginsOnFilesystem.get(pluginJar);
            Plugin plugin = null;
            if (pluginWithDescriptor != null) {
                plugin = pluginWithDescriptor.plugin;
            }

            try {
                if (plugin != null) {
                    if (pluginJar.lastModified() == 0L) {
                        // for some reason the operating system can't give us the last mod time, we need to do MD5 check
                        md5 = MessageDigestGenerator.getDigestString(pluginJar);
                        if (!md5.equals(plugin.getMd5())) {
                            plugin = null; // this plugin jar has changed - force it to refresh the cache.
                        }
                    } else if (pluginJar.lastModified() != plugin.getMtime()) {
                        plugin = null; // this plugin jar has changed - force it to refresh the cache.
                    }
                }

                if (plugin == null) {
                    cacheFilesystemServerPluginJar(pluginJar, md5);
                    updated.add(pluginJar);
                }
            } catch (Exception e) {
                log.warn("Failed to scan server plugin [" + pluginJar + "] found on filesystem. Skipping. Cause: " + e);
                this.serverPluginsOnFilesystem.remove(pluginJar); // act like we never saw it
                updated.remove(pluginJar);
            }
        }

        // Let's check to see if there are any obsolete plugins that need to be deleted.
        // This is needed if plugin-A-1.0.jar exists and someone deployed plugin-A-1.1.jar but fails to delete plugin-A-1.0.jar.
        doomedPluginFiles.clear();
        HashMap<String, Plugin> pluginsByName = new HashMap<String, Plugin>();
        for (Entry<File, PluginWithDescriptor> currentPluginFileEntry : this.serverPluginsOnFilesystem.entrySet()) {
            Plugin currentPlugin = currentPluginFileEntry.getValue().plugin;
            Plugin existingPlugin = pluginsByName.get(currentPlugin.getName());
            if (existingPlugin == null) {
                // this is the usual case - this is the only plugin with the given name we've seen
                pluginsByName.put(currentPlugin.getName(), currentPlugin);
            } else {
                Plugin obsolete = ServerPluginDescriptorUtil.determineObsoletePlugin(currentPlugin, existingPlugin);
                if (obsolete == null) {
                    obsolete = currentPlugin; // both were identical, but we only want one file so pick one to get rid of
                }
                doomedPluginFiles.add(new File(this.getServerPluginDir(), obsolete.getPath()));
                if (obsolete == existingPlugin) { // yes use == for reference equality!
                    pluginsByName.put(currentPlugin.getName(), currentPlugin); // override the original one we saw with this latest one
                }
            }
        }

        // now we need to actually delete any obsolete plugin files from the file system
        for (File doomedPluginFile : doomedPluginFiles) {
            if (doomedPluginFile.delete()) {
                log.info("Deleted an obsolete server plugin file: " + doomedPluginFile);
                this.serverPluginsOnFilesystem.remove(doomedPluginFile);
                updated.remove(doomedPluginFile);
            } else {
                log.warn("Failed to delete what was deemed an obsolete server plugin file: " + doomedPluginFile);
            }
        }

        return updated;
    }

    /**
     * Creates a {@link Plugin} object for the given plugin jar and caches it.
     * @param pluginJar information about this plugin jar will be cached
     * @param md5 if known, this is the plugin jar's MD5, <code>null</code> if not known
     * @return the plugin jar files's information that has been cached
     * @throws Exception if failed to get information about the plugin
     */
    private Plugin cacheFilesystemServerPluginJar(File pluginJar, String md5) throws Exception {
        if (md5 == null) { // don't calculate the MD5 is we've already done it before
            md5 = MessageDigestGenerator.getDigestString(pluginJar);
        }
        URL pluginUrl = pluginJar.toURI().toURL();
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
        String version = ServerPluginDescriptorUtil.getPluginVersion(pluginJar, descriptor).toString();
        String name = descriptor.getName();
        Plugin plugin = new Plugin(name, pluginJar.getName());
        plugin.setMd5(md5);
        plugin.setVersion(version);
        plugin.setMtime(pluginJar.lastModified());
        plugin.setDeployment(PluginDeploymentType.SERVER);
        this.serverPluginsOnFilesystem.put(pluginJar, new PluginWithDescriptor(plugin, descriptor));
        return plugin;
    }

    /**
     * This method scans the database for any new or updated server plugins and make sure this server
     * has a plugin file on the filesystem for each of those new/updated server plugins.
     *
     * @return a list of files that appear to be new or updated and should be deployed
     */
    private List<File> serverPluginScanDatabase() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        // these are plugins (name/path/md5/mtime) that have changed in the DB but are missing from the file system
        List<Plugin> updatedPlugins = new ArrayList<Plugin>();

        // the same list as above, only they are the files that are written to the filesystem and no longer missing
        List<File> updatedFiles = new ArrayList<File>();

        try {
            DataSource ds = LookupUtil.getDataSource();
            conn = ds.getConnection();

            // get all the plugins
            ps = conn.prepareStatement("SELECT NAME, PATH, MD5, MTIME, VERSION FROM " + Plugin.TABLE_NAME
                + " WHERE DEPLOYMENT = 'SERVER'");
            rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String path = rs.getString(2);
                String md5 = rs.getString(3);
                long mtime = rs.getLong(4);
                String version = rs.getString(5);

                // let's see if we have this logical plugin on the filesystem (it may or may not be under the same filename)
                File expectedFile = new File(this.getServerPluginDir(), path);
                File currentFile = null; // will be non-null if we find that we have this plugin on the filesystem already
                PluginWithDescriptor pluginWithDescriptor = this.serverPluginsOnFilesystem.get(expectedFile);

                if (pluginWithDescriptor != null) {
                    currentFile = expectedFile; // we have it where we are expected to have it
                    if (!pluginWithDescriptor.plugin.getName().equals(name)) {
                        // I have no idea when or if this would ever happen, but at least log it so we'll see it if it does happen
                        log.warn("For some reason, the server plugin file [" + expectedFile + "] is plugin ["
                            + pluginWithDescriptor.plugin.getName() + "] but the database says it should be [" + name
                            + "]");
                    } else {
                        log.debug("File system and database agree on a server plugin location for [" + expectedFile
                            + "]");
                    }
                } else {
                    // the plugin might still be on the file system but under a different filename, see if we can find it
                    for (Map.Entry<File, PluginWithDescriptor> cacheEntry : this.serverPluginsOnFilesystem.entrySet()) {
                        if (cacheEntry.getValue().plugin.getName().equals(name)) {
                            currentFile = cacheEntry.getKey();
                            pluginWithDescriptor = cacheEntry.getValue();
                            log.info("Filesystem has a server plugin [" + name + "] at the file [" + currentFile
                                + "] which is different than where the DB thinks it should be [" + expectedFile + "]");
                            break; // we found it, no need to continue the loop
                        }
                    }
                }

                if (pluginWithDescriptor != null && currentFile != null && currentFile.exists()) {
                    Plugin dbPlugin = new Plugin(name, path);
                    dbPlugin.setMd5(md5);
                    dbPlugin.setVersion(version);
                    dbPlugin.setMtime(mtime);
                    dbPlugin.setDeployment(PluginDeploymentType.SERVER);

                    Plugin obsoletePlugin = ServerPluginDescriptorUtil.determineObsoletePlugin(dbPlugin,
                        pluginWithDescriptor.plugin);

                    if (obsoletePlugin == pluginWithDescriptor.plugin) { // yes use == for reference equality!
                        StringBuilder logMsg = new StringBuilder();
                        logMsg.append("Found server plugin [").append(name);
                        logMsg.append("] in the DB that is newer than the one on the filesystem: ");
                        logMsg.append("DB path=[").append(path);
                        logMsg.append("]; file path=[").append(currentFile.getName());
                        logMsg.append("]; DB MD5=[").append(md5);
                        logMsg.append("]; file MD5=[").append(pluginWithDescriptor.plugin.getMd5());
                        logMsg.append("]; DB version=[").append(version);
                        logMsg.append("]; file version=[").append(pluginWithDescriptor.plugin.getVersion());
                        logMsg.append("]; DB timestamp=[").append(new Date(mtime));
                        logMsg.append("]; file timestamp=[").append(new Date(pluginWithDescriptor.plugin.getMtime()));
                        logMsg.append("]");
                        log.info(logMsg.toString());

                        updatedPlugins.add(dbPlugin);

                        if (currentFile.delete()) {
                            log.info("Deleted the obsolete server plugin file to be updated: " + currentFile);
                            this.serverPluginsOnFilesystem.remove(currentFile);
                        } else {
                            log
                                .warn("Failed to delete the obsolete (to-be-updated) server plugin file: "
                                    + currentFile);
                        }
                        currentFile = null;
                    } else if (obsoletePlugin == null) {
                        // the db is up-to-date, but update the cache so we don't check MD5 or parse the descriptor again
                        currentFile.setLastModified(mtime);
                        pluginWithDescriptor.plugin.setMtime(mtime);
                        pluginWithDescriptor.plugin.setVersion(version);
                        pluginWithDescriptor.plugin.setMd5(md5);
                    } else {
                        log.info("It appears that the server plugin [" + dbPlugin
                            + "] in the database may be obsolete. If so, it will be updated later.");
                    }
                } else {
                    log.info("Found server plugin in the DB that we do not yet have: " + name);
                    Plugin plugin = new Plugin(name, path, md5);
                    plugin.setMtime(mtime);
                    plugin.setVersion(version);
                    plugin.setDeployment(PluginDeploymentType.SERVER);
                    updatedPlugins.add(plugin);
                    this.serverPluginsOnFilesystem.remove(expectedFile); // paranoia, make sure the cache doesn't have this
                }
            }
            JDBCUtil.safeClose(ps, rs);

            // write all our updated plugins to the file system
            ps = conn.prepareStatement("SELECT CONTENT FROM " + Plugin.TABLE_NAME
                + " WHERE DEPLOYMENT = 'SERVER' AND NAME = ?");
            for (Plugin plugin : updatedPlugins) {
                File file = new File(this.getServerPluginDir(), plugin.getPath());

                ps.setString(1, plugin.getName());
                rs = ps.executeQuery();
                rs.next();
                InputStream content = rs.getBinaryStream(1);
                StreamUtil.copy(content, new FileOutputStream(file));
                rs.close();
                file.setLastModified(plugin.getMtime()); // so our file matches the database mtime
                updatedFiles.add(file);

                // we are writing a new file to the filesystem, cache it since we know about it now
                cacheFilesystemServerPluginJar(file, null);
            }
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);
        }

        return updatedFiles;
    }

    private void setEnabledFlag(Connection conn, PreparedStatement ps, int index, boolean enabled) throws Exception {
        if (null == this.dbType) {
            this.dbType = DatabaseTypeFactory.getDatabaseType(conn);
        }
        if (dbType instanceof PostgresqlDatabaseType || dbType instanceof H2DatabaseType) {
            ps.setBoolean(index, enabled);
        } else if (dbType instanceof OracleDatabaseType || dbType instanceof SQLServerDatabaseType) {
            ps.setInt(index, (enabled ? 1 : 0));
        } else {
            throw new RuntimeException("Unknown database type : " + dbType);
        }
    }

    /**
     * This will write the contents of the given plugin file to the database.
     * This will store both the contents and the MD5 in an atomic transaction
     * so they remain insync.
     *
     * When <code>different</code> is <code>false</code>, it means the original
     * plugin and the one currently found on the file system are the same.
     *
     * When <code>different</code> is <code>true</code>, it means the plugin
     * is most likely a different one than the one that originally existed.
     * When this happens, it is assumed that the we need
     * to see the plugin on the file system as new and needing to be processed, therefore
     * the MD5, CONTENT and MTIME columns will be updated to ensure the deployer
     * will process this plugin and thus update all the metadata for this plugin.
     *
     * @param name the name of the plugin whose content is being updated
     * @param file the plugin file whose content will be streamed to the database
     * @param different this will be <code>true</code> if the given file has a different filename
     *                  that the plugin's "path" as found in the database.
     *
     *
     * @throws Exception
     */
    private void streamPluginFileContentToDatabase(String name, File file, boolean different) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        TransactionManager tm = null;

        String sql = "UPDATE " + Plugin.TABLE_NAME
            + " SET CONTENT = ?, MD5 = ?, MTIME = ?, PATH = ? WHERE DEPLOYMENT = 'SERVER' AND NAME = ?";

        // if 'different' is true, give bogus data so the plugin deployer will think the plugin on the file system is new
        String md5 = (!different) ? MessageDigestGenerator.getDigestString(file) : "TO BE UPDATED";
        long mtime = (!different) ? file.lastModified() : 0L;
        InputStream fis = (!different) ? new FileInputStream(file) : new ByteArrayInputStream(new byte[0]);
        int contentSize = (int) ((!different) ? file.length() : 0);

        try {
            tm = LookupUtil.getTransactionManager();
            tm.begin();
            DataSource ds = LookupUtil.getDataSource();
            conn = ds.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setBinaryStream(1, new BufferedInputStream(fis), contentSize);
            ps.setString(2, md5);
            ps.setLong(3, mtime);
            ps.setString(4, file.getName());
            ps.setString(5, name);
            int updateResults = ps.executeUpdate();
            if (updateResults == 1) {
                log.info("Stored content for plugin [" + name + "] in the db. file=" + file);
            } else {
                throw new Exception("Failed to update content for plugin [" + name + "] from [" + file + "]");
            }
        } catch (Exception e) {
            tm.rollback();
            tm = null;
            throw e;
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);

            try {
                fis.close();
            } catch (Throwable t) {
            }

            if (tm != null) {
                tm.commit();
            }
        }
        return;
    }

    private class PluginWithDescriptor {
        public PluginWithDescriptor(Plugin plugin, ServerPluginDescriptorType descriptor) {
            this.plugin = plugin;
            this.descriptor = descriptor;
        }

        public Plugin plugin;
        public ServerPluginDescriptorType descriptor;
    }
}
