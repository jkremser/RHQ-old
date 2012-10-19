/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.json;

import java.util.List;

/**
 * Operation that reads names of children of a given type from an address
 * @author Heiko W. Rupp
 */
public class ReadChildrenNames extends Operation {

    /**
     * Read children names of a given type below a given address
     * @param address Address to look at e.g. /profile/default/subsystem/messaging
     * @param childType e.g. queue
     */
    public ReadChildrenNames(List<PROPERTY_VALUE> address, String childType) {
        super("read-children-names",address,"child-type",childType);
    }
}
