package org.teavm.classlib.java.awt;

import java.util.HashMap;

public class TRenderingHints extends HashMap<Object, Object> {
    public abstract static class Key {
        private final int key;

        protected Key(int key) {
            this.key = key;
        }

        public boolean isCompatibleValue(Object value) {
            return true;
        }

        protected final int intKey() {
            return key;
        }
    }

    static final class SimpleKey extends Key {
        SimpleKey(int key) {
            super(key);
        }
    }

    public static final Key KEY_ANTIALIASING = new SimpleKey(1);
    public static final Object VALUE_ANTIALIAS_ON = "on";
    public static final Object VALUE_ANTIALIAS_OFF = "off";
    public static final Object VALUE_ANTIALIAS_DEFAULT = "default";
    public static final Key KEY_RENDERING = new SimpleKey(2);
    public static final Object VALUE_RENDER_SPEED = "speed";
    public static final Object VALUE_RENDER_QUALITY = "quality";
    public static final Object VALUE_RENDER_DEFAULT = "default";
    public static final Key KEY_INTERPOLATION = new SimpleKey(5);
    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = "nearest";
    public static final Object VALUE_INTERPOLATION_BILINEAR = "bilinear";
    public static final Object VALUE_INTERPOLATION_BICUBIC = "bicubic";
    public static final Key KEY_TEXT_ANTIALIASING = new SimpleKey(3);
    public static final Object VALUE_TEXT_ANTIALIAS_ON = "on";
    public static final Object VALUE_TEXT_ANTIALIAS_OFF = "off";
    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = "default";
    public static final Key KEY_COLOR_RENDERING = new SimpleKey(4);
    public static final Object VALUE_COLOR_RENDER_SPEED = "speed";
    public static final Object VALUE_COLOR_RENDER_QUALITY = "quality";
    public static final Key KEY_ALPHA_INTERPOLATION = new SimpleKey(6);
    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED = "speed";
    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY = "quality";
    public static final Key KEY_DITHERING = new SimpleKey(7);
    public static final Object VALUE_DITHER_DISABLE = "off";
    public static final Object VALUE_DITHER_ENABLE = "on";

    public TRenderingHints(Key key, Object value) {
        if (key != null) {
            put(key, value);
        }
    }
}
