package org.lwjgl.openal;

import org.lwjgl.LWJGLException;

/** OpenAL on the Web Audio API (see web/js/al.js). */
public final class AL {
    private static boolean created;

    private AL() {
    }

    public static void create() throws LWJGLException {
        if (!AL10.available()) {
            throw new LWJGLException("Web Audio is not available");
        }
        created = true;
    }

    public static void create(String deviceArguments, int contextFrequency, int contextRefresh,
            boolean contextSynchronized) throws LWJGLException {
        create();
    }

    public static void create(String deviceArguments, int contextFrequency, int contextRefresh,
            boolean contextSynchronized, boolean openDevice) throws LWJGLException {
        create();
    }

    public static boolean isCreated() {
        return created;
    }

    public static void destroy() {
        created = false;
    }
}
