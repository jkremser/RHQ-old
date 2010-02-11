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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import org.rhq.enterprise.server.plugin.pc.content.ContentProviderManager;

public class ApacheXmlRpcExecutor implements XmlRpcExecutor {

    private final Log log = LogFactory.getLog(ApacheXmlRpcExecutor.class);
    private XmlRpcClient client;
    protected int retryTimes = 3;

    public ApacheXmlRpcExecutor(XmlRpcClient clientIn) {
        this.client = clientIn;
        String val = System.getProperty(ContentProviderManager.RETRY_PROP_NAME);
        if (!StringUtils.isEmpty(val)) {
            retryTimes = Integer.parseInt(val);
        }
        log
            .info("Configuring ApacheXmlRpcExecutor:  retryTimes = " + retryTimes
                + ", this value can be modified by changing the System Property: "
                + ContentProviderManager.RETRY_PROP_NAME);
    }

    public Object execute(String methodName, Object[] params) throws XmlRpcException {
        return execute(methodName, params, retryTimes);
    }

    public Object execute(String methodName, List pParams) throws XmlRpcException {
        return execute(methodName, pParams, retryTimes);
    }

    public Object execute(String methodName, Object[] params, int retryTimesLeft) throws XmlRpcException {
        Object retVal = null;
        try {
            retVal = client.execute(methodName, params);
        } catch (XmlRpcException e) {
            log.info("Exception: " + e);
            if (retryTimesLeft <= 0) {
                throw e;
            }
            log.info("Ignoring exception, will retry " + retryTimesLeft + " more times before stopping");
            retryTimesLeft = retryTimesLeft - 1;
            return execute(methodName, params, retryTimesLeft);
        }
        return retVal;
    }

    public Object execute(String pMethodName, List pParams, int retryTimesLeft) throws XmlRpcException {
        Object retVal = null;
        try {
            retVal = client.execute(pMethodName, pParams);
        } catch (XmlRpcException e) {
            log.info("Exception : " + e);
            if (retryTimesLeft <= 0) {
                throw e;
            }
            log.info("Ignoring exception, will retry " + retryTimesLeft + " more times before stopping");
            retryTimesLeft = retryTimesLeft - 1;
            return execute(pMethodName, pParams, retryTimesLeft);
        }

        return retVal;
    }

}
