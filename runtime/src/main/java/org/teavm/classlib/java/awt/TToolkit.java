package org.teavm.classlib.java.awt;

import org.teavm.classlib.java.awt.datatransfer.TClipboard;

public class TToolkit {
    private static final TToolkit INSTANCE = new TToolkit();
    private static final TClipboard CLIPBOARD = new TClipboard("System");

    public static TToolkit getDefaultToolkit() {
        return INSTANCE;
    }

    public TClipboard getSystemClipboard() {
        return CLIPBOARD;
    }

    public TDimension getScreenSize() {
        return new TDimension(retro.gl.Display.screenWidth(), retro.gl.Display.screenHeight());
    }

    public int getScreenResolution() {
        return 96;
    }

    public void sync() {
    }

    public void beep() {
    }

    public TImage getImage(String file) {
        return null;
    }

    public TImage getImage(java.net.URL url) {
        return null;
    }

    public TImage createImage(byte[] data) {
        try {
            return org.teavm.classlib.javax.imageio.TImageIO.read(new java.io.ByteArrayInputStream(data));
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
