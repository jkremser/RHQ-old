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

package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;

/**
 * This is a test tool, it will download package metadata for a particular channel, parse it and save it to a file.
 * @author jmatthews
 *
 */
public class DownloadPackageMetadataTool {
    private final Log log = LogFactory.getLog(DownloadPackageMetadataTool.class);

    public static final String CHANNEL_PROP_NAME = "rhn.channel";
    public static final String START_INDEX_PROP = "rhn.index.start";
    public static final String END_INDEX_PROP = "rhn.index.end";
    public static final String SAVE_FILE_PATH_PROP = "rhn.save.file.path";
    public static final String CHUNK_SIZE_PROP = "rhn.chunk.size";

    //String rhnURL = "http://satellite.rhn.stage.redhat.com";
    String rhnURL = "http://satellite.rhn.redhat.com";
    String certLoc = "./entitlement-cert.xml";
    public String systemIdPath = "/etc/sysconfig/rhn/systemid";

    protected String getSystemId() throws Exception {
        return FileUtils.readFileToString(new File(systemIdPath));
    }

    public Configuration getConfiguration() {
        String certData = "";
        try {
            certData = FileUtils.readFileToString(new File(certLoc));
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        return getConfiguration(rhnURL, certData);
    }

    public Configuration getConfiguration(String location, String certData) {
        Configuration config = new Configuration();
        PropertySimple locProp = new PropertySimple();
        locProp.setName("location");
        locProp.setStringValue(location);
        config.put(locProp);
        PropertySimple certProp = new PropertySimple();
        certProp.setName("certificate");
        certProp.setStringValue(certData);
        config.put(certProp);
        return config;
    }

    protected Map getRequestProperties() {
        Map reqProps = new HashMap();
        reqProps.put("X-RHN-Satellite-XML-Dump-Version", "3.3");
        return reqProps;
    }

    /**
     * Allow separate calls to get a XmlRpcClient, so we can configure which calls save data, and which ones don't
     */
    protected XmlRpcClient getClient(boolean saveData, String saveFilePath) throws Exception {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(rhnURL + "/SAT-DUMP"));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        transportFactory.setDumpMessageToFile(saveData);
        transportFactory.setDumpFilePath(saveFilePath);
        client.setTransportFactory(transportFactory);
        return client;
    }

    public String[] getPackagesInChannel(String channelName) throws Exception {

        XmlRpcClient client = getClient(false, "");
        List<String> channel_labels = new ArrayList<String>();
        channel_labels.add(channelName);
        Object[] params = new Object[] { getSystemId(), channel_labels };
        log.info("Calling 'dump.channels'");
        long startVal = System.currentTimeMillis();
        JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.channels", params);
        long endVal = System.currentTimeMillis();
        RhnSatelliteType sat = result.getValue();
        log.info("Data came back from 'dump.channels', took " + (endVal - startVal) + "ms");
        List<RhnChannelType> channels = sat.getRhnChannels().getRhnChannel();
        if (channels.size() != 1) {
            log.info("Error:  got back " + channels.size() + " expected 1");
            return null;
        }
        assert channels.size() == 1;
        RhnChannelType channel = channels.get(0);
        log.info("Parsing packages for channel: " + channel.getRhnChannelName() + " : "
            + channel.getRhnChannelSummary());
        String packages = channel.getPackages();
        String[] pkgIds = packages.split(" ");
        log.info(pkgIds.length + " package ids were found");
        return pkgIds;
    }

    /**
     * 
     * @param channelName channel name to sync
     * @param saveFilePath where to save the raw xml data
     * @param chunkSize how many packages to fetch per output file
     * @throws Exception
     */
    public void saveMetadata(String channelName, String saveFilePath, int chunkSize) throws Exception {

        // Get list of packages in channel
        String[] pkgIds = getPackagesInChannel(channelName);
        // Construct list of package id's to fetch
        List<String> reqPackages = new ArrayList<String>();
        for (String p : pkgIds) {
            reqPackages.add(p);
        }
        int counter = 0;
        // Below will chunk the metadata fetches
        for (int start = 0; start < reqPackages.size(); start += chunkSize) {
            int endSize = start + chunkSize;
            if (endSize > reqPackages.size()) {
                endSize = reqPackages.size();
            }
            List<String> pkgs = reqPackages.subList(start, endSize);
            // Setting up a XmlRpcClient to only use for fetching of package metadata.
            // Filename is appended with ".counter" so filename.0, filename.1, filename.2, etc
            XmlRpcClient client = getClient(false, saveFilePath + "." + counter);
            Object[] params = new Object[] { getSystemId(), pkgs };
            log.info("Calling 'dump.packages' on " + pkgs.size() + " packages [" + counter * chunkSize + "|"
                + reqPackages.size() + "]");
            long startTime = System.currentTimeMillis();
            JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.packages", params);
            long endTime = System.currentTimeMillis();
            RhnSatelliteType sat = result.getValue();
            List<RhnPackageType> rhnPkgs = sat.getRhnPackages().getRhnPackage();
            log.info("Finished 'dump.packages', fetch of package metadata for " + rhnPkgs.size() + " packages took "
                + (endTime - startTime) + "ms");
            counter++;
        }
    }

    /**
     * 
     * @param channelName channel name to sync
     * @param saveFilePath where to save the raw xml data
     * @param start the index to start fetching package metadata from
     * @param end the index to stop fetching package metadata for, use -1 if you want all packages
     * @param chunkSize how many packages to fetch per output file
     * @throws Exception
     */
    public List<RhnPackageType> saveMetadata(String channelName, String saveFilePath, int start, int end)
        throws Exception {

        // Setting up a XmlRpcClient to only use for fetching of package metadata.
        XmlRpcClient client = getClient(true, saveFilePath);

        // Get list of packages in channel
        String[] pkgIds = getPackagesInChannel(channelName);
        // Construct list of package id's to fetch
        List<String> reqPackages = new ArrayList<String>();
        /**
         * We might want to only process a slice of the packages later, so breaking it out now
         * eg. might only want to get metadata for packages 2300 - 2700
         */
        int startIndex = start;
        if ((startIndex < 0) || (startIndex > pkgIds.length)) {
            startIndex = 0;
        }
        int endIndex = end;
        if ((endIndex > pkgIds.length) || (endIndex < 0)) {
            endIndex = pkgIds.length;
        }

        for (int index = startIndex; index < endIndex; index++) {
            String pkgId = pkgIds[index];
            reqPackages.add(pkgId);
        }
        // Fetch metadata
        Object[] params = new Object[] { getSystemId(), reqPackages };
        log.info("Calling 'dump.packages' on " + reqPackages.size() + " packages");
        long startTime = System.currentTimeMillis();
        JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.packages", params);
        long endTime = System.currentTimeMillis();
        log.info("Finished reading data/parsing from 'dump.packages'.");

        RhnSatelliteType sat = result.getValue();
        List<RhnPackageType> pkgs = sat.getRhnPackages().getRhnPackage();
        log.info("Fetch of package metadata for " + pkgs.size() + " packages took " + (endTime - startTime) + "ms");
        return pkgs;
    }

    public static void main(String[] args) throws Exception {
        DownloadPackageMetadataTool pkgMetadata = new DownloadPackageMetadataTool();

        String channelName = System.getProperty(CHANNEL_PROP_NAME);
        if (StringUtils.isBlank(channelName)) {
            System.out.println("Usage error.  Required java property '" + CHANNEL_PROP_NAME + "' is missing.");
            System.exit(-1);
        }

        String startRange = System.getProperty(START_INDEX_PROP);
        if (StringUtils.isBlank(startRange)) {
            System.out.println("No property set for " + START_INDEX_PROP + " so default to 0");
            startRange = "0";
        }
        int start = Integer.parseInt(startRange);

        String endRange = System.getProperty(END_INDEX_PROP);
        if (StringUtils.isBlank(endRange)) {
            System.out.println("No property set for " + END_INDEX_PROP + " so default to -1");
            endRange = "-1";
        }
        int end = Integer.parseInt(endRange);

        String saveFilePath = System.getProperty(SAVE_FILE_PATH_PROP);
        if (StringUtils.isBlank(saveFilePath)) {
            saveFilePath = System.getProperty("java.io.tmpdir") + File.separator + channelName
                + "-package-metadata.xml";
            System.out.println("Setting saveFilePath to " + saveFilePath);
        }

        System.out.println("Will fetch package metadata for: " + channelName + " and save it to: " + saveFilePath);

        String chunk = System.getProperty(CHUNK_SIZE_PROP);
        System.out.println(CHUNK_SIZE_PROP + " = " + chunk);
        if (StringUtils.isBlank(chunk)) {
            System.out.println("Calling saveMetadata start = " + start + ", end = " + end);
            pkgMetadata.saveMetadata(channelName, saveFilePath, start, end);
        } else {
            int chunkSize = Integer.parseInt(chunk);
            System.out.println("Calling saveMetadata chunkSize = " + chunkSize);
            pkgMetadata.saveMetadata(channelName, saveFilePath, chunkSize);
        }
        System.out.println("Package metadata for channel {" + channelName + "} written to " + saveFilePath);
    }
}
