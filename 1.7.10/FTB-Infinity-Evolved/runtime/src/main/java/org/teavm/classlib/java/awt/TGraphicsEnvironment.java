package org.teavm.classlib.java.awt;

/** java.awt.GraphicsEnvironment: there is no AWT display, so the environment is headless. */
public abstract class TGraphicsEnvironment {
    private static final TGraphicsEnvironment INSTANCE = new TGraphicsEnvironment() {
    };

    protected TGraphicsEnvironment() {
    }

    public static TGraphicsEnvironment getLocalGraphicsEnvironment() {
        return INSTANCE;
    }

    public static boolean isHeadless() {
        return true;
    }

    public boolean isHeadlessInstance() {
        return true;
    }
}
