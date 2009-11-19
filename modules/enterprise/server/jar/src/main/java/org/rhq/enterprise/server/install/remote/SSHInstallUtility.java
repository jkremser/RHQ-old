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

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * @author Greg Hinkle
 */
public class SSHInstallUtility {

    private RemoteAccessInfo accessInfo;
    private Session session;
    private String agentFile = "rhq-enterprise-agent-1.4.0-SNAPSHOT.jar";
    private String agentDestination = "/tmp/rhqAgent";

    public SSHInstallUtility(RemoteAccessInfo accessInfo) {
        this.accessInfo = accessInfo;
    }

    public void connect() {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(accessInfo.getUser(), accessInfo.getHost(), accessInfo.getPort());

            session.setPassword(accessInfo.getPass());
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            //session.connect();
            session.connect(30000);   // making a connection with timeout.
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }


    public void disconnect() {
        session.disconnect();
    }

    public String getUname() {
        String result = executeCommand("uname -a");
        System.out.println("uname: " + result);
        return result;
    }


    public String[] pathDiscovery(String parentPath) {
        String result = executeCommand("ls " + parentPath);
        System.out.println(result);
        return null;

    }

    static final int DEFAULT_BUFFER_SIZE = 4096;
    static final long TIMEOUT = 10000L;
    static final long POLL_TIMEOUT = 1000L;

    private String executeCommand(String command, String description) {

        System.out.println("Running: " + description);
        String result = executeCommand(command);
        System.out.println("Result [" + description + "]: " + result);
        return result;
    }


    private String executeCommand(String command) {
        ChannelExec channel = null;
        int exitStatus = 0;

        try {
            channel = (ChannelExec) session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            InputStream is = channel.getInputStream();
            InputStream es = channel.getErrStream();


            channel.connect(10000); // connect and execute command


            String out = read(is, channel);
            String err = read(es, channel);

            //System.out.println("Output: " + out);
            if (err.length() > 0) {
//                System.out.println("Error [" + channel.getExitStatus() + "]: " + err);
                if (channel.getExitStatus() != 0 || out.length() == 0) {
                    return "Error[" + channel.getExitStatus() + "]: " + err;
                }
            }
            return out;

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
//            try {
//                stdoutReader.close();
//                stdoutReader.close();
//            } catch (IOException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
            if (channel != null) {
                channel.disconnect();
            }
        }
        return "exit: " + exitStatus;
    }


    public String read(InputStream is, Channel channel) throws IOException {

        // read command output
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final long endTime = System.currentTimeMillis() + TIMEOUT;
        while (System.currentTimeMillis() < endTime) {
            while (is.available() > 0) {
                int count = is.read(buffer, 0, DEFAULT_BUFFER_SIZE);
                if (count >= 0) {
                    bos.write(buffer, 0, count);
                } else {
                    break;
                }
            }
            if (channel.isClosed()) {
//                    int exitStatus = channel.getExitStatus();
//                    System.out.println("exit status: " + exitStatus);
                break;
            }
            try {
                Thread.sleep(POLL_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return bos.toString();
    }


    public String read2(BufferedReader reader) throws IOException {
        StringBuilder buf = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            buf.append(line + "\n");
        }
        return buf.toString();
    }

    public void agentStop() {
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";

        String result = executeCommand(agentWrapperScript + " stop", "Agent Stop");
    }

    public void agentStart() {
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";

        String result = executeCommand(agentWrapperScript + " start", "Agent Start");
    }

    public void agentStatus() {
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";

        String result = executeCommand(agentWrapperScript + " status", "Agent Status");
    }

    public void installAgent() {
        String agentPath = "/projects/rhq/dev-container/jbossas/server/default/deploy/rhq.ear/rhq-downloads/rhq-agent/" + agentFile;
        String result;


        result = executeCommand("uname -a", "Machine uname");

        result = executeCommand("java -version", "Java Version Check");

        result = executeCommand("mkdir -p " + agentDestination, "Create Agent Install Directory");


        System.out.println("Copying Agent Distribution");
        SSHFileSend.sendFile(session, agentPath, agentDestination);
        System.out.println("Agent Distribution Copied");


        result = executeCommand("java -jar " + agentDestination + "/" + agentFile + " --install", "Install Agent");


        AgentInstallInfo agentInfo = new AgentInstallInfo("172.31.2.2", "172.31.2.4");

        String agentScript = agentDestination + "/rhq-agent/bin/rhq-agent.sh ";
        String agentWrapperScript = agentDestination + "/rhq-agent/bin/rhq-agent-wrapper.sh ";


        String properties = agentInfo.getConfigurationStartString();

        // Tell the script to store a pid file to make the wrapper script work
        String pidFileProp = "export RHQ_AGENT_IN_BACKGROUND=" + agentDestination + "/rhq-agent/bin/rhq-agent.pid";

        String startCommand = pidFileProp + " ; nohup " + agentScript + properties + "&";
        result = executeCommand(startCommand, "Agent Start With Configuration");

    }


    public static void main(String[] args) {
        RemoteAccessInfo info = new RemoteAccessInfo(
                args[0], args[1], args[2]);
                

        SSHInstallUtility ssh = new SSHInstallUtility(info);
        ssh.connect();


        ssh.agentStatus();
        ssh.agentStop();

        ssh.installAgent();

        ssh.agentStatus();

        ssh.agentStop();
        ssh.agentStatus();
        ssh.agentStart();

        ssh.disconnect();


    }

}
