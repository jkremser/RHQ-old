package org.rhq.enterprise.server.install.remote;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;

/**
 * @author Greg Hinkle
 */
@Local
public interface RemoteInstallManagerLocal {


    AgentInstallInfo agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo);

    AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String path);

    String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath);

    String startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);

    String stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);

    String agentStatus(Subject subject, RemoteAccessInfo remoteAccessInfo);

}
