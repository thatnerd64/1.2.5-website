package org.teavm.classlib.java.awt;

public abstract class TGraphics2D extends TGraphics {
    protected TGraphics2D() {
    }

    public abstract void setRenderingHint(TRenderingHints.Key key, Object value);

    public abstract Object getRenderingHint(TRenderingHints.Key key);

    public void setRenderingHints(java.util.Map<?, ?> hints) {
        for (java.util.Map.Entry<?, ?> e : hints.entrySet()) {
            if (e.getKey() instanceof TRenderingHints.Key) {
                setRenderingHint((TRenderingHints.Key) e.getKey(), e.getValue());
            }
        }
    }

    public void addRenderingHints(java.util.Map<?, ?> hints) {
        setRenderingHints(hints);
    }

    public void setComposite(TComposite composite) {
    }

    public TComposite getComposite() {
        return TAlphaComposite.SrcOver;
    }

    public void setBackground(TColor color) {
    }

    public void scale(double sx, double sy) {
    }

    public void rotate(double theta) {
    }

    public org.teavm.classlib.java.awt.geom.TAffineTransform getTransform() {
        return new org.teavm.classlib.java.awt.geom.TAffineTransform();
    }

    public void setTransform(org.teavm.classlib.java.awt.geom.TAffineTransform t) {
    }

    public void transform(org.teavm.classlib.java.awt.geom.TAffineTransform t) {
    }

    public TRectangle getClipBounds() {
        return null;
    }

    public void setStroke(Object stroke) {
    }

    public void setPaint(Object paint) {
        if (paint instanceof TColor) {
            setColor((TColor) paint);
        }
    }
}
