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

import static org.testng.Assert.*;
import static org.hamcrest.core.AllOf.*;
import static org.hamcrest.collection.IsMapContaining.*;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.test.JMockTest;
import org.rhq.test.jmock.PropertyMatcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class AltLangComponentTest extends JMockTest {

    AltLangComponent component;

    ScriptResolverFactory scriptResolverFactory;

    ScriptResolver scriptResolver;

    ScriptExecutorService scriptExecutor;

    ResourceContext resourceContext;

    ScriptMetadata metadata;

    Action action;

    @BeforeMethod
    public void setup() {
        component = new AltLangComponent();

        scriptResolverFactory = context.mock(ScriptResolverFactory.class);
        component.setScriptResolverFactory(scriptResolverFactory);

        scriptResolver = context.mock(ScriptResolver.class);

        scriptExecutor = context.mock(ScriptExecutorService.class);
        component.setScriptExecutor(scriptExecutor);

        resourceContext = new FakeResourceContext(createResource());

        metadata = new ScriptMetadata();

        action = null;
    }

    @Test
    public void scriptToStartComponentShouldBeCalledWithCorrectBindings() throws Exception {
        action = new Action("resource_component", "start", resourceContext.getResourceType());

        final Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("resourceContext", resourceContext);
        bindings.put("action", action);

        context.checking(new Expectations() {{
            allowing(scriptResolverFactory).getScriptResolver(); will(returnValue(scriptResolver));

            atLeast(1).of(scriptResolver).resolveScript(with(resourceContext.getPluginConfiguration()),
                with(matchingAction(action))); will(returnValue(metadata));

            // This expectation verifies that scriptExecutor is passes the correct arguments. First, it checks that
            // the metadata argument matches and then it checks the bindings argument. The bindings argument is a map
            // of objects to be bound as script variables. This expectation verifies that the minimum, required
            // variables are bound.
            oneOf(scriptExecutor).executeScript(with(matchingMetadata(metadata)),
                                                with(allOf(hasEntry(equal("action"), matchingAction(action)),
                                                           hasEntry(equal("resourceContext"), same(resourceContext)))));
        }});

        component.start(resourceContext);
    }

    @Test
    public void scriptToStopComponentShouldBeCalledWithCorrectBindings() throws Exception {
        component.setResourceContext(resourceContext);

        action = new Action("resource_component", "stop", resourceContext.getResourceType());

        final Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("resourceContext", resourceContext);
        bindings.put("action", action);

        context.checking(new Expectations() {{
            allowing(scriptResolverFactory).getScriptResolver(); will(returnValue(scriptResolver));

            atLeast(1).of(scriptResolver).resolveScript(with(resourceContext.getPluginConfiguration()),
                with(matchingAction(action))); will(returnValue(metadata));

            // This expectation verifies that scriptExecutor is passes the correct arguments. First, it checks that
            // the metadata argument matches and then it checks the bindings argument. The bindings argument is a map
            // of objects to be bound as script variables. This expectation verifies that the minimum, required
            // variables are bound.
            oneOf(scriptExecutor).executeScript(with(matchingMetadata(metadata)),
                                                with(allOf(hasEntry(equal("action"), matchingAction(action)),
                                                           hasEntry(equal("resourceContext"), same(resourceContext)))));
        }});

        component.stop();
    }

    @Test
    public void scriptToCheckAvailabilityShouldBeCalledWithCorrectBindings() throws Exception {
        component.setResourceContext(resourceContext);

        action = new Action("resource_component", "get_availability", resourceContext.getResourceType());

        final Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("resourceContext", resourceContext);
        bindings.put("action", action);

        context.checking(new Expectations() {{
            allowing(scriptResolverFactory).getScriptResolver(); will(returnValue(scriptResolver));

            atLeast(1).of(scriptResolver).resolveScript(with(resourceContext.getPluginConfiguration()),
                with(matchingAction(action))); will(returnValue(metadata));

            // This expectation verifies that scriptExecutor is passes the correct arguments. First, it checks that
            // the metadata argument matches and then it checks the bindings argument. The bindings argument is a map
            // of objects to be bound as script variables. This expectation verifies that the minimum, required
            // variables are bound.
            oneOf(scriptExecutor).executeScript(with(matchingMetadata(metadata)),
                                                with(allOf(hasEntry(equal("action"), matchingAction(action)),
                                                           hasEntry(equal("resourceContext"), same(resourceContext)))));
            will(returnValue(AvailabilityType.UP));
        }});

        AvailabilityType expectedAvailability = AvailabilityType.UP;
        AvailabilityType actualAvailability = component.getAvailability();

        assertEquals(
            actualAvailability,
            expectedAvailability,
            "Expected to get back the availability returned from the script"
        );
    }

    public static Matcher<Action> matchingAction(Action expected) {
        return new PropertyMatcher<Action>(expected);
    }

    public static Matcher<ScriptMetadata> matchingMetadata(ScriptMetadata expected) {
        return new PropertyMatcher<ScriptMetadata>(expected);
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
