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
package org.rhq.enterprise.gui.authentication;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.auth.SessionTimeoutException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class SessionAccessServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/plain");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        // Stronger according to blog comment below that references HTTP spec
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "must-revalidate");
        // some date in the past
        response.addHeader("Expires", "Mon, 8 Aug 2006 10:00:00 GMT");

        // do not go any further unless we know the server has been fully initialized
        boolean serverInitialized;
        try {
            serverInitialized = LookupUtil.getStartupLocal().isInitialized();
        } catch (Throwable t) {
            serverInitialized = false; // this probably means we are still starting up and app server hasn't made EJBs available yet
        }

        if (!serverInitialized) {
            response.setHeader("Retry-After", "30");
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is not ready - still booting up");
            return;
        }

        //if a session does not already exist this call will create one
        HttpSession session = request.getSession();

        // check for web user update request from coregui. This is usually only set during ldap logins (case insensitive
        // or registration.)
        String sessionWebUserUpdate = request.getHeader("rhq_webuser_update");

        // check for HTTP session lastAccess update request from coregui. This "ping" happens at regular intervals
        // to keep the http session alive until a coreGui logout event takes place. Note, the HTTP session
        // lastAccess value is different than the rhq subject's session lastAccess. 
        String sessionLastAccessUpdate = request.getHeader("rhq_last_access_update");

        // If this is an HTTP session update request just return success. The access time has been updated already,
        // just due to this request being sent.
        if (sessionLastAccessUpdate != null) {
            PrintWriter writer = response.getWriter();
            writer.print("success");
            return;
        }

        /* 
         * check if the user object is in the session.  if not, then the user is not validated, the response output
         * will not contain the "<subjectId>:<sessionId>:<lastAccess>", which will forward the user to the login page
         */
        WebUser webUser = SessionUtils.getWebUser(session);

        if (webUser != null && webUser.getSubject() != null) {

            // if sessionWebUserUpdate header sent then request for WebUser to be updated
            if ((sessionWebUserUpdate != null) && (!sessionWebUserUpdate.trim().isEmpty())) {
                // if webUser.getSubject.getName is same as user with session id passed in
                try {
                    // attempt to retrieve Subject for the requested session update
                    Subject currentSubject = SessionManager.getInstance().getSubject(
                        Integer.valueOf(sessionWebUserUpdate));
                    if (currentSubject != null) {//located associated subject
                        // if userNames match (case insensitive) then update webUser appropriately and re-associate in
                        // session
                        if (webUser.getSubject().getName().equalsIgnoreCase(currentSubject.getName())) {
                            webUser = new WebUser(currentSubject);
                            SessionUtils.setWebUser(session, webUser);
                        }
                    }
                } catch (SessionNotFoundException snfe) {
                } catch (NumberFormatException e) {
                } catch (SessionTimeoutException e) {
                }
            }

            // the web user exists, so update our SessionManager's session last-access-time
            Subject subject = webUser.getSubject();
            try {
                SessionManager.getInstance().getSubject(subject.getSessionId());
                long lastAccess = SessionManager.getInstance().getLastAccess(subject.getSessionId());

                PrintWriter writer = response.getWriter();
                String output = subject.getId() + ":" + webUser.getSessionId() + ":" + lastAccess;
                writer.print(output);
            } catch (SessionNotFoundException snfe) {
                session.removeAttribute(ParamConstants.USER_PARAM);
                SessionUtils.setWebUser(session, null);
            } catch (SessionTimeoutException ste) {
                session.removeAttribute(ParamConstants.USER_PARAM);
                SessionUtils.setWebUser(session, null);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "must-revalidate");
        // some date in the past
        response.addHeader("Expires", "Mon, 8 Aug 2006 10:00:00 GMT");
        boolean serverInitialized;
        try {
            serverInitialized = LookupUtil.getStartupLocal().isInitialized();
        } catch (Throwable t) {
            serverInitialized = false; // this probably means we are still starting up and app server hasn't made EJBs available yet
        }
        PrintWriter out = response.getWriter();
        out.println(serverInitialized);
    }
}
