package org.rhq.modules.plugins.jbossas7;

import java.net.URL;

import org.testng.annotations.Test;

/**
 * Test the ability to read information from the AS7 xml files (standalone.xml and so on)
 * @author Heiko W. Rupp
 */
@Test
public class XmlFileReadingTest {

    public void hostPort70() throws Exception {

        BaseProcessDiscovery bd = new BaseProcessDiscovery();
        URL url = getClass().getClassLoader().getResource("standalone70.xml");
        bd.readStandaloneOrHostXmlFromFile(url.getFile());

        AbstractBaseDiscovery.HostPort hp = bd.getManagementPortFromHostXml(new String[]{});
        System.out.println(hp);
        assert hp.host.equals("127.0.0.70") : "Host is " + hp.host;
        assert hp.port==19990 : "Port is " + hp.port;
    }

    public void hostPort71() throws Exception {

        BaseProcessDiscovery bd = new BaseProcessDiscovery();
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        bd.readStandaloneOrHostXmlFromFile(url.getFile());

        AbstractBaseDiscovery.HostPort hp = bd.getManagementPortFromHostXml(new String[]{});
        System.out.println(hp);
        // hp : HostPort{host='localhost', port=9990, isLocal=true}
        assert hp.host.equals("127.0.0.71") : "Host is " + hp.host;
        assert hp.port==29990 : "Port is " + hp.port;
    }

    public void domainController1() throws Exception {

        BaseProcessDiscovery bd = new BaseProcessDiscovery();
        URL url = getClass().getClassLoader().getResource("host1.xml");
        bd.readStandaloneOrHostXmlFromFile(url.getFile());

        AbstractBaseDiscovery.HostPort hp = bd.getDomainControllerFromHostXml();
        assert hp.isLocal : "DC is not local as expected: " + hp;

    }

    public void domainController2() throws Exception {

        BaseProcessDiscovery bd = new BaseProcessDiscovery();
        URL url = getClass().getClassLoader().getResource("host2.xml");
        bd.readStandaloneOrHostXmlFromFile(url.getFile());

        AbstractBaseDiscovery.HostPort hp = bd.getDomainControllerFromHostXml();
        assert "192.168.100.1".equals(hp.host) : "DC is at " + hp.host;
        assert hp.port == 9559 : "DC port is at " + hp.port;
    }



    public void testXpath70() throws Exception {

        BaseProcessDiscovery bd = new BaseProcessDiscovery();
        URL url = getClass().getClassLoader().getResource("standalone70.xml");
        bd.readStandaloneOrHostXmlFromFile(url.getFile());

/*
        String realm = bd.obtainXmlPropertyViaXPath("/management/management-interfaces/http-interface/@security-realm");
        assert "ManagementRealm".equals(realm) : "Realm was " + realm;
*/

        String pathExpr = "//management/management-interfaces/http-interface/@port";
        String port = bd.obtainXmlPropertyViaXPath(pathExpr);
        assert "19990".equals(port) : "Port was [" + port + "]";

        pathExpr = "//management/management-interfaces/http-interface/@interface";
        String interfName = bd.obtainXmlPropertyViaXPath(pathExpr);
        assert "management".equals(interfName) : "Interface was " + interfName;

        pathExpr = "/server/interfaces/interface[@name='" + interfName + "']/inet-address/@value";
        String interfElem = bd.obtainXmlPropertyViaXPath(pathExpr);
        assert "${jboss.bind.address.management:127.0.0.70}".equals(interfElem) : "InterfElem was " + interfElem;

    }


    public void testXpath71() throws Exception {

        BaseProcessDiscovery bd = new BaseProcessDiscovery();
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        bd.readStandaloneOrHostXmlFromFile(url.getFile());

        String realm = bd.obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");
        assert "ManagementRealm".equals(realm) : "Realm was " + realm;
        String sbindingRef = bd.obtainXmlPropertyViaXPath(
                ("//management/management-interfaces/http-interface/socket-binding/@http"));
        assert "management-http".equals(sbindingRef): "Socketbinding was " + sbindingRef;

        String pathExpr = "/server/socket-binding-group/socket-binding[@name='" + sbindingRef + "']/@port";
        String port = bd.obtainXmlPropertyViaXPath(pathExpr);
        assert "29990".equals(port) : "Port was [" + port + "]";

        pathExpr = "/server/socket-binding-group/socket-binding[@name='" + sbindingRef + "']/@interface";
        String interfName = bd.obtainXmlPropertyViaXPath(pathExpr);
        assert "management".equals(interfName) : "Interface was " + interfName;

        pathExpr = "/server/interfaces/interface[@name='" + interfName + "']/inet-address/@value";
        String interfElem = bd.obtainXmlPropertyViaXPath(pathExpr);
        assert "${jboss.bind.address.management:127.0.0.71}".equals(interfElem) : "InterfElem was " + interfElem;

    }

    public void testGetRealm() throws Exception {

        BaseProcessDiscovery bd = new BaseProcessDiscovery();
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        bd.readStandaloneOrHostXmlFromFile(url.getFile());

        String realm = bd.obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");
        assert "ManagementRealm".equals(realm) : "Realm was " + realm;

        String xpathExpression = "//management//security-realm[@name ='%s']/authentication/properties/@path";

        String propsFileName = bd.obtainXmlPropertyViaXPath(String.format(xpathExpression,realm));
        assert "mgmt-users.properties".equals(propsFileName) : "File name was " + propsFileName;

        String propsFilePathRel = bd.obtainXmlPropertyViaXPath("//management//security-realm[@name ='" + realm + "']/authentication/properties/@relative-to");
        assert "jboss.server.config.dir".equals(propsFilePathRel) : "Path was " + propsFileName;

    }
}
