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

import org.apache.xmlrpc.XmlRpcException;

public interface XmlRpcExecutor {
    /**
     * Execute an XMLRPC method
     * @param methodName
     * @param params
     * @param retryTimes
     * @return
     */
    public Object execute(String methodName, Object[] params, int retryTimes) throws XmlRpcException;

    /**
     * Execute an XMLRPC method
     * @param methodName 
     * @param params
     * @return
     */
    public Object execute(String methodName, Object[] params) throws XmlRpcException;

    /** Performs a request with the clients default configuration.
     * @param pMethodName The method being performed.
     * @param pParams The parameters.
     * @param retryTimes
     * @return The result object.
     * @throws XmlRpcException Performing the request failed.
     */
    public Object execute(String pMethodName, List pParams, int retryTimes) throws XmlRpcException;

    /** Performs a request with the clients default configuration.
     * @param pMethodName The method being performed.
     * @param pParams The parameters.
     * @return The result object.
     * @throws XmlRpcException Performing the request failed.
     */
    public Object execute(String pMethodName, List pParams) throws XmlRpcException;

}
