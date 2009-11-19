package org.rhq.enterprise.server.install.remote;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;

/**
 * @author Greg Hinkle
 */
@Local
public interface RemoteInstallManagerLocal {


    AgentInstallInfo agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo);

    void installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String path);

    String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath);

    void startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);

    void stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);


}
