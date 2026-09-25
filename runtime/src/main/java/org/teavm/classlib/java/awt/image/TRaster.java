package org.teavm.classlib.java.awt.image;

/** Raster over a BufferedImage's packed ARGB pixels (one int per pixel, one "band" per channel). */
public class TRaster {
    final TBufferedImage image;

    TRaster(TBufferedImage image) {
        this.image = image;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getMinX() {
        return 0;
    }

    public int getMinY() {
        return 0;
    }

    public int getNumBands() {
        return image.hasAlpha() ? 4 : 3;
    }

    public TDataBuffer getDataBuffer() {
        return image.dataBuffer;
    }

    public int getSample(int x, int y, int b) {
        int argb = image.pixels[y * image.getWidth() + x];
        return channel(argb, b);
    }

    private int channel(int argb, int b) {
        switch (b) {
            case 0:
                return (argb >> 16) & 0xFF;
            case 1:
                return (argb >> 8) & 0xFF;
            case 2:
                return argb & 0xFF;
            default:
                return (argb >>> 24) & 0xFF;
        }
    }

    public int[] getPixel(int x, int y, int[] out) {
        int bands = getNumBands();
        if (out == null) {
            out = new int[bands];
        }
        for (int b = 0; b < bands; b++) {
            out[b] = getSample(x, y, b);
        }
        return out;
    }

    public int[] getPixels(int x, int y, int w, int h, int[] out) {
        int bands = getNumBands();
        if (out == null) {
            out = new int[w * h * bands];
        }
        int k = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int argb = image.pixels[(y + j) * image.getWidth() + x + i];
                for (int b = 0; b < bands; b++) {
                    out[k++] = channel(argb, b);
                }
            }
        }
        return out;
    }

    public Object getDataElements(int x, int y, Object out) {
        int[] result = out instanceof int[] ? (int[]) out : new int[1];
        result[0] = image.pixels[y * image.getWidth() + x];
        return result;
    }
}
