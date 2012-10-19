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
package org.rhq.modules.plugins.jbossas7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Provide connections to the AS and reading / writing date from/to it.
 * @author Heiko W. Rupp
 */
public class ASConnection {

    public static final String MANAGEMENT = "/management";
    private final Log log = LogFactory.getLog(ASConnection.class);
    URL url;
    String urlString;
    private ObjectMapper mapper;
    public static boolean verbose = false; // This is a variable on purpose, so devs can switch it on in the debugger or in the agent
    private HttpURLConnection conn;

    /**
     * Construct an ASConnection object. The real "physical" connection is done in
     * #executeRaw.
     * @param host Host of the DomainController or standalone server
     * @param port Port of the JSON api.
     */
    public ASConnection(String host, int port) {

        try {
            url = new URL("http", host, port, MANAGEMENT);
            urlString = url.toString();

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // read system property "as7plugin.verbose"
        verbose = Boolean.getBoolean("as7plugin.verbose");

        mapper = new ObjectMapper();
    }

    /**
     * Execute an operation against the domain api. This method is doing the
     * real work by talking to the remote server and sending JSON data, that
     * is obtained by serializing the operation.
     *
     * Please do not use this API , but execute()
     * @return JsonNode that describes the result
     * @param operation an Operation that should be run on the domain controller
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation)
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     * @see #executeComplex(org.rhq.modules.plugins.jbossas7.json.Operation)
     */
    public JsonNode executeRaw(Operation operation) {

        InputStream inputStream = null;
        BufferedReader br = null;
        InputStream es = null;
        long t1 = System.currentTimeMillis();
        try {

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();

            String result = mapper.writeValueAsString(operation);
            if (verbose) {
                log.info("Json to send: " + result);
            }
            mapper.writeValue(out, operation);

            out.flush();
            out.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            if (inputStream != null) {

                br = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    builder.append(line);
                }

                String outcome;
                JsonNode operationResult = null;
                if (builder.length() > 0) {
                    outcome = builder.toString();
                    operationResult = mapper.readTree(outcome);
                    if (verbose) {
                        ObjectMapper om2 = new ObjectMapper();
                        om2.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
                        String tmp = om2.writeValueAsString(operationResult);
                        log.info(tmp);
                    }
                } else {
                    outcome = "- no response from server -";
                    Result noResult = new Result();
                    noResult.setFailureDescription(outcome);
                    noResult.setOutcome("failure");
                    operationResult = mapper.valueToTree(noResult);
                }
                return operationResult;
            } else {
                log.error("IS was null and code was " + responseCode);
            }

        } catch (IOException e) {
            log.error("Failed to get data: " + e.getMessage());

            //the following code is in place to help keep-alive http connection re-use to occur.
            if (conn != null) {//on error conditions it's still necessary to read the response so JDK knows can reuse
                //the http connections behind the scenes.
                es = conn.getErrorStream();
                if (es != null) {
                    BufferedReader dr = new BufferedReader(new InputStreamReader(es));
                    String ignore = null;
                    try {
                        while ((ignore = dr.readLine()) != null) {
                            //already reported error. just empty stream.
                        }
                        es.close();
                    } catch (IOException e1) {
                    }
                }
            }

            Result failure = new Result();
            failure.setFailureDescription(e.getMessage());
            failure.setOutcome("failure");
            failure.setThrowable(e);

            JsonNode ret = mapper.valueToTree(failure);
            return ret;

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace(); // TODO: Customise this generated block
                }
            }
            if (es != null) {
                try {
                    es.close();
                } catch (IOException e) {
                    e.printStackTrace(); // TODO: Customise this generated block
                }
            }
            long t2 = System.currentTimeMillis();
            PluginStats stats = PluginStats.getInstance();
            stats.incrementRequestCount();
            stats.addRequestTime(t2 - t1);
        }

        return null;
    }

    /**
     * Execute the passed Operation and return its Result. This is a shortcut of
     * #execute(Operation, false)
     * @param op Operation to execute
     * @return Result of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public Result execute(Operation op) {
        return execute(op, false);
    }

    /**
     * Execute the passed Operation and return its ComplexResult. This is a shortcut of
     * #execute(Operation, true)
     * @param op Operation to execute
     * @return ComplexResult of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public ComplexResult executeComplex(Operation op) {
        return (ComplexResult) execute(op, true);
    }

    /**
     * Execute the passed Operation and return its Result. Depending on <i>isComplex</i>
     * the return type is a simple Result or a ComplexResult
     * @param op Operation to execute
     * @param isComplex should a complex result be returned?
     * @return ComplexResult of the execution
     */
    public Result execute(Operation op, boolean isComplex) {
        JsonNode node = executeRaw(op);

        if (node==null) {
            log.warn("Operation [" + op + "] returned null");
        }
        try {
            Result res;
            if (isComplex)
                res = mapper.readValue(node, ComplexResult.class);
            else
                res = mapper.readValue(node, Result.class);
            return res;
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Customise this generated block
            return null;
        }
    }

    public void writeValue(OutputStream out, Object value) throws IOException, JsonGenerationException,
        JsonMappingException {
        //    JsonGenerator jgen = _jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
        //    JsonGenerator jgen = mapper.createJsonGenerator(out, JsonEncoding.UTF8);
        //    JsonGenerator jgen = new Js
        //    boolean closed = false;
        //    try {
        //        writeValue(jgen, value);
        //        closed = true;
        //        jgen.close();
        //    } finally {
        //        if (!closed) {
        //            jgen.close();
        //        }
        //    }

    }

}
