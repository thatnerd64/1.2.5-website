package org.teavm.classlib.java.awt;

public abstract class TFontMetrics {
    protected TFont font;

    protected TFontMetrics(TFont font) {
        this.font = font;
    }

    public TFont getFont() {
        return font;
    }

    public int getHeight() {
        return font.getSize() + 2;
    }

    public int getAscent() {
        return font.getSize();
    }

    public int getMaxAscent() {
        return getAscent();
    }

    public int getMaxDescent() {
        return getDescent();
    }

    public int getDescent() {
        return 2;
    }

    public int charWidth(char c) {
        return font.getSize() * 6 / 10;
    }

    public int stringWidth(String s) {
        return s.length() * charWidth('m');
    }
}
