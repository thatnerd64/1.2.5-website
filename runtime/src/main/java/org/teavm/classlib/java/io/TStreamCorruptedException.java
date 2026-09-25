package org.teavm.classlib.java.io;

public class TStreamCorruptedException extends java.io.ObjectStreamException {
    private static final long serialVersionUID = 1L;

    public TStreamCorruptedException() {
        super();
    }

    public TStreamCorruptedException(String message) {
        super(message);
    }
}
