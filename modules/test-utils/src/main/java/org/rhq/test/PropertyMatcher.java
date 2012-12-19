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

package org.rhq.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

public class PropertyMatcher<T> {

    private T expected;

    private T actual;

    private Set<String> ignoredProperties = new HashSet<String>();

    private Double maxDifference;

    public void setExpected(T expected) {
        this.expected = expected;
    }

    public void setActual(T actual) {
        this.actual = actual;
    }

    public void setIgnoredProperties(Collection<String> ignoredProperties) {
        this.ignoredProperties.addAll(ignoredProperties);
    }

    public void setMaxDifference(Double maxDifference) {
        this.maxDifference = maxDifference;
    }

    public MatchResult execute() {
        try {
            Map<String, ?> expectedProps = PropertyUtils.describe(expected);
            Map<String, ?> actualProps = PropertyUtils.describe(actual);

            boolean isMatch = true;
            StringBuilder details = new StringBuilder(expected.getClass().getSimpleName() +
                " objects do not match:\n");

            for (String name : expectedProps.keySet()) {
                Object expectedValue = expectedProps.get(name);
                Object actualValue = actualProps.get(name);

                if (ignoredProperties.contains(name)) {
                    continue;
                }

                if (!propertyEquals(expectedValue, actualValue)) {
                    isMatch = false;
                    details.append("expected." + name + " = " + expectedValue + "\n");
                    details.append("actual." + name + " = " + actualValue + "\n\n");
                }
            }

            return new MatchResult(isMatch, details.toString());
        }
        catch (IllegalAccessException e) {
            throw new PropertyMatchException(e);
        }
        catch (InvocationTargetException e) {
            throw new PropertyMatchException(e);
        }
        catch (NoSuchMethodException e) {
            throw new PropertyMatchException(e);
        }
    }

    private boolean propertyEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return true;
        }

        if (expected == null && actual != null) {
            return false;
        }

        if (maxDifference != null && expected instanceof Double) {
            boolean result = expected.equals(actual);
            if (!result) {
                Double dExpected = (Double) expected;
                Double dActual = (Double) actual;
                Double diff = Math.abs(dExpected - dActual);
                result = ((!diff.isNaN()) && (!diff.isInfinite()) && (diff < maxDifference));
            }
            return result;
        }

        return expected.equals(actual);
    }
}
