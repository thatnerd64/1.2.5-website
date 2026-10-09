package org.teavm.classlib.java.io;

public class TNotSerializableException extends java.io.ObjectStreamException {
    private static final long serialVersionUID = 1L;

    public TNotSerializableException() {
        super();
    }

    public TNotSerializableException(String message) {
        super(message);
    }
}
