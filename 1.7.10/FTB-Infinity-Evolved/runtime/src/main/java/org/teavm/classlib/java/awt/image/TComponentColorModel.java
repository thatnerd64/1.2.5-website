package org.teavm.classlib.java.awt.image;

import org.teavm.classlib.java.awt.color.TColorSpace;

/** java.awt.image.ComponentColorModel: one sample per colour component (8-bit bytes here). */
public class TComponentColorModel extends TColorModel {
    private final TColorSpace space;

    public TComponentColorModel(TColorSpace colorSpace, int[] bits, boolean hasAlpha, boolean isAlphaPremultiplied,
            int transparency, int transferType) {
        super(hasAlpha);
        this.space = colorSpace;
    }

    public TComponentColorModel(TColorSpace colorSpace, boolean hasAlpha, boolean isAlphaPremultiplied,
            int transparency, int transferType) {
        this(colorSpace, null, hasAlpha, isAlphaPremultiplied, transparency, transferType);
    }

    public TColorSpace getColorSpace() {
        return space;
    }

    @Override
    public int getNumComponents() {
        return space.getNumComponents() + (hasAlpha() ? 1 : 0);
    }
}
