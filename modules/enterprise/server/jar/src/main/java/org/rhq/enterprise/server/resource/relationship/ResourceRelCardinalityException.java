package org.rhq.enterprise.server.resource.relationship;

public class ResourceRelCardinalityException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResourceRelCardinalityException() {
    }

    public ResourceRelCardinalityException(String message) {
        super(message);
    }

    public ResourceRelCardinalityException(Throwable cause) {
        super(cause);
    }

    public ResourceRelCardinalityException(String message, Throwable cause) {
        super(message, cause);
    }

}