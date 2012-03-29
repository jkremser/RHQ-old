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
package org.rhq.enterprise.server.test;

import static org.rhq.test.JPAUtils.clearDB;
import static org.rhq.test.JPAUtils.lookupEntityManager;
import static org.rhq.test.JPAUtils.lookupTransactionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import org.jboss.ejb3.embedded.EJB3StandaloneBootstrap;
import org.jboss.ejb3.embedded.EJB3StandaloneDeployer;
import org.jboss.mx.util.MBeanServerLocator;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.content.ContentSourceManagerBean;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScanner;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.scheduler.SchedulerService;
import org.rhq.enterprise.server.scheduler.SchedulerServiceMBean;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.JPAUtils;
import org.rhq.test.TransactionCallback;

/**
 * This is the abstract test base for server jar tests.
 *
 * @author Greg Hinkle
 */
public abstract class AbstractEJB3Test extends AssertJUnit {

    private static EJB3StandaloneDeployer deployer;
    private static Statistics stats;
    @SuppressWarnings("unused")
    private static long start; // see endTest() if you want to output this
    private SchedulerService schedulerService;
    private ServerPluginService serverPluginService;
    private MBeanServer dummyJBossMBeanServer;
    private PluginDeploymentScannerMBean pluginScannerService;

    @BeforeClass
    public void resetDB() throws Exception {
        if (isDBResetNeeded()) {
            clearDB();
        }
    }

    protected boolean isDBResetNeeded() {
        return true;
    }

    //@BeforeSuite(groups = {"integration.ejb3","PERF"}) // TODO investigate again
    @BeforeSuite(alwaysRun = true)
    public static void startupEmbeddedJboss() throws Exception {
        // The embeddedDeployment property needs to be set for running tests
        // with the embedded container. It is set in the surefire configuration
        // in pom.xml but setting here makes it easier to run tests directly
        // from your IDE.
        //
        // jsanda
        System.setProperty("embeddedDeployment", "true");

        // Setting content location to the tmp dir
        System.setProperty(ContentSourceManagerBean.FILESYSTEM_PROPERTY, System.getProperty("java.io.tmpdir"));

        System.out.println("Starting JBoss EJB3 Embedded Container...");
        String deployDir = System.getProperty("deploymentDirectory", "target/classes");
        System.out.println("Loading EJB3 deployments from directory: " + deployDir);
        try {
            EJB3StandaloneBootstrap.boot(null);
            //         EJB3StandaloneBootstrap.scanClasspath();

            System.err.println("...... embedded container booted....");

            deployer = EJB3StandaloneBootstrap.createDeployer();

            deployer.setClassLoader(AbstractEJB3Test.class.getClassLoader());
            System.err.println("...... embedded container classloader set....");

            deployer.getArchivesByResource().add("META-INF/persistence.xml");
            System.err.println("...... embedded container persistence xml deployed....");

            deployer.getArchivesByResource().add("META-INF/ejb-jar.xml");
            System.err.println("...... embedded container ejb-jar xml deployed....");

            EJB3StandaloneBootstrap.deployXmlResource("jboss-jms-beans.xml");
            System.err.println("...... embedded container jboss-jms-beans xml deployed....");

            EJB3StandaloneBootstrap.deployXmlResource("rhq-mdb-beans.xml");
            System.err.println("...... embedded container rhq-mdb-beans xml deployed....");

            /*
             * File core = new File(deployDir, "on-core-domain-ejb.ejb3");      if (!core.exists())
             * System.err.println("Deployment directory does not exist: " + core.getAbsolutePath());
             * deployer.getArchives().add(core.toURI().toURL());
             *
             * File server = new File(deployDir, "on-enterprise-server-ejb.ejb3");      if (!server.exists())
             * System.err.println("Deployment directory does not exist: " + server.getAbsolutePath());
             * deployer.getArchives().add(server.toURI().toURL());
             *
             */

            //deployer.setKernel(EJB3StandaloneBootstrap.getKernel());
            deployer.create();
            System.err.println("...... deployer created....");

            deployer.start();
            System.err.println("...... deployer started....");

            System.err.println("...... start statistics");
            SessionFactory sessionFactory = PersistenceUtility.getHibernateSession(getEntityManager())
                .getSessionFactory();
            stats = sessionFactory.getStatistics();
            stats.setStatisticsEnabled(true);

            System.err.println("...... embedded container initialized and ready for testing....");

        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    //@Configuration(groups = "integration.ejb3", afterSuite = true)
    @AfterSuite(alwaysRun = true)
    public static void shutdownEmbeddedJboss() {
        System.out.println("Stopping JBoss EJB3 Embedded Container...");

        System.err.println("!!! Any errors occurring after this point    !!!");
        System.err.println("!!! occurred during embedded server shutdown !!!");
        System.err.println("!!! and are probably not a real problem.     !!!");
        if (deployer != null) {
            try {
                deployer.stop();
                deployer.destroy();
                deployer = null;
            } catch (Throwable t) {
                System.err.println("Failed to stop embedded deployer");
                t.printStackTrace(System.err);
            }
        }
        EJB3StandaloneBootstrap.shutdown();
    }

    @BeforeMethod
    public static void startTest() {
        if (DatabaseTypeFactory.getDefaultDatabaseType() == null) {
            try {
                Connection conn = getConnection();
                DatabaseTypeFactory.setDefaultDatabaseType(DatabaseTypeFactory.getDatabaseType(conn));
            } catch (Exception e) {
                System.err.println("!!! WARNING !!! cannot set default database type, some tests may fail");
                e.printStackTrace();
            }
        }

        if (stats != null)
            start = stats.getQueryExecutionCount();
        else
            start = 0;
    }

    public static Connection getConnection() throws SQLException {
        return LookupUtil.getDataSource().getConnection();
    }

    @AfterMethod
    public static void endTest() {
        //System.out.println("Connections used: " + (stats.getQueryExecutionCount() - start));
    }

    public TransactionManager getTransactionManager() {
        return lookupTransactionManager();
    }

    public static EntityManager getEntityManager() {
        return lookupEntityManager();
    }

    public static InitialContext getInitialContext() {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        try {
            return new InitialContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load initial context", e);
        }
    }

    public boolean isPostgres() throws Exception {
        return DatabaseTypeFactory.getDatabaseType(getInitialContext(), RHQConstants.DATASOURCE_JNDI_NAME) instanceof PostgresqlDatabaseType;
    }

    /**
     * This creates a session for the given user and associates that session with the subject. You can test the security
     * annotations by creating sessions for different users with different permissions.
     *
     * @param subject a JON subject
     * @return the session activated subject, a copy of the subject passed in. 
     */
    public Subject createSession(Subject subject) {
        return SessionManager.getInstance().put(subject);
    }

    /**
     * Returns an MBeanServer that simulates the JBossAS MBeanServer.
     *
     * @return MBeanServer instance
     */
    public MBeanServer getJBossMBeanServer() {
        if (dummyJBossMBeanServer == null) {
            dummyJBossMBeanServer = MBeanServerFactory.createMBeanServer("jboss");
            MBeanServerLocator.setJBoss(dummyJBossMBeanServer);
        }

        return dummyJBossMBeanServer;
    }

    public void releaseJBossMBeanServer() {
        if (dummyJBossMBeanServer != null) {
            MBeanServerFactory.releaseMBeanServer(dummyJBossMBeanServer);
            dummyJBossMBeanServer = null;
        }
    }

    /**
     * If you need to test round trips from server to agent and back, you first must install the server communications
     * service that houses all the agent clients. Call this method and add your test agent services to the public fields
     * in the returned object.
     *
     * @return the object that will house your test agent service impls and the agent clients.
     *
     * @throws RuntimeException
     */
    public TestServerCommunicationsService prepareForTestAgents() {
        try {
            MBeanServer mbs = getJBossMBeanServer();
            if (mbs.isRegistered(ServerCommunicationsServiceMBean.OBJECT_NAME)) {
                mbs.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
            }
            TestServerCommunicationsService testAgentContainer = new TestServerCommunicationsService();
            mbs.registerMBean(testAgentContainer, ServerCommunicationsServiceMBean.OBJECT_NAME);
            return testAgentContainer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call this after your tests have finished. You only need to call this if your test previously called
     * {@link #prepareForTestAgents()}.
     */
    public void unprepareForTestAgents() {
        unprepareForTestAgents(false);
    }

    public void unprepareForTestAgents(boolean beanOnly) {
        try {
            if (beanOnly) {
                MBeanServer mbs = getJBossMBeanServer();
                if (mbs.isRegistered(ServerCommunicationsServiceMBean.OBJECT_NAME)) {
                    mbs.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
                }
            } else {
                releaseJBossMBeanServer();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * If you need to test server plugins, you must first prepare the server plugin service.
     * After this returns, the caller must explicitly start the PC by using the appropriate API
     * on the given mbean; this method will only start the service, it will NOT start the master PC.
     *
     * @param testServiceMBean the object that will house your test server plugins
     *
     * @throws RuntimeException
     */
    public void prepareCustomServerPluginService(ServerPluginService testServiceMBean) {
        try {
            MBeanServer mbs = getJBossMBeanServer();
            testServiceMBean.start();
            mbs.registerMBean(testServiceMBean, ServerPluginServiceManagement.OBJECT_NAME);
            serverPluginService = testServiceMBean;
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unprepareServerPluginService() throws Exception {
        unprepareServerPluginService(false);
    }

    public void unprepareServerPluginService(boolean beanOnly) throws Exception {
        if (serverPluginService != null) {
            serverPluginService.stopMasterPluginContainer();
            serverPluginService.stop();
            if (beanOnly) {
                MBeanServer mbs = getJBossMBeanServer();
                if (mbs.isRegistered(ServerPluginService.OBJECT_NAME)) {
                    getJBossMBeanServer().unregisterMBean(ServerPluginService.OBJECT_NAME);
                }
                if (mbs.isRegistered(ServerPluginServiceManagement.OBJECT_NAME)) {
                    getJBossMBeanServer().unregisterMBean(ServerPluginServiceManagement.OBJECT_NAME);
                }

            } else {
                releaseJBossMBeanServer();
            }
            serverPluginService = null;
        }
    }

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public void prepareScheduler() {
        try {
            if (schedulerService != null) {
                return;
            }

            Properties quartzProps = new Properties();
            quartzProps.load(this.getClass().getClassLoader().getResourceAsStream("test-scheduler.properties"));

            schedulerService = new SchedulerService();
            schedulerService.setQuartzProperties(quartzProps);
            schedulerService.start();
            getJBossMBeanServer().registerMBean(schedulerService, SchedulerServiceMBean.SCHEDULER_MBEAN_NAME);
            schedulerService.startQuartzScheduler();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unprepareScheduler() throws Exception {
        unprepareScheduler(false);
    }

    public void unprepareScheduler(boolean beanOnly) throws Exception {
        if (schedulerService != null) {
            schedulerService.stop();
            if (beanOnly) {
                MBeanServer mbs = getJBossMBeanServer();
                if (mbs.isRegistered(SchedulerServiceMBean.SCHEDULER_MBEAN_NAME)) {
                    getJBossMBeanServer().unregisterMBean(SchedulerServiceMBean.SCHEDULER_MBEAN_NAME);
                }
            } else {
                releaseJBossMBeanServer();
            }

            schedulerService = null;
        }
    }

    public PluginDeploymentScannerMBean getPluginScannerService() {
        return pluginScannerService;
    }

    public void preparePluginScannerService() {
        preparePluginScannerService(null);
    }

    public void preparePluginScannerService(PluginDeploymentScannerMBean scannerService) {
        try {
            if (scannerService == null) {
                scannerService = new PluginDeploymentScanner();
            }
            MBeanServer mbs = getJBossMBeanServer();
            mbs.registerMBean(scannerService, PluginDeploymentScannerMBean.OBJECT_NAME);
            pluginScannerService = scannerService;
            return;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unpreparePluginScannerService() throws Exception {
        unpreparePluginScannerService(false);
    }

    public void unpreparePluginScannerService(boolean beanOnly) throws Exception {
        if (pluginScannerService != null) {
            pluginScannerService.stop();
            if (beanOnly) {
                MBeanServer mbs = getJBossMBeanServer();
                if (mbs.isRegistered(PluginDeploymentScannerMBean.OBJECT_NAME)) {
                    getJBossMBeanServer().unregisterMBean(PluginDeploymentScannerMBean.OBJECT_NAME);
                }
            } else {
                releaseJBossMBeanServer();
            }

            pluginScannerService = null;
        }
    }

    protected void executeInTransaction(TransactionCallback callback) {
        JPAUtils.executeInTransaction(callback);
    }
}
