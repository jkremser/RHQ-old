/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.admin.topology;

import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_ADDRESS;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_AGENT_TOKEN;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_LAST_AVAILABILITY_REPORT;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_NAME;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.IconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.IconClickHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * Shows details of a server.
 * 
 * @author Jirka Kremser
 */
public class AgentDetailView extends LocatableVLayout {

    private final int agentId;

    private static final int SECTION_COUNT = 2;
    private final LocatableSectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection failoverListSection = null;

    private volatile int initSectionCount = 0;

    public AgentDetailView(String locatorId, int agentId) {
        super(locatorId);
        this.agentId = agentId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new LocatableSectionStack(extendLocatorId("stack"));
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onInit() {
        super.onInit();
        AgentCriteria criteria = new AgentCriteria();
        criteria.addFilterId(agentId);
        GWTServiceLookup.getCloudService().findAgentsByCriteria(criteria, new AsyncCallback<PageList<Agent>>() {
            public void onSuccess(final PageList<Agent> agents) {
                if (agents == null || agents.isEmpty() || agents.size() != 1) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchAgentFail(String.valueOf(agentId)));
                    initSectionCount = SECTION_COUNT;
                    return;
                }
                prepareDetailsSection(sectionStack, agents.get(0));
                prepareFailoverListSection(sectionStack, agents.get(0));
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_message_fetchAgentFail(String.valueOf(agentId)) + " " + caught.getMessage(),
                    caught);
                initSectionCount = SECTION_COUNT;
            }
        });
    }

    public boolean isInitialized() {
        return initSectionCount >= SECTION_COUNT;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != failoverListSection) {
                        sectionStack.addSection(failoverListSection);
                    }

                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = SECTION_COUNT;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareFailoverListSection(SectionStack stack, Agent agent) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_agentDetail_agentFailoverList());
        section.setExpanded(true);
        ServerTableView agentsTable = new ServerTableView(extendLocatorId(ServerTableView.VIEW_ID.getName()),
            agent.getId(), false);
        section.setItems(agentsTable);

        failoverListSection = section;
        ++initSectionCount;
        return;
    }

    private void prepareDetailsSection(SectionStack stack, Agent agent) {
        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("detailsForm"));
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        StaticTextItem nameItem = new StaticTextItem(FIELD_NAME.propertyName(), FIELD_NAME.title());
        nameItem.setValue("<b>" + agent.getName() + "</b>");

        StaticTextItem addressItem = new StaticTextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
        addressItem.setValue(agent.getAddress());

        StaticTextItem portItem = new StaticTextItem(FIELD_PORT.propertyName(), FIELD_PORT.title());
        portItem.setValue(agent.getPort());

        FormItemIcon removeTokenIcon = new FormItemIcon();
        removeTokenIcon.setSrc("[SKIN]/actions/remove.png");
        final StaticTextItem tokenItem = new StaticTextItem(FIELD_AGENT_TOKEN.propertyName(), FIELD_AGENT_TOKEN.title());
        tokenItem.setValue(agent.getAgentToken());
        tokenItem.setIcons(removeTokenIcon);
        tokenItem.addIconClickHandler(new IconClickHandler() {
            public void onIconClick(IconClickEvent event) {
                removeToken(tokenItem);
            }
        });

        StaticTextItem lastAvailabilityItem = new StaticTextItem(FIELD_LAST_AVAILABILITY_REPORT.propertyName(),
            FIELD_LAST_AVAILABILITY_REPORT.title());
        String lastReport = agent.getLastAvailabilityReport() == null ? "unknown" : TimestampCellFormatter.format(
            Long.valueOf(agent.getLastAvailabilityReport()), TimestampCellFormatter.DATE_TIME_FORMAT_LONG);
        lastAvailabilityItem.setValue(lastReport);

        // make clickable link for affinity group
        StaticTextItem affinityGroupItem = new StaticTextItem(FIELD_AFFINITY_GROUP.propertyName(),
            FIELD_AFFINITY_GROUP.title());
        String affinityGroupItemText = "";
        AffinityGroup ag = agent.getAffinityGroup();
        if (ag != null && ag.getName() != null && !ag.getName().isEmpty()) {
            String detailsUrl = "#" + AffinityGroupTableView.VIEW_PATH + "/" + ag.getId();
            String formattedValue = StringUtility.escapeHtml(ag.getName());
            affinityGroupItemText = SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);
        }
        affinityGroupItem.setValue(affinityGroupItemText);

        StaticTextItem currentServerItem = new StaticTextItem(FIELD_SERVER.propertyName(), FIELD_SERVER.title());
        String serverValue = null;
        if (agent.getServer() == null) {
            serverValue = "";
        } else {
            String detailsUrl = "#" + ServerTableView.VIEW_PATH + "/" + agent.getServer().getId();
            String formattedValue = StringUtility.escapeHtml(agent.getServer().getName());
            serverValue = SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);
        }
        currentServerItem.setValue(serverValue);

        form.setItems(nameItem, addressItem, portItem, tokenItem, lastAvailabilityItem, affinityGroupItem,
            currentServerItem);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form);

        detailsSection = section;
        ++initSectionCount;
    }

    private void removeToken(final StaticTextItem tokenItem) {
        SC.ask("really?", new BooleanCallback() {

            @Override
            public void execute(Boolean value) {
                if (value) {
                    GWTServiceLookup.getCloudService().purgeAgentSecurityToken(agentId, new AsyncCallback<Void>() {
                        public void onSuccess(Void result) {
                            tokenItem.setValue(Agent.SECURITY_TOKEN_RESET);
                        }

                        public void onFailure(Throwable caught) {
                            //todo: handle
                        }
                    });
                }
            }
        });
    }
}
