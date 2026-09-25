package org.teavm.classlib.java.io;

public class TNotActiveException extends java.io.ObjectStreamException {
    private static final long serialVersionUID = 1L;

    public TNotActiveException() {
        super();
    }

    public TNotActiveException(String message) {
        super(message);
    }
}
