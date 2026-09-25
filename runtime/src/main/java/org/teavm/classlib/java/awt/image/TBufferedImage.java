package org.teavm.classlib.java.awt.image;

import java.util.Hashtable;
import org.teavm.classlib.java.awt.TGraphics;
import org.teavm.classlib.java.awt.TGraphics2D;
import org.teavm.classlib.java.awt.TImage;

/**
 * java.awt.image.BufferedImage. Pixels are always stored as packed non-premultiplied ARGB ints, which is what
 * Minecraft and mods expect from {@code getRGB} and from the {@code DataBufferInt} of TYPE_INT_ARGB images.
 */
public class TBufferedImage extends TImage implements TRenderedImage {
    public static final int TYPE_CUSTOM = 0;
    public static final int TYPE_INT_RGB = 1;
    public static final int TYPE_INT_ARGB = 2;
    public static final int TYPE_INT_ARGB_PRE = 3;
    public static final int TYPE_INT_BGR = 4;
    public static final int TYPE_3BYTE_BGR = 5;
    public static final int TYPE_4BYTE_ABGR = 6;
    public static final int TYPE_4BYTE_ABGR_PRE = 7;
    public static final int TYPE_USHORT_565_RGB = 8;
    public static final int TYPE_USHORT_555_RGB = 9;
    public static final int TYPE_BYTE_GRAY = 10;
    public static final int TYPE_USHORT_GRAY = 11;
    public static final int TYPE_BYTE_BINARY = 12;
    public static final int TYPE_BYTE_INDEXED = 13;

    private final int width;
    private final int height;
    private final int type;
    final int[] pixels;
    final TDataBufferInt dataBuffer;
    private TWritableRaster raster;

    public TBufferedImage(int width, int height, int type) {
        this(width, height, type, new int[Math.max(0, width * height)]);
    }

    public TBufferedImage(TColorModel model, TWritableRaster raster, boolean premultiplied,
            Hashtable<?, ?> properties) {
        this(raster.getWidth(), raster.getHeight(), TYPE_INT_ARGB);
        System.arraycopy(raster.image.pixels, 0, pixels, 0, pixels.length);
    }

    /** Wraps existing ARGB pixel data (used by the PNG decoder). */
    public TBufferedImage(int width, int height, int type, int[] argb) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width (" + width + ") and height (" + height + ") must be > 0");
        }
        this.width = width;
        this.height = height;
        this.type = type;
        this.pixels = argb;
        this.dataBuffer = new TDataBufferInt(pixels, pixels.length);
    }

    boolean hasAlpha() {
        return type == TYPE_INT_ARGB || type == TYPE_INT_ARGB_PRE || type == TYPE_4BYTE_ABGR
                || type == TYPE_4BYTE_ABGR_PRE || type == TYPE_CUSTOM;
    }

    public int[] argbPixels() {
        return pixels;
    }

    public int getType() {
        return type;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth(TImageObserver observer) {
        return width;
    }

    @Override
    public int getHeight(TImageObserver observer) {
        return height;
    }

    public int getMinX() {
        return 0;
    }

    public int getMinY() {
        return 0;
    }

    public TColorModel getColorModel() {
        return new TColorModel(hasAlpha());
    }

    public int getTransparency() {
        return hasAlpha() ? 3 : 1;
    }

    private int store(int argb) {
        return hasAlpha() ? argb : argb | 0xFF000000;
    }

    public int getRGB(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
        return store(pixels[y * width + x]);
    }

    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        if (rgbArray == null) {
            rgbArray = new int[offset + h * scansize];
        }
        boolean alpha = hasAlpha();
        for (int y = 0; y < h; y++) {
            int src = (startY + y) * width + startX;
            int dst = offset + y * scansize;
            if (alpha) {
                System.arraycopy(pixels, src, rgbArray, dst, w);
            } else {
                for (int x = 0; x < w; x++) {
                    rgbArray[dst + x] = pixels[src + x] | 0xFF000000;
                }
            }
        }
        return rgbArray;
    }

    public void setRGB(int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
        pixels[y * width + x] = store(rgb);
    }

    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        boolean alpha = hasAlpha();
        for (int y = 0; y < h; y++) {
            int dst = (startY + y) * width + startX;
            int src = offset + y * scansize;
            if (alpha) {
                System.arraycopy(rgbArray, src, pixels, dst, w);
            } else {
                for (int x = 0; x < w; x++) {
                    pixels[dst + x] = rgbArray[src + x] | 0xFF000000;
                }
            }
        }
    }

    public TWritableRaster getRaster() {
        if (raster == null) {
            raster = new TWritableRaster(this);
        }
        return raster;
    }

    public TRaster getData() {
        return getRaster();
    }

    public TWritableRaster getAlphaRaster() {
        return hasAlpha() ? getRaster() : null;
    }

    public TBufferedImage getSubimage(int x, int y, int w, int h) {
        TBufferedImage sub = new TBufferedImage(w, h, type);
        for (int j = 0; j < h; j++) {
            System.arraycopy(pixels, (y + j) * width + x, sub.pixels, j * w, w);
        }
        return sub;
    }

    @Override
    public TGraphics getGraphics() {
        return createGraphics();
    }

    public TGraphics2D createGraphics() {
        return new TBufferedImageGraphics(this);
    }

    public boolean isAlphaPremultiplied() {
        return false;
    }

    public void flush() {
    }

    public Object getProperty(String name) {
        return null;
    }
}
