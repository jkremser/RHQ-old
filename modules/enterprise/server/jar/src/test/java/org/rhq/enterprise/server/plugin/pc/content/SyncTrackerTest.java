/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.content;

import junit.framework.TestCase;

import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoSyncResults;

public class SyncTrackerTest extends TestCase {

    Repo repo;
    RepoSyncResults results;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repo = new Repo();
        repo.setId(1);
        results = new RepoSyncResults(repo);
    }

    public void testProgressWatcher() {

        SyncTracker pw = new SyncTracker(null, results);
        boolean failed = false;
        try {
            pw.getPercentComplete();
        } catch (IllegalStateException e) {
            failed = true;
        }
        assertTrue(failed);

        pw.start();
        assertTrue(pw.getPercentComplete() == 0);
        for (int i = 0; i < 5; i++) {
            pw.addWork(100);
        }
        pw.finishWork(500);
        assertTrue(pw.getPercentComplete() == 100);

    }

    // Check to make sure we don't lose precision as we divide by close numbers
    public void test98Percent() {
        SyncTracker pw = new SyncTracker(null, results);
        pw.start();
        pw.setTotalWork(102);
        pw.finishWork(100);
        assertTrue(pw.getPercentComplete() == 98);
    }

    public void testResetToZero() {
        SyncTracker pw = new SyncTracker(null, results);
        pw.start();
        pw.setTotalWork(102);
        pw.finishWork(100);
        pw.resetToZero();
        assertTrue(pw.getPercentComplete() == 0);

    }

}
