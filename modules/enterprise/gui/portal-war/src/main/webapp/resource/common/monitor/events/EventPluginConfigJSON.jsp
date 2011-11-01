<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.configuration.ConfigurationUpdateStatus" %>
<%@ page import="org.rhq.core.domain.configuration.PluginConfigurationUpdate" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.configuration.ConfigurationManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    PageList<PluginConfigurationUpdate> configurationUpdates;
    try {
        configurationUpdates = configurationManager.findPluginConfigurationUpdates(subject, resourceId, begin, end, new PageControl(0,100));
    } catch (Exception e) {
        configurationUpdates = new PageList<PluginConfigurationUpdate>(0, PageControl.getUnlimitedInstance());
    }

%>


{ "events": [

    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);

        boolean first = true;

        for (PluginConfigurationUpdate configUpdate : configurationUpdates) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String icon = null;
            switch (configUpdate.getStatus()) {
                case SUCCESS:
                    icon = "/images/icons/Connection_ok_16.png";
                    break;
                case FAILURE:
                    icon = "/images/icons/Connection_failed_16.png";
                    break;
                case INPROGRESS:
                    icon = "/images/icons/Connection_16.png";
                    break;
            }

            String link = "/coregui/CoreGUI.html#Resource/" + resourceId + "/Inventory/PluginConfigurationHistoryView/" + configUpdate.getId();

            %>

{ "start" : "<%=sdf.format(configUpdate.getCreatedTime())%>",
  "title" : "Connection Settings Change",
  "link" : "<%=link%>",
  "description" : "<b>User:</b> <%=(""+configUpdate.getSubjectName()).replaceAll("[\"']","").trim()%><br/><b>Status:</b> <%=(""+configUpdate.getStatus()).replaceAll("[\"']","").trim()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(configUpdate.getStatus() != ConfigurationUpdateStatus.FAILURE ? "#4EB84E" : "#DD5656")%>"
}

    <%
        }
    %>

]}