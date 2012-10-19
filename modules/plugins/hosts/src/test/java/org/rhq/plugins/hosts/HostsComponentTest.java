/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts;

import java.io.File;
import java.io.IOException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;
import org.testng.annotations.BeforeTest;

/**
 * An integration test for {@link HostsComponent}.
 *
 * @author Ian Springer
 */
public class HostsComponentTest extends AbstractAugeasConfigurationComponentTest {
    private static final String PLUGIN_NAME = "Hosts";
    private static final String RESOURCE_TYPE_NAME = "Hosts File";

    @Override
    protected String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected String getResourceTypeName() {
        return RESOURCE_TYPE_NAME;
    }

    @Override
    protected boolean isResourceConfigSupported() {
        // The hosts plugin will use Augeas if it's available but will fall back to direct file parsing if it's not.
        return true;
    }

    @Override
    protected void tweakDefaultPluginConfig(Configuration defaultPluginConfig)
    {
        super.tweakDefaultPluginConfig(defaultPluginConfig);
        String hostsFilePath = new File("/etc/hosts").getAbsolutePath();
        defaultPluginConfig.put(new PropertySimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP, hostsFilePath));
    }

    @BeforeTest
    @Override
    public void resetConfigFiles() throws IOException
    {
        super.resetConfigFiles();
    }

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);

        PropertyMap entry;

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "127.0.0.1"));
        entry.put(new PropertySimple("canonical", "localhost"));
        entry.put(new PropertySimple("alias", "localhost.localdomain\nlocalhost4\nlocalhost4.localdomain4"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "::1"));
        entry.put(new PropertySimple("canonical", "localhost"));
        entry.put(new PropertySimple("alias", "localhost.localdomain\nlocalhost6\nlocalhost6.localdomain6"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "1.1.1.1"));
        entry.put(new PropertySimple("canonical", "one-one-one-one.com"));
        entry.put(new PropertySimple("alias", null));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "2.2.2.2"));
        entry.put(new PropertySimple("canonical", "two-two-two-two.com"));
        entry.put(new PropertySimple("alias", "alias"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "3.3.3.3"));
        entry.put(new PropertySimple("canonical", "three-three-three-three.com"));
        entry.put(new PropertySimple("alias", "alias1\nalias2"));
        entries.getList().add(entry);

        return config;
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);

        PropertyMap entry;

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "127.0.0.1"));
        entry.put(new PropertySimple("canonical", "localhost"));
        entry.put(new PropertySimple("alias", "hehe\nlocalhost4\nlocalhost4.localdomain4"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "::1"));
        entry.put(new PropertySimple("canonical", "localhost"));
        entry.put(new PropertySimple("alias", "localhost.localdomain\nlocalhost6\nlocalhost6.localdomain6"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "1.1.1.1"));
        entry.put(new PropertySimple("canonical", "one-one-one-one.com"));
        entry.put(new PropertySimple("alias", ""));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "2.2.2.2"));
        entry.put(new PropertySimple("canonical", "two-two-two-two.com"));
        entry.put(new PropertySimple("alias", ""));
        entries.getList().add(entry);

        entry = new PropertyMap("*[canonical]");
        entry.put(new PropertySimple("ipaddr", "3.3.3.3"));
        entry.put(new PropertySimple("canonical", "three-three-three-three.com"));
        entry.put(new PropertySimple("alias", "aliast1.org\nalias2.org"));
        entries.getList().add(entry);
        return config;
    }
}
