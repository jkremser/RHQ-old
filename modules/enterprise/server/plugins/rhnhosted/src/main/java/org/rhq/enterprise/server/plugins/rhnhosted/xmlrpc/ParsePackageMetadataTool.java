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
package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;

/**
 * This is a test tool, it will parse through JAXB package metadata for a particular channel.
 * @author jmatthews
 *
 */
public class ParsePackageMetadataTool {

    public final static String XMLFILE_PROP_NAME = "rhn.xml_file_name";

    public static void main(String[] args) throws JAXBException, IOException {
        ParsePackageMetadataTool pkgMetadata = new ParsePackageMetadataTool();

        // Get file name
        String fileName = System.getProperty(XMLFILE_PROP_NAME);
        if (StringUtils.isBlank(fileName)) {
            System.out.println("Usage error.  Required java property '" + XMLFILE_PROP_NAME + "' is missing.");
            System.exit(-1);
        }

        // Get file name
        // Parse data
        JAXBContext jc = JAXBContext.newInstance("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        Unmarshaller u = jc.createUnmarshaller();
        long startVal = System.currentTimeMillis();
        JAXBElement jb = (JAXBElement) u.unmarshal(new FileInputStream(fileName));
        long endVal = System.currentTimeMillis();

        System.out.println("JAXBElement = " + jb);
        System.out.println("JAXBElement.getDeclaredType() = " + jb.getDeclaredType());
        System.out.println("JAXBElement.getName() = " + jb.getName());
        System.out.println("JAXBElement.getClass() = " + jb.getClass());
        System.out.println("JAXB Parse of: " + fileName + ", completed in " + (endVal - startVal) + "ms");
    }
}
