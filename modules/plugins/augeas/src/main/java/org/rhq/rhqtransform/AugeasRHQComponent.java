/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.rhqtransform;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;


/**
 * An interface the RHQ resource components can implement to provide access 
 * to the Augeas proxy.
 * 
 * @author Filip Drabek
 *
 */
public interface AugeasRHQComponent<T extends ResourceComponent> extends ResourceComponent<T> {

    /**
     * @return the augeas tree for the component
     * @throws AugeasTreeException
     */
    public AugeasTree getAugeasTree() throws AugeasTreeException;

    /**
     * Provides access to the Augeas proxy configured in the way this component needs
     * so that users are able to perform non-trivial operations on it.
     * 
     * @return
     * @throws AugeasTreeException
     */
    public AugeasProxy getAugeasProxy() throws AugeasTreeException;
}
