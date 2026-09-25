package org.teavm.classlib.java.awt.image;

public class TColorModel {
    private final boolean alpha;

    TColorModel(boolean alpha) {
        this.alpha = alpha;
    }

    public static TColorModel getRGBdefault() {
        return new TColorModel(true);
    }

    public boolean hasAlpha() {
        return alpha;
    }

    public boolean isAlphaPremultiplied() {
        return false;
    }

    public int getPixelSize() {
        return alpha ? 32 : 24;
    }

    public int getNumComponents() {
        return alpha ? 4 : 3;
    }

    public int getRGB(int pixel) {
        return alpha ? pixel : pixel | 0xFF000000;
    }
}
