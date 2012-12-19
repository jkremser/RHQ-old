/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.cloud.FailoverListDetails;
import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.cloud.composite.AffinityGroupCountComposite;
import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.CloudGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.cloud.AffinityGroupManagerLocal;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.cloud.PartitionEventManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jiri Kremser
 */
public class CloudGWTServiceImpl extends AbstractGWTServiceImpl implements CloudGWTService {

    private static final long serialVersionUID = 1L;

    private CloudManagerLocal cloudManager = LookupUtil.getCloudManager();

    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();

    private PartitionEventManagerLocal partitionEventManager = LookupUtil.getPartitionEventManager();

    private AffinityGroupManagerLocal affinityGroupManager = LookupUtil.getAffinityGroupManager();
    
    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    @Override
    public PageList<ServerWithAgentCountComposite> getServers(PageControl pc) throws RuntimeException {
        try {
            return SerialUtility.prepare(cloudManager.getServerComposites(getSessionSubject(), pc),
                "CloudGWTServiceImpl.getServers");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteServers(int[] serverIds) throws RuntimeException {
        try {
            cloudManager.deleteServers(getSessionSubject(), ArrayUtils.toObject(serverIds));
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateServerMode(int[] serverIds, OperationMode mode) throws RuntimeException {
        try {
            cloudManager.updateServerMode(getSessionSubject(), ArrayUtils.toObject(serverIds), mode);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public List<FailoverListDetails> getFailoverListDetailsByAgentId(int agentId, PageControl pc)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                cloudManager.getFailoverListDetailsByAgentId(getSessionSubject(), agentId, pc),
                "CloudGWTServiceImpl.getFailoverListDetailsByAgentId");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<PartitionEvent> findPartitionEventsByCriteria(PartitionEventCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                partitionEventManager.findPartitionEventsByCriteria(getSessionSubject(), criteria),
                "CloudGWTServiceImpl.findPartitionEventsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<Server> findServersByCriteria(ServerCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(cloudManager.findServersByCriteria(getSessionSubject(), criteria),
                "CloudGWTServiceImpl.findServersByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<Agent> findAgentsByCriteria(AgentCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(agentManager.findAgentsByCriteria(getSessionSubject(), criteria),
                "CloudGWTServiceImpl.findAgentsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void cloudPartitionEventRequest() throws RuntimeException {
        try {
            partitionEventManager.cloudPartitionEventRequest(getSessionSubject(),
                PartitionEventType.ADMIN_INITIATED_PARTITION, "");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void purgeAllEvents() throws RuntimeException {
        try {
            partitionEventManager.purgeAllEvents(getSessionSubject());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deletePartitionEvents(int[] eventIds) throws RuntimeException {
        try {
            partitionEventManager.deletePartitionEvents(getSessionSubject(), ArrayUtils.toObject(eventIds));
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<PartitionEventDetails> getPartitionEventDetails(int partitionEventId, PageControl pageControl)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                partitionEventManager.getPartitionEventDetails(getSessionSubject(), partitionEventId, pageControl),
                "CloudGWTServiceImpl.getPartitionEventDetails");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<AffinityGroupCountComposite> getAffinityGroupCountComposites(PageControl pageControl)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(affinityGroupManager.getComposites(getSessionSubject(), pageControl),
                "CloudGWTServiceImpl.getAffinityGroupCountComposites");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int deleteAffinityGroups(int[] affinityGroupIds) throws RuntimeException {
        try {
            return affinityGroupManager.delete(getSessionSubject(), ArrayUtils.toObject(affinityGroupIds));
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int createAffinityGroup(AffinityGroup affinityGroup) throws RuntimeException {
        try {
            return affinityGroupManager.create(getSessionSubject(), affinityGroup);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public AffinityGroup getAffinityGroupById(int affinityGroupId) throws RuntimeException {
        try {
            return SerialUtility.prepare(affinityGroupManager.getById(getSessionSubject(), affinityGroupId),
                "CloudGWTServiceImpl.getAffinityGroupById");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void addServersToGroup(int affinityGroupId, Integer[] serverIds) throws RuntimeException {
        try {
            affinityGroupManager.addServersToGroup(getSessionSubject(), affinityGroupId, serverIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void removeServersFromGroup(Integer[] serverIds) throws RuntimeException {
        try {
            affinityGroupManager.removeServersFromGroup(getSessionSubject(), serverIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void addAgentsToGroup(int affinityGroupId, Integer[] agentIds) throws RuntimeException {
        try {
            affinityGroupManager.addAgentsToGroup(getSessionSubject(), affinityGroupId, agentIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void removeAgentsFromGroup(Integer[] agentIds) throws RuntimeException {
        try {
            affinityGroupManager.removeAgentsFromGroup(getSessionSubject(), agentIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateServer(Server server) throws RuntimeException {
        try {
            cloudManager.updateServer(getSessionSubject(), server);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateAffinityGroup(AffinityGroup affinityGroup) throws RuntimeException {
        try {
            affinityGroupManager.update(getSessionSubject(), affinityGroup);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
    
    @Override
    public void purgeAgentSecurityToken(int agentId) throws RuntimeException {
        try {
            configurationManager.purgeAgentSecurityToken(agentId);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}
