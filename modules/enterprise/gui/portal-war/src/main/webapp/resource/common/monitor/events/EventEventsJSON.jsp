<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.common.EntityContext"%>
<%@ page import="org.rhq.core.domain.event.composite.EventComposite" %>
<%@ page import="org.rhq.core.domain.util.OrderingField" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.common.tag.FunctionTagLibrary" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.event.EventManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.regex.Pattern"%>
<%@ page import="java.util.*"%>
<%@ page import="org.rhq.core.domain.event.EventSeverity"%>
<%@ page import="org.rhq.core.domain.util.PageOrdering"%>
<%@ page contentType="text/javascript" language="java" %>


<%
    EventManagerLocal eventManager = LookupUtil.getEventManager();

    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    boolean tooManyEvents = false;

    PageList<EventComposite> list =
        eventManager.findEventComposites(subject, EntityContext.forResource(resourceId), begin, end, null, 
            null, null, new PageControl(0,10000, new OrderingField("timestamp", PageOrdering.ASC)));
%>

<%! public String trimLength(String string, int length) {
        return string.substring(0, Math.min(length,string.length()));
    }

    public String eventColor(EventSeverity severity) {
        switch (severity) {
            case DEBUG:
                return "#00FF00";
            case INFO:
                return "#0000FF";
            case WARN:
                return "#FFA500";
            case ERROR:
                return "#FF0000";
            case FATAL:
            default:
                return "#FF0000";
        }
    }

    public String escapeBackslashes(String s) {
        return s.replaceAll("\\\\", "\\\\\\\\");
    }
%>

<%! public class GroupedEventComposite extends EventComposite {
    public List<EventComposite> events = new ArrayList<EventComposite>();

    public GroupedEventComposite(EventComposite... events) {
        for (EventComposite event : events) {
            this.events.add(event);
        }
    }
    public Date getTimestamp() {
        return events.get(0).getTimestamp();
    }
    public String getEventDetail() {
        return events.size() + " Events";
    }
    public EventSeverity getSeverity() {
        EventSeverity highestSeverity = events.get(0).getSeverity();
        for (EventComposite event :events) {
            if (event.getSeverity().isMoreSevereThan(highestSeverity))
                highestSeverity = event.getSeverity();
        }

        return highestSeverity;
    }}

%>


{ "events": [


    <%
        List<EventComposite> groupedList = new ArrayList<EventComposite>();


        if (!list.isEmpty()) {
            Iterator<EventComposite> iter = list.iterator();
            GroupedEventComposite comp = null;
            EventComposite current = iter.next();
            do {
                EventComposite next = null;

                if (iter.hasNext())
                    next = iter.next();

                if (current == null) {

                } else if (next == null && current != null) {
                    groupedList.add(current);
                    current = null;
                } else if (current.getTimestamp().getTime() + 60 * 1000 < next.getTimestamp().getTime()) {
                    groupedList.add(current);
                    if (iter.hasNext())
                        current = iter.next();
                    else
                        current = next;
                    comp = null;
                } else {

                    if (comp == null) {
                        comp = new GroupedEventComposite(current,next);
                        current = comp;
                    } else {
                        comp.events.add(next);
                    }
                }
            } while (current != null);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z",Locale.US);

        boolean first = true;
        for (EventComposite event : groupedList) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String title = event.getEventDetail();
            title = title.replace('\n', ' ');

            title = escapeBackslashes(trimLength(title,30));

            boolean grouped = (event instanceof GroupedEventComposite);

            String color = eventColor(event.getSeverity());
            String icon = FunctionTagLibrary.getEventSeverityURL(event.getSeverity(), grouped);

            String link;
            if (grouped) {
                link = "/coregui/CoreGUI.html#Resource/" + resourceId + "/Events/History";
            } else {
                link = "/coregui/CoreGUI.html#Resource/" + resourceId + "/Events/History/" + event.getEventId();            	
            }

            String detail = null;
            if (grouped) {
                StringBuilder buf = new StringBuilder();
                for (EventComposite childEvent : ((GroupedEventComposite)event).events) {
                	String childColor = eventColor(childEvent.getSeverity());
                    buf.append("<a href='/coregui/CoreGUI.html#Resource/" + resourceId + "/Events/History/" + childEvent.getEventId() + "'>");
                    buf.append("<font color=\"" + childColor + "\">" + escapeBackslashes(trimLength(childEvent.getEventDetail(),80)) + "</font></a><br />");
                }
                detail = buf.toString();
            } else {
                detail = escapeBackslashes(event.getEventDetail());
                detail = "<b>Source:</b> " + escapeBackslashes(event.getSourceLocation()) + "<br/><b>Detail:</b> " + detail;
            }


            detail = detail.replaceAll("\"","\\\\\"");
            detail = Pattern.compile("\n",Pattern.MULTILINE).matcher(detail).replaceAll("<br />");

            %>

{ "start" : "<%=sdf.format(event.getTimestamp())%>",
  "title" : "<%= (""+title).replaceAll("[\"']","").trim()%>",
  "link" : "<%=link%>",
  "description" : "<%=(""+detail).replaceAll("[\"']","").trim()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=color%>"
}

    <%
        }
    %>

]}