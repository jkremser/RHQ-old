package org.rhq.enterprise.server.resource.relationship;

public class ResourceRelChangeableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResourceRelChangeableException() {
    }

    public ResourceRelChangeableException(String message) {
        super(message);
    }

    public ResourceRelChangeableException(Throwable cause) {
        super(cause);
    }

    public ResourceRelChangeableException(String message, Throwable cause) {
        super(message, cause);
    }

}