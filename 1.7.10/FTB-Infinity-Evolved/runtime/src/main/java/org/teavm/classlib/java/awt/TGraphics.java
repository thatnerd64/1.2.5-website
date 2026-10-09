package org.teavm.classlib.java.awt;

import org.teavm.classlib.java.awt.image.TImageObserver;

public abstract class TGraphics {
    protected TGraphics() {
    }

    public abstract TGraphics create();

    public abstract TColor getColor();

    public abstract void setColor(TColor c);

    public abstract TFont getFont();

    public abstract void setFont(TFont font);

    public TFontMetrics getFontMetrics() {
        return getFontMetrics(getFont());
    }

    public TFontMetrics getFontMetrics(TFont f) {
        return new TFontMetrics(f != null ? f : new TFont("Dialog", 0, 12)) {
        };
    }

    public abstract void fillRect(int x, int y, int width, int height);

    public abstract void clearRect(int x, int y, int width, int height);

    public void drawRect(int x, int y, int width, int height) {
        fillRect(x, y, width + 1, 1);
        fillRect(x, y + height, width + 1, 1);
        fillRect(x, y, 1, height + 1);
        fillRect(x + width, y, 1, height + 1);
    }

    public abstract void drawLine(int x1, int y1, int x2, int y2);

    public abstract void drawString(String str, int x, int y);

    public abstract boolean drawImage(TImage img, int x, int y, TImageObserver observer);

    public abstract boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer);

    public boolean drawImage(TImage img, int x, int y, TColor bgcolor, TImageObserver observer) {
        return drawImage(img, x, y, observer);
    }

    public abstract boolean drawImage(TImage img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
            int sy2, TImageObserver observer);

    public void translate(int x, int y) {
    }

    public void setClip(int x, int y, int width, int height) {
    }

    public void clipRect(int x, int y, int width, int height) {
    }

    public void setPaintMode() {
    }

    public void setXORMode(TColor c) {
    }

    public abstract void dispose();
}
