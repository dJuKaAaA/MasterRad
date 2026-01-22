package org.ftn.exception;

public class PrepareException extends RuntimeException {

    private final int status;

    public PrepareException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
