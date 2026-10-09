package org.lwjgl.opengl;

/** LWJGL's Drawable (a GL context holder); only the type is needed. */
public interface Drawable {
    boolean isCurrent() throws org.lwjgl.LWJGLException;

    void makeCurrent() throws org.lwjgl.LWJGLException;

    void releaseContext() throws org.lwjgl.LWJGLException;

    void destroy();
}
