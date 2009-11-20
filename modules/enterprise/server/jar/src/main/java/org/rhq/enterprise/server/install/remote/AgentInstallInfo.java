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
package org.rhq.enterprise.server.install.remote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class AgentInstallInfo implements Serializable {

    private String serverAddress;
    private String agentAddress;

    private String path;
    private String owner;
    private String version;


    public List<Step> steps = new ArrayList<Step>();

    public static final String SETUP_PROP = "rhq.agent.configuration-setup-flag";


    public static final String SERVER_ADDRESS_PROP = "rhq.agent.server.bind-address";
    public static final String SERVER_PORT_PROP = "rhq.agent.server.bind-port";

    public static final String AGENT_ADDRESS_PROP = "rhq.communications.connector.bind-address";
    public static final String AGENT_PORT_PROP = "rhq.communications.connector.bind-port";

    public static final int DEFAULT_SERVER_PORT = 7080;
    public static final int DEFAULT_AGENT_PORT = 16163;





    public AgentInstallInfo(String serverAddress, String agentAddress) {
        this.serverAddress = serverAddress;
        this.agentAddress = agentAddress;
    }

    public AgentInstallInfo(String path, String owner, String version) {
        this.path = path;
        this.owner = owner;
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public void addStep(Step step) {
        steps.add(step);
    }

    public List<Step> getSteps() {
        return steps;
    }




    public String getConfigurationStartString() {
        StringBuilder buf = new StringBuilder();

        buf.append("-D").append(SERVER_ADDRESS_PROP).append("=").append(serverAddress);
        buf.append(" ");

        buf.append("-D").append(AGENT_ADDRESS_PROP).append("=").append(agentAddress);
        buf.append(" ");

        buf.append("-D").append(SETUP_PROP).append("=").append("true");
        buf.append(" ");
        buf.append("-d ");

        return buf.toString();
    }



    public static class Step {
        int resultCode;
        String command;
        String description;
        String result;
        long time;

        public Step(int resultCode, String command, String description, String result, long time) {
            this.resultCode = resultCode;
            this.command = command;
            this.description = description;
            this.result = result;
            this.time = time;
        }

        public int getResultCode() {
            return resultCode;
        }

        public String getDescription() {
            return description;
        }

        public String getCommand() {
            return command;
        }

        public String getResult() {
            return result;
        }

        public long getTime() {
            return time;
        }
    }

}
