package org.teavm.classlib.java.awt.image;

public class TWritableRaster extends TRaster {
    TWritableRaster(TBufferedImage image) {
        super(image);
    }

    public void setSample(int x, int y, int b, int value) {
        int i = y * image.getWidth() + x;
        int argb = image.pixels[i];
        int shift = b == 0 ? 16 : b == 1 ? 8 : b == 2 ? 0 : 24;
        image.pixels[i] = (argb & ~(0xFF << shift)) | ((value & 0xFF) << shift);
    }

    public void setPixel(int x, int y, int[] values) {
        for (int b = 0; b < values.length && b < 4; b++) {
            setSample(x, y, b, values[b]);
        }
    }

    public void setPixels(int x, int y, int w, int h, int[] values) {
        int bands = getNumBands();
        int k = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                for (int b = 0; b < bands; b++) {
                    setSample(x + i, y + j, b, values[k++]);
                }
            }
        }
    }

    public void setDataElements(int x, int y, Object data) {
        image.pixels[y * image.getWidth() + x] = ((int[]) data)[0];
    }
}
