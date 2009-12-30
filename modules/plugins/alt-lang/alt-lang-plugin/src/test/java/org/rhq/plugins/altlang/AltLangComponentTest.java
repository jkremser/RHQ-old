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

package org.rhq.plugins.altlang;

import static org.hamcrest.core.AllOf.*;
import static org.hamcrest.collection.IsMapContaining.*;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.test.JMockTest;
import org.rhq.test.jmock.PropertyMatcher;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class AltLangComponentTest extends JMockTest {

    @Test
    public void testStart() throws Exception {
        AltLangComponent component = new AltLangComponent();

        final ResourceContext resourceContext = new FakeResourceContext(createResource());

        final ScriptResolverFactory scriptResolverFactory = context.mock(ScriptResolverFactory.class);
        component.setScriptResolverFactory(scriptResolverFactory);

        final ScriptResolver scriptResolver = context.mock(ScriptResolver.class);

        final ScriptExecutorService scriptExecutor = context.mock(ScriptExecutorService.class);
        component.setScriptExecutor(scriptExecutor);

        final Action action = new Action("resource_component", "start", resourceContext.getResourceType());

        final Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("resourceContext", resourceContext);
        bindings.put("action", action);

        final ScriptMetadata metadata = new ScriptMetadata();

        context.checking(new Expectations() {{
            allowing(scriptResolverFactory).getScriptResolver(); will(returnValue(scriptResolver));

            atLeast(1).of(scriptResolver).resolveScript(with(resourceContext.getPluginConfiguration()),
                with(matchingAction(action))); will(returnValue(metadata));

            oneOf(scriptExecutor).executeScript(with(aNonNull(ScriptMetadata.class)),
                                                with(allOf(hasEntry(equal("action"), matchingAction(action)),
                                                           hasEntry(equal("resourceContext"), same(resourceContext)))));
        }});

        component.start(resourceContext);
    }

    public static Matcher<Action> matchingAction(Action expected) {
        return new PropertyMatcher<Action>(expected);
    }

    private Resource createResource() {
        Resource resource = new Resource();
        resource.setPluginConfiguration(new Configuration());
        resource.setResourceType(new ResourceType());

        return resource;
    }

    static class FakeResourceContext extends ResourceContext {
        private Configuration pluginConfiguration;
        private ResourceType resourceType;

        public FakeResourceContext(Resource resource) {
            super(resource, null, null, null, null, null, null, null, null, null, null, null);
        }

    }

}
