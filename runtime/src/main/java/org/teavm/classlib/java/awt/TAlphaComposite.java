package org.teavm.classlib.java.awt;

public final class TAlphaComposite implements TComposite {
    public static final int CLEAR = 1;
    public static final int SRC = 2;
    public static final int SRC_OVER = 3;
    public static final TAlphaComposite Clear = new TAlphaComposite(CLEAR, 1);
    public static final TAlphaComposite Src = new TAlphaComposite(SRC, 1);
    public static final TAlphaComposite SrcOver = new TAlphaComposite(SRC_OVER, 1);
    private final int rule;
    private final float alpha;

    private TAlphaComposite(int rule, float alpha) {
        this.rule = rule;
        this.alpha = alpha;
    }

    public static TAlphaComposite getInstance(int rule) {
        return new TAlphaComposite(rule, 1);
    }

    public static TAlphaComposite getInstance(int rule, float alpha) {
        return new TAlphaComposite(rule, alpha);
    }

    public int getRule() {
        return rule;
    }

    public float getAlpha() {
        return alpha;
    }
}
