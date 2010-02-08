/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.ws.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.Repo;
import org.rhq.enterprise.server.ws.RepoVisibility;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.test.util.WsSubjectUtility;

/**
 * Web service tests for the repo manager services.
 * <p/>
 * This class does not contain all of the tests in {@link org.rhq.enterprise.server.ws.WsRepoManagerTest} yet.
 * The remainder should be ported over to use the new model of testing. 
 *
 * @author Jason Dobies
 */
@Test(groups = "ws")
public class WsRepoManager2Test extends WsUnitTestBase {

    private static final boolean TESTS_ENABLED = true;

    @Test(enabled = TESTS_ENABLED)
    public void updateVisibility() throws Exception {

        // Setup
        WsSubjectUtility subjectUtil = new WsSubjectUtility(service);
        Subject admin = subjectUtil.admin();

        Repo repo1 = objectFactory.createRepo();
        repo1.setName(this.getClass().getName() + ".updateVisibility.repo1");
        repo1.setVisibility(RepoVisibility.PUBLIC);

        Repo repo2 = objectFactory.createRepo();
        repo2.setName(this.getClass().getName() + ".updateVisibility.repo2");
        repo2.setVisibility(RepoVisibility.PRIVATE);

        repo1 = service.createRepo(admin, repo1);
        repo2 = service.createRepo(admin, repo2);

        List<Integer> repoIds = new ArrayList<Integer>(2);
        repoIds.add(repo1.getId());
        repoIds.add(repo2.getId());

        // Test
        service.updateRepoVisibility(repoIds, RepoVisibility.PUBLIC);

        // Verify & Cleanup
        try {
            repo1 = service.getRepo(admin, repo1.getId());
            assert repo1.getVisibility() == RepoVisibility.PUBLIC;

            repo2 = service.getRepo(admin, repo2.getId());
            assert repo2.getVisibility() == RepoVisibility.PUBLIC;
        }
        finally {
            service.deleteRepo(admin, repo1.getId());
            service.deleteRepo(admin, repo2.getId());
        }

    }

}
