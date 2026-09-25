package org.lwjgl.openal;

public class OpenALException extends RuntimeException {
    public OpenALException() {
    }

    public OpenALException(int errorCode) {
        super("OpenAL error " + errorCode);
    }

    public OpenALException(String msg) {
        super(msg);
    }
}
