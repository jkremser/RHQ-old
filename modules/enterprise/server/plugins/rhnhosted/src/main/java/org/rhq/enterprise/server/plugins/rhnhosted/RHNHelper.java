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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugin.pc.content.AdvisoryBugDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryCVEDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetailsKey;
import org.rhq.enterprise.server.plugin.pc.content.DistributionDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionFileDetails;
import org.rhq.enterprise.server.plugin.pc.content.SyncTracker;
import org.rhq.enterprise.server.plugin.pc.content.ThreadUtil;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnErratumBugType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnErratumType;
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

    //
    // Used to speed up Advisory Synchs
    // This is a hashmap with first index being the RHN package ID to AdvisoryPackageDetails
    // Example of a RHN package ID is "rhn-package-405014"
    // Background:  Hosted uses the rhn package id and we need to translate this to the package
    // info which has been stored in RHQ.
    //
    static protected HashMap<String, AdvisoryPackageDetails> cacheAdvPkgDetails;

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
        if (cacheAdvPkgDetails == null) {
            cacheAdvPkgDetails = new HashMap<String, AdvisoryPackageDetails>();
        }
    }

    public boolean checkSystemId(String systemId) throws IOException, XmlRpcException {
        return this.rhndata.checkSystemId(systemId);
    }

    public List<DistributionDetails> getDistributionMetaData(List<String> labels, SyncTracker tracker)
        throws IOException, XmlRpcException {
        log.debug("getDistributionMetaData(" + labels + ") invoked");

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

    public List<AdvisoryDetails> getAdvisoryMetadata(List<String> advisoryList, String repoName, SyncTracker tracker)
        throws XmlRpcException, IOException, InterruptedException {

        List<AdvisoryDetails> erratadetails = new ArrayList<AdvisoryDetails>();
        // ADD WORK
        tracker.addWork(advisoryList.size());
        tracker.persistResults();
        int counter = 0;
        for (int i = 0; i < advisoryList.size(); i++) {
            String[] advisory = { advisoryList.get(i) };
            List<RhnErratumType> errata = rhndata.getErrataMetadata(this.systemid, Arrays.asList(advisory));
            log.info("getAdvisoryMetadata() processing [" + counter + "/" + advisoryList.size() + "] advisory");
            for (RhnErratumType erratum : errata) {
                ThreadUtil.checkInterrupted();
                if (log.isDebugEnabled()) {
                    log.debug("[" + counter + "/" + advisoryList.size() + "] Forming AdvisoryDetails("
                        + erratum.getAdvisory() + ")");
                }
                AdvisoryDetails details = new AdvisoryDetails(erratum.getAdvisory(), erratum
                    .getRhnErratumAdvisoryType(), erratum.getRhnErratumSynopsis());
                details.setDescription(erratum.getRhnErratumDescription());
                details.setSolution(erratum.getRhnErratumSolution());
                details.setTopic(erratum.getRhnErratumTopic());
                details.setIssue_date(getLongForDate(erratum.getRhnErratumIssueDate()));
                details.setUpdate_date(getLongForDate(erratum.getRhnErratumUpdateDate()));
                details.setAdvisory_name(erratum.getRhnErratumAdvisoryName());
                details.setAdvisory_rel(erratum.getRhnErratumAdvisoryRel());
                String cvestr = erratum.getCveNames();
                String[] cves = cvestr.split(" ");
                if (log.isDebugEnabled()) {
                    log.debug("AdvisoryDetails = " + details);
                    log.debug("list of cves " + cvestr + cves.toString());
                    log.debug("[" + counter + "/" + advisoryList.size() + "] AdvisoryDetails = " + details);
                }
                //
                // CVES RELATED TO ERRATA
                //
                for (String cve : cves) {
                    if (log.isDebugEnabled()) {
                        log.debug("[" + counter + "/" + advisoryList.size()
                            + "] RHNHelper::getAdvisoryMetaData<Advisory=" + erratum.getAdvisory() + "> CVEs<" + cve
                            + ">");
                    }
                    AdvisoryCVEDetails acve = new AdvisoryCVEDetails(cve);
                    details.addCVE(acve);
                }
                //
                // BUGS RELATED TO ERRATA
                //
                List<RhnErratumBugType> ebugs = erratum.getRhnErratumBugs().getRhnErratumBug();
                if (ebugs != null) {
                    for (RhnErratumBugType ebug : ebugs) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + counter + "/" + advisoryList.size()
                                + "] RHNHelper::getAdvisoryMetaData<Advisory=" + erratum.getAdvisory() + "> Bugs<"
                                + ebug + ">");
                        }
                        AdvisoryBugDetails dbug = new AdvisoryBugDetails(ebug.getRhnErratumBugId());
                        details.addBug(dbug);
                    }
                }
                //
                // PACKAGES RELATED TO ERRATA
                //
                // Find out what packages are associated to this errata
                // Background info:  RHN tells us package ids as rhn-package-45839
                // We don't store this info on RHQ...so our problem is how do we map RHN package ID to the
                // name-epoch:version-release.arch format.
                //
                // We need the info we get from getPackageMetadata, we pass in the RHN package ID and it comes back
                // with name,version,release, etc info about the package.
                //
                // To speed things up we are caching this info when we do a package metadata sync, we cache the info
                // in a lookup map held in this class's memory, if that lookup fails then we default to talking to RHN
                //
                // Note: that while we may be synching saying rhel-i386-server-5, an Errata will span more packages than
                // just what is in the repo.  Therefore the Errata may include packages of all arches, including those
                // that we may not be "entitled" to see...hence "Package not found" errors are possible and likely during
                // this operation.  We will skip those errors, since they are for packages we don't care about anyway.
                //
                String pkgs = erratum.getPackages();
                List<AdvisoryPackageDetails> apkgdetails = new ArrayList<AdvisoryPackageDetails>();
                if (log.isDebugEnabled()) {
                    log.debug("getAdvisoryMetadata() [" + counter + "/" + advisoryList.size() + "] :  pkgs.length() = "
                        + pkgs.length() + " = " + pkgs);
                }
                String[] pkgIds = pkgs.split(" ");
                for (String packageId : pkgIds) {
                    List<String> tempList = new ArrayList<String>();
                    tempList.add(packageId);
                    // See if we already have cached data on this rhn package id
                    AdvisoryPackageDetails cachedDetails = lookupAdvisoryPackageDetails(packageId);
                    if (cachedDetails != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + counter + "/" + advisoryList.size()
                                + "] Lookup cached AdvisoryPackageDetails<" + cachedDetails.getRpmFilename()
                                + "> object for rhn pkg id: " + packageId);
                        }
                        apkgdetails.add(cachedDetails);
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("[" + counter + "/" + advisoryList.size() + "] Unable to fetch " + packageId
                            + " from cache, will talk to RHN to retrieve data.");
                    }
                    //
                    //  We are seeing some package ids which fail on lookup to Hosted.
                    //  We believe this might be related to the Certificate used to create the Content Provider
                    //  not having access to get package metadata for packages outside of it's allowed channels.
                    //  Adding a try/catch to ignore the XmlRpcException
                    //  ...we think those packages are not ones we are interested in anyway....so we are adding a
                    //  try/catch here to ignore the XmlRpcException and continue with the next package.
                    //
                    try {
                        List<RhnPackageType> pkgdetails = rhndata.getPackageMetadata(this.systemid, tempList, 0);
                        for (RhnPackageType pkgd : pkgdetails) {
                            if (log.isDebugEnabled()) {
                                log.debug("RHNHelper::getAdvisoryMetaData<Advisory=" + erratum.getAdvisory()
                                    + "> Package<" + pkgd + ">: " + tempList);
                            }
                            String name = pkgd.getName();
                            String epoch = pkgd.getEpoch();
                            String version = pkgd.getVersion();
                            String arch = pkgd.getPackageArch();
                            String release = pkgd.getRelease();
                            String rpmname = constructRpmDisplayName(name, version, release, epoch, arch);
                            AdvisoryPackageDetails apkgd = new AdvisoryPackageDetails(name, version, arch, rpmname);
                            apkgdetails.add(apkgd);
                            // Add to cache
                            cacheAdvisoryPackageDetails(packageId, apkgd);
                            log.debug("Fetched info for " + packageId + " = " + rpmname);
                        }
                    } catch (XmlRpcException e) {
                        if (!e.getMessage().contains("No such package")) {
                            // re-throw exception, it's something other than the one we wanted to ignore
                            log.info(e.getMessage());
                            log.warn(e);
                            throw e;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("We shall continue and not include this package " + packageId
                                + " with advisory info since"
                                + " RHN Hosted reported 'No such package' for our credentials.");
                        }
                    }
                }
                details.addPkgs(apkgdetails);
                erratadetails.add(details);
            }
            counter++;
            log.debug("RHNHelper gathered data for Errata: [" + counter + "/" + advisoryList.size() + "]");
            tracker.finishWork(1);
            tracker.persistResults();
        }
        return erratadetails;
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
        int skippedPackages = 0;
        int pkgNoName = 0;
        List<ContentProviderPackageDetails> pdlist = new ArrayList<ContentProviderPackageDetails>();
        List<RhnPackageType> pkgs = rhndata.getPackageMetadata(this.systemid, packageIds);
        log.debug(pkgs.size() + " packages were returned from getPackageMetadata().");
        for (RhnPackageType pkg : pkgs) {
            try {
                ContentProviderPackageDetails details = getDetails(pkg, channelName);
                pdlist.add(details);
                cacheAdvisoryPackageDetails(pkg.getId(), pkg);
            } catch (Exception e) {
                if (pkg != null) {
                    if (StringUtils.isBlank(pkg.getName())) {
                        log.debug("getPackageDetails skipping package with no name.");
                        pkgNoName++;
                        continue;
                    }
                }
                log.warn("Caught exception with getDetails() of package: " + pkg.getName() + " : " + e);
                log.warn("Package:  id = " + pkg.getId() + " getSourceRpm() = " + pkg.getSourceRpm()
                    + ", getDescription() = " + pkg.getRhnPackageDescription());
                // something went wrong while constructing the pkg object.
                // Proceed to next and get as many packages as we can.
                skippedPackages++;
                continue;
            }
        }
        log.debug("We skipped: " + skippedPackages + " packages. " + "We also skipped " + pkgNoName
            + " packages because they had no name");
        log.debug("getPackageDetails was called with a list of package ids size = " + packageIds.size()
            + " we have fetched metadata for " + pdlist.size() + " packages");
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
        String release = p.getRelease();
        String epoch = p.getEpoch();
        String arch = p.getPackageArch();
        String downloadName = constructRpmDownloadName(name, version, release, epoch, arch);
        String displayName = constructRpmDisplayName(name, version, release, epoch, arch);
        //
        // Release and epoch definition is added to Package domain model, so we do not need to append release and epoch
        // information to version string anymore. 

        ContentProviderPackageDetailsKey key = new ContentProviderPackageDetailsKey(name, version, release, epoch,
            "rpm", arch, "Linux", "Platforms");
        ContentProviderPackageDetails pkg = new ContentProviderPackageDetails(key);

        pkg.setDisplayName(name);
        pkg.setShortDescription(p.getRhnPackageSummary());
        pkg.setLongDescription(p.getRhnPackageDescription());
        pkg.setFileName(displayName);
        pkg.setFileSize(new Long(p.getPackageSize()));
        pkg.setFileCreatedDate(new Long(p.getLastModified()));
        pkg.setLicenseName("license");
        pkg.setMD5(p.getMd5Sum());
        pkg.setLocation(constructPackageUrl(channelName, downloadName));

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
     * Get List of errataIds for Given Channels
     * @param channelName channel name
     * @return List of all errata ids associated to the channel
     * @throws IOException  on io errors on systemid reads
     * @throws XmlRpcException on xmlrpc faults
     */
    public List<String> getChannelAdvisory(String channelName) throws IOException, XmlRpcException {
        log.debug("getChannelAdvisory(" + channelName + ")");
        ArrayList<String> allAdvisory = new ArrayList();
        List<RhnChannelType> channels = rhndata.getChannels(this.systemid, Arrays.asList(channelName));

        for (RhnChannelType channel : channels) {
            String errata = channel.getChannelErrata();
            String[] errataIds = errata.split(" ");
            if (errataIds.length > 1) {
                allAdvisory.addAll(Arrays.asList(errataIds));
            }
        }

        return allAdvisory;
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
    static public String constructRpmDownloadName(String name, String version, String release, String epoch, String arch) {

        String releaseepoch = release + ":" + epoch;
        return name + "-" + version + "-" + releaseepoch + "." + arch + ".rpm";
    }

    /**
     * construct a legitimate rpm name to display
     * @param name
     * @param version
     * @param release
     * @param epoch
     * @param arch
     * @return rpm name String
     */
    static public String constructRpmDisplayName(String name, String version, String release, String epoch, String arch) {
        String val = name;
        if (!StringUtils.isEmpty(epoch)) {
            val = val + "-" + epoch + ":";
        }
        return val + version + "-" + release + "." + arch + ".rpm";
    }

    private byte[] gzip(byte[] input) throws IOException {

        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(zipped);
        gzip.write(input);
        gzip.flush();
        gzip.close();
        return zipped.toByteArray();
    }

    private long getLongForDate(String dateIn) {
        return Long.parseLong(dateIn);
    }

    /*
     * (non-Javadoc) @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return baseurl;
    }

    /**
     * If a cache of AdvisoryPackageDetails objects exists it is cleared.
     */
    public void clearCacheAdvisoryPackageDetails() {
        if (cacheAdvPkgDetails == null) {
            return;
        }
        cacheAdvPkgDetails.clear();
    }

    protected AdvisoryPackageDetails lookupAdvisoryPackageDetails(String rhnPkgId) {

        if (cacheAdvPkgDetails == null) {
            return null;
        }
        if (cacheAdvPkgDetails.containsKey(rhnPkgId)) {
            return cacheAdvPkgDetails.get(rhnPkgId);
        }
        return null;
    }

    protected void cacheAdvisoryPackageDetails(String rhnPkgId, AdvisoryPackageDetails pkgDetails) {
        if (cacheAdvPkgDetails == null) {
            cacheAdvPkgDetails = new HashMap<String, AdvisoryPackageDetails>();
        }
        if (cacheAdvPkgDetails.containsKey(rhnPkgId)) {
            AdvisoryPackageDetails existing = cacheAdvPkgDetails.get(rhnPkgId);
            if (!existing.equals(pkgDetails)) {
                log.warn("cacheToAdvisoryDetails(" + rhnPkgId + ", " + pkgDetails
                    + ") packages differ from what's cached.");
                log.warn("Existing AdvisoryDetailsPackage in cache = " + existing);
                log.warn("New AdvisoryDetailsPackage to add to cache is " + pkgDetails);
            }
        }
        // Will override rhnPkgId if it was already in cache
        cacheAdvPkgDetails.put(rhnPkgId, pkgDetails);
    }

    protected void cacheAdvisoryPackageDetails(String rhnPkgId, RhnPackageType pType) {

        String name = pType.getName();
        String epoch = pType.getEpoch();
        String version = pType.getVersion();
        String arch = pType.getPackageArch();
        String release = pType.getRelease();
        String rpmname = constructRpmDisplayName(name, version, release, epoch, arch);
        AdvisoryPackageDetails apkgd = new AdvisoryPackageDetails(name, version, arch, rpmname);
        cacheAdvisoryPackageDetails(rhnPkgId, apkgd);
    }
}
