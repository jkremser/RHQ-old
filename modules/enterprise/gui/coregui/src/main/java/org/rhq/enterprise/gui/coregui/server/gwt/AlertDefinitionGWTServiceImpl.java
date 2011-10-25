/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertDefinitionGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AlertDefinitionGWTServiceImpl extends AbstractGWTServiceImpl implements AlertDefinitionGWTService {
    private static final long serialVersionUID = 1L;

    private AlertDefinitionManagerLocal alertDefManager = LookupUtil.getAlertDefinitionManager();
    private AlertNotificationManagerLocal alertNotifManager = LookupUtil.getAlertNotificationManager();

    @Override
    public PageList<AlertDefinition> findAlertDefinitionsByCriteria(AlertDefinitionCriteria criteria)
        throws RuntimeException {
        try {
            PageList<AlertDefinition> results = this.alertDefManager.findAlertDefinitionsByCriteria(
                getSessionSubject(), criteria);
            if (!results.isEmpty()) {
                List<Resource> resources = new ArrayList<Resource>(results.size());
                for (AlertDefinition alertDefinition : results) {
                    Resource res = alertDefinition.getResource();
                    if (null != res) {
                        resources.add(res);
                    }
                }
                ObjectFilter.filterFieldsInCollection(resources, ResourceGWTServiceImpl.importantFieldsSet);
            }

            return SerialUtility.prepare(results, "findAlertDefinitionsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int createAlertDefinition(AlertDefinition alertDefinition, Integer resourceId) throws RuntimeException {
        try {
            int results = alertDefManager.createAlertDefinition(getSessionSubject(), alertDefinition, resourceId);
            return results;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public AlertDefinition updateAlertDefinition(int alertDefinitionId, AlertDefinition alertDefinition,
        boolean updateInternals) throws RuntimeException {
        try {
            AlertDefinition results = alertDefManager.updateAlertDefinition(getSessionSubject(), alertDefinitionId,
                alertDefinition, updateInternals);
            return SerialUtility.prepare(results, "updateAlertDefinition");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int enableAlertDefinitions(int[] alertDefinitionIds) throws RuntimeException {
        try {
            int results = alertDefManager.enableAlertDefinitions(getSessionSubject(), alertDefinitionIds);
            return results;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int disableAlertDefinitions(int[] alertDefinitionIds) throws RuntimeException {
        try {
            int results = alertDefManager.disableAlertDefinitions(getSessionSubject(), alertDefinitionIds);
            return results;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int removeAlertDefinitions(int[] alertDefinitionIds) throws RuntimeException {
        try {
            int results = alertDefManager.removeAlertDefinitions(getSessionSubject(), alertDefinitionIds);
            return results;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public String[] getAlertNotificationConfigurationPreview(AlertNotification[] notifs) throws RuntimeException {
        try {
            String[] results = alertDefManager.getAlertNotificationConfigurationPreview(getSessionSubject(), notifs);
            return SerialUtility.prepare(results, "getAlertNotificationConfigurationPreview");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public String[] getAllAlertSenders() throws RuntimeException {
        try {
            List<String> results = alertNotifManager.listAllAlertSenders();
            if (results == null) {
                return null;
            }
            return SerialUtility.prepare(results.toArray(new String[results.size()]), "getAllAlertSenders");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ConfigurationDefinition getConfigurationDefinitionForSender(String sender) throws RuntimeException {
        try {
            ConfigurationDefinition results = alertNotifManager.getConfigurationDefinitionForSender(sender);
            return SerialUtility.prepare(results, "getConfigurationDefinitionForSender");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}