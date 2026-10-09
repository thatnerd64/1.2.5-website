package org.teavm.classlib.java.awt;

/** java.awt.BasicStroke: kept for its width only (the image Graphics draws one-pixel lines). */
public class TBasicStroke implements TStroke {
    public static final int CAP_BUTT = 0;
    public static final int CAP_ROUND = 1;
    public static final int CAP_SQUARE = 2;
    public static final int JOIN_MITER = 0;
    public static final int JOIN_ROUND = 1;
    public static final int JOIN_BEVEL = 2;

    private final float width;

    public TBasicStroke() {
        this(1f);
    }

    public TBasicStroke(float width) {
        this.width = width;
    }

    public TBasicStroke(float width, int cap, int join) {
        this(width);
    }

    public TBasicStroke(float width, int cap, int join, float miterlimit) {
        this(width);
    }

    public TBasicStroke(float width, int cap, int join, float miterlimit, float[] dash, float dashPhase) {
        this(width);
    }

    public float getLineWidth() {
        return width;
    }
}
