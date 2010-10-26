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
package org.rhq.core.domain.content;

import java.util.Random;

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.domain.test.JPATest;

@Test
public class ContentSourceTest extends JPATest {
    public void testInsert() throws Exception {
        ResourceType rt = new ResourceType("testCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource("testCSResource", "testCSResource", rt);
        resource.setUuid("" + new Random().nextInt());
        Architecture arch = new Architecture("testCSInsertArch");
        PackageType pt = new PackageType("testCSInsertPT", resource.getResourceType());
        Package pkg = new Package("testCSInsertPackage", pt);
        PackageVersion pv = new PackageVersion(pkg, "version", arch);
        ContentSourceType cst = new ContentSourceType("testCSContentSourceType");
        ContentSource cs = new ContentSource("testCSContentSource", cst);

        Configuration config = new Configuration();
        config.put(new PropertySimple("one", "oneValue"));
        cs.setConfiguration(config);

        entityMgr.persist(rt);
        entityMgr.persist(resource);
        entityMgr.persist(arch);
        entityMgr.persist(pt);
        entityMgr.persist(pkg);
        entityMgr.persist(pv);
        entityMgr.persist(cst);
        entityMgr.persist(cs);
        entityMgr.flush();
        
        cs = entityMgr.find(ContentSource.class, cs.getId());
        assert cs != null;
        assert cs.getConfiguration() != null;
        assert cs.getConfiguration().getSimple("one").getStringValue().equals("oneValue");
        assert cs.getSyncSchedule() != null;
        assert cs.getContentSourceType().getDefaultSyncSchedule() != null;

        entityMgr.remove(cs);

        cs = entityMgr.find(ContentSource.class, cs.getId());
        assert cs == null;
    }

    public void testNullSyncSchedule() throws Exception {
        ResourceType rt = new ResourceType("testCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource("testCSResource", "testCSResource", rt);
        resource.setUuid("" + new Random().nextInt());
        Architecture arch = new Architecture("testCSInsertArch");
        PackageType pt = new PackageType("testCSInsertPT", resource.getResourceType());
        Package pkg = new Package("testCSInsertPackage", pt);
        PackageVersion pv = new PackageVersion(pkg, "version", arch);
        ContentSourceType cst = new ContentSourceType("testCSContentSourceType");
        cst.setDefaultSyncSchedule(null);
        ContentSource cs = new ContentSource("testCSContentSource", cst);
        cs.setSyncSchedule(null);

        Configuration config = new Configuration();
        config.put(new PropertySimple("one", "oneValue"));
        cs.setConfiguration(config);

        entityMgr.persist(rt);
        entityMgr.persist(resource);
        entityMgr.persist(arch);
        entityMgr.persist(pt);
        entityMgr.persist(pkg);
        entityMgr.persist(pv);
        entityMgr.persist(cst);
        entityMgr.persist(cs);
        entityMgr.flush();
     
        cs = entityMgr.find(ContentSource.class, cs.getId());
        assert cs != null;
        assert cs.getConfiguration() != null;
        assert cs.getConfiguration().getSimple("one").getStringValue().equals("oneValue");
        assert cs.getSyncSchedule() == null;
        assert cs.getContentSourceType().getDefaultSyncSchedule() == null;

        entityMgr.remove(cs);

        cs = entityMgr.find(ContentSource.class, cs.getId());
        assert cs == null;
    }

    public void testEmptySyncSchedule() throws Exception {
        // using empty strings to see that Oracle still behaves itself
        ResourceType rt = new ResourceType("testCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource("testCSResource", "testCSResource", rt);
        resource.setUuid("" + new Random().nextInt());
        Architecture arch = new Architecture("testCSInsertArch");
        PackageType pt = new PackageType("testCSInsertPT", resource.getResourceType());
        Package pkg = new Package("testCSInsertPackage", pt);
        PackageVersion pv = new PackageVersion(pkg, "version", arch);
        ContentSourceType cst = new ContentSourceType("testCSContentSourceType");
        cst.setDefaultSyncSchedule("");
        ContentSource cs = new ContentSource("testCSContentSource", cst);
        cs.setSyncSchedule("");

        Configuration config = new Configuration();
        config.put(new PropertySimple("one", "oneValue"));
        cs.setConfiguration(config);

        entityMgr.persist(rt);
        entityMgr.persist(resource);
        entityMgr.persist(arch);
        entityMgr.persist(pt);
        entityMgr.persist(pkg);
        entityMgr.persist(pv);
        entityMgr.persist(cst);
        entityMgr.persist(cs);
        entityMgr.flush();

        cs = entityMgr.find(ContentSource.class, cs.getId());
        assert cs != null;
        assert cs.getConfiguration() != null;
        assert cs.getConfiguration().getSimple("one").getStringValue().equals("oneValue");
        assert cs.getSyncSchedule() == null;
        assert cs.getContentSourceType().getDefaultSyncSchedule() == null;

        entityMgr.remove(cs);

        cs = entityMgr.find(ContentSource.class, cs.getId());
        assert cs == null;
    }
}