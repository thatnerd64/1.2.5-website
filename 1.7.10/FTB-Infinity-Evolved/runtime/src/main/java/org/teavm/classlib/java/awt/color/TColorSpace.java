package org.teavm.classlib.java.awt.color;

/** java.awt.color.ColorSpace: the predefined spaces, as used to describe raster layouts. */
public class TColorSpace {
    public static final int TYPE_RGB = 5;
    public static final int TYPE_GRAY = 6;
    public static final int CS_sRGB = 1000;
    public static final int CS_LINEAR_RGB = 1004;
    public static final int CS_CIEXYZ = 1001;
    public static final int CS_PYCC = 1002;
    public static final int CS_GRAY = 1003;

    private static final TColorSpace SRGB = new TColorSpace(TYPE_RGB, 3);
    private static final TColorSpace LINEAR_RGB = new TColorSpace(TYPE_RGB, 3);
    private static final TColorSpace GRAY = new TColorSpace(TYPE_GRAY, 1);
    public static final int TYPE_XYZ = 0;
    private static final TColorSpace CIEXYZ = new TColorSpace(TYPE_XYZ, 3);

    private final int type;
    private final int numComponents;

    protected TColorSpace(int type, int numComponents) {
        this.type = type;
        this.numComponents = numComponents;
    }

    public static TColorSpace getInstance(int colorspace) {
        switch (colorspace) {
            case CS_sRGB:
                return SRGB;
            case CS_LINEAR_RGB:
                return LINEAR_RGB;
            case CS_GRAY:
                return GRAY;
            case CS_CIEXYZ:
                return CIEXYZ;
            default:
                throw new IllegalArgumentException("Unknown color space");
        }
    }

    public boolean isCS_sRGB() {
        return this == SRGB;
    }

    public int getType() {
        return type;
    }

    public int getNumComponents() {
        return numComponents;
    }

    public float getMinValue(int component) {
        return 0f;
    }

    public float getMaxValue(int component) {
        return this == CIEXYZ ? 1f + 32767f / 32768f : 1f;
    }

    public String getName(int component) {
        String names = this == CIEXYZ ? "XYZ" : type == TYPE_GRAY ? "G" : "RGB";
        return component < names.length() ? String.valueOf(names.charAt(component)) : "Unnamed color component(" + component + ")";
    }

    // Conversions as the JDK's ICC profiles do them: sRGB with its transfer curve, the CIE XYZ space relative to a
    // D50 white point (ExtraUtilities' CIELab colour space builds on these)
    private static float toLinear(float c) {
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    private static float fromLinear(float c) {
        float v = c <= 0.0031308f ? c * 12.92f : (float) (1.055 * Math.pow(c, 1 / 2.4) - 0.055);
        return Math.max(0f, Math.min(1f, v));
    }

    private static float[] linearRgbToXyz(float r, float g, float b) {
        return new float[] {
            0.4360747f * r + 0.3850649f * g + 0.1430804f * b,
            0.2225045f * r + 0.7168786f * g + 0.0606169f * b,
            0.0139322f * r + 0.0971045f * g + 0.7141733f * b
        };
    }

    private static float[] xyzToLinearRgb(float x, float y, float z) {
        return new float[] {
            3.1338561f * x - 1.6168667f * y - 0.4906146f * z,
            -0.9787684f * x + 1.9161415f * y + 0.0334540f * z,
            0.0719453f * x - 0.2289914f * y + 1.4052427f * z
        };
    }

    public float[] toCIEXYZ(float[] value) {
        if (this == CIEXYZ) {
            return value.clone();
        }
        if (type == TYPE_GRAY) {
            float l = value[0];
            return linearRgbToXyz(l, l, l);
        }
        if (this == LINEAR_RGB) {
            return linearRgbToXyz(value[0], value[1], value[2]);
        }
        return linearRgbToXyz(toLinear(value[0]), toLinear(value[1]), toLinear(value[2]));
    }

    public float[] fromCIEXYZ(float[] value) {
        if (this == CIEXYZ) {
            return value.clone();
        }
        float[] lin = xyzToLinearRgb(value[0], value[1], value[2]);
        if (type == TYPE_GRAY) {
            return new float[] {Math.max(0f, Math.min(1f, value[1]))};
        }
        if (this == LINEAR_RGB) {
            return new float[] {clamp(lin[0]), clamp(lin[1]), clamp(lin[2])};
        }
        return new float[] {fromLinear(lin[0]), fromLinear(lin[1]), fromLinear(lin[2])};
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public float[] toRGB(float[] value) {
        if (this == SRGB) {
            return value.clone();
        }
        return SRGB.fromCIEXYZ(toCIEXYZ(value));
    }

    public float[] fromRGB(float[] rgb) {
        if (this == SRGB) {
            return rgb.clone();
        }
        return fromCIEXYZ(SRGB.toCIEXYZ(rgb));
    }
}
