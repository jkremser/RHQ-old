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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.domain.test.JPATest;

/**
 * Simple test case that loads a composite object (with constructor queries). This serves to be able to identify the
 * number of selects generated.
 *
 * To see the SQL generated, on needs to set hibernate.show_sql=true in the default.persistence.properties
 *
 * @author Heiko W. Rupp
 */
@Test
public class CompositeLoadingTest extends JPATest {

    @SuppressWarnings("unchecked")
    public void testLoading() throws Exception {
        List<Integer> ids = setupTables(entityMgr);
        entityMgr.flush();
        entityMgr.clear(); // clear out the entity manager, so we have to go to the DB for everything.

        // Now trigger the loading
        Query q = entityMgr
            .createQuery(
                "SELECT new org.rhq.core.domain.resource.composite.ResourceWithAvailability(res, a.availabilityType) "
                    + "  FROM Resource res JOIN res.availability a "
                    + " WHERE a.endTime IS NULL AND a.resource.id = res.id AND res.id IN (:ids) ");
        q.setParameter("ids", ids);
        List<ResourceWithAvailability> rwas = q.getResultList();
        assert rwas.size() == ids.size();                   

        rwas.clear();
        entityMgr.clear();
        q = entityMgr.createQuery("SELECT res,a.availabilityType " //
            + " FROM Resource res LEFT JOIN  res.availability a "
            + " WHERE a.endTime IS NULL AND a.resource.id = res.id AND res.id IN (:ids) " //
            + " ORDER BY res.name");
        q.setParameter("ids", ids);
        List<Object[]> rwas2 = q.getResultList();
        assert rwas2.size() == ids.size();
        for (Object[] ob : rwas2) {
            Resource r = (Resource) ob[0];
            AvailabilityType at = (AvailabilityType) ob[1];
            ResourceWithAvailability rwa = new ResourceWithAvailability(r, at);
            rwas.add(rwa);
        }
        assert rwas.size() == ids.size();
    }

    private List<Integer> setupTables(EntityManager em) {
        List<Integer> ids = new ArrayList<Integer>();
        long t = System.currentTimeMillis() - 1000L;
        Date date = new Date(t);
        ResourceType resourceType = new ResourceType("fake platform", "fake plugin", ResourceCategory.PLATFORM, null);
        em.persist(resourceType);
        Resource platform = new Resource("org.jboss.on.TestPlatfor", "Fake Platform", resourceType);
        platform.setUuid("" + new Random().nextInt());
        em.persist(platform);
        Availability a = new Availability(platform, date, AvailabilityType.UP);
        ids.add(platform.getId());
        Resource platform2 = new Resource("org.jboss.on.TestPlatform2", "Fake Platform2", resourceType);
        platform2.setUuid("" + new Random().nextInt());
        em.persist(platform2);
        Availability a2 = new Availability(platform2, date, AvailabilityType.UP);
        ids.add(platform2.getId());
        Resource platform3 = new Resource("org.jboss.on.TestPlatform3", "Fake Platform3", resourceType);
        platform3.setUuid("" + new Random().nextInt());
        em.persist(platform3);
        Availability a3 = new Availability(platform3, date, AvailabilityType.UP);
        ids.add(platform3.getId());
        em.persist(a);
        em.persist(a2);
        em.persist(a3);
        return ids;
    }
}
