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
/**
 */
package org.rhq.core.domain.measurement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.domain.test.JPATest;
import org.rhq.core.util.collection.ArrayUtils;

/**
 * @author Heiko W. Rupp
 */
public class MeasurementTest extends JPATest {
    private Resource testPlatform;
    private Resource testPlatform2;

    @Test
    public void testNewDefinition() throws SystemException, NotSupportedException, HeuristicMixedException,
        HeuristicRollbackException, RollbackException {
            /*
             *       Query a = entityManager.createQuery(         "SELECT md.defaultOn, md, md.defaultInterval, res " +
             *        "FROM Resource res JOIN res.resourceType rt JOIN rt.metricDefinitions md " +         "WHERE res.id
             * = 500051");      List result = a.getResultList();      System.out.println("FOUND: " + result);      for
             * (Object res : result)      {         System.out.print("ROW: ");
             *
             *    }
             *
             *    Resource resource = entityManager.find(Resource.class, 500051);
             *
             *    Query insertQuery = entityManager.createQuery(         "INSERT into MeasurementSchedule(enabled,
             * interval, definition, resource) " +         "SELECT md.defaultOn, md.defaultInterval, md, res " +
             * "FROM MeasurementDefinition md, Resource res " +         "WHERE md.resourceType.id = 500050 and res.id =
             * 500051");
             *
             *    //insertQuery.setParameter("resource", resource);
             *
             *    int created = insertQuery.executeUpdate();      System.out.println("Created entries: " + created);
             *
             */

            ResourceType resourceType = new ResourceType("fake platform", "fake plugin", ResourceCategory.PLATFORM,
                null);
            entityMgr.persist(resourceType);
            MeasurementDefinition md = new MeasurementDefinition(resourceType, "Heiko");
            md.setDefaultOn(true);
            getEntityManager().persist(md);
            getEntityManager().flush();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNewSchedule() throws Exception {
            MeasurementDefinition def = setupTables(entityMgr);
            int id = def.getId();

            MeasurementDefinition d = entityMgr.find(MeasurementDefinition.class, id);
            assert d != null : "Did not find the definition previously persisted";

            assert d.isDefaultOn();

            assert d.getSchedules() != null;

            MeasurementSchedule ms = d.getSchedules().get(0);
            assert ms != null;

            assert ms.isEnabled();

            assert ms.getBaseline() != null;

            MeasurementBaseline mbl = ms.getBaseline();

            assert !mbl.isUserEntered();

            List<MeasurementSchedule> l;

            Query qu = entityMgr.createNamedQuery(MeasurementSchedule.FIND_ALL_FOR_DEFINITIONS);
            qu.setParameter("definitions", Arrays.asList(d));
            l = qu.getResultList();

            assert l != null;
            assert l.size() == 2;
    }

    @Test
    public void testUpdateBaseline() throws Exception {
            MeasurementDefinition def = setupTables(entityMgr);
            int id = def.getId();

            MeasurementDefinition md = entityMgr.find(MeasurementDefinition.class, id);
            List<MeasurementSchedule> schedules = md.getSchedules();
            MeasurementSchedule sch = schedules.get(0);
            MeasurementBaseline mb = sch.getBaseline();
            mb.setMax(3D);
            entityMgr.flush();
    }

    /**
     * Actually a test on how to handle things in JP-QL
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testScheduleUpdates() throws Exception {
            MeasurementDefinition def = setupTables(entityMgr);
            Query q = entityMgr.createQuery("SELECT ms FROM MeasurementSchedule ms WHERE ms.definition = :def");
            q.setParameter("def", def);
            List<MeasurementSchedule> scheds = q.getResultList();

            assert scheds.size() == 2;

            q = entityMgr.createQuery("UPDATE MeasurementSchedule AS ms SET ms.enabled = true WHERE ms IN (:scheds)");
            q.setParameter("scheds", scheds);
            int count = q.executeUpdate();

            assert count == 2;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindScheduleByResourceAndDefinition() throws Exception {
            MeasurementDefinition def = setupTables(entityMgr);
            List<MeasurementSchedule> schedules = def.getSchedules();
            MeasurementSchedule sched = schedules.get(0);
            Resource res = sched.getResource();

            Query q = entityMgr.createNamedQuery(MeasurementSchedule.FIND_BY_RESOURCE_IDS_AND_DEFINITION_IDS);
            q.setParameter("definitionIds", ArrayUtils.wrapInList(new int[] { def.getId() }));
            q.setParameter("resourceIds", ArrayUtils.wrapInList(new int[] { res.getId() }));
            List<MeasurementSchedule> scheds = q.getResultList();

            assert scheds.size() == 1 : "Did not find 1 schedule, but " + scheds.size();

            int rid = res.getId();
            q = entityMgr.createNamedQuery(MeasurementSchedule.FIND_BY_RESOURCE_IDS_AND_DEFINITION_IDS);
            q.setParameter("definitionIds", ArrayUtils.wrapInList(new int[] { def.getId() }));
            q.setParameter("resourceIds", ArrayUtils.wrapInList(new int[] { rid, rid, rid, rid }));
            scheds = q.getResultList();

            assert scheds.size() == 1 : "Did not find 1 schedule, but " + scheds.size();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindBaselinesToCalculate() throws Exception {
            setupTables(entityMgr);

            Thread.sleep(200);

            long now = System.currentTimeMillis();

            Query q = entityMgr.createNamedQuery(MeasurementSchedule.FIND_SCHEDULES_WITH_BASLINES_TO_CALC);
            q.setParameter("measType", NumericType.DYNAMIC);
            q.setParameter("ctime", now);
            List<MeasurementSchedule> schedules = q.getResultList();

            assert schedules.size() > 0 : "Should find at least one baseline";
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAvailability() throws Exception {
            MeasurementDefinition def = setupTables(entityMgr);

            Resource res = def.getSchedules().get(0).getResource();

            Availability beginUpTime = new Availability(res, getAnotherDate(), AvailabilityType.UP);
            entityMgr.persist(beginUpTime);
            entityMgr.flush();

            Date middleOfAvailabilityUP = getAnotherDate();

            Availability endUpTime = new Availability(res, getAnotherDate(), AvailabilityType.DOWN);
            entityMgr.persist(endUpTime);
            entityMgr.flush();

            beginUpTime.setEndTime(endUpTime.getStartTime());
            beginUpTime = entityMgr.merge(beginUpTime);
            entityMgr.flush();

            /*
             * We now have 2 AVailabilities for the resource FIND_CURRENT_BY_RESOURCE should return avail2
             */
            Query q = entityMgr.createNamedQuery(Availability.FIND_CURRENT_BY_RESOURCE);
            q.setParameter("resourceId", res.getId());
            Availability ava = (Availability) q.getSingleResult();

            assert ava.equals(endUpTime);

            Date afterAvailabilityDown = getAnotherDate();
            /*
             * Now try to get all relevant data for a certain time interval [ middleOfAvailabilityUP,
             * afterAvailabilityDown ]
             */

            q = entityMgr.createNamedQuery(Availability.FIND_FOR_RESOURCE_WITHIN_INTERVAL);
            q.setParameter("resourceId", res.getId());
            q.setParameter("start", middleOfAvailabilityUP.getTime());
            q.setParameter("end", afterAvailabilityDown.getTime());
            List<Availability> results = q.getResultList();

            /*
             * we should get beginUpTime availability because it straddles the middleOfAvailabilityUP we're passing; we
             * should get endUpTime availability because it starts between the two times we're passing
             */
            assert results.size() == 2;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTraitQuery() throws Exception {
        System.out.println("testTraitQuery ...");
            MeasurementDefinition def = setupTables(entityMgr);
            MeasurementSchedule sched = def.getSchedules().get(0);
            Resource resource = sched.getResource();
            int resourceId = resource.getId();

            MeasurementDataPK pk = new MeasurementDataPK(sched.getId());
            MeasurementDataTrait mdt = new MeasurementDataTrait(pk, "Hello World");
            entityMgr.persist(mdt);
            entityMgr.flush();

            Query q = entityMgr.createNamedQuery(MeasurementDataTrait.FIND_CURRENT_FOR_RESOURCE);
            q.setParameter("resourceId", resourceId);
            List<Object[]> res = q.getResultList();
            System.out.println("testTraitQuery: found " + res.size() + " item(s)");
            if (res.size() > 0) {
                MeasurementDataTrait foo = (MeasurementDataTrait) res.get(0)[0];
                String name = (String) res.get(0)[1];
                System.out.println("  and it is " + foo.toString() + " and name " + name);
            }

            Query q2 = entityMgr.createNamedQuery(MeasurementDataTrait.FIND_CURRENT_FOR_SCHEDULES);
            List<Integer> ids = new ArrayList<Integer>();
            ids.add(1);
            ids.add(2);
            ids.add(3);
            ids.add(sched.getId());
            q2.setParameter("scheduleIds", ids);
            List<MeasurementDataTrait> traits = q.getResultList();
            assert traits.size() >= 1; // at least the one for the schedule above should be found
    }

    /**
     * Setup some entities to check query against them etc.
     *
     * @param  em EntityManager to use
     *
     * @return the id of a created MeasurementDefinition
     */
    private MeasurementDefinition setupTables(EntityManager em) {
        ResourceType resourceType = new ResourceType("fake platform", "fake plugin", ResourceCategory.PLATFORM, null);
        em.persist(resourceType);
        Resource platform = new Resource("org.jboss.on.TestPlatfor", "Fake Platform", resourceType);
        platform.setUuid("" + new Random().nextInt());
        em.persist(platform);
        Resource platform2 = new Resource("org.jboss.on.TestPlatform2", "Fake Platform2", resourceType);
        platform2.setUuid("" + new Random().nextInt());
        em.persist(platform2);

        MeasurementDefinition md = new MeasurementDefinition(resourceType, "Heiko");
        md.setDefaultOn(true);
        md.setDisplayName("Fake display name");
        md.setMeasurementType(NumericType.DYNAMIC);
        MeasurementSchedule sched = new MeasurementSchedule(md, platform);
        sched.setEnabled(true);
        MeasurementSchedule sched2 = new MeasurementSchedule(md, platform2);

        platform.addSchedule(sched);
        platform2.addSchedule(sched2);

        md.getSchedules().add(sched);
        md.getSchedules().add(sched2);
        em.persist(md);
        em.persist(sched);
        em.persist(sched2);

        em.flush();

        int id = md.getId();

        assert id != 0 : "id of MeasurementDefinition is 0, but should not be";

        em.persist(sched);
        MeasurementBaseline bl = new MeasurementBaseline();
        bl.setUserEntered(false);
        bl.setMax(1.2);
        bl.setMin(1.0);
        bl.setMean(1.1);
        bl.setSchedule(sched);
        em.persist(bl);

        em.flush();

        testPlatform = platform;
        testPlatform2 = platform2;

        return md;
    }

    private final long DEFAULT_OFFSET = 50;
    private long referenceTime = new Date().getTime();

    public Date getAnotherDate() {
        return getAnotherDate(DEFAULT_OFFSET);
    }

    public Date getAnotherDate(long offset) {
        referenceTime += offset;
        return new Date(referenceTime);
    }

}