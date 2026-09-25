package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

/** LWJGL's Display, backed by the page canvas (see retro.gl.Display). */
public final class Display {
    private static DisplayMode mode = new DisplayMode(854, 480);
    private static java.awt.Canvas parent;
    private static boolean fullscreen;

    private Display() {
    }

    public static void create() throws LWJGLException {
        create(new PixelFormat());
    }

    public static void create(PixelFormat format) throws LWJGLException {
        try {
            retro.gl.Display.create();
        } catch (RuntimeException e) {
            throw new LWJGLException(e.getMessage());
        }
    }

    public static void create(PixelFormat format, Object shared) throws LWJGLException {
        create(format);
    }

    public static void destroy() {
    }

    public static boolean isCreated() {
        return retro.gl.Display.isCreated();
    }

    public static void update() {
        update(true);
    }

    public static void update(boolean processMessages) {
        retro.gl.Display.update();
    }

    public static void processMessages() {
    }

    public static void swapBuffers() {
        retro.gl.Display.update();
    }

    public static void sync(int fps) {
    }

    public static void setVSyncEnabled(boolean sync) {
    }

    public static void setSwapInterval(int value) {
    }

    public static boolean isActive() {
        return retro.gl.Display.isActive();
    }

    public static boolean isVisible() {
        return retro.gl.Display.isVisible();
    }

    public static boolean isDirty() {
        return false;
    }

    public static boolean isCloseRequested() {
        return false;
    }

    public static void setTitle(String title) {
        retro.gl.Display.setTitle(title);
    }

    public static String getTitle() {
        return retro.gl.Display.getTitle();
    }

    public static void setDisplayMode(DisplayMode m) {
        mode = m;
    }

    public static DisplayMode getDisplayMode() {
        if (retro.gl.Display.isCreated()) {
            return new DisplayMode(retro.gl.Display.canvasWidth(), retro.gl.Display.canvasHeight());
        }
        return mode;
    }

    public static DisplayMode getDesktopDisplayMode() {
        return new DisplayMode(retro.gl.Display.screenWidth(), retro.gl.Display.screenHeight(), 32, 60, true);
    }

    public static DisplayMode[] getAvailableDisplayModes() {
        return new DisplayMode[] { getDesktopDisplayMode() };
    }

    public static void setFullscreen(boolean on) {
        fullscreen = on;
        retro.gl.Display.setFullscreen(on);
    }

    public static void setDisplayModeAndFullscreen(DisplayMode m) {
        mode = m;
    }

    public static boolean isFullscreen() {
        return retro.gl.Display.isFullscreen();
    }

    public static void setParent(java.awt.Canvas canvas) {
        parent = canvas;
    }

    public static java.awt.Canvas getParent() {
        return parent;
    }

    public static void setResizable(boolean resizable) {
    }

    public static boolean isResizable() {
        return true;
    }

    public static boolean wasResized() {
        return false;
    }

    public static int getWidth() {
        return retro.gl.Display.canvasWidth();
    }

    public static int getHeight() {
        return retro.gl.Display.canvasHeight();
    }

    public static int getX() {
        return 0;
    }

    public static int getY() {
        return 0;
    }

    public static void setLocation(int x, int y) {
    }

    public static int setIcon(java.nio.ByteBuffer[] icons) {
        return 0;
    }

    public static void setInitialBackground(float red, float green, float blue) {
    }

    public static void makeCurrent() {
    }

    public static void releaseContext() {
    }

    public static boolean isCurrent() {
        return true;
    }

    public static String getAdapter() {
        return "WebGL";
    }

    public static String getVersion() {
        return "2";
    }
}
