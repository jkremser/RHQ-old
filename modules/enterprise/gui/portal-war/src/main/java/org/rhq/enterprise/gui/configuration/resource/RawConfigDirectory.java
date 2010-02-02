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

package org.rhq.enterprise.gui.configuration.resource;

import org.rhq.core.domain.configuration.RawConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RawConfigDirectory {

    private String path;

    private List<RawConfigUIBean> rawConfigUIBeans = new ArrayList<RawConfigUIBean>();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<RawConfigUIBean> getRawConfigUIBeans() {
        return rawConfigUIBeans;
    }

    public void addRawConfig(RawConfiguration rawConfig) {
        RawConfigUIBean bean = new RawConfigUIBean();
        bean.setRawConfiguration(rawConfig);
        rawConfigUIBeans.add(bean);
    }
}
