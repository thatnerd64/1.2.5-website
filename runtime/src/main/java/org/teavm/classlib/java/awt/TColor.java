package org.teavm.classlib.java.awt;

/** java.awt.Color (subset). */
public class TColor {
    public static final TColor white = new TColor(255, 255, 255);
    public static final TColor WHITE = white;
    public static final TColor lightGray = new TColor(192, 192, 192);
    public static final TColor LIGHT_GRAY = lightGray;
    public static final TColor gray = new TColor(128, 128, 128);
    public static final TColor GRAY = gray;
    public static final TColor darkGray = new TColor(64, 64, 64);
    public static final TColor DARK_GRAY = darkGray;
    public static final TColor black = new TColor(0, 0, 0);
    public static final TColor BLACK = black;
    public static final TColor red = new TColor(255, 0, 0);
    public static final TColor RED = red;
    public static final TColor pink = new TColor(255, 175, 175);
    public static final TColor PINK = pink;
    public static final TColor orange = new TColor(255, 200, 0);
    public static final TColor ORANGE = orange;
    public static final TColor yellow = new TColor(255, 255, 0);
    public static final TColor YELLOW = yellow;
    public static final TColor green = new TColor(0, 255, 0);
    public static final TColor GREEN = green;
    public static final TColor magenta = new TColor(255, 0, 255);
    public static final TColor MAGENTA = magenta;
    public static final TColor cyan = new TColor(0, 255, 255);
    public static final TColor CYAN = cyan;
    public static final TColor blue = new TColor(0, 0, 255);
    public static final TColor BLUE = blue;

    private final int value;

    public TColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public TColor(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public TColor(float r, float g, float b) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5));
    }

    public TColor(float r, float g, float b, float a) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
    }

    public TColor(int rgb) {
        value = 0xFF000000 | rgb;
    }

    public TColor(int rgba, boolean hasAlpha) {
        value = hasAlpha ? rgba : 0xFF000000 | rgba;
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return value & 0xFF;
    }

    public int getAlpha() {
        return (value >>> 24) & 0xFF;
    }

    public int getRGB() {
        return value;
    }

    public TColor brighter() {
        return new TColor(Math.min(255, (int) (getRed() / 0.7)), Math.min(255, (int) (getGreen() / 0.7)),
                Math.min(255, (int) (getBlue() / 0.7)), getAlpha());
    }

    public TColor darker() {
        return new TColor((int) (getRed() * 0.7), (int) (getGreen() * 0.7), (int) (getBlue() * 0.7), getAlpha());
    }

    public float[] getRGBComponents(float[] out) {
        if (out == null) {
            out = new float[4];
        }
        out[0] = getRed() / 255f;
        out[1] = getGreen() / 255f;
        out[2] = getBlue() / 255f;
        out[3] = getAlpha() / 255f;
        return out;
    }

    public float[] getRGBColorComponents(float[] out) {
        if (out == null) {
            out = new float[3];
        }
        out[0] = getRed() / 255f;
        out[1] = getGreen() / 255f;
        out[2] = getBlue() / 255f;
        return out;
    }

    public static TColor decode(String s) {
        return new TColor(Integer.decode(s));
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                default:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = Math.max(r, Math.max(g, b));
        int cmin = Math.min(r, Math.min(g, b));
        float brightness = cmax / 255.0f;
        float saturation = cmax != 0 ? (float) (cmax - cmin) / cmax : 0;
        float hue;
        if (saturation == 0) {
            hue = 0;
        } else {
            float redc = (float) (cmax - r) / (cmax - cmin);
            float greenc = (float) (cmax - g) / (cmax - cmin);
            float bluec = (float) (cmax - b) / (cmax - cmin);
            if (r == cmax) {
                hue = bluec - greenc;
            } else if (g == cmax) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue = hue / 6.0f;
            if (hue < 0) {
                hue = hue + 1.0f;
            }
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static TColor getHSBColor(float h, float s, float b) {
        return new TColor(HSBtoRGB(h, s, b));
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TColor && ((TColor) obj).value == value;
    }

    @Override
    public String toString() {
        return "java.awt.Color[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
    }
}
