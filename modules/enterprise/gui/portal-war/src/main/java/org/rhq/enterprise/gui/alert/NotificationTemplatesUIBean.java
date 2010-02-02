/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;

import org.richfaces.model.selection.Selection;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.NotificationTemplate;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for NotificationTemplates for Alert Sender Plugins configuration
 *
 * @author jharris
 * @author Heiko Rupp
 */
@Scope(ScopeType.PAGE)
@Name("notificationTemplateUIBean")
@SuppressWarnings("unused")
public class NotificationTemplatesUIBean implements Serializable {

    private final static String CONTINUE_OUTCOME = "continue";
    private final static String NEW_OUTCOME = "new";
    @RequestParameter("nid")
    private Integer notificationId;
    @RequestParameter("tid")
    private Integer templateId;
    private String opMode;
    private List<AlertNotification> alertNotifications;
    private Set<AlertNotification> selectedNotifications;
    private String newAlertName;
    private String newTemplateName;
    private String newTemplateDescription;
    private String selectedNewSender;
    private Selection selectedTemplates;
    private Boolean clearExistingNotifications;
    private AlertNotification activeNotification;
    private NotificationTemplate activeTemplate;
    private ConfigurationDefinition activeConfigDefinition;
    private AlertNotificationConverter notificationConverter;
    private AlertNotificationManagerLocal alertNotificationManager;
    private static final String EDIT = "EDIT";
    private List<NotificationTemplate> notificationTemplates;
    NotificationTemplate selectedTemplate;

    public List<AlertNotification> getAlertNotifications() {
        return alertNotifications;
    }

    public void setAlertNotifications(List<AlertNotification> alertNotifications) {
        this.alertNotifications = alertNotifications;
    }

    public Set<AlertNotification> getSelectedNotifications() {
        return selectedNotifications;
    }

    public void setSelectedNotifications(Set<AlertNotification> selectedNotifications) {
        this.selectedNotifications = selectedNotifications;
    }

    public String getNewAlertName() {
        return newAlertName;
    }

    public void setNewAlertName(String newAlertName) {
        this.newAlertName = newAlertName;
    }

    public String getSelectedNewSender() {
        return selectedNewSender;
    }

    public void setSelectedNewSender(String selectedNewSender) {
        this.selectedNewSender = selectedNewSender;
    }

    public Selection getSelectedTemplates() {
        return selectedTemplates;
    }

    public void setSelectedTemplates(Selection selectedTemplates) {
        this.selectedTemplates = selectedTemplates;
        System.out.println("Set selected templates " + selectedTemplates.toString());
    }

    public Boolean getClearExistingNotifications() {
        return clearExistingNotifications;
    }

    public void setClearExistingNotifications(Boolean clearExistingNotifications) {
        this.clearExistingNotifications = clearExistingNotifications;
    }

    public AlertNotification getActiveNotification() {
        return activeNotification;
    }

    public void setActiveNotification(AlertNotification activeNotification) {
        System.out.println("setActiveNotification: " + activeNotification);
        this.activeNotification = activeNotification;

        lookupActiveConfigDefinition();
    }

    public NotificationTemplate getActiveTemplate() {
        return activeTemplate;
    }

    public void setActiveTemplate(NotificationTemplate activeTemplate) {
        this.activeTemplate = activeTemplate;
    }

    public ConfigurationDefinition getActiveConfigDefinition() {
        return activeConfigDefinition;
    }

    public AlertNotificationConverter getNotificationConverter() {
        return notificationConverter;
    }

    public String getNewTemplateName() {
        return newTemplateName;
    }

    public void setNewTemplateName(String newTemplateName) {
        this.newTemplateName = newTemplateName;
    }

    public String getNewTemplateDescription() {
        return newTemplateDescription;
    }

    public void setNewTemplateDescription(String newTemplateDescription) {
        this.newTemplateDescription = newTemplateDescription;
    }

    public String getOpMode() {
        return opMode;
    }

    public void setOpMode(String opMode) {
        this.opMode = opMode;
    }

    public NotificationTemplate getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(NotificationTemplate selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public List<NotificationTemplate> getNotificationTemplates() {
        return this.notificationTemplates;
    }

    private void lookupActiveConfigDefinition() {
        System.out.println("lookupActiveConfigDef: activeNotification is " + activeNotification);

        if (this.activeNotification != null) {
            String senderName = this.activeNotification.getSenderName();
            this.activeConfigDefinition = this.alertNotificationManager.getConfigurationDefinitionForSender(senderName);
        }
    }

    @Create
    public void initNotifications() {
        this.alertNotificationManager = LookupUtil.getAlertNotificationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();

//        this.alertNotifications = this.alertNotificationManager.getNotificationsForAlertDefinition(subject, alertDefinitionId)
        this.alertNotifications = new ArrayList<AlertNotification>(); // TODO ?
        this.selectedNotifications = new HashSet<AlertNotification>();
        this.notificationConverter = new AlertNotificationConverter();
        this.notificationConverter.setAlertNotifications(alertNotifications);
        this.notificationTemplates = alertNotificationManager.listNotificationTemplates(EnterpriseFacesContextUtility.getSubject());

        selectActiveTemplate();
        selectActiveNotification();
    }

    private void selectActiveTemplate() {
        if (this.templateId != null) {
            for (NotificationTemplate template : this.notificationTemplates) {
                if (template.getId() == this.templateId) {
                    selectTemplate(template);

                    return;
                }
            }
        }

        opMode = "";
    }

    // Sets the initial state of the bean given the requrest parameters, this allows
    // us to maintain the selected item across requests.
    private void selectActiveNotification() {
        if (this.notificationId != null) {
            for (AlertNotification notification : this.alertNotifications) {
                if (notification.getId() == this.notificationId) {
                    setActiveNotification(notification);
                    this.selectedNotifications.add(notification);

                    return;
                }
            }
        }
    }

    public Map<String, String> getAllAlertSenders() {
        Map<String, String> result = new TreeMap<String, String>();

        for (String sender : this.alertNotificationManager.listAllAlertSenders()) {
            result.put(sender, sender);
        }

        return result;
    }

    public String addAlertSender() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        Configuration newSenderConfig = null;
        ConfigurationDefinition configDefinition = this.alertNotificationManager.getConfigurationDefinitionForSender(selectedNewSender);

        if (configDefinition != null) {
            newSenderConfig = configDefinition.getDefaultTemplate().createConfiguration();
        } else {
            newSenderConfig = new Configuration();
        }

        try {
            this.activeNotification = alertNotificationManager.addAlertNotificationToTemplate(subject,selectedTemplate.getName(),selectedNewSender,newAlertName,newSenderConfig);
            System.out.println("addAlertSender: activeNotification is " + activeNotification);
            alertNotifications = alertNotificationManager.getNotificationsForTemplate(subject,selectedTemplate.getId());
            this.notificationConverter.setAlertNotifications(alertNotifications);
            selectedNotifications.clear();
            selectedNotifications.add(activeNotification);
        }
        catch (IllegalArgumentException iae) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,"Adding the sender failed: " + iae.getMessage() );
        }

        return CONTINUE_OUTCOME;
    }

    public String saveConfiguration() {
        System.out.println("saveConfiguration: activeNotification is " + activeNotification);

        if (this.activeNotification != null) {
            this.alertNotificationManager.updateAlertNotification(this.activeNotification);
        }

        return CONTINUE_OUTCOME;
    }

    public String removeSelected() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        List<Integer> ids = getSelectedIds(this.selectedNotifications);
        System.out.println("removeSelected: " + ids);

        alertNotificationManager.removeNotificationsFromTemplate(subject, selectedTemplate.getId(), toArray(ids));

        return CONTINUE_OUTCOME;
    }

    public String saveOrder() {
        int orderIndex = 0;

        for (AlertNotification notification : this.alertNotifications) {
            notification.setOrder(orderIndex++);

            // TODO:  Wait and persist these all in a single operation?
            this.alertNotificationManager.updateAlertNotification(notification);
        }

        return CONTINUE_OUTCOME;
    }

    private List<Integer> getSelectedIds(Collection<AlertNotification> notifications) {
        List<Integer> ids = new ArrayList<Integer>(notifications.size());
        for (AlertNotification notification : notifications) {
            ids.add(notification.getId());
        }

        return ids;
    }

    private Integer[] toArray(List<Integer> intList) {
        return intList.toArray(new Integer[intList.size()]);
    }

    public Map<String, String> getAllNotificationTemplates() {
        Map<String, String> result = new TreeMap<String, String>();

        List<NotificationTemplate> templates = alertNotificationManager.listNotificationTemplates(EnterpriseFacesContextUtility.getSubject());

        for (NotificationTemplate nt : templates) {
            String tmp = nt.getName() + " (" + nt.getDescription() + ")";
            result.put(tmp,nt.getName()); // displayed text, option value
        }
        return result;
    }

    public String createNotificationTemplate() {

        NotificationTemplate templ;

        try {
            templ = alertNotificationManager.createNotificationTemplate(newTemplateName,newTemplateDescription,new ArrayList<AlertNotification>());

            selectedTemplate = templ;
            alertNotifications = selectedTemplate.getNotifications();
        } catch (IllegalArgumentException iae) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,"Creation of template failed: " + iae.getMessage());
        }

        return NEW_OUTCOME;
    }

    public String deleteNotificationTemplate() {

        List<Integer> ids = new ArrayList<Integer>(selectedTemplates.size());

        Iterator<Object> iter = selectedTemplates.getKeys();
        while (iter.hasNext()) {
            Integer row = (Integer) iter.next();
            NotificationTemplate templ = notificationTemplates.get(row);
            ids.add(templ.getId());
        }


        int num = alertNotificationManager.deleteNotificationTemplates(EnterpriseFacesContextUtility.getSubject(),
                ids.toArray(new Integer[ids.size()]));

        String summary = "Deleted " + num + " templates";
        if (num!=1)
            summary+="s";
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, summary);

        return NEW_OUTCOME;
    }

    public void editNotificationTemplate() {

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        System.out.println("in edit Template");
        Iterator<Object> iter = selectedTemplates.getKeys();
        if (selectedTemplates.size()==1) {
            Integer row = (Integer) iter.next();
            selectTemplate(notificationTemplates.get(row));
        }
        else {
            opMode = "";
        }
    }

    private void selectTemplate(NotificationTemplate template) {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        selectedTemplate = template;
        alertNotifications = alertNotificationManager.getNotificationsForTemplate(subject, selectedTemplate.getId());
        this.notificationConverter.setAlertNotifications(alertNotifications);
        System.out.println("Selected template is " + selectedTemplate);
        opMode = EDIT;
    }
}
