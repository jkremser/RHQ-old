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
package org.rhq.core.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemMap;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.Swap;

import org.rhq.core.system.pquery.ProcessInfoQuery;

/**
 * The superclass for all the native {@link SystemInfo} implementations. You are free to subclass this implementation if
 * there are additional platform-specific methods that need to be exposed. Most functionality, however, can be exposed
 * via this native superclass implementation.
 *
 * <p>This implementation uses SIGAR. To enable debug logging in SIGAR, set the system property "sigar.nativeLogging" or
 * call {@link Sigar#enableLogging(boolean)}.</p>
 *
 * @author John Mazzitelli
 */
public class NativeSystemInfo implements SystemInfo {
    private final Log log = LogFactory.getLog(NativeSystemInfo.class);

    private SigarProxy sigar;

    /**
     * Always returns <code>true</code> to indicate that the native library is available.
     *
     * @see SystemInfo#isNative()
     */
    public boolean isNative() {
        return true;
    }

    public OperatingSystemType getOperatingSystemType() {
        OperatingSystem os = OperatingSystem.getInstance();
        if (OperatingSystem.NAME_LINUX.equals(os.getName())) {
            return OperatingSystemType.LINUX;
        }

        if (OperatingSystem.NAME_SOLARIS.equals(os.getName())) {
            return OperatingSystemType.SOLARIS;
        }

        if (OperatingSystem.NAME_WIN32.equals(os.getName())) {
            return OperatingSystemType.WINDOWS;
        }

        if (OperatingSystem.NAME_HPUX.equals(os.getName())) {
            return OperatingSystemType.HPUX;
        }

        if (OperatingSystem.NAME_AIX.equals(os.getName())) {
            return OperatingSystemType.AIX;
        }

        if (OperatingSystem.NAME_MACOSX.equals(os.getName())) {
            return OperatingSystemType.OSX;
        }

        if (OperatingSystem.NAME_FREEBSD.equals(os.getName())) {
            return OperatingSystemType.BSD;
        }

        return OperatingSystemType.JAVA;
    }

    public String getOperatingSystemName() {
        return OperatingSystem.getInstance().getName();
    }

    public String getOperatingSystemVersion() {
        return OperatingSystem.getInstance().getVersion();
    }

    public String getHostname() throws SystemInfoException {
        try {
            return sigar.getNetInfo().getHostName();
        } catch (Exception e) {
            // For some reason, the native layer failed to get the hostname
            // Let's fallback and ask Java for help. But if that fails, too,
            // let's wrap the native layer's exception since we'll want to
            // see its cause since it'll probably have a more descriptive error message
            try {
                return InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException uhe) {
                throw new SystemInfoException(e);
            }
        }
    }

    public List<NetworkAdapterInfo> getAllNetworkAdapters() throws SystemInfoException {
        ArrayList<NetworkAdapterInfo> adapters = new ArrayList<NetworkAdapterInfo>();

        try {
            String[] interfaceNames = sigar.getNetInterfaceList();

            if (interfaceNames != null) {
                for (String interfaceName : interfaceNames) {
                    if (interfaceName.indexOf(':') != -1) {
                        continue; //filter out virtual IPs
                    }

                    adapters.add(new NetworkAdapterInfo(sigar.getNetInterfaceConfig(interfaceName)));
                }
            }
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }

        return adapters;
    }

    public NetworkAdapterStats getNetworkAdapterStats(String interfaceName) {
        try {
            NetInterfaceStat interfaceStat = sigar.getNetInterfaceStat(interfaceName);
            return new NetworkAdapterStats(interfaceStat);
        } catch (SigarException e) {
            throw new SystemInfoException(e);
        }
    }

    public NetworkStats getNetworkStats(String addressName, int port) {
        try {
            int flags = NetFlags.CONN_SERVER | NetFlags.CONN_CLIENT | NetFlags.CONN_TCP;
            NetConnection[] conns = sigar.getNetConnectionList(flags);

            InetAddress matchAddress = InetAddress.getByName(addressName);

            List<NetConnection> matches = new ArrayList<NetConnection>();
            for (NetConnection conn : conns) {

                InetAddress connAddress = InetAddress.getByName(conn.getLocalAddress());
                if (conn.getLocalPort() == port && matchAddress.equals(connAddress)) {
                    matches.add(conn);
                }
            }

            NetworkStats stats = new NetworkStats(matches.toArray(new NetConnection[matches.size()]));

            return stats;
        } catch (SigarException e) {
            throw new SystemInfoException(e);
        } catch (UnknownHostException e) {
            throw new SystemInfoException(e);
        }
    }

    private List<InetAddress> getInetAddressInList(String address) throws UnknownHostException {
        List<InetAddress> inetAddresses = new ArrayList<InetAddress>();

        if (address != null) {
            inetAddresses.add(InetAddress.getByName(address));
        }

        return inetAddresses;
    }

    public List<ServiceInfo> getAllServices() throws SystemInfoException {
        throw new UnsupportedOperationException("Cannot get services for this plaform");
    }

    public List<ProcessInfo> getAllProcesses() {
        ArrayList<ProcessInfo> processes = new ArrayList<ProcessInfo>();
        long[] pids = null;
        final int timeout = 2 * 60 * 1000; // 2 minutes

        log.debug("Retrieving PIDs of all running processes...");
        long startTime = System.currentTimeMillis();
        try {
            pids = sigar.getProcList();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("Retrieval of " + pids.length + " PIDs took " + elapsedTime + " ms.");
            // NOTE: Do not close sigarImpl on success, as the ProcessInfos created below will reuse it.
        } catch (Exception e) {
            log.warn("Failed to retrieve PIDs of all running processes.", e);
        }

        if (pids != null) {
            for (long pid : pids) {
                if (log.isTraceEnabled()) {
                    log.trace("Loading process info for pid " + pid + "...");
                }
                ProcessInfo info = new ProcessInfo(pid, sigar);
                processes.add(info);
            }
        }

        return processes;
    }

    public List<ProcessInfo> getProcesses(String piq) {
        ProcessInfoQuery piql = new ProcessInfoQuery(getAllProcesses());
        return piql.query(piq);
    }

    public ProcessInfo getThisProcess() {
        long self;

        self = sigar.getPid();

        ProcessInfo info = new ProcessInfo(self);
        return info;
    }

    public ProcessExecutionResults executeProcess(ProcessExecution processExecution) {
        // TODO: doesn't look like SIGAR has an API to fork/execute processes? fallback to using the Java way
        return SystemInfoFactory.createJavaSystemInfo().executeProcess(processExecution);
    }

    public int getNumberOfCpus() {
        try {
            // NOTE: This will return the number of cores, not the number of sockets.
            return sigar.getCpuPercList().length;
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get number of CPUs from native layer", e);
        }
    }

    public Mem getMemoryInfo() {
        try {
            return sigar.getMem();
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get memory info from native layer", e);
        }
    }

    public Swap getSwapInfo() {

        try {
            // Removed this check since http://jira.hyperic.com/browse/SIGAR-112 is fixed.
            /*
            int enabledCpuCount = sigar.getCpuPercList().length;
            int totalCpuCount = sigar.getCpuInfoList().length;
            if (enabledCpuCount < totalCpuCount) {
                log.info("Aborting swap info collection because one or more CPUs is disabled - " + enabledCpuCount
                    + " out of " + totalCpuCount + " CPUs are enabled.");
                return null;
            }
            */
            return sigar.getSwap();
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get swap info from native layer", e);
        }
    }

    public String readLineFromConsole(boolean noEcho) throws IOException {
        String input;

        if (noEcho) {
            input = Sigar.getPassword("");
        } else {
            input = new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        return input;
    }

    public void writeLineToConsole(String line) throws IOException {
        System.out.print(line); // note: don't use println - let the caller append newline char to 'line' if needed
    }

    public CpuInformation getCpu(int cpuIndex) {
        return new CpuInformation(cpuIndex, sigar);
    }

    public List<FileSystemInfo> getFileSystems() {
        List<String> mountPoints = new ArrayList<String>();

        try {
            FileSystemMap map = sigar.getFileSystemMap();
            mountPoints.addAll(map.keySet());
        } catch (Exception e) {
            log.warn("Cannot obtain native file system information", e); // ignore native error otherwise
        }

        List<FileSystemInfo> infos = new ArrayList<FileSystemInfo>();
        for (String mountPoint : mountPoints) {
            infos.add(new FileSystemInfo(mountPoint));
        }

        return infos;
    }

    public FileSystemInfo getFileSystem(String path) {
        String mountPoint = null;

        try {
            FileSystem mountPointForPath = sigar.getFileSystemMap().getMountPoint(path);
            if (mountPointForPath != null)
                mountPoint = mountPointForPath.getDirName();
        } catch (Throwable e) {
            log.warn("Cannot obtain native file system information for [" + path + "]", e); // ignore native error otherwise
        }

        FileSystemInfo fileSystem = new FileSystemInfo(mountPoint);
        return fileSystem;
    }

    public String getSystemArchitecture() {
        OperatingSystem op = OperatingSystem.getInstance();
        return op.getArch();
    }

    /**
     * Constructor for {@link NativeSystemInfo} with package scope so only the {@link SystemInfoFactory} can instantiate
     * this object.
     */
    public NativeSystemInfo() {
        this.sigar = SigarAccess.getSigar();
    }
}