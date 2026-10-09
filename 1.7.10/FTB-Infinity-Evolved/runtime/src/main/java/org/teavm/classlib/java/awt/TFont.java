package org.teavm.classlib.java.awt;

public class TFont {
    public static final int PLAIN = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final String MONOSPACED = "Monospaced";
    public static final String SANS_SERIF = "SansSerif";
    public static final String SERIF = "Serif";
    public static final String DIALOG = "Dialog";

    protected String name;
    protected int style;
    protected int size;

    public static final int TRUETYPE_FONT = 0;
    public static final int TYPE1_FONT = 1;

    /** Font files cannot be rasterised here: callers (Waila, Tinkers' book) fall back to Minecraft's own font. */
    public static TFont createFont(int format, java.io.InputStream in) throws java.io.IOException {
        throw new java.io.IOException("Loading font files is not supported in the browser");
    }

    public static TFont createFont(int format, java.io.File file) throws java.io.IOException {
        throw new java.io.IOException("Loading font files is not supported in the browser");
    }

    public TFont(String name, int style, int size) {
        this.name = name;
        this.style = style;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return name;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public boolean isBold() {
        return (style & BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & ITALIC) != 0;
    }

    public TFont deriveFont(float size) {
        return new TFont(name, style, (int) size);
    }

    public TFont deriveFont(int style) {
        return new TFont(name, style, size);
    }

    public TFont deriveFont(int style, float size) {
        return new TFont(name, style, (int) size);
    }
}
