package org.example.exception;

public class FrameworkException extends RuntimeException{
    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(Throwable cause) {
        super("Unknown Exception", cause);
    }
}
