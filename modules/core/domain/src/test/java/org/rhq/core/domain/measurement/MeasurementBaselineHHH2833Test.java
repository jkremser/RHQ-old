/*
* RHQ Management Platform
* Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.measurement;

import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.test.JPATest;

/**
 * Tests that demonstrate the Hibernate bug HHH-2833.
 *
 * <p>See Hiberate JIRA: http://opensource.atlassian.com/projects/hibernate/browse/HHH-2833</p>
 *
 * I confirmed that both testHibernateBugXXX methods fail with Hibernate 3.2.3.GA and 3.2.5.GA. They both succeed when
 * running with 3.2.0.CR4.
 *
 * @author John Mazzitelli
 */
@Test
public class MeasurementBaselineHHH2833Test extends JPATest {
    private static boolean SKIP_TESTS = false;

    public void testSelectQueryThatSucceeds() throws Exception {
        Query q = entityMgr.createQuery(getSelectQueryThatSucceeds());
        assert q != null;
        q.setParameter("startTime", 500L).setParameter("endTime", 1000L);
        q.getResultList();
    }

    public void testHibernateBug_HHH_2833_WithSelect() throws Exception {
        Query q = entityMgr.createQuery(getSelectQueryThatFails());
        assert q != null;
        q.setParameter("startTime", 500L).setParameter("endTime", 1000L);
        q.getResultList();
    }

    public void testHibernateBug_HHH_2833_WithInsert() throws Exception {
        if (SKIP_TESTS) {
            System.out
                .println("!!!!!MeasurementBaselineHHH2833Test NEEDS TO BE RE-ENABLED ONCE HHH-2833 IS FIXED!!!!!");
            return;
        }

        if (true) // Does not work on Oracle
        {
            return;
        }

        Query q = entityMgr.createQuery(getInsertQueryThatFails());
        assert q != null;
        q.setParameter("startTime", 500L).setParameter("endTime", 1000L);
        q.executeUpdate();
    }

    public void testHibernateBug_HHH_2833_WithDelete() throws Exception {
        if (SKIP_TESTS) {
            System.out
                .println("!!!!!MeasurementBaselineHHH2833Test NEEDS TO BE RE-ENABLED ONCE HHH-2833 IS FIXED!!!!!");
            return;
        }

        Query q = entityMgr.createQuery(getDeleteQueryThatFails());
        assert q != null;
        q.setParameter("startTime", 500L).setParameter("endTime", 1000L);
        q.executeUpdate();
    }

    private String getSelectQueryThatSucceeds() {
        return "      SELECT min(d.min) AS baselineMin, " //
            + "           max(d.max) AS baselineMax, " //
            + "           avg(d.value) AS baselineMean, " //
            + "           CURRENT_TIMESTAMP AS computeTime, " //
            + "           d.id.scheduleId AS scheduleId " //
            + "      FROM MeasurementDataNumeric1H d " //
            + "           JOIN d.schedule s " //
            + "           LEFT JOIN s.baseline b " //
            + "     WHERE b.id IS NULL " //
            + "       AND d.id.timestamp BETWEEN :startTime AND :endTime " //
            + "  GROUP BY d.id.scheduleId " //
            + "    HAVING d.id.scheduleId IN (SELECT d1.id.scheduleId " //
            + "                                 FROM MeasurementDataNumeric1H d1 " //
            + "                                WHERE d1.id.timestamp <= :startTime) ";
    }

    private String getSelectQueryThatFails() {
        return "      SELECT min(d.min) AS baselineMin, " //
            + "           max(d.max) AS baselineMax, " //
            + "           avg(d.value) AS baselineMean, " //
            + "           CURRENT_TIMESTAMP AS computeTime, " //
            + "           d.id.scheduleId AS scheduleId " //
            + "      FROM MeasurementDataNumeric1H d " //
            + "           JOIN d.schedule s " //
            + "           LEFT JOIN s.baseline b " //
            + "     WHERE b.id IS NULL " //
            + "       AND d.id.timestamp BETWEEN :startTime AND :endTime " //
            + "  GROUP BY d.id.scheduleId " //
            + "    HAVING d.id.scheduleId IN (SELECT d1.id.scheduleId " //
            + "                                 FROM MeasurementDataNumeric1H d1 " //
            + "                                WHERE d1.id.timestamp <= :startTime " //
            + "                                  AND d1.id.scheduleId = d.id.scheduleId) "; // this AND causes problems
    }

    private String getInsertQueryThatFails() {
        return "INSERT INTO MeasurementBaseline (baselineMin,baselineMax,baselineMean,computeTime,scheduleId) "
            + getSelectQueryThatSucceeds();
    }

    private String getDeleteQueryThatFails() {
        // only the SELECT clause is different - the FROM on down is the same as the getSelectQueryThatSucceeds query
        return "DELETE MeasurementBaseline AS doomed WHERE doomed.scheduleId IN " //
            + "( " //
            + "   SELECT d.id.scheduleId AS scheduleId " //
            + "     FROM MeasurementDataNumeric1H d " //
            + "          JOIN d.schedule s " //
            + "          LEFT JOIN s.baseline b " //
            + "    WHERE b.id IS NOT NULL " //
            + "      AND d.id.timestamp BETWEEN :startTime AND :endTime " //
            + "      AND b.userEntered = FALSE " //
            + " GROUP BY d.id.scheduleId " //
            + "   HAVING d.id.scheduleId IN (SELECT d1.id.scheduleId " //
            + "                                FROM MeasurementDataNumeric1H d1 "//
            + "                               WHERE d1.id.timestamp <= :startTime) " //
            + ") ";
    }
}