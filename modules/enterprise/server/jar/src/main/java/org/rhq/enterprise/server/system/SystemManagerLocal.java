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

import javax.ejb.Local;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.common.composite.SystemSettings;

/**
 * Provides access to the server cloud's system configuration as well as some methods
 * to perform configuration on the server in which this bean is running.
 */
@Local
public interface SystemManagerLocal {
    /**
     * Returns the {@link DatabaseType} that corresponds to the database the JON Server uses for its backend.
     *
     * <p>This method is mainly to allow the caller to determine the kind of database in use so as to determine what
     * syntax to use for a particular native query.</p>
     *
     * @return the type of database
     */
    DatabaseType getDatabaseType();

    /**
     * Schedules the internal timer job that periodically refreshes the configuration cache.
     * This is needed in case a user changed the system configuration on another server in the HA
     * cloud - this config cache reloader will load in that new configuration.
     */
    void scheduleConfigCacheReloader();

    /**
     * Creates and registers the Hibernate Statistics MBean. This allows us to monitor
     * our own Hibernate usage.
     */
    void enableHibernateStatistics();

    /**
     * Performs some reconfiguration things on the server where we are running.
     * This includes redeploying the configured JAAS modules.
     */
    void reconfigureSystem(Subject whoami);

    /**
     * Run analyze command on PostgreSQL databases. On non-PostgreSQL, this returns -1.
     *
     * @param whoami the user requesting the operation
     *
     * @return The time it took to analyze, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long analyze(Subject whoami);

    /**
     * Reindexes all tables that need to be periodically reindexed.
     * For Oracle, this "rebuilds" the indexes, for PostgreSQL, its a "reindex". 
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to reindex, in milliseconds
     */
    long reindex(Subject whoami);

    /**
     * Run database-specific cleanup routines.
     * On PostgreSQL we do a VACUUM ANALYZE on all tables. On other databases we just return -1.
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuum(Subject whoami);

    /**
     * Run database-specific cleanup routines for the given tables.
     * On PostgreSQL we do a VACUUM ANALYZE on the given tables. On other databases we just return -1.
     *
     * @param  whoami     the user requesting the operation
     * @param  tableNames names of specific tables that will be vacuumed.
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuum(Subject whoami, String[] tableNames);

    /**
     * Run database-specific cleanup routines on appdef tables.
     * On PostgreSQL we do a VACUUM ANALYZE against the relevant tables.  On other databases we just return -1.
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuumAppdef(Subject whoami);

    /**
     * Ensures the installer has been undeployed. Installer must be undeployed
     * to ensure the server deployment is secure.
     */
    void undeployInstaller();

    /**
     * Grabs the current system configuration from the database and reloads the cache with it.
     * This is meant for internal use only! You probably want to use {@link #getSystemConfiguration()}
     * instead.
     */
    void loadSystemConfigurationCacheInNewTx();

    /**
     * Grabs the current system configuration from the database and reloads the cache with it.
     * This is meant for internal use only! You probably want to use {@link #getSystemConfiguration()}
     * instead.
     */
    void loadSystemConfigurationCache();

    boolean isDebugModeEnabled();

    boolean isExperimentalFeaturesEnabled();

    boolean isLdapAuthorizationEnabled();
    
    void validateSystemConfiguration(Subject subject, Properties properties) throws InvalidSystemConfigurationException;
    
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    ProductInfo getProductInfo(Subject subject);

    ServerDetails getServerDetails(Subject subject);

    SystemSettings getSystemSettings(Subject subject);

    void setSystemSettings(Subject subject, SystemSettings settings) throws Exception;
    
    @Deprecated
    Properties getSystemConfiguration(Subject subject);

    @Deprecated
    void setSystemConfiguration(Subject subject, Properties properties, boolean skipValidation) throws Exception;
}