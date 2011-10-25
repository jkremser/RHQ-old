/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest;


import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;

/**
 * Interceptor to set the 'caller' variable of the AbstractRestBean from the princpial
 * that was passed in from the WEB tier.
 *
 * For this to work, classes need to
 * <ul>
 * <li>extends AbstractRestBean</li>
 * <li>Add this class as Interceptor like this : @Interceptors(SetCallerInterceptor.class) </li>
 * </ul>
 * @author Heiko W. Rupp
 */
public class SetCallerInterceptor {

    @Resource
    EJBContext ejbContext;

    @EJB
    SubjectManagerLocal subjectManager;

    private SessionManager sessionManager = SessionManager.getInstance();

    /**
     * We need to take the Principal that was passed through the web-integration,
     * get an RHQ Subject and set a session for it. When the call was made, we need
     * to invalidate the session again.
     * @param ctx InvocationContext from the EJB invocation chain
     * @return result of the method call
     * @throws Exception from method call or if no (valid) principal was provided
     */
    @AroundInvoke
    public Object setCaller(InvocationContext ctx) throws Exception {

        Subject caller=null;
        java.security.Principal p = ejbContext.getCallerPrincipal();
        if (p!=null) {
            caller = subjectManager.getSubjectByName(p.getName());
        }

        if (caller==null)
            throw new IllegalAccessException("No calling principal provided");

        // Get Subject with a session
        caller = sessionManager.put(caller);

        // Provide it to the EJB
        AbstractRestBean target = (AbstractRestBean) ctx.getTarget();
        target.caller = caller;

        // Call the EJBs
        Object result =  ctx.proceed();

        // Invalidate the session again.
        sessionManager.invalidate(caller.getSessionId());

        return result;
    }

}
