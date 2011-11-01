<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.alert.Alert" %>
<%@ page import="org.rhq.core.domain.alert.AlertConditionLog" %>
<%@ page import="org.rhq.core.domain.alert.AlertPriority" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.core.domain.util.PageOrdering" %>
<%@ page import="org.rhq.core.domain.criteria.AlertCriteria" %>
<%@ page import="org.rhq.enterprise.gui.common.tag.FunctionTagLibrary" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.alert.AlertManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil"%>

<%@ page contentType="text/javascript" language="java" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    AlertManagerLocal alertManager = LookupUtil.getAlertManager();

    PageControl pc = new PageControl(0, 100);
    pc.setPrimarySortOrder(PageOrdering.DESC);

    AlertCriteria criteria = new AlertCriteria();
    criteria.addFilterResourceIds(resourceId);
    criteria.addFilterStartTime(begin);
    criteria.addFilterEndTime(end);
    criteria.setPageControl(pc);

    PageList<Alert> alerts;
    try {
        alerts = alertManager.findAlertsByCriteria(user.getSubject(), criteria);
    } catch (Exception e) {
        alerts = new PageList<Alert>(0, PageControl.getUnlimitedInstance());
    }

%>

{ "events":

[
    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);

        boolean first = true;

        for (Alert alert : alerts) {
            if (!first)
                out.write(",\n");
            else
                first = false;

            String icon = FunctionTagLibrary.getAlertPriorityURL(alert.getAlertDefinition().getPriority());
            StringBuilder buf = new StringBuilder();
            for (AlertConditionLog log : alert.getConditionLogs()) {
                buf.append("<b>Condition:</b> " + log.getCondition().getName() + "<br/>");
                buf.append("<b>Value:</b> " + log.getValue());
            }

            String link = "/coregui/CoreGUI.html#Resource/" + resourceId + "/Alerts/History/" + alert.getId();
%>

{ "start" : "<%=sdf.format(new Date(alert.getCtime()))%>",
  "title" : "<%= (""+alert.getAlertDefinition().getName()).replaceAll("[\"']","").trim()%>",
  "link" : "<%=link%>",
  "description" : "<%= (""+buf.toString()).replaceAll("[\"']","").trim()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(alert.getAlertDefinition().getPriority() == AlertPriority.LOW ? "#4EB84E" : "#DD5656")%>"
}

    <%
        }
    %>
]
}