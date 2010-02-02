/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.client;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVWriter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.client.utility.LazyLoadScenario;
import org.rhq.enterprise.client.utility.ShortOutput;

/**
 * @author Greg Hinkle
 */
public class TabularWriter {

    private static final String CSV = "csv";

    String[] headers;
    int[] maxColumnLength;
    boolean[] noShrinkColumns;
    int width = 160;
    PrintWriter out;
    private String format = "raw";
    private CSVWriter csvWriter;
    private SummaryFilter summaryFilter = new SummaryFilter();
    boolean exportMode;

    static Set<String> IGNORED_PROPS = new HashSet<String>();

    static {
        IGNORED_PROPS.add("mtime");
        IGNORED_PROPS.add("ctime");
        IGNORED_PROPS.add("itime");
        IGNORED_PROPS.add("uuid");
        IGNORED_PROPS.add("parentResource");
    }

    static Set<Class> SIMPLE_TYPES = new HashSet<Class>();

    static {
        SIMPLE_TYPES.add(Byte.class);
        SIMPLE_TYPES.add(Byte.TYPE);
        SIMPLE_TYPES.add(Short.class);
        SIMPLE_TYPES.add(Short.TYPE);
        SIMPLE_TYPES.add(Integer.class);
        SIMPLE_TYPES.add(Integer.TYPE);
        SIMPLE_TYPES.add(Long.class);
        SIMPLE_TYPES.add(Long.TYPE);
        SIMPLE_TYPES.add(Float.class);
        SIMPLE_TYPES.add(Float.TYPE);
        SIMPLE_TYPES.add(Double.class);
        SIMPLE_TYPES.add(Double.TYPE);
        SIMPLE_TYPES.add(Boolean.class);
        SIMPLE_TYPES.add(Boolean.TYPE);
        SIMPLE_TYPES.add(String.class);
    }

    public TabularWriter(PrintWriter out, String... headers) {
        this.headers = headers;
        this.out = out;
    }

    public TabularWriter(PrintWriter out) {
        this.out = out;
    }

    public TabularWriter(PrintWriter out, String format) {
        this.out = out;
        this.format = format;

        if (CSV.equals(format)) {
            csvWriter = new CSVWriter(out);
        }
    }

    public void print(Object object) {

        if (object instanceof Map) {
            print((Map) object);
            return;
        }

        if (object instanceof Collection) {
            print((Collection) object);
            return;
        }

        if (object instanceof Configuration) {
            print((Configuration) object);
            return;
        }

        if (object != null && object.getClass().isArray()) {
            print((Object[]) object);
            return;
        }

        try {

            if (SIMPLE_TYPES.contains(object.getClass())) {
                this.out.println(String.valueOf(object));
                return;
            }

            out.println(object.getClass().getSimpleName() + ":");
            Map<String, PropertyInfo> properties = new LinkedHashMap<String, PropertyInfo>();
            int maxLength = 0;

            for (PropertyDescriptor pd : summaryFilter.getPropertyDescriptors(object, exportMode)) {
                Method m = pd.getReadMethod();
                Object val = null;
                if (m != null) {
                    val = invoke(object, m);
                }

                if (val != null) {
                    try {
                        String str = shortVersion(val);
                        maxLength = Math.max(maxLength, pd.getName().length());
                        properties.put(pd.getName(), new PropertyInfo(str, pd.getPropertyType()));
                    } catch (Exception e) {
                    }
                }
            }

            for (String key : properties.keySet()) {
                printProperty(key, properties.get(key), maxLength);
            }

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }

    }

    private static class PropertyInfo {
        String value;

        Class<?> type;

        PropertyInfo(String propertyValue, Class<?> propertyType) {
            value = propertyValue;
            type = propertyType;
        }
    }

    private void printProperty(String name, PropertyInfo propertyInfo, int maxLength) {
        out.print("\t");
        printPreSpaced(out, name, maxLength);
        out.print(": ");

        if (exportMode || !String.class.equals(propertyInfo.type)) {
            out.println(propertyInfo.value);
        } else {
            out.println(abbreviate(propertyInfo.value, width - 12 - maxLength));
        }
    }

    // This method is taken verbatim from the Commons Lang project in the org.apache.commons.lang.StringUtils class
    // TODO Should this method go into one our StringUtil classes?
    private String abbreviate(String string, int maxWidth) {
        int offset = 0;

        if (string == null) {
            return null;
        }

        if (maxWidth < 4) {
            throw new IllegalArgumentException("Minimum abbreviation width is 4");
        }

        if (string.length() <= maxWidth) {
            return string;
        }

        if (offset > string.length()) {
            offset = string.length();
        }

        if ((string.length() - offset) < (maxWidth - 3)) {
            offset = string.length() - (maxWidth - 3);
        }

        if (offset <= 4) {
            return string.substring(0, maxWidth - 3) + "...";
        }

        if (maxWidth < 7) {
            throw new IllegalArgumentException("Minimum abbreviation width with offset is 7");
        }

        if ((offset + (maxWidth - 3)) < string.length()) {
            return "..." + abbreviate(string.substring(offset), maxWidth - 3);
        }

        return "..." + string.substring(string.length() - (maxWidth - 3));
    }

    public void print(Map map) {

        String[][] data = new String[map.size()][];
        int i = 0;
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            data[i] = new String[2];
            data[i][0] = shortVersion(key);
            data[i][1] = shortVersion(value);
            i++;
        }
        this.headers = new String[] { "Key", "Value" };
        print(data);
    }

    public void print(Collection list) {
        // List of arbitrary objects
        if (list == null || list.size() == 0)
            out.println("no data");
        else if (list.size() == 1 && !CSV.equals(format)) {
            out.println("one row");

            print(list.iterator().next());
        } else {

            String[][] data = null;

            if (!allOneType(list)) {
                printStrings(list);
            } else {

                Object firstObject = list.iterator().next();
                try {

                    if (firstObject instanceof String) {
                        headers = new String[] { "Value" };
                        data = new String[list.size()][1];
                        int i = 0;
                        for (Object object : list) {
                            data[i++][0] = (String) object;
                        }

                    } else {

                        if (consistentMaps(list)) {
                            // results printed

                        } else {

                            int i = 0;

                            List<PropertyDescriptor> pdList = new ArrayList<PropertyDescriptor>();
                            for (PropertyDescriptor pd : summaryFilter.getPropertyDescriptors(firstObject, exportMode)) {
                                try {
                                    boolean allNull = true;
                                    for (Object row : list) {
                                        Method m = pd.getReadMethod();
                                        Object val = null;
                                        if (m != null) {
                                            val = invoke(row, pd.getReadMethod());
                                        }
                                        if ((val != null && !(val instanceof Collection))
                                            || ((val != null && (val instanceof Collection) && !((Collection) val)
                                                .isEmpty())))
                                            allNull = false;
                                    }
                                    if (!allNull && !IGNORED_PROPS.contains(pd.getName())) {
                                        pdList.add(pd);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            headers = new String[pdList.size()];
                            data = new String[list.size()][pdList.size()];

                            for (PropertyDescriptor pd : pdList) {
                                headers[i++] = pd.getName();
                            }
                            i = 0;
                            for (Object row : list) {
                                int j = 0;
                                for (PropertyDescriptor pd : pdList) {

                                    Object val = "?";
                                    val = invoke(row, pd.getReadMethod());
                                    data[i][j++] = shortVersion(val);
                                }
                                i++;
                            }

                            this.print(data);

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
                } finally {
                    headers = null;
                }
            }

        }

    }

    private boolean consistentMaps(Collection list) {
        List<String> keys = null;
        String[][] data = new String[list.size()][];
        int i = 0;
        for (Object row : list) {
            if (!(row instanceof Map) && !(row instanceof PropertyMap)) {
                return false;
            }
            if (keys == null) {
                keys = new ArrayList<String>();

                //insert check for PropertyMap as they are not instances of Map 
                if (row instanceof PropertyMap) {
                    //TODO: spinder 1/15/10. PropertyMap is arbitrarily complex. Only serialize simple props?
                    for (Object key : ((PropertyMap) row).getMap().keySet()) {
                        String headerKey = stringValueOf(key);
                        keys.add(headerKey);
                    }
                } else {//else row is a Map
                    for (Object key : ((Map) row).keySet()) {
                        String headerKey = stringValueOf(key);
                        keys.add(headerKey);
                    }
                }
                //conditionally put order on the headers to mimic core gui listing style/order
                //Ex. Pid   Name    Size    User Time       Kernel Time
                if (keys.contains("pid")) {
                    String[] processAttribute = { "pid", "name", "size", "userTime", "kernelTime" };
                    List<String> newKeyOrder = new ArrayList<String>();
                    for (String attribute : processAttribute) {
                        newKeyOrder.add(attribute);
                        keys.remove(attribute);
                    }
                    //postpend remaining keys if any to the newHeader list
                    for (String key : keys) {
                        newKeyOrder.add(key);
                    }
                    keys = newKeyOrder;
                }
            }

            data[i] = new String[keys.size()];
            if (row instanceof PropertyMap) {
                for (String key : keys) {
                    if (!keys.contains(stringValueOf(key))) {
                        return false;
                    }
                    data[i][keys.lastIndexOf(stringValueOf(key))] = shortVersion(((PropertyMap) row).get(String
                        .valueOf(key)));
                }
            } else {//else row is a Map
                for (Object key : ((Map) row).keySet()) {
                    if (!keys.contains(stringValueOf(key))) {
                        return false;
                    }
                    data[i][keys.lastIndexOf(stringValueOf(key))] = shortVersion(((Map) row).get(key));
                }
            }
            i++;
        }

        if (keys != null) {
            headers = keys.toArray(new String[keys.size()]);
            print(data);
            return true;
        } else {
            return false;
        }

    }

    public void print(Configuration config) {
        out.println("Configuration [" + config.getId() + "] - " + config.getNotes());
        for (PropertySimple p : config.getSimpleProperties().values()) {
            print(p, 1);
        }
        for (PropertyList p : config.getListProperties().values()) {
            print(p, 1);
        }
        for (PropertyMap p : config.getMapProperties().values()) {
            print(p, 1);
        }

    }

    public void print(PropertySimple p, int depth) {
        out.println(indent(depth) + p.getName() + " = " + p.getStringValue());
    }

    public void print(PropertyList p, int depth) {
        out.println(indent(depth) + p.getName() + " [" + p.getList().size() + "] {");
        if (p.getList().get(0) instanceof PropertyMap) {
            consistentMaps(p.getList());

        } else {
            for (Property entry : p.getList()) {
                if (entry instanceof PropertySimple) {
                    print((PropertySimple) entry, depth + 1);
                } else if (entry instanceof PropertyMap) {
                    print((PropertyMap) entry, depth + 1);
                }
            }
        }

        out.println(indent(depth) + "}");
    }

    public void print(PropertyMap p, int depth) {
        out.println(indent(depth) + p.getName() + " [" + p.getMap().size() + "] {");
        for (String key : p.getMap().keySet()) {
            Property entry = p.getMap().get(key);
            if (entry instanceof PropertySimple) {
                print((PropertySimple) entry, depth + 1);
            } else if (entry instanceof PropertyMap) {
                print((PropertyMap) entry, depth + 1);
            }
        }
        out.println(indent(depth) + "}");
    }

    private String indent(int x) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < x; i++) {
            buf.append("  ");
        }
        return buf.toString();
    }

    private void printStrings(Collection list) {
        for (Object object : list) {
            out.println(stringValueOf(object));
        }
    }

    private boolean allOneType(Collection list) {
        Class lastClass = null;
        for (Object object : list) {
            if (lastClass == null) {
                lastClass = object.getClass();
            } else if (!object.getClass().equals(lastClass)) {
                return false;
            }
        }
        return true;
    }

    public void print(Object[] data) {
        if (data == null || data.length == 0) {
            out.println("0 rows");
            return;
        }
        out.println("Array of " + (data.getClass().getComponentType().getName()));

        print(Arrays.asList(data));
    }

    public void print(String[][] data) {

        if (data == null || data.length == 0) {
            out.println("0 rows");
            return;
        }
        maxColumnLength = new int[data[0].length];

        for (String[] row : data) {

            for (int i = 0; i < row.length; i++) {
                if (row[i] == null) {
                    row[i] = "";
                }
                maxColumnLength[i] = Math.max(maxColumnLength[i], row[i].length());
            }
        }

        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                maxColumnLength[i] = Math.max(maxColumnLength[i], headers[i].length());
            }
        }

        int totalColumnLength = 0;
        for (int len : maxColumnLength) {
            totalColumnLength += len;
        }
        // add space for spaces
        totalColumnLength += maxColumnLength.length;

        double shrink = 1;
        if (totalColumnLength > width) {
            shrink = ((double) width) / totalColumnLength;
        }

        for (int i = 0; i < maxColumnLength.length; i++) {
            maxColumnLength[i] = (int) Math.floor(shrink * maxColumnLength[i]);
        }

        if (headers != null) {
            if (CSV.equals(format)) {
                csvWriter.writeNext(headers);
            } else {
                for (int i = 0; i < maxColumnLength.length; i++) {
                    int colSize = maxColumnLength[i];
                    printSpaced(out, headers[i], colSize);
                    out.print(" ");
                }

                out.print("\n");

                for (int i = 0; i < width; i++) {
                    out.print("-");
                }
            }
            out.print("\n");

        }

        if (CSV.equals(format)) {
            for (String[] row : data) {
                csvWriter.writeNext(row);
            }
        } else {
            for (String[] row : data) {
                for (int i = 0; i < maxColumnLength.length; i++) {
                    int colSize = maxColumnLength[i];

                    printSpaced(out, row[i], colSize);
                    out.print(" ");
                }
                out.print("\n");
            }
        }

        out.print(data.length + " rows\n");
    }

    private void printSpaced(PrintWriter out, String data, int length) {
        int dataLength = data.length();
        if (dataLength > length) {
            //out.println(abbreviate(data, length));
            out.print(data.substring(0, length));
        } else {
            out.print(data);

            for (int i = dataLength; i < length; i++) {
                out.print(" ");
            }
        }

    }

    private void printPreSpaced(PrintWriter out, String data, int length) {
        int dataLength = data.length();
        if (dataLength > length) {
            out.print(data.substring(0, length));
        } else {
            for (int i = dataLength; i < length; i++) {
                out.print(" ");
            }
            out.print(data);
        }

    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isExportMode() {
        return exportMode;
    }

    public void setExportMode(boolean exportMode) {
        this.exportMode = exportMode;
    }

    private static String shortVersion(Object object) {

        if (object instanceof ShortOutput) {
            return ((ShortOutput) object).getShortOutput();
        } else if (object instanceof PropertySimple) {
            return ((PropertySimple) object).getStringValue();
        } else if (object instanceof ResourceType) {
            return ((ResourceType) object).getName();
        } else if (object instanceof ResourceAvailability) {
            return ((ResourceAvailability) object).getAvailabilityType().getName();
        } else if (object != null && object.getClass().isArray()) {
            return Arrays.toString((Object[]) object);
        } else {
            return stringValueOf(object);
        }
    }

    private static Object invoke(Object o, Method m) throws IllegalAccessException, InvocationTargetException {
        boolean access = m.isAccessible();
        m.setAccessible(true);
        try {
            LazyLoadScenario.setShouldLoad(false);

            return m.invoke(o);
        } catch (Exception e) {
            // That's fine
            return null;
        } finally {
            LazyLoadScenario.setShouldLoad(true);
            m.setAccessible(access);
        }
    }

    private static String stringValueOf(Object object) {
        try {
            LazyLoadScenario.setShouldLoad(false);
            return String.valueOf(object);
        } catch (Exception e) {
            return "null";
        } finally {
            LazyLoadScenario.setShouldLoad(true);
        }
    }
}
