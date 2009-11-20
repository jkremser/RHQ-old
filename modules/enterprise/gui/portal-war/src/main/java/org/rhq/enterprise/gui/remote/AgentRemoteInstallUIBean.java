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
package org.rhq.enterprise.gui.remote;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.EnterpriseFacesContextUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.install.remote.AgentInstallInfo;
import org.rhq.enterprise.server.install.remote.RemoteAccessInfo;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerBean;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.faces.application.FacesMessage;

/**
 * @author Greg Hinkle
 */
public class AgentRemoteInstallUIBean extends EnterpriseFacesContextUIBean {

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private RemoteInstallManagerLocal remoteInstallManager = LookupUtil.getRemoteInstallManager();
    private RemoteAccessInfo targetAccessInfo = new RemoteAccessInfo();

    private String agentStatus;
    private AgentInstallInfo agentInstallInfo;
    private String result;
    private AvailabilityType agentAvailability;

    public RemoteAccessInfo getTargetAccessInfo() {
        return targetAccessInfo;
    }

    public void setTargetAccessInfo(RemoteAccessInfo targetAccessInfo) {
        this.targetAccessInfo = targetAccessInfo;
    }


    public String getAgentStatus() {
        return agentStatus;
    }

    public String getResult() {
        return result;
    }

    public AgentInstallInfo getAgentInstallInfo() {
        return agentInstallInfo;
    }

    public AvailabilityType getAgentAvailability() {
        return agentAvailability;
    }

    public void checkAgentStatus(javax.faces.event.ActionEvent event) {
        try {
            agentStatus = remoteInstallManager.agentStatus(EnterpriseFacesContextUtility.getSubject(), targetAccessInfo);
            agentAvailability = agentStatus.contains("NOT") ? AvailabilityType.DOWN : AvailabilityType.UP;
        } catch (RuntimeException e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), e);
            agentStatus = null;
            agentAvailability = null;
        }
    }

    public void startAgent(javax.faces.event.ActionEvent event) {
        clear();
        result = "Agent Start Result: " + remoteInstallManager.startAgent(
                EnterpriseFacesContextUtility.getSubject(), targetAccessInfo);
        checkAgentStatus(null);
    }

    public void stopAgent(javax.faces.event.ActionEvent event) {
        clear();
        result = "Agent Stop Result: " + remoteInstallManager.stopAgent(
                EnterpriseFacesContextUtility.getSubject(), targetAccessInfo);
        checkAgentStatus(null);
    }

    public void installAgent(javax.faces.event.ActionEvent event) {
        clear();
        agentInstallInfo = remoteInstallManager.installAgent(getSubject(), targetAccessInfo, "/tmp/rhqAgent");
        checkAgentStatus(null);
    }

    private void clear() {
        agentStatus = null;
        agentAvailability = null;
        agentInstallInfo = null;
        result = null;
    }
}
