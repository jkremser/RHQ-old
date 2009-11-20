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

package org.rhq.enterprise.server.plugins.rhnhosted;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetailsKey;
import org.rhq.enterprise.server.plugin.pc.content.DistributionDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionFileDetails;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartFileType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnComm;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnDownloader;

/**
 * @author pkilambi
 *
 */
public class RHNHelper {

    private final String baseurl;
    private final RhnComm rhndata;
    private final RhnDownloader rhndownload;
    private final String systemid;
    private final String distributionType;

    private final Log log = LogFactory.getLog(RHNHelper.class);

    /**
     * Constructor.
     *
     * @param baseurl The base url to connect to hosted
     * @param systemIdIn systemId to use for auth
     */
    public RHNHelper(String baseurl, String systemIdIn) {

        this.baseurl = baseurl;
        this.rhndata = new RhnComm(baseurl);
        this.rhndownload = new RhnDownloader(baseurl);
        this.systemid = systemIdIn;
        this.distributionType = "kickstart";

    }

    public List<DistributionDetails> getDistributionMetaData(List<String> labels) throws IOException, XmlRpcException {
        log.debug("getDistributionMetaData(" + labels + " invoked");

        List<DistributionDetails> distros = new ArrayList<DistributionDetails>();
        List<RhnKickstartableTreeType> ksTreeTypes = rhndata.getKickstartTreeMetadata(this.systemid, labels);
        for (RhnKickstartableTreeType ksTree : ksTreeTypes) {
            log.debug("Forming DistributionDetails(" + ksTree.getLabel() + ", " + ksTree.getBasePath() + " , "
                + distributionType);
            DistributionDetails details = new DistributionDetails(ksTree.getLabel(), ksTree.getBasePath(),
                distributionType);
            distros.add(details);

            List<RhnKickstartFileType> ksFiles = ksTree.getRhnKickstartFiles().getRhnKickstartFile();
            for (RhnKickstartFileType ksFile : ksFiles) {
                if (log.isDebugEnabled()) {
                    log.debug("RHNHelper::getDistributionMetaData<ksLabel=" + ksTree.getLabel() + "> current file = "
                        + ksFile.getRelativePath() + ", md5sum = " + ksFile.getMd5Sum() + ", lastModified = "
                        + ksFile.getLastModified() + ", fileSize = " + ksFile.getFileSize());
                }
                Long lastMod = Long.parseLong(ksFile.getLastModified());
                DistributionFileDetails dFile = new DistributionFileDetails(ksFile.getRelativePath(), lastMod, ksFile
                    .getMd5Sum());
                Long fileSize = Long.parseLong(ksFile.getFileSize());
                dFile.setFileSize(fileSize);
                details.addFile(dFile);
            }
        }
        return distros;
    }

    /**
     * Extract the package metadata for all available packages to sync
     * @param packageIds Valid package ids for getPackageMatadata call to fetch from hosted
     * @param channelName channel name of passed in package ids
     * @return A list of package detail objects
     * @throws Exception On all errors
     */
    public List<ContentProviderPackageDetails> getPackageDetails(List packageIds, String channelName) throws Exception {
        log.debug("getPackageDetails() for " + packageIds.size() + " packageIds on channel " + channelName);
        List<ContentProviderPackageDetails> pdlist = new ArrayList<ContentProviderPackageDetails>();
        List<RhnPackageType> pkgs = rhndata.getPackageMetadata(this.systemid, packageIds);
        log.debug(pkgs.size() + " packages were returned from getPackageMetadata().");
        for (RhnPackageType pkg : pkgs) {
            try {
                pdlist.add(getDetails(pkg, channelName));
            } catch (Exception e) {
                // something went wrong while constructing the pkg object.
                // Proceed to next and get as many packages as we can.
                continue;
            }
        }
        return pdlist;
    }

    /**
     * Extract the package details for each rpm metadata fetched
     * 
     * @param p an rpm package metadata object
     * @param channelName channel name
     *
     * @return ContentProviderPackageDetails pkg object
     */
    private ContentProviderPackageDetails getDetails(RhnPackageType p, String channelName) throws IOException {

        String name = p.getName();
        String version = p.getVersion();
        String arch = p.getPackageArch();
        String rpmname = constructRpmName(name, version, p.getRelease(), p.getEpoch(), arch);

        ContentProviderPackageDetailsKey key = new ContentProviderPackageDetailsKey(name, version, "rpm", arch,
            "Linux", "Platforms");
        ContentProviderPackageDetails pkg = new ContentProviderPackageDetails(key);


        pkg.setDisplayName(name);
        pkg.setShortDescription(p.getRhnPackageSummary());
        pkg.setLongDescription(p.getRhnPackageDescription());
        pkg.setFileName(rpmname);
        pkg.setFileSize(new Long(p.getPackageSize()));
        pkg.setFileCreatedDate(new Long(p.getLastModified()));
        pkg.setLicenseName("license");
        pkg.setMD5(p.getMd5Sum());
        pkg.setLocation(constructPackageUrl(channelName, rpmname));

        String metadata = PrimaryXML.createPackageXML(p);
        byte[] gzippedMetadata = gzip(metadata.getBytes());
        pkg.setMetadata(gzippedMetadata);

        return pkg;

    }

    /**
     * Get List of packagesIds for Given Channels
     * @param channelName channel name
     * @return List of all package ids associated to the channel
     * @throws IOException  on io errors on systemid reads
     * @throws XmlRpcException on xmlrpc faults
     */
    public List<String> getChannelPackages(String channelName) throws IOException, XmlRpcException {
        log.debug("getChannelPackages(" + channelName + ")");
        ArrayList<String> allPackages = new ArrayList();
        List<RhnChannelType> channels = rhndata.getChannels(this.systemid, Arrays.asList(channelName));

        for (RhnChannelType channel : channels) {
            String packages = channel.getPackages();
            String[] pkgIds = packages.split(" ");
            if (pkgIds.length > 1) {
                allPackages.addAll(Arrays.asList(pkgIds));
            }
        }

        return allPackages;
    }

    /**
     * Get a list of all Syncable Channels based on entitled channel families
     * @return A list of channel labels
     * @throws IOException on systemid reads
     * @throws XmlRpcException on xmlrpc faults
     */
    public List<String> getSyncableChannels() throws IOException, XmlRpcException {
        log.debug("getSyncableChannels()");
        ArrayList<String> allchannels = new ArrayList<String>();
        String[] ignoredChannelFamiliesArray = { "education", "k12ltsp", "rh-public", "rhel-devsuite", "rhel-gfs" };
        List<String> ignoredChannelFamilies = Arrays.asList(ignoredChannelFamiliesArray);
        log.debug("Ignoring expired channel families :");
        log.debug(ignoredChannelFamilies.toString());

        List<RhnChannelFamilyType> cfts = rhndata.getChannelFamilies(this.systemid);
        for (RhnChannelFamilyType cf : cfts) {
            if (ignoredChannelFamilies.contains(cf.getLabel())) {
                continue;
            }
            String channeldata = cf.getChannelLabels();
            String[] clabels = channeldata.split(" ");
            if (clabels.length > 1) {
                allchannels.addAll(Arrays.asList(clabels));
            }
        }

        return allchannels;
    }

    /**
     *
     * @return returns list of all possible kickstart labels from all possible channels
     * @throws IOException
     * @throws XmlRpcException
     */
    public List<String> getSyncableKickstartLabels() throws IOException, XmlRpcException {
        log.debug("getSyncableKickstartLabels() - no channels passed, will use all available channels");
        List<String> allChannels = getSyncableChannels();
        return getSyncableKickstartLabels(allChannels);
    }

    /**
     *
     * @param channelName channel name
     * @return kickstart labels part of the passed in channel name
     * @throws IOException
     * @throws XmlRpcException
     */
    public List<String> getSyncableKickstartLabels(String channelName) throws IOException, XmlRpcException {
        log.debug("getSyncableKickstartLabels(" + channelName + ")");
        List<String> names = new ArrayList<String>();
        names.add(channelName);
        return getSyncableKickstartLabels(names);
    }

    /**
     *
     * @param channelLabels list of channel names to restrict return data to
     * @return kickstart labels from the passed in list of channel names
     * @throws IOException
     * @throws XmlRpcException
     */
    public List<String> getSyncableKickstartLabels(List<String> channelLabels) throws IOException, XmlRpcException {
        log.debug("getSyncableKickstartLabels(" + channelLabels + ")");
        List<String> ksLabels = new ArrayList<String>();
        List<RhnChannelType> rct = rhndata.getChannels(this.systemid, channelLabels);
        for (RhnChannelType ct : rct) {
            String ksTrees = ct.getKickstartableTrees();
            String[] trees = ksTrees.split(" ");
            ksLabels.addAll(Arrays.asList(trees));
        }
        return ksLabels;
    }

    /**
     * Open an input stream to specifed relative url. Prepends the baseurl to the <i>url</i> and opens and opens and
     * input stream. Files with a .gz suffix will be unziped (inline).
     *
     * @param   location A url that is relative to the <i>baseurl</i> and references a file within the repo.
     *
     * @return An open input stream that <b>must</b> be closed by the caller.
     *
     * @throws IOException  On io errors.
     *
     * @throws XmlRpcException On all errors.
     */
    public InputStream openStream(String location) throws IOException, XmlRpcException {
        log.info("File being fetched from: " + location);
        return rhndownload.getFileStream(this.systemid, location);
    }

    /**
     * Constructs a downloadable url for package downloads.
     * @param channelName channel label to be synced.
     * @param rpmName rpm file name
     * @return a valid url location to fetch the rpm from.
     */
    public String constructPackageUrl(String channelName, String rpmName) {

        return constructPackageUrl(baseurl, channelName, rpmName);
    }

    static public String constructPackageUrl(String baseurl, String channelName, String rpmName) {

        String appendurl = "/SAT/$RHN/" + channelName + "/getPackage/" + rpmName;
        return baseurl + appendurl;
    }

    /**
    * Constructs a downloadable url for package downloads.
    * @param channelName channel label to be synced.
    * @param ksTreeLabel kickstart tree label name
    * @param ksFilePath path to kickstart file
    * @return a valid url location to fetch the rpm from.
    */
    public String constructKickstartFileUrl(String channelName, String ksTreeLabel, String ksFilePath) {

        return constructKickstartFileUrl(baseurl, channelName, ksTreeLabel, ksFilePath);
    }

    static public String constructKickstartFileUrl(String baseurl, String channelName, String ksTreeLabel,
        String ksFilePath) {
        String appendurl = "/SAT/$RHN/" + channelName + "/getKickstartFile/" + ksTreeLabel + "/" + ksFilePath;
        return baseurl + appendurl;
    }

    /**
     * Method to construct an rpm format filename for download url
     * @param name  rpm package name
     * @param version  rpm package version
     * @param release  rpm package release
     * @param epoch   rpm package epoch
     * @param arch    rpm package arch
     * @return an rpm package name string
     */
    static public String constructRpmName(String name, String version, String release, String epoch, String arch) {

        String releaseepoch = release + ":" + epoch;
        return name + "-" + version + "-" + releaseepoch + "." + arch + ".rpm";
    }

    private byte[] gzip(byte[] input ) throws IOException {

        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(zipped);
        gzip.write(input);
        gzip.flush();
        gzip.close();
        return zipped.toByteArray();
    }

    /*
     * (non-Javadoc) @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return baseurl;
    }
}
