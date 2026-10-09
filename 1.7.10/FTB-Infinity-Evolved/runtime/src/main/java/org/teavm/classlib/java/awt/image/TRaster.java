package org.teavm.classlib.java.awt.image;

/** Raster over a BufferedImage's packed ARGB pixels (one int per pixel, one "band" per channel). */
public class TRaster {
    final TBufferedImage image;
    /** For interleaved byte rasters (Raster.createInterleavedRaster): the samples, stored as the JDK would. */
    byte[] bytes;
    int scanlineStride;
    int pixelStride;
    int[] bandOffsets;

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
        if (bytes != null) {
            return bandOffsets.length;
        }
        return image.hasAlpha() ? 4 : 3;
    }

    public TDataBuffer getDataBuffer() {
        return bytes != null ? new TDataBufferByte(bytes, bytes.length) : image.dataBuffer;
    }

    /**
     * An interleaved 8-bit raster (for example RGBA with band offsets {0, 1, 2, 3}). Its samples live in a byte
     * array, which a BufferedImage created over it keeps in sync with its ARGB pixels.
     */
    public static TWritableRaster createInterleavedRaster(int dataType, int w, int h, int scanlineStride,
            int pixelStride, int[] bandOffsets, org.teavm.classlib.java.awt.TPoint location) {
        if (dataType != TDataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        TWritableRaster r = new TWritableRaster(new TBufferedImage(w, h, TBufferedImage.TYPE_INT_ARGB));
        r.bytes = new byte[scanlineStride * (h - 1) + pixelStride * w];
        r.scanlineStride = scanlineStride;
        r.pixelStride = pixelStride;
        r.bandOffsets = bandOffsets.clone();
        return r;
    }

    public static TWritableRaster createInterleavedRaster(int dataType, int w, int h, int bands,
            org.teavm.classlib.java.awt.TPoint location) {
        int[] offsets = new int[bands];
        for (int i = 0; i < bands; i++) {
            offsets[i] = i;
        }
        return createInterleavedRaster(dataType, w, h, w * bands, bands, offsets, location);
    }

    /** ARGB of pixel i from the byte samples (bands in R, G, B[, A] order). */
    int readBytes(int x, int y) {
        int base = y * scanlineStride + x * pixelStride;
        int r = bytes[base + bandOffsets[0]] & 0xFF;
        if (bandOffsets.length < 3) {
            return 0xFF000000 | r << 16 | r << 8 | r;
        }
        int g = bytes[base + bandOffsets[1]] & 0xFF;
        int b = bytes[base + bandOffsets[2]] & 0xFF;
        int a = bandOffsets.length > 3 ? bytes[base + bandOffsets[3]] & 0xFF : 0xFF;
        return a << 24 | r << 16 | g << 8 | b;
    }

    void writeBytes(int x, int y, int argb) {
        int base = y * scanlineStride + x * pixelStride;
        if (bandOffsets.length < 3) {
            bytes[base + bandOffsets[0]] = (byte) (((argb >> 16 & 0xFF) * 77 + (argb >> 8 & 0xFF) * 150
                    + (argb & 0xFF) * 29) >> 8);
            return;
        }
        bytes[base + bandOffsets[0]] = (byte) (argb >> 16);
        bytes[base + bandOffsets[1]] = (byte) (argb >> 8);
        bytes[base + bandOffsets[2]] = (byte) argb;
        if (bandOffsets.length > 3) {
            bytes[base + bandOffsets[3]] = (byte) (argb >>> 24);
        }
    }

    public int getSample(int x, int y, int b) {
        if (bytes != null) {
            return bytes[y * scanlineStride + x * pixelStride + bandOffsets[b]] & 0xFF;
        }
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
        if (bytes != null) {
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    for (int b = 0; b < bands; b++) {
                        out[k++] = getSample(x + i, y + j, b);
                    }
                }
            }
            return out;
        }
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
