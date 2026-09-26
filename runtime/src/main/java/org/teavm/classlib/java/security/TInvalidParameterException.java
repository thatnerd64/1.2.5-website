package org.teavm.classlib.java.security;

public class TInvalidParameterException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public TInvalidParameterException() {
        super();
    }

    public TInvalidParameterException(String message) {
        super(message);
    }
}
