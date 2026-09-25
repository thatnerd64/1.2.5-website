package org.lwjgl.opengl;

public final class Util {
    private Util() {
    }

    public static void checkGLError() {
    }

    public static String translateGLErrorString(int errorCode) {
        return "GL error " + errorCode;
    }
}
