package org.rhq.enterprise.server.install.remote;

import javax.ejb.Local;
import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;

/**
 * @author Greg Hinkle
 */
@Remote
public interface RemoteInstallManagerRemote {


    AgentInstallInfo agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo);

    void installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String path);

    String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath);

    void startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);

    void stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);


}