package org.rhq.enterprise.server.resource.relationship;

public class ResourceRelWrongTypeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResourceRelWrongTypeException() {
    }

    public ResourceRelWrongTypeException(String message) {
        super(message);
    }

    public ResourceRelWrongTypeException(Throwable cause) {
        super(cause);
    }

    public ResourceRelWrongTypeException(String message, Throwable cause) {
        super(message, cause);
    }

}
