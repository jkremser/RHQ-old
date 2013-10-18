package org.rhq.core.domain.cloud;

import java.io.Serializable;

/**
 * @author John Sanda
 */
public class StorageClusterSettings implements Serializable {

    private static final long serialVersionUID = 1;

    private int cqlPort;

    private int gossipPort;
    
    private Boolean automaticDeployment;
    
    private String password;

    public int getCqlPort() {
        return cqlPort;
    }

    public void setCqlPort(int cqlPort) {
        this.cqlPort = cqlPort;
    }

    public int getGossipPort() {
        return gossipPort;
    }

    public void setGossipPort(int gossipPort) {
        this.gossipPort = gossipPort;
    }

    public Boolean getAutomaticDeployment() {
        return automaticDeployment;
    }

    public void setAutomaticDeployment(Boolean automaticDeployment) {
        this.automaticDeployment = automaticDeployment;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageClusterSettings that = (StorageClusterSettings) o;

        if (cqlPort != that.cqlPort) return false;
        if (gossipPort != that.gossipPort) return false;
        if (automaticDeployment != that.automaticDeployment) return false;
        if (password != that.password) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = cqlPort;
        result = 29 * result + gossipPort;
        result = 29 * result + (automaticDeployment ? 1231 : 1237);
        result = 29 * result + (password == null ? 0 : password.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "StorageClusterSettings[cqlPort=" + cqlPort + ", gossipPort=" + gossipPort + ", automaticDeployment="
            + automaticDeployment + ", password=********]";
    }
}
