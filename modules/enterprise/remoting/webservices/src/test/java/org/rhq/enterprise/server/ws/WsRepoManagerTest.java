package org.rhq.enterprise.server.ws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.ConfigurationDefinition.PropertyDefinitions;
import org.rhq.enterprise.server.ws.ConfigurationDefinition.PropertyDefinitions.Entry;
import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * These tests can not be executed in our standard unit test fashion as they
 * require a running RHQ Server with our web services deployed.
 *
 * This is still in development and has the current restrictions:
 * <ul>
 * <li>add [dev_root]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices-{version}.jar to TOP of
 * eclipse classpath to run from your IDE(actually need to use classpath setup from bin/jbossas/bin/wsrunclient.sh to
 * take advantage of type substitution correctly)</li>
 * <li>Server running on localhost</li>
 * <li>ws-test user defined in database with full permissions</li>
 * <li>Non RHQ Server JBossAS in inventory</li>
 * <li>The -Ptest-ws profile specified when running mvn test from webservices dir</li>
 * <li>Perftest plugin installed and agent started as described in modules/enterprise/remoting/scripts/README.txt</li>
 * </ul>
 *
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
public class WsRepoManagerTest extends AssertJUnit implements TestPropertiesInterface {

    private static final boolean TESTS_ENABLED = true;

    // Test variables
    private static ObjectFactory WS_OBJECT_FACTORY;
    private static WebservicesRemote WEBSERVICE_REMOTE;
    private static Subject subject = null;
    private static PageControl pc_unlimited = null;
    // In some cases testing is run outside of mvn project tree. following needs
    // to be set to location of correct .war Ex.
    // [jon_root]/modules/enterprise/remoting/scripts/src/test/resources
    private static String HARDCODED_WAR_DIRECTORY = "/home/spinder/workspace/remoting/modules/enterprise/remoting/scripts/src/test/resources";

    @BeforeClass
    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
        LoginException_Exception {

        // build reference variable bits
        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);

        WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
        WS_OBJECT_FACTORY = new ObjectFactory();
        WsSubjectTest.checkForWsTestUserAndRole();

        subject = WEBSERVICE_REMOTE.login(credentials, credentials);
        // create unlimited pageControl instance
        pc_unlimited = WS_OBJECT_FACTORY.createPageControl();
        pc_unlimited.setPageNumber(0);
        pc_unlimited.setPageSize(-1);
    }

    @Test(enabled = TESTS_ENABLED)
    void testCRUD() throws RepoException_Exception {
        if (!TESTS_ENABLED) {
            return;
        }

        // delete any existing test channels in the db
        List<Repo> channels = WEBSERVICE_REMOTE.findRepos(subject, pc_unlimited);
        for (int i = 0; (i < channels.size()); ++i) {
            Repo channel = channels.get(i);
            if (channel.getName().startsWith("test-channel-")) {
                WEBSERVICE_REMOTE.deleteRepo(subject, channel.getId());
            }
        }

        // ensure test channel does not exist
        RepoCriteria criteria = new RepoCriteria();
        criteria.caseSensitive = true;
        criteria.setFilterName("test-channel-0");

        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertTrue("test channel should not exist.", channels.size() == 0);

        // create a test channel
        // newRepo = new Repo("test-channel-0");
        Repo newRepo = WS_OBJECT_FACTORY.createRepo();
        newRepo.setName("test-channel-0");
        newRepo.setDescription("description-0");
        Repo testRepo = WEBSERVICE_REMOTE.createRepo(subject, newRepo);
        assertNotNull("test channel should exist.", testRepo);
        assertEquals("test-channel-0", testRepo.getName());

        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertTrue("test channel should exist.", channels.size() == 1);

        // test getter
        testRepo = WEBSERVICE_REMOTE.getRepo(subject, 8888888);
        assertNull("bogus channel should not exist.", testRepo);
        testRepo = WEBSERVICE_REMOTE.getRepo(subject, channels.get(0).getId());
        assertNotNull("test channel should exist.", testRepo);
        assertEquals("test-channel-0", testRepo.getName());
        assertEquals("description-0", testRepo.getDescription());

        // test update
        testRepo.setDescription("description-1");
        testRepo = WEBSERVICE_REMOTE.updateRepo(subject, testRepo);
        assertEquals("description-1", testRepo.getDescription());
        testRepo = WEBSERVICE_REMOTE.getRepo(subject, testRepo.getId());
        assertNotNull("test channel should exist.", testRepo);
        assertEquals("test-channel-0", testRepo.getName());
        assertEquals("description-1", testRepo.getDescription());

        // test delete
        WEBSERVICE_REMOTE.deleteRepo(subject, testRepo.getId());
        testRepo = WEBSERVICE_REMOTE.getRepo(subject, testRepo.getId());
        assertNull("channel should not exist.", testRepo);
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertTrue("test channel should not exist.", channels.size() == 0);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindByCriteria() throws RepoException_Exception {
        if (!TESTS_ENABLED) {
            return;
        }

        // delete any existing test channels in the db
        List<Repo> channels = WEBSERVICE_REMOTE.findRepos(subject, pc_unlimited);
        int numRealRepos = channels.size();
        Repo channel;
        for (int i = 0; (i < channels.size()); ++i) {
            channel = channels.get(i);
            if (channel.getName().startsWith("test-channel-")) {
                WEBSERVICE_REMOTE.deleteRepo(subject, channel.getId());
                --numRealRepos;
            }
        }

        Repo newRepo = WS_OBJECT_FACTORY.createRepo();
        newRepo.setName("test-channel-xxx");
        newRepo.setDescription("description-0");
        Repo testRepo = WEBSERVICE_REMOTE.createRepo(subject, newRepo);

        newRepo = WS_OBJECT_FACTORY.createRepo();
        newRepo.setName("test-channel-yyy");
        newRepo.setDescription("description-1");
        testRepo = WEBSERVICE_REMOTE.createRepo(subject, newRepo);

        newRepo = WS_OBJECT_FACTORY.createRepo();
        newRepo.setName("test-channel-xyz");
        newRepo.setDescription("description-2");
        testRepo = WEBSERVICE_REMOTE.createRepo(subject, newRepo);

        RepoCriteria criteria = new RepoCriteria();
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("empty criteria failed.", channels.size(), numRealRepos + 3);

        criteria.caseSensitive = true;
        criteria.strict = true;

        criteria.setFilterName("test-channel-xyz");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name criteria failed.", channels.size(), 1);

        criteria.setFilterName("TEST-channel-xyz");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name criteria failed.", channels.size(), 0);

        criteria.caseSensitive = false;
        criteria.strict = true;

        criteria.setFilterName("TEST-channel-xyz");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name criteria failed.", channels.size(), 1);

        criteria.caseSensitive = true;
        criteria.strict = false;

        criteria.setFilterName("XXX");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name criteria failed.", channels.size(), 0);

        criteria.caseSensitive = false;
        criteria.strict = false;

        criteria.setFilterName("XXX");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name criteria failed.", channels.size(), 1);

        criteria.setFilterName("test-channel-");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name criteria failed.", channels.size(), 3);

        criteria.setFilterName("-x");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name criteria failed.", channels.size(), 2);

        criteria.setFilterDescription("-2");
        channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteria);
        assertEquals("CS/Strict name/descrip criteria failed.", channels.size(), 1);

        // delete any existing test channels in the db
        channels = WEBSERVICE_REMOTE.findRepos(subject, pc_unlimited);
        for (int i = 0; (i < channels.size()); ++i) {
            channel = channels.get(i);
            if (channel.getName().startsWith("test-channel-")) {
                WEBSERVICE_REMOTE.deleteRepo(subject, channel.getId());
            }
        }
    }

    @Test(enabled = false)
    void testDeploy() throws InterruptedException, IOException, ResourceTypeNotFoundException_Exception,
        RepoException_Exception {

        // check prequisites

        // Available Tomcat Server
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.strict = true;
        // criteria.setFilterName("Tomcat (");
//        criteria.setFilterName("Tomcat VHost (localhost)");
         criteria.setFilterName("Tomcat (8080)");
        criteria.setFilterCurrentAvailability(AvailabilityType.UP);
        // criteria.setFilterCurrentAvailability(AvailabilityType.DOWN);
        List<Resource> tomcatServers = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertNotNull("Test requires and available Tomcat Server in inventory.", tomcatServers);
        assertTrue("Test requires and available Tomcat Server in inventory.", (tomcatServers.size() > 0));
        Resource tomcatServer = tomcatServers.get(0);

        // delete test-channel-war if in inventory
        criteria = new ResourceCriteria();
        criteria.strict = true;
        criteria.setFilterName("test-channel-war");
        criteria.setFilterResourceTypeName("Tomcat Web Application (WAR)");
        List<Resource> wars = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
        Resource war = null;

        if ((null != wars) && !wars.isEmpty()) {
            System.out.println("\n Deleting existing test-channel-war in order to test create...");
            assertEquals("Found more than 1 test-channel-war", wars.size(), 1);
            war = wars.get(0);
            // ResourceFactoryManager.deleteResource(war.getId());
            WEBSERVICE_REMOTE.deleteResource(subject, war.getId());

            // up to 60 seconds to get the job done.
            for (int i = 0; (i < 60); ++i) {
                Thread.sleep(1000);

                wars = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
                if ((null == wars) || wars.isEmpty()) {
                    break;
                }
            }

            wars = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
            assertTrue("test-channel-war should not exist", ((null == wars) || wars.isEmpty()));
            System.out.println("\n Done deleting existing test-channel-war in order to test create...");

            // Give Tomcat a few additional seconds to perform its cleanup of
            // the app, just in case the resource is
            // gone but TC is still mopping up, before we try and deploy the
            // same exact app
            Thread.sleep(10000);
        }

        // Create the war resource

        // Get the parent
        criteria = new ResourceCriteria();
        criteria.strict = true;
        criteria.setFilterName("Tomcat VHost (localhost)");
        criteria.setFilterCurrentAvailability(AvailabilityType.UP);
        List<Resource> vhosts = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
        assertNotNull("Test requires Tomcat VHost (localhost) in inventory.", vhosts);
        assertEquals("Test requires Tomcat VHost (localhost) in inventory.", 1, vhosts.size());
        Resource vhost = vhosts.get(0);

        // get the resource type
        ResourceType warType = WEBSERVICE_REMOTE.getResourceTypeByNameAndPlugin(subject,
            "Tomcat Web Application (WAR)", "Tomcat");
        assertNotNull("Test requires Tomcat WAR resource type.", warType);

        // read in the file
        File file = new java.io.File("./src/test/resources/test-channel-war.war");
        if ((file == null) || (!file.exists())) {
            file = new java.io.File("../scripts/src/test/resources/test-channel-war.war");
        }
        if ((file == null) || (!file.exists())) {
            // THE FOLLOWING IS BAD, but there are some test situations where
            // mvn is NOT running from within project structure
            file = new java.io.File(HARDCODED_WAR_DIRECTORY + "/test-channel-war.war");
        }
        assertNotNull("Unable to locate .war file to continue with testing.", file);
        FileInputStream inputStream = new java.io.FileInputStream(file);
        long fileLength = file.length();
        // fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE,
        // fileLength);
        byte[] fileBytes = new byte[(int) fileLength];
        int offset = 0;
        for (int numRead = 0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead) {
            numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset);
        }
        assertEquals("Read bytes not equal to file length", offset, fileLength);

        inputStream.close();
        assertTrue("Could not completely read file " + file.getName(), (offset == fileBytes.length));

        // get package type for both create and update

        List<PackageType> packageTypes = WEBSERVICE_REMOTE.findPackageTypes(subject, "Tomcat Web Application (WAR)",
            "Tomcat");
        assertNotNull("missing package type", packageTypes);
        assertEquals("unexpected package type", packageTypes.size(), 1);
        PackageType packageType = packageTypes.get(0);

        // get package config def
        ConfigurationDefinition deployConfigDef = WEBSERVICE_REMOTE.getPackageTypeConfigurationDefinition(subject,
            packageType.getId());
        assertNotNull("deployConfigDef should exist.", deployConfigDef);

        // explodeOnDeploy = deployConfigDef.getPropertyDefinitionSimple(
        // "explodeOnDeploy" );
        PropertyDefinitions propertyDefinitions = deployConfigDef.getPropertyDefinitions();
        assertNotNull("PropertyDefinitions have not been populated.", propertyDefinitions);
        PropertyDefinition explodeOnDeploy = (PropertyDefinition) locateProperty(propertyDefinitions, "explodeOnDeploy");

        assertNotNull("explodeOnDeploy prop should exist.", explodeOnDeploy);
        Configuration deployConfig = new Configuration();
        // property = new PropertySimple(explodeOnDeploy.getName(), "true");
        PropertySimple propSimp = new PropertySimple();
        propSimp.setName(explodeOnDeploy.getName());
        propSimp.setStringValue("true");
        PropertySimple property = propSimp;
        // deployConfig.put( property );
        List<Property> prop = deployConfig.getPropertyListOrPropertySimpleOrPropertyMap();
        assertNotNull("Could not located property container.", prop);
        prop.add(property);

        WsConfiguration wsConfig = new WsConfiguration();
        wsConfig.propertySimpleContainer = new ArrayList<PropertySimple>();
        wsConfig.propertySimpleContainer.add(property);

        // null arch -> noarch
        // no required plugin config for this resource type
        // no required resource config for this resource type
        // resource name defaults to war file name / context root
        // WEBSERVICE_REMOTE.createResource( vhost.getId(), warType.getId(),
        // null, null, file.getName(), "1.0", null, deployConfig, fileBytes);
        WEBSERVICE_REMOTE.createPackageBackedResource(subject, vhost.getId(), warType.getId(), null, null, file
            .getName(), "1.0", null,
        // deployConfig, fileBytes);
            wsConfig, fileBytes);

        criteria = new ResourceCriteria();
        criteria.strict = false;
        criteria.setFilterName("test-channel-war");
        criteria.setFilterResourceTypeName("Tomcat Web Application (WAR)");
        criteria.setFilterCurrentAvailability(AvailabilityType.UP);

        // up to 60 seconds to get the job done.
        for (int i = 0; (i < 60); ++i) {
            Thread.sleep(1000);

            wars = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
            if ((null != wars) && !wars.isEmpty()) {
                break;
            }
        }

        wars = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
        war = null;
        if ((null != wars) && !wars.isEmpty()) {
            assertEquals("Found more than 1 test-channel-war", wars.size(), 1);
            war = wars.get(0);
        }
        assertNotNull("War should have been created", war);
        Thread.sleep(15 * 1000);

        // The backing package (InstalledPackage) for a new PBR may not exist
        // immediately. It
        // requires an agent-side content discovery. Wait for up to 120 seconds
        // (to take care of initial delay
        // and schedule interval timing.
        InstalledPackage backingPackage;
        for (int i = 0; (i < 120); ++i) {
            Thread.sleep(1000);

            backingPackage = WEBSERVICE_REMOTE.getBackingPackageForResource(subject, war.getId());
            if (null != backingPackage) {
                break;
            }
        }

        backingPackage = WEBSERVICE_REMOTE.getBackingPackageForResource(subject, war.getId());
        assertNotNull("backing package should exist after create", backingPackage);
        System.out.println("\n After Create: Backing Package=" + backingPackage.getId());

        // delete existing test channel in the db, this will unsubscribe
        // resources and remove orphaned pvs
        RepoCriteria criteriaChan = new RepoCriteria();
        criteriaChan.caseSensitive = true;
        criteriaChan.strict = true;
        criteriaChan.setFilterName("test-channel-0");

        List<Repo> channels = WEBSERVICE_REMOTE.findReposByCriteria(subject, criteriaChan);
        Repo channel;
        if (!channels.isEmpty()) {
            channel = channels.get(0);
            WEBSERVICE_REMOTE.deleteRepo(subject, channel.getId());
        }

        // create a test channel
        // newRepo = new Repo("test-channel-0");
        Repo newRepo = new Repo();
        newRepo.setName("test-channel-0");
        newRepo.setDescription("description-0");
        channel = WEBSERVICE_REMOTE.createRepo(subject, newRepo);

        assertNotNull("channel should have existed or been created", channel);
        assertTrue("channel should have existed or been created", (channel.getId() > 0));

        // test channel subscription
        // subscribedResources;
        List<Resource> subscribedResources = WEBSERVICE_REMOTE.findSubscribedResources(subject, channel.getId(),
            pc_unlimited);
        assertTrue("test channel should not have resources", ((null == subscribedResources) || subscribedResources
            .isEmpty()));

        // RepoManager.subscribeResourceToRepos( war.getId(),
        // [channel.getId()] );
        List<Integer> bag = new ArrayList<Integer>();
        bag.add(channel.getId());
        WEBSERVICE_REMOTE.subscribeResourceToRepos(subject, war.getId(), bag);

        subscribedResources = WEBSERVICE_REMOTE.findSubscribedResources(subject, channel.getId(), pc_unlimited);
        assertEquals("channel should have the test war", subscribedResources.size(), 1);

        // RepoManager.unsubscribeResourceFromRepos( war.getId(),
        // [channel.getId()] );
        WEBSERVICE_REMOTE.unsubscribeResourceFromRepos(subject, war.getId(), bag);

        subscribedResources = WEBSERVICE_REMOTE.findSubscribedResources(subject, channel.getId(), pc_unlimited);
        assertTrue("test channel should not have resources", ((null == subscribedResources) || subscribedResources
            .isEmpty()));

        // Create packageVersion in an attempt to upgrade the web-app

        List<PackageVersion> pvsInRepo = WEBSERVICE_REMOTE.findPackageVersionsInRepo(subject, channel.getId(), null,
            pc_unlimited);
        assertTrue("test channel should not have pvs", ((null == pvsInRepo) || pvsInRepo.isEmpty()));

        List<Architecture> architectures = WEBSERVICE_REMOTE.findArchitectures(subject);
        assertNotNull("missing architectures", architectures);
        assertTrue("missing architectures", !architectures.isEmpty());

        // read in the package file
        file = new java.io.File("./src/test/resources/test-channel-war-2.0.war");
        if ((file == null) || (!file.exists())) {
            file = new java.io.File("../scripts/src/test/resources/test-channel-war-2.0.war");
        }
        if ((file == null) || (!file.exists())) {
            // THE FOLLOWING IS BAD, but there are some test situations where
            // mvn is NOT running from within project structure
            file = new java.io.File(HARDCODED_WAR_DIRECTORY + "/test-channel-war-2.0.war");
        }

        inputStream = new java.io.FileInputStream(file);
        fileLength = file.length();
        fileBytes = new byte[(int) fileLength];
        offset = 0;
        for (int numRead = 0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead) {
            numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset);
        }
        inputStream.close();
        assertTrue("Could not completely read file " + file.getName(), (offset == fileBytes.length));

        PackageVersion pv = WEBSERVICE_REMOTE.createPackageVersion(subject, "test-channel-war-2.0.war", packageType
            .getId(), "2.0", null, fileBytes);
        assertNotNull("failed to create packageVersion", pv);
        assertTrue(" Bad PV Id from createPV", (pv.getId() > 0));

        // RepoManager.addPackageVersionsToRepo( channel.getId(),
        // [pv.getId()] );
        List<Integer> pvBag = new ArrayList<Integer>();
        pvBag.add(pv.getId());
        WEBSERVICE_REMOTE.addPackageVersionsToRepo(subject, channel.getId(), pvBag);

        pvsInRepo = WEBSERVICE_REMOTE.findPackageVersionsInRepo(subject, channel.getId(), null, pc_unlimited);
        assertNotNull("pv should be in channel", pvsInRepo);
        assertEquals("unexpected pvs", pvsInRepo.size(), 1);
        assertEquals("unexpected pv returned", pvsInRepo.get(0).getId(), pv.getId());

        // do the update
        // ContentManager.deployPackages( [war.getId()], [pv.getId()] );
        List<Integer> warBag = new ArrayList<Integer>();
        warBag.add(war.getId());
        WEBSERVICE_REMOTE.deployPackages(subject, warBag, pvBag, "Test Notes");

        // Make sure things still look good

        criteria = new ResourceCriteria();
        criteria.strict = false;
        criteria.setFilterName("test-channel-war");
        criteria.setFilterResourceTypeName("Tomcat Web Application (WAR)");
        criteria.setFilterCurrentAvailability(AvailabilityType.UP);

        // up to 60 seconds to get the job done.
        for (int i = 0; (i < 60); ++i) {
            Thread.sleep(1000);

            wars = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
            if ((null != wars) && !wars.isEmpty()) {
                break;
            }
        }

        wars = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
        war = null;
        if ((null != wars) && !wars.isEmpty()) {
            assertEquals("Found more than 1 test-channel-war", wars.size(), 1);
            war = wars.get(0);
        }
        assertNotNull("War should have been updated", war);

        InstalledPackage newBackingPackage = WEBSERVICE_REMOTE.getBackingPackageForResource(subject, war.getId());
        Assert.assertNotNull(newBackingPackage, "backing package should exist after update.");
        System.out.println("\n After Update: BackingPackage=" + newBackingPackage.getId());

        // TODO: This test may fail due to RHQ-2387, uncomment when fixed
        // Assert.assertTrue( ( backingPackage,getId() !=
        // newBackingPackage.getId() ),
        // "Backing ackage should differ after update" );

        // delete any existing test channels in the db
        channels = WEBSERVICE_REMOTE.findRepos(subject, pc_unlimited);
        for (int i = 0; (i < channels.size()); ++i) {
            channel = channels.get(i);
            if (channel.getName().startsWith("test-channel-")) {
                WEBSERVICE_REMOTE.deleteRepo(subject, channel.getId());
            }
        }

    }

    private PropertyDefinition locateProperty(PropertyDefinitions propertyDefinitions, String name) {
        PropertyDefinition located = null;
        if ((propertyDefinitions != null) && (name != null) && (name.trim().length() > 0)) {
            List<Entry> list = propertyDefinitions.getEntry();
            for (int i = 0; (located == null) && i < list.size(); i++) {
                Entry entry = list.get(i);
                if (entry.getKey().equals(name)) {
                    located = entry.getValue();
                }
            }

        }
        return located;
    }
}