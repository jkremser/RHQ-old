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
package org.rhq.core.domain.configuration.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.domain.test.JPATest;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;

import static org.testng.Assert.*;

public class ConfigurationTest extends JPATest {
    @Test
    public void testPersistConfigurationUpdateHistory() throws Exception {
        ResourceType type = new ResourceType("platform", "", ResourceCategory.PLATFORM, null);
        entityMgr.persist(type);
        Resource resource = new Resource("key", "name", type);
        resource.setUuid("uuid");
        entityMgr.persist(resource);

        Configuration c = new Configuration();
        PropertySimple p1 = new PropertySimple("first", "firstValue");
        p1.setErrorMessage(ThrowableUtil.getStackAsString(new Exception(
            "This should be a boolean value - true or false")));
        c.put(p1);
        entityMgr.persist(c);

        AbstractResourceConfigurationUpdate cur = new ResourceConfigurationUpdate(resource, c, "dummy");
        entityMgr.persist(cur);

        AbstractResourceConfigurationUpdate copy = entityMgr
            .find(AbstractResourceConfigurationUpdate.class, cur.getId());
        assert copy.getStatus().equals(ConfigurationUpdateStatus.INPROGRESS) : copy;
        assert copy.getSubjectName().equals("dummy") : copy;
        assert copy.getCreatedTime() > 0 : copy;
        assert copy.getModifiedTime() > 0 : copy;
        assert copy.getErrorMessage() == null : copy;
        assert copy.getConfiguration().getSimple("first") != null : copy;
        assert copy.getConfiguration().getSimple("first").getErrorMessage().indexOf(
            "This should be a boolean value - true or false") > -1 : copy;
        assert copy.getConfiguration().getSimple("first").getStringValue().equals("firstValue") : copy;

        // let's pretend we failed the update
        cur.setErrorMessage(ThrowableUtil.getStackAsString((new Exception("update error here"))));
        assert copy.getStatus()
            .equals(ConfigurationUpdateStatus.FAILURE) : copy; // setting the error message also sets status to failure

        copy = entityMgr.find(AbstractResourceConfigurationUpdate.class, cur.getId());
        assert copy.getStatus().equals(ConfigurationUpdateStatus.FAILURE) : copy;
        assert copy.getErrorMessage().indexOf("update error here") > -1 : copy;
        assert copy.getConfiguration().getSimple("first") != null : copy;
        assert copy.getConfiguration().getSimple("first").getErrorMessage().indexOf(
            "This should be a boolean value - true or false") > -1 : copy;
        assert copy.getConfiguration().getSimple("first").getStringValue().equals("firstValue") : copy;
    }

    @Test
    public void testPersistConfiguration() throws Exception {
        Configuration c = new Configuration();
        PropertySimple p1 = new PropertySimple("first", "firstValue");
        p1.setErrorMessage(ThrowableUtil.getStackAsString(new Exception(
            "This should be a boolean value - true or false")));
        c.put(p1);
        entityMgr.persist(c);
        Configuration copy = entityMgr.find(Configuration.class, c.getId());
        assert c.equals(copy);
        assert copy.getSimple("first") != null;
        assert copy.getSimple("first").getErrorMessage().indexOf("This should be a boolean value - true or false") > -1;
        assert copy.getSimple("first").getStringValue().equals("firstValue");
    }

    @Test
    public void testConfigurationSerialization() throws Exception {
        Configuration c = new Configuration();
        c.setId(1);
        c.setNotes("hi");
        c.setVersion(1);

        PropertySimple p1 = new PropertySimple("a", true);
        c.put(p1);

        PropertyMap p2 = new PropertyMap("b");
        p2.put(new PropertySimple("b1", "alpha"));
        p2.put(new PropertySimple("b2", "beta"));
        c.put(p2);

        PropertyMap p3 = new PropertyMap("c");
        c.put(p3);

        PropertyList p4 = new PropertyList("d", new PropertySimple("d1", "alpha"));
        c.put(p4);

        PropertyList p5 = new PropertyList("e");
        c.put(p5);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(c);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);

        Configuration c1 = (Configuration) ois.readObject();

        assert c.getNotes().equals(c1.getNotes());
        assert c1.getNames().containsAll(c.getNames());
        assert c1.getMap("c").getMap().isEmpty();

        System.out.println("Serialized version of config was " + baos.size() + " bytes");
    }

    @Test
    public void testStoreConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setNotes("Testing");
        configuration.setVersion(1);

        configuration.put(new PropertySimple("Integer", 3));

        configuration.put(new PropertySimple("String", "Hello, World"));

        configuration.put(new PropertyList("EmptyList"));
        configuration.put(new PropertyList("MyList", new PropertySimple("letter", "a"), new PropertySimple("letter",
            "b"), new PropertySimple("letter", "c")));

        PropertyMap myMap = new PropertyMap("MyMap");
        myMap.put(new PropertySimple("Alpha", "Uno"));
        myMap.put(new PropertySimple("Beta", "Dose"));
        myMap.put(new PropertyList("ListInAMap", new PropertySimple("foo", Math.PI), new PropertySimple("foo", Math.E),
            new PropertySimple("foo", Double.MAX_VALUE), new PropertySimple("foo", Double.MIN_VALUE)));
        configuration.put(myMap);

        entityMgr.persist(configuration);
        entityMgr.flush();
        entityMgr.remove(configuration); // added by ips (03/29/07)
    }

    @Test
    public void testReadConfigurations() throws Exception {
        EntityManager entityMgr = getEntityManager();
        List<Configuration> configurations = entityMgr.createQuery("Select c from Configuration c").setMaxResults(5)
            .getResultList();
        for (Configuration configuration : configurations) {
            System.out.println("Configuration Found: " + configuration.getNotes());

            for (Property prop : configuration.getProperties()) {
                prettyPrintProperty(prop, 1);
            }
        }
    }

    @Test
    public void verifyPersistSavesRawConfiguration() throws Exception {
        RawConfiguration rawConfig = createRawConfiguration();

        Configuration config = new Configuration();
        config.addRawConfiguration(rawConfig);

        entityMgr.persist(config);

        assertTrue(rawConfig.getId() != 0, "Failed to cascade save to " + RawConfiguration.class.getSimpleName());
    }

    @Test
    public void verifyOrphanedRawConfigurationDeletedFromDatabase() throws Exception {
        RawConfiguration rawConfiguration = createRawConfiguration();

        Configuration config = new Configuration();
        config.addRawConfiguration(rawConfiguration);

        entityMgr.persist(config);

        config.removeRawConfiguration(rawConfiguration);

        config = entityMgr.merge(config);

        entityMgr.flush();
        entityMgr.clear();

        RawConfiguration removedRawConfig = entityMgr.find(RawConfiguration.class, rawConfiguration.getId());

        assertNull(removedRawConfig, "Failed to remove the orphaned " + RawConfiguration.class.getSimpleName() +
            " from the persistence context.");
    }

    RawConfiguration createRawConfiguration() {
        RawConfiguration rawConfig = new RawConfiguration();
        String contents = "contents";
        String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(contents);
        rawConfig.setContents(contents, sha256);
        rawConfig.setPath("/tmp/foo");

        return rawConfig;
    }

    public static void prettyPrintConfiguration(Configuration configuration) {
        System.out.println("Configuration: " + configuration.getNotes());
        for (Property p : configuration.getProperties()) {
            prettyPrintProperty(p, 1);
        }
    }

    private static void prettyPrintProperty(Property property, int indent) {
        if (property instanceof PropertyList) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println("List Property [" + property.getName() + "]");

            for (Property p : ((PropertyList) property).getList()) {
                prettyPrintProperty(p, indent + 1);
            }
        } else if (property instanceof PropertyMap) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println("Map Property [" + property.getName() + "]");
            for (Property p : ((PropertyMap) property).getMap().values()) {
                prettyPrintProperty(p, indent + 1);
            }
        } else if (property instanceof PropertySimple) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println(property.getName() + " = " + ((PropertySimple) property).getStringValue());
        }
    }
}