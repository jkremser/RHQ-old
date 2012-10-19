/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;

/**
 * Abstract base class for some discovery related functionality - especially
 * in the area of processes and host.xml
 * @author Heiko W. Rupp
 */
public class AbstractBaseDiscovery {

    static final String BOOT_LOG_FILE_SYSPROP = "org.jboss.boot.log.file";
    private static final String HOME_DIR_SYSPROP = "jboss.home.dir";

    static final int DEFAULT_MGMT_PORT = 9990;
    private static final String JBOSS_AS_PREFIX = "jboss-as-";
    static final String CALL_READ_STANDALONE_OR_HOST_XML_FIRST = "hostXml is null. You need to call 'readStandaloneOrHostXml' first.";
    private static final String SOCKET_BINDING_PORT_OFFSET_SYSPROP = "jboss.socket.binding.port-offset";
    protected Document hostXml;
    protected final Log log = LogFactory.getLog(this.getClass());
    private static final String JBOSS_EAP_PREFIX = "jboss-eap-";
    public static final String EAP = "EAP";
    public static final String JDG = "JDG";
    public static final String EAP_PREFIX = EAP + " ";
    public static final String JDG_PREFIX = JDG + " ";
    private XPathFactory factory;

    protected AbstractBaseDiscovery() {
        synchronized (this) {
            factory = XPathFactory.newInstance();
        }
    }

    protected void readStandaloneOrHostXmlFromFile(String hostXmlFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            try {
                hostXml = builder.parse(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Determine the location of the boot log file of the server by parsing the command line
     * @param commandLine command line arguments of the process
     * @return The log file location or empty string otherwise
     */
    //-Dorg.jboss.boot.log.file=/devel/jbas7/jboss-as/build/target/jboss-7.0.0.Alpha2/domain/log/server-manager/boot.log
    //-Dlogging.configuration=file:/devel/jbas7/jboss-as/build/target/jboss-7.0.0.Alpha2/domain/configuration/logging.properties
    String getLogFileFromCommandLine(String[] commandLine) {
        return getSystemPropertyFromCommandLine(commandLine, BOOT_LOG_FILE_SYSPROP, "");
    }

    /**
     * Try to obtain the management IP and port from the already parsed host.xml or standalone.xml
     * @return an Object containing host and port
     * @see #readStandaloneOrHostXmlFromFile(String) for how to obtain the parsed xml
     * @param commandLine Command line arguments of the process to
     */
    protected HostPort getManagementPortFromHostXml(String[] commandLine) {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        String portString;
        String interfaceExpession;

        String socketBindingName;

        socketBindingName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket-binding/@http");
        String socketInterface = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@interface");
        String portOffset = null;

        if (!socketInterface.isEmpty()) {
            interfaceExpession = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                + "']/inet-address/@value");
            if (interfaceExpession.isEmpty()) {
                interfaceExpession = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                                + "']/loopback-address/@value");
            }
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@port");
        } else if (socketBindingName.isEmpty()) {
            // old AS7.0, early 7.1 style
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@port");
            String interfaceName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@interface");
            interfaceExpession = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                + "']/inet-address/@value");
            if (interfaceExpession.isEmpty()) {
                interfaceExpession = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                                + "']/loopback-address/@value");
            }
        } else {
            // later AS7.1 and EAP6 standalone.xml
            portString = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@port");
            String interfaceName = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@interface");
            String socketBindingGroupName = "standard-sockets";
            // /server/socket-binding-group[@name='standard-sockets']/@port-offset
            String xpathExpression =
                    "/server/socket-binding-group[@name='" + socketBindingGroupName + "']/@port-offset";
            portOffset = obtainXmlPropertyViaXPath(xpathExpression);

            // TODO the next may also be expressed differently
            interfaceExpession = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                + "']/inet-address/@value");
            if (interfaceExpession.isEmpty()) {
                interfaceExpession = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                                + "']/loopback-address/@value");
            }
        }
        HostPort hp = new HostPort();

        if (!interfaceExpession.isEmpty())
            hp.host = replaceDollarExpression(interfaceExpession, commandLine, "localhost");
        else
            hp.host = "localhost"; // Fallback

        hp.port = 0;

        if (portString != null && !portString.isEmpty()) {
            String tmp = replaceDollarExpression(portString, commandLine, String.valueOf(DEFAULT_MGMT_PORT));
            hp.port = Integer.valueOf(tmp);
        }

        if (portOffset!=null && !portOffset.isEmpty()) {
            String tmp = replaceDollarExpression(portOffset, commandLine, "0");
            Integer offset = Integer.valueOf(tmp);
            hp.port += offset;
            hp.withOffset=true;
        }
        return hp;
    }

    /**
     * Check if the passed value has an expression in the form of ${var} or ${var:default},
     * try to resolve it. Resolution is done by looking at the command line to see if
     * there are -bmanagement or -Djboss.bind.address.management arguments present
     *
     * @param value a hostname or hostname expression
     * @param commandLine The command line from the process
     * @param lastResort fall back to this value if the value could not be found on the command line and
     *                   the expression did not specify a default value
     * @return resolved value
     */
    private String replaceDollarExpression(String value, String[] commandLine, String lastResort) {
        if (!value.contains("${"))
            return value;

        // remove ${ }
        value = value.substring(2, value.length() - 1);
        String fallback = lastResort;
        String expression;
        if (value.contains(":")) {
            int i = value.indexOf(":");
            expression = value.substring(0, i);
            fallback = value.substring(i + 1);
        } else {
            expression = value;
        }
        /*
         * Now try to find the expression in the arguments.
         * AS 7 unfortunately is "too clever" and we need to look for
         * -D jboss.bind.address.management
         * or
         * -b management
         * to find the management addresss
         */

        String ret = null;
        for (int i = 0, commandLineLength = commandLine.length; i < commandLineLength; i++) {
            String line = commandLine[i];
            if (expression.contains("address")) {
                if (line.contains("-bmanagement") || line.contains("jboss.bind.address.management")) {
                    if (line.contains("="))
                        ret = line.substring(line.indexOf("=") + 1); // -bmanagement=1.2.3.4
                    else
                        ret = commandLine[i + 1]; // -bmanagement 1.2.3.4
                    break;
                }
            } else if (expression.equals("jboss.management.http.port")) {
                if (line.contains(expression)) {
                    ret = line.substring(line.indexOf("=") + 1);
                    break;
                }
            } else if (expression.equals(SOCKET_BINDING_PORT_OFFSET_SYSPROP)) {
                if (line.contains(expression)) {
                    ret = line.substring(line.indexOf('=')+1);
                    break;
                }
            }
        }
        if (ret == null)
            ret = fallback;

        return ret;

    }

    /**
     * Try to determine the host name - that is the name of a standalone server or a
     * host in domain mode by looking at the standalone.xml/host.xml files
     * @return server name
     */
    protected String findHostName() {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        String hostName = hostXml.getDocumentElement().getAttribute("name");
        return hostName;
    }

    /**
     * Try to obtain the domain controller's location from looking at host.xml
     * @return host and port of the domain controller
     */
    protected HostPort getHostPortFromHostXml() {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        // first check remote, as we can't distinguish between a missing local element or
        // and empty one which is the default
        String remoteHost = obtainXmlPropertyViaXPath("/host/domain-controller/remote/@host");
        String portString = obtainXmlPropertyViaXPath("/host/domain-controller/remote/@port");

        HostPort hp;
        if (!remoteHost.isEmpty() && !portString.isEmpty()) {
            hp = new HostPort(false);
            hp.host = remoteHost;
            hp.port = Integer.parseInt(portString);
        } else {
            hp = new HostPort(true);
            hp.port = 9999;
        }

        return hp;

    }

    String getManagementSecurityRealmFromHostXml() {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        String realm = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");

        return realm;
    }

    String getSecurityPropertyFileFromHostXml(String baseDir, AS7Mode mode, String realm) {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        String fileName = obtainXmlPropertyViaXPath("//security-realms/security-realm[@name='" + realm
            + "']/authentication/properties/@path");
        String relDir = obtainXmlPropertyViaXPath("//security-realms/security-realm[@name='" + realm
            + "']/authentication/properties/@relative-to");

        String dmode;
        if (mode == AS7Mode.STANDALONE)
            dmode = "server";
        else
            dmode = "domain";

        String fullName;
        if (relDir.equals("jboss." + dmode + ".config.dir"))
            fullName = baseDir + File.separator + mode.getBaseDir() + File.separator + "configuration" + File.separator
                + fileName;
        else
            fullName = relDir + File.separator + fileName;

        return fullName;
    }

    protected File getHomeDir(ProcessInfo processInfo) {
        String home = getSystemPropertyFromCommandLine(processInfo.getCommandLine(), HOME_DIR_SYSPROP,
                processInfo.getEnvironmentVariable("JBOSS_HOME"));
        return new File(FileUtils.getCanonicalPath(home));
    }

    protected String determineServerVersionFromHomeDir(String homeDir) {
        String version;
        String tmp = homeDir.substring(homeDir.lastIndexOf("/") + 1);
        if (tmp.startsWith(JBOSS_AS_PREFIX)) {
            version = tmp.substring(JBOSS_AS_PREFIX.length());
        } else if (tmp.startsWith(JBOSS_EAP_PREFIX)) {
            version = tmp.substring(JBOSS_EAP_PREFIX.length());
        } else {
            version = homeDir.substring(homeDir.lastIndexOf("-") + 1);
        }
        return version;
    }

    /**
     * Run the passed xpathExpression on the prepopulated hostXml document and
     * return the target element or attribute as a String.
     * @param xpathExpression XPath Expression to evaluate
     * @return String value of the Element or Attribute the XPath was pointing to.
     *     Null in case the xpathExpression could not be evaluated.
     * @throws IllegalArgumentException if hostXml is null
     *
     */
    protected String obtainXmlPropertyViaXPath(String xpathExpression) {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        XPath xpath = factory.newXPath();
        try {
            XPathExpression expr = xpath.compile(xpathExpression);

            Object result = expr.evaluate(hostXml, XPathConstants.STRING);

            return result.toString();
        } catch (XPathExpressionException e) {
            log.error("Evaluation XPath expression failed: " + e.getMessage());
            return null;
        }
    }

    protected static String getSystemPropertyFromCommandLine(String[] commandLine, String systemPropertyName) {
        return getSystemPropertyFromCommandLine(commandLine, systemPropertyName, null);
    }

    protected static String getSystemPropertyFromCommandLine(String[] commandLine, String systemPropertyName,
                                                           String defaultValue) {
        for (String arg : commandLine) {
            String prefix = "-D" + systemPropertyName;
            String prefixWithEqualsSign = prefix + "=";
            if (arg.startsWith(prefixWithEqualsSign)) {
                return arg.substring(prefixWithEqualsSign.length());
            } else if (arg.equals(prefix)) {
                return "";
            }
        }
        return defaultValue;
    }

    protected HostPort checkForSocketBindingOffset(HostPort managementPort, String[] commandLine) {
        for (String line : commandLine) {
            if (line.contains(SOCKET_BINDING_PORT_OFFSET_SYSPROP)) {
                String tmp = line.substring(line.indexOf('=')+1);
                Integer offset = Integer.valueOf(tmp);
                managementPort.port+=offset;
                break;
            }
        }

        return managementPort;
    }

    /**
     * Helper class that holds information about the host,port tuple
     */
    protected static class HostPort {
        String host;
        int port;
        boolean isLocal = true;
        boolean withOffset = false;

        public HostPort() {
            host = "localhost";
            port = DEFAULT_MGMT_PORT;
            isLocal = true;
        }

        public HostPort(boolean local) {
            this();
            isLocal = local;
        }

        @Override
        public String toString() {
            return "HostPort{" + "host='" + host + '\'' + ", port=" + port + ", isLocal=" + isLocal + '}';
        }
    }
}
