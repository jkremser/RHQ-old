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
package org.rhq.plugins.modcluster.test;

import java.io.IOException;
import java.io.InputStream;

import org.testng.annotations.Test;

import org.rhq.plugins.modcluster.model.ProxyInfo;

/**
 * @author Stefan Negrea
 */
@Test(groups = "modcluster.plugin")
public class ProxyInfoTest {

    @Test
    public void testProxyInfo() throws Exception {
        String[] availableFiles = new String[] { "/proxy_config/proxy_config_1.txt", "/proxy_config/proxy_config_2.txt" };

        for (String testConfigurationFile : availableFiles) {
            String testConfiguration = readConfigFile(testConfigurationFile);
            ProxyInfo proxyInfo = new ProxyInfo(testConfiguration);

            for (ProxyInfo.Context context : proxyInfo.getAvailableContexts()) {
                assert (proxyInfo.getAvailableContexts().indexOf(context) != -1) : "Equals and hash functions not implemented correctly for "
                    + ProxyInfo.Context.class.getCanonicalName();

                assert (context.equals(ProxyInfo.Context.fromString(context.toString())) == true) : "fromString and toString are not equivalent for:"
                    + ProxyInfo.Context.class.getCanonicalName();
            }

            assert (proxyInfo.getAvailableContexts().size() != 0) : "Raw proxy info parsing failed to parse application contexts!";
            assert (proxyInfo.getAvailableVhosts().size() != 0) : "Raw proxy info parsing failed to parse vhosts!";
        }
    }

    @Test
    public void testProxyInfoWithEmptyConfig() throws IOException {
        String testConfigurationFile = "/proxy_config/proxy_config_empty.txt";
        String testConfiguration = readConfigFile(testConfigurationFile);
        ProxyInfo proxyInfo = new ProxyInfo(testConfiguration);

        assert (proxyInfo.getAvailableContexts().size() == 0) : "Raw proxy info parsing failed to parse an empty proxy config.";
        assert (proxyInfo.getAvailableVhosts().size() == 0) : "Raw proxy info parsing failed to parse an empty proxy config.";
    }

    private String readConfigFile(String filePath) throws IOException {
        StringBuffer tempBuffer = new StringBuffer();
        byte[] readBuffer = new byte[1024];
        int bytesRead = 0;

        InputStream resourceStream = getClass().getResourceAsStream(filePath);

        while ((bytesRead = resourceStream.read(readBuffer)) != -1) {
            tempBuffer.append(new String(readBuffer, 0, bytesRead));
        }

        resourceStream.close();

        return tempBuffer.toString();
    }
}