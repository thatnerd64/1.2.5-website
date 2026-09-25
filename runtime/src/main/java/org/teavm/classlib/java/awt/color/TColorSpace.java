package org.teavm.classlib.java.awt.color;

/** java.awt.color.ColorSpace: the predefined spaces, as used to describe raster layouts. */
public class TColorSpace {
    public static final int TYPE_RGB = 5;
    public static final int TYPE_GRAY = 6;
    public static final int CS_sRGB = 1000;
    public static final int CS_LINEAR_RGB = 1004;
    public static final int CS_CIEXYZ = 1001;
    public static final int CS_PYCC = 1002;
    public static final int CS_GRAY = 1003;

    private static final TColorSpace SRGB = new TColorSpace(TYPE_RGB, 3);
    private static final TColorSpace LINEAR_RGB = new TColorSpace(TYPE_RGB, 3);
    private static final TColorSpace GRAY = new TColorSpace(TYPE_GRAY, 1);

    private final int type;
    private final int numComponents;

    protected TColorSpace(int type, int numComponents) {
        this.type = type;
        this.numComponents = numComponents;
    }

    public static TColorSpace getInstance(int colorspace) {
        switch (colorspace) {
            case CS_sRGB:
                return SRGB;
            case CS_LINEAR_RGB:
                return LINEAR_RGB;
            case CS_GRAY:
                return GRAY;
            default:
                throw new IllegalArgumentException("Unknown color space");
        }
    }

    public boolean isCS_sRGB() {
        return this == SRGB;
    }

    public int getType() {
        return type;
    }

    public int getNumComponents() {
        return numComponents;
    }
}
