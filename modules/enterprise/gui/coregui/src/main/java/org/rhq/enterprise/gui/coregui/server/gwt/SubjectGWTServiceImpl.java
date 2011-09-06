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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.auth.prefs.SubjectPreferencesCache;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class SubjectGWTServiceImpl extends AbstractGWTServiceImpl implements SubjectGWTService {

    private static final long serialVersionUID = 1L;

    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

    public void createPrincipal(String username, String password) throws RuntimeException {
        try {
            subjectManager.createPrincipal(getSessionSubject(), username, password);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Subject createSubject(Subject subjectToCreate) throws RuntimeException {
        try {
            return SerialUtility.prepare(subjectManager.createSubject(getSessionSubject(), subjectToCreate),
                "SubjectManager.createSubject");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Subject createSubject(Subject subjectToCreate, String password) throws RuntimeException {
        try {
            return SerialUtility.prepare(subjectManager.createSubject(getSessionSubject(), subjectToCreate, password),
                "SubjectManager.createSubject");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void deleteSubjects(int[] subjectIds) throws RuntimeException {
        try {
            subjectManager.deleteSubjects(getSessionSubject(), subjectIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Subject login(String username, String password) throws RuntimeException {
        try {
            return SerialUtility.prepare(subjectManager.login(username, password), "SubjectManager.login");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void logout(int sessionId) throws RuntimeException {
        try {
            subjectManager.logout(sessionId);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Subject updateSubject(Subject subjectToModify) throws RuntimeException {
        try {
            Subject subject = SerialUtility.prepare(subjectManager.updateSubject(getSessionSubject(), subjectToModify),
                    "SubjectManager.updateSubject");
            // Clear the prefs for this subject from the user prefs cache that portal-war uses, in case we just
            // changed any prefs; otherwise the cache would contain stale prefs.
            SubjectPreferencesCache.getInstance().clearConfiguration(subject.getId());
            return subject;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Subject updateSubject(Subject subjectToModify, String newPassword) throws RuntimeException {
        try {
            return SerialUtility.prepare(subjectManager
                .updateSubject(getSessionSubject(), subjectToModify, newPassword), "SubjectManager.updateSubject");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Subject processSubjectForLdap(Subject subjectToModify, String password) throws RuntimeException {
        //no permissions check as embedded in the SLSB call.
        try {
            Subject processedSubject = subjectManager.processSubjectForLdap(subjectToModify, password);
            return SerialUtility.prepare(processedSubject, "SubjectManager.processSubjectForLdap");
        } catch (LoginException le) {
            throw new RuntimeException("LoginException: " + le.getMessage());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<Subject> findSubjectsByCriteria(SubjectCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(subjectManager.findSubjectsByCriteria(getSessionSubject(), criteria),
                "SubjectManager.findSubjectsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public boolean isUserWithPrincipal(String username) throws RuntimeException {
        try {
            return subjectManager.isUserWithPrincipal(username);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Subject checkAuthentication(String username, String password) {
        try {
            return SerialUtility.prepare(subjectManager.checkAuthentication(username, password),
                "SubjectManager.checkAuthentication");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}
