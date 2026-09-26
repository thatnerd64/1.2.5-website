package org.teavm.classlib.java.util.concurrent;

public class TTimeoutException extends Exception {
    private static final long serialVersionUID = 1L;

    public TTimeoutException() {
        super();
    }

    public TTimeoutException(String message) {
        super(message);
    }
}
