/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.itest.domain;

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Test stuff around socket bindings.
 * This could actually also run for domain mode
 * @author Heiko W. Rupp
 */
@Test(groups = {"integration", "pc", "domain"}, singleThreaded = true)
public class DomainSocketBindingTest extends AbstractJBossAS7PluginTest {

    public static final ResourceType RESOURCE_TYPE = new ResourceType("SocketBindingGroup", PLUGIN_NAME, ResourceCategory.SERVICE, null);
    private static final String RESOURCE_KEY = "socket-binding-group=standard-sockets";
    private static final String PORT_EXPR = "port:expr";
    private static final String MULTICAST_PORT_EXPR = "multicast-port:expr";

    @Test(priority = 1010,groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void runDiscovery() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();

        Thread.sleep(10*1000L); // delay so that PC gets a chance to scan for resources

        assertNotNull(platform);
        assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Thread.sleep(20*1000L); // delay so that PC gets a chance to scan for resources
    }


    @Test(priority = 1011)
    public void loadBindings() throws Exception {

        loadConfig();
    }

    @Test(priority = 1011)
    public void addBinding() throws Exception {

        Configuration configuration = loadConfig();
        PropertyList pl = (PropertyList) configuration.get("*");
        int count = pl.getList().size();

        // Now add a binding
        PropertyMap map = new PropertyMap("binding");
        PropertySimple ps = new PropertySimple("name","bla");
        map.put(ps);
        ps = new PropertySimple(PORT_EXPR,12345);
        map.put(ps);
        ps = new PropertySimple("fixed-port",false);
        map.put(ps);
        ps = new PropertySimple(MULTICAST_PORT_EXPR,"${foo.bar.baz:12346}");
        map.put(ps);
        pl.add(map);

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1,configuration,getResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response!=null;
        assert response.getConfigurationUpdateId() == 1 : "Expected update id 1, but got " + response.getConfigurationUpdateId();
        assert response.getErrorMessage()==null: "Config add resulted in this error: " + response.getErrorMessage();

        configuration = loadConfig();
        pl = (PropertyList) configuration.get("*");
        assert pl.getList().size() == count+1 : "Got only " + pl.getList().size() + " items, expected "+ (count+1);
    }

    @Test(priority = 1011)
    public void addModifyBinding() throws Exception {

        Configuration configuration = loadConfig();
        PropertyMap map = new PropertyMap("binding");
        PropertySimple ps = new PropertySimple("name","bla2");
        map.put(ps);
        ps = new PropertySimple(PORT_EXPR,12355);
        map.put(ps);
        ps = new PropertySimple("fixed-port",false);
        map.put(ps);
        ps = new PropertySimple(MULTICAST_PORT_EXPR,"${foo.bar.baz:12356}");
        map.put(ps);
        PropertyList pl = (PropertyList) configuration.get("*");
        pl.add(map);

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(2,configuration,getResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response!=null;
        assert response.getErrorMessage()==null: "Config add resulted in this error: " + response.getErrorMessage();

        configuration = loadConfig();
        map=null;
        pl = (PropertyList) configuration.get("*");
        for (Property prop: pl.getList()) {
            PropertyMap pm = (PropertyMap) prop;
            PropertySimple ps2 = pm.getSimple("name");
            if (!ps2.getStringValue().equals("bla2"))
                continue;

            map = pm;
        }
        assert map != null : "Did not find 'bla2' in the returned config";
        map.put(new PropertySimple(PORT_EXPR,22355));
        map.put(new PropertySimple("fixed-port",true));
        request = new ConfigurationUpdateRequest(3,configuration,getResource().getId());
        response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response!=null;
        assert response.getErrorMessage()==null: "Config update resulted in this error: " + response.getErrorMessage();

    }

    @Test(priority = 1011)
    public void addRemoveBinding() throws Exception {

        Configuration configuration = loadConfig();
        PropertyMap map = new PropertyMap("binding");
        PropertySimple ps = new PropertySimple("name","bla3");
        map.put(ps);
        ps = new PropertySimple(PORT_EXPR,12365);
        map.put(ps);
        ps = new PropertySimple("fixed-port",false);
        map.put(ps);
        ps = new PropertySimple(MULTICAST_PORT_EXPR,"${foo.bar.baz:12366}");
        map.put(ps);
        PropertyList pl = (PropertyList) configuration.get("*");
        int count = pl.getList().size();
        pl.add(map);

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(4,configuration,getResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response!=null;
        assert response.getErrorMessage()==null: "Config add resulted in this error: " + response.getErrorMessage();

        configuration = loadConfig();
        pl = (PropertyList) configuration.get("*");
        assert pl.getList().size() == count+1;

        Iterator<Property> iter = pl.getList().iterator();
        while (iter.hasNext()) {
            Property prop = iter.next();
            PropertyMap pm = (PropertyMap) prop;
            if (pm.getSimpleValue("name","xxx").equals("bla3")) {
                iter.remove();
                break;
            }
        }
        assert pl.getList().size() == count;

        request = new ConfigurationUpdateRequest(5,configuration,getResource().getId());
        response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response!=null;
        assert response.getErrorMessage()==null: "Property removal resulted in this error: " + response.getErrorMessage();
    }




    @NotNull
    private Configuration loadConfig() throws PluginContainerException {
        Configuration config = pluginContainer.getConfigurationManager().loadResourceConfiguration(getResource().getId());
        assert config != null;
        assert !config.getProperties().isEmpty();
        return config;
    }

    private Resource getResource() {

        InventoryManager im = pluginContainer.getInventoryManager();
        Resource platform = im.getPlatform();
        assert platform != null : "Did not find a platform";
        Resource server = getResourceByTypeAndKey(platform,DomainServerComponentTest.RESOURCE_TYPE,DomainServerComponentTest.RESOURCE_KEY);
        assert server != null : "Did not find the domain server";
        Resource bindings = getResourceByTypeAndKey(server,RESOURCE_TYPE,RESOURCE_KEY);
        assert bindings != null : "Did not find " + RESOURCE_KEY;
        return bindings;
    }

}
