package org.teavm.classlib.java.awt;

/** java.awt.Desktop: not supported in a browser (mods check isDesktopSupported() before opening folders). */
public final class TDesktop {
    private TDesktop() {
    }

    public static boolean isDesktopSupported() {
        return false;
    }

    public static TDesktop getDesktop() {
        throw new UnsupportedOperationException("Desktop API is not supported on the current platform");
    }

    public void browse(java.net.URI uri) throws java.io.IOException {
        throw new UnsupportedOperationException();
    }

    public void open(java.io.File file) throws java.io.IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isSupported(Action action) {
        return false;
    }

    public enum Action {
        OPEN, EDIT, PRINT, MAIL, BROWSE
    }
}
