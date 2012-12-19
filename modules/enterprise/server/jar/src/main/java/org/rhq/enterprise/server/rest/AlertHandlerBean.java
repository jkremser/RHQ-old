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
package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.rest.domain.*;

/**
 * Deal with alert related stuff
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain"})
@Path("/alert")
@Api(value = "Deal with Alerts",description = "This api deals with alerts that have fired. It does not offer to create/update AlertDefinitions (yet)")
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class AlertHandlerBean extends AbstractRestBean {

//    private final Log log = LogFactory.getLog(AlertHandlerBean.class);

    @EJB
    AlertManagerLocal alertManager;

    @EJB
    AlertDefinitionManagerLocal alertDefinitionManager;


    @GZIP
    @GET
    @Path("/")
    @ApiOperation(value = "List all alerts", multiValueResponse = true, responseClass = "List<AlertRest>")
    public Response listAlerts(
            @ApiParam(value = "Page number", defaultValue = "1") @QueryParam("page") int page,
            @ApiParam(value = "Limit to priority", allowableValues = "High, Medium, Low, All") @DefaultValue("All") @QueryParam("prio") String prio,
            @ApiParam(value = "Should full resources and definitions be sent") @QueryParam("slim") @DefaultValue(
                    "false") boolean slim,
            @ApiParam(
                    value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch")
            @QueryParam("since") Long since,
            @ApiParam(value = "Id of a resource to limit search for") @QueryParam("resourceId") Integer resourceId,
            @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers) {


        AlertCriteria criteria = new AlertCriteria();
        criteria.setPaging(page,20); // TODO implement linking to next page
        if (since!=null) {
            criteria.addFilterStartTime(since);
        }

        if (resourceId!=null) {
            criteria.addFilterResourceIds(resourceId);
        }

        if (!prio.equals("All")) {
            AlertPriority alertPriority = AlertPriority.valueOf(prio.toUpperCase());
            criteria.addFilterPriorities(alertPriority);
        }
        criteria.addSortCtime(PageOrdering.DESC);

        PageList<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        List<AlertRest> ret = new ArrayList<AlertRest>(alerts.size());
        for (Alert al : alerts) {
            AlertRest ar = alertToDomain(al, uriInfo, slim);
            ret.add(ar);
        }

        MediaType type = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (type.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listAlerts.ftl",ret),type);
        } else {
            GenericEntity<List<AlertRest>> entity = new GenericEntity<List<AlertRest>>(ret) {};
            builder = Response.ok(entity);
        }

        return builder.build();
    }

    @GET
    @Path("count")
    @ApiOperation("Return a count of alerts in the system depending on criteria")
    public int countAlerts(@ApiParam(value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch")
                        @QueryParam("since") Long since) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        criteria.fetchAlertDefinition(false);
        criteria.fetchConditionLogs(false);
        criteria.fetchRecoveryAlertDefinition(false);
        criteria.fetchNotificationLogs(false);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        if (since!=null) {
            criteria.addFilterStartTime(since);
        }
        PageList<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        int count = alerts.getTotalSize();

        return count;
    }

    @GET
    @Cache(maxAge = 60)
    @Path("/{id}")
    @ApiOperation(value = "Get one alert with the passed id", responseClass = "AlertRest")
    public Response getAlert(
            @ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
            @ApiParam(value = "Should full resources and definitions be sent") @QueryParam("slim") @DefaultValue("false") boolean slim,
            @Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {

        Alert al = findAlertWithId(id);
        MediaType type = headers.getAcceptableMediaTypes().get(0);

        EntityTag eTag = new EntityTag(Integer.toHexString(al.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {
            AlertRest ar = alertToDomain(al, uriInfo, slim);
            if (type.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("alert.ftl",ar),type);
            } else {
                builder = Response.ok(ar);
            }
        }
        builder.tag(eTag);

        return builder.build();
    }

    @GET
    @Path("/{id}/conditions")
    @Cache(maxAge = 300)
    @ApiOperation(value = "Return the notification logs for the given alert")
    public Response getConditionLogs(@ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
                                  @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers) {

        Alert al = findAlertWithId(id);
        Set<AlertConditionLog> conditions =  al.getConditionLogs();
        MediaType type = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (type.equals(MediaType.APPLICATION_XML_TYPE)) {
            List<StringValue> result = new ArrayList<StringValue>(conditions.size());
            for (AlertConditionLog log : conditions) {
                AlertCondition condition = log.getCondition();
                String entry = String.format("category='%s', name='%s', comparator='%s', threshold='%s', option='%s' : %s",
                        condition.getCategory(), condition.getName(), condition.getComparator(), condition.getThreshold(), condition.getOption(), log.getValue() );
                StringValue sv = new StringValue(entry);
                result.add(sv);
            }
            GenericEntity<List<StringValue>> entity = new GenericEntity<List<StringValue>>(result){};
            builder = Response.ok(entity);
        }
        else {
            List<String> result = new ArrayList<String>(conditions.size());

            for (AlertConditionLog log : conditions) {
                AlertCondition condition = log.getCondition();
                String entry = String.format("category='%s', name='%s', comparator='%s', threshold='%s', option='%s' : %s",
                        condition.getCategory(), condition.getName(), condition.getComparator(), condition.getThreshold(), condition.getOption(), log.getValue() );
                result.add(entry);
            }
            if (type.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("genericStringList.ftl",result),type);
            } else {
                builder = Response.ok(result);
            }
        }

        return builder.build();

    }

    @GET
    @Path("/{id}/notifications")
    @Cache(maxAge = 60)
    @ApiOperation(value = "Return the notification logs for the given alert")
    public Response getNotificationLogs(@ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
                                     @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        Alert al = findAlertWithId(id);
        MediaType type = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        List<AlertNotificationLog> notifications =  al.getAlertNotificationLogs();
        if (type.equals(MediaType.APPLICATION_XML_TYPE)) {
            List<StringValue> result = new ArrayList<StringValue>(notifications.size());
            for (AlertNotificationLog log : notifications) {
                String entry = log.getSender() + ": " + log.getResultState() + ": " + log.getMessage();
                StringValue sv = new StringValue(entry);
                result.add(sv);
            }

            GenericEntity<List<StringValue>> entity = new GenericEntity<List<StringValue>>(result){};
            builder = Response.ok(entity);
        } else {
            List<String> result = new ArrayList<String>(notifications.size());
            for (AlertNotificationLog log : notifications) {
                String entry = log.getSender() + ": " + log.getResultState() + ": " + log.getMessage();
                result.add(entry);
            }
            if (type.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("genericStringList.ftl",result),type);
            } else {
                builder = Response.ok(result);
            }
        }

        return builder.build();
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "Mark the alert as acknowledged (by the caller)", notes = "Returns a slim version of the alert")
    public AlertRest ackAlert(@ApiParam(value = "Id of the alert to acknowledge") @PathParam("id") int id, @Context UriInfo uriInfo) {
        findAlertWithId(id); // Ensure the alert exists
        int count = alertManager.acknowledgeAlerts(caller,new int[]{id});

        // TODO this is not reliable due to Tx constraints ( the above may only run after this ackAlert() method has finished )

        Alert al = findAlertWithId(id);
        AlertRest ar = alertToDomain(al, uriInfo, true);
        return ar;
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Remove the alert from the lit of alerts")
    public void purgeAlert(@ApiParam(value = "Id of the alert to remove") @PathParam("id") int id) {
        alertManager.deleteAlerts(caller, new int[]{id});

    }

    @GET
    @Cache(maxAge = 300)
    @Path("/{id}/definition")
    @ApiOperation("Get the alert definition (basics) for the alert")
    public AlertDefinitionRest getDefinitionForAlert(@ApiParam("Id of the alert to show the definition") @PathParam("id") int alertId) {
        Alert al = findAlertWithId(alertId);
        AlertDefinition def = al.getAlertDefinition();
        AlertDefinitionRest ret = definitionToDomain(def);
        return ret;
    }

    @GZIP
    @GET
    @Path("/definition")
    @ApiOperation("List all Alert Definition")
    public List<AlertDefinitionRest> listAlertDefinitions(
            @ApiParam(value = "Page number", defaultValue = "0") @QueryParam("page") int page,
            @ApiParam(value = "Limit to status, UNUSED AT THE MOMENT ") @QueryParam("status") String status) {

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.setPaging(page,20); // TODO add link to next page
        List<AlertDefinition> defs = alertDefinitionManager.findAlertDefinitionsByCriteria(caller, criteria);
        List<AlertDefinitionRest> ret = new ArrayList<AlertDefinitionRest>(defs.size());
        for (AlertDefinition def : defs) {
            AlertDefinitionRest adr = definitionToDomain(def);
            ret.add(adr);
        }
        return ret;
    }

    @GET
    @Path("/definition/{id}")
    @ApiOperation(value = "Get one AlertDefinition by id", responseClass = "AlertDefinitionRest")
    public Response getAlertDefinition(@ApiParam("Id of the alert definition to retrieve") @PathParam("id") int definitionId,
            @Context Request request) {

        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId );

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {
            AlertDefinitionRest adr = definitionToDomain(def);
            builder = Response.ok(adr);
        }
        builder.tag(eTag);

        return builder.build();
    }

    @PUT
    @Path("/definition/{id}")
    @ApiOperation(value = "Update the alert definition (priority, enablement)", notes = "Priority must be HIGH,LOW,MEDIUM")
    public Response updateDefinition(
            @ApiParam("Id of the alert definition to update") @PathParam("id") int definitionId,
            AlertDefinitionRest definitionRest) {
        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        def.setEnabled(definitionRest.isEnabled());
        def.setPriority(AlertPriority.valueOf(definitionRest.getPriority()));

        def = alertDefinitionManager.updateAlertDefinition(caller,def.getId(),def,false);

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        AlertDefinitionRest adr = definitionToDomain(def);

        Response.ResponseBuilder builder = Response.ok(adr);
        builder.tag(eTag);

        return builder.build();

    }

    private AlertDefinitionRest definitionToDomain(AlertDefinition def) {
        AlertDefinitionRest adr = new AlertDefinitionRest(def.getId());
        adr.setName(def.getName());
        adr.setEnabled(def.getEnabled());
        adr.setPriority(def.getPriority().getName());

        return adr;
    }

    /**
     * Retrieve the alert with id id.
     * @param id Primary key of the alert
     * @return Alert domain object
     * @throws StuffNotFoundException if no such alert exists in the system.
     */
    private Alert findAlertWithId(int id) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterId(id);
        List<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        if (alerts.isEmpty())
            throw new StuffNotFoundException("Alert with id " + id);

        return alerts.get(0);
    }

    public AlertRest alertToDomain(Alert al, UriInfo uriInfo, boolean slim) {
        AlertRest ret = new AlertRest();
        ret.setId(al.getId());
        AlertDefinition alertDefinition = al.getAlertDefinition();
        ret.setName(alertDefinition.getName());
        AlertDefinitionRest alertDefinitionRest;
        if (slim) {
            alertDefinitionRest = new AlertDefinitionRest(alertDefinition.getId());
        } else {
            alertDefinitionRest = definitionToDomain(alertDefinition);
        }
        ret.setAlertDefinition(alertDefinitionRest);
        ret.setDefinitionEnabled(alertDefinition.getEnabled());
        if (al.getAcknowledgingSubject()!=null) {
            ret.setAckBy(al.getAcknowledgingSubject());
            ret.setAckTime(al.getAcknowledgeTime());
        }
        ret.setAlertTime(al.getCtime());
        ret.setDescription(alertManager.prettyPrintAlertConditions(al,false));

        Resource r = fetchResource(alertDefinition.getResource().getId());
        ResourceWithType rwt;
        if (slim) {
            rwt = new ResourceWithType(r.getName(),r.getId());
        } else {
            rwt = fillRWT(r,uriInfo);
        }
        ret.setResource(rwt);

        // add some links
        UriBuilder builder = uriInfo.getBaseUriBuilder();
        builder.path("/alert/{id}/conditions");
        URI uri = builder.build(al.getId());
        Link link = new Link("conditions",uri.toString());
        ret.addLink(link);
        builder = uriInfo.getBaseUriBuilder();
        builder.path("/alert/{id}/notifications");
        uri = builder.build(al.getId());
        link = new Link("notification",uri.toString());
        ret.addLink(link);
        builder = uriInfo.getBaseUriBuilder();
        builder.path("/alert/{id}/definition");
        uri = builder.build(al.getId());
        link = new Link("definition",uri.toString());
        ret.addLink(link);


        return ret;
    }
}
