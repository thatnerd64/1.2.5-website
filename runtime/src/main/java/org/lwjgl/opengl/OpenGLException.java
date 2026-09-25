package org.lwjgl.opengl;

public class OpenGLException extends RuntimeException {
    public OpenGLException() {
    }

    public OpenGLException(int errorCode) {
        super("OpenGL error " + errorCode);
    }

    public OpenGLException(String msg) {
        super(msg);
    }
}
