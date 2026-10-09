package org.teavm.classlib.java.awt;

public class THeadlessException extends UnsupportedOperationException {
    public THeadlessException() {
    }

    public THeadlessException(String msg) {
        super(msg);
    }
}
