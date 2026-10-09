package org.teavm.classlib.java.awt.image;

/**
 * java.awt.image.DirectColorModel: pixels packed into an int by bit masks (ExtraUtilities derives textures through one).
 * Colour components are scaled to 0-255, as the JDK does for the default sRGB space.
 */
public class TDirectColorModel extends TColorModel {
    private final int bits;
    private final int[] masks;
    private final int[] shifts = new int[4];
    private final int[] sizes = new int[4];

    public TDirectColorModel(int bits, int rmask, int gmask, int bmask) {
        this(bits, rmask, gmask, bmask, 0);
    }

    public TDirectColorModel(int bits, int rmask, int gmask, int bmask, int amask) {
        super(amask != 0);
        this.bits = bits;
        this.masks = new int[] {rmask, gmask, bmask, amask};
        for (int i = 0; i < 4; i++) {
            int m = masks[i];
            if (m != 0) {
                shifts[i] = Integer.numberOfTrailingZeros(m);
                sizes[i] = Integer.bitCount(m);
            }
        }
    }

    private int component(int pixel, int i) {
        if (masks[i] == 0) {
            return i == 3 ? 255 : 0;
        }
        int v = (pixel & masks[i]) >>> shifts[i];
        int max = (1 << sizes[i]) - 1;
        return sizes[i] == 8 ? v : (v * 255 + max / 2) / max;
    }

    public int getRed(int pixel) {
        return component(pixel, 0);
    }

    public int getGreen(int pixel) {
        return component(pixel, 1);
    }

    public int getBlue(int pixel) {
        return component(pixel, 2);
    }

    public int getAlpha(int pixel) {
        return component(pixel, 3);
    }

    @Override
    public int getRGB(int pixel) {
        return (getAlpha(pixel) << 24) | (getRed(pixel) << 16) | (getGreen(pixel) << 8) | getBlue(pixel);
    }

    @Override
    public int getPixelSize() {
        return bits;
    }

    @Override
    public int getNumComponents() {
        return masks[3] != 0 ? 4 : 3;
    }

    public final int getRedMask() {
        return masks[0];
    }

    public final int getGreenMask() {
        return masks[1];
    }

    public final int getBlueMask() {
        return masks[2];
    }

    public final int getAlphaMask() {
        return masks[3];
    }
}
