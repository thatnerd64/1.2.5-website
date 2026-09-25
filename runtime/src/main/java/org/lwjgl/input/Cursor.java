package org.lwjgl.input;

import java.nio.IntBuffer;

public class Cursor {
    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1;
    public static final int CURSOR_8_BIT_ALPHA = 2;
    public static final int CURSOR_ANIMATION = 4;

    public Cursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images,
            IntBuffer delays) {
    }

    public static int getMinCursorSize() {
        return 1;
    }

    public static int getMaxCursorSize() {
        return 64;
    }

    public static int getCapabilities() {
        return CURSOR_8_BIT_ALPHA;
    }

    public void destroy() {
    }
}
