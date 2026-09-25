package org.teavm.classlib.java.awt;

public class TCursor {
    public static final int DEFAULT_CURSOR = 0;
    public static final int CROSSHAIR_CURSOR = 1;
    public static final int HAND_CURSOR = 12;
    private final int type;

    public TCursor(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static TCursor getPredefinedCursor(int type) {
        return new TCursor(type);
    }

    public static TCursor getDefaultCursor() {
        return new TCursor(DEFAULT_CURSOR);
    }
}
