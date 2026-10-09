package org.teavm.classlib.java.awt;

import org.teavm.classlib.java.awt.image.TImageObserver;

public abstract class TImage {
    public static final int SCALE_DEFAULT = 1;
    public static final int SCALE_FAST = 2;
    public static final int SCALE_SMOOTH = 4;
    public static final int SCALE_REPLICATE = 8;
    public static final int SCALE_AREA_AVERAGING = 16;

    public abstract int getWidth(TImageObserver observer);

    public abstract int getHeight(TImageObserver observer);

    public abstract TGraphics getGraphics();

    public TImage getScaledInstance(int width, int height, int hints) {
        org.teavm.classlib.java.awt.image.TBufferedImage result = new org.teavm.classlib.java.awt.image.TBufferedImage(
                width, height, org.teavm.classlib.java.awt.image.TBufferedImage.TYPE_INT_ARGB);
        TGraphics g = result.getGraphics();
        g.drawImage(this, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    public void flush() {
    }
}
