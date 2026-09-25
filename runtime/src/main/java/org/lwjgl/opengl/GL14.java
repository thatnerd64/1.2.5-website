package org.lwjgl.opengl;

/** LWJGL GL14 for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class GL14 {
    public static final int GL_GENERATE_MIPMAP = 0x8191;
    public static final int GL_GENERATE_MIPMAP_HINT = 0x8192;
    public static final int GL_DEPTH_COMPONENT16 = 0x81a5;
    public static final int GL_DEPTH_COMPONENT24 = 0x81a6;
    public static final int GL_DEPTH_COMPONENT32 = 0x81a7;
    public static final int GL_TEXTURE_DEPTH_SIZE = 0x884a;
    public static final int GL_DEPTH_TEXTURE_MODE = 0x884b;
    public static final int GL_TEXTURE_COMPARE_MODE = 0x884c;
    public static final int GL_TEXTURE_COMPARE_FUNC = 0x884d;
    public static final int GL_COMPARE_R_TO_TEXTURE = 0x884e;
    public static final int GL_FOG_COORDINATE_SOURCE = 0x8450;
    public static final int GL_FOG_COORDINATE = 0x8451;
    public static final int GL_FRAGMENT_DEPTH = 0x8452;
    public static final int GL_CURRENT_FOG_COORDINATE = 0x8453;
    public static final int GL_FOG_COORDINATE_ARRAY_TYPE = 0x8454;
    public static final int GL_FOG_COORDINATE_ARRAY_STRIDE = 0x8455;
    public static final int GL_FOG_COORDINATE_ARRAY_POINTER = 0x8456;
    public static final int GL_FOG_COORDINATE_ARRAY = 0x8457;
    public static final int GL_POINT_SIZE_MIN = 0x8126;
    public static final int GL_POINT_SIZE_MAX = 0x8127;
    public static final int GL_POINT_FADE_THRESHOLD_SIZE = 0x8128;
    public static final int GL_POINT_DISTANCE_ATTENUATION = 0x8129;
    public static final int GL_COLOR_SUM = 0x8458;
    public static final int GL_CURRENT_SECONDARY_COLOR = 0x8459;
    public static final int GL_SECONDARY_COLOR_ARRAY_SIZE = 0x845a;
    public static final int GL_SECONDARY_COLOR_ARRAY_TYPE = 0x845b;
    public static final int GL_SECONDARY_COLOR_ARRAY_STRIDE = 0x845c;
    public static final int GL_SECONDARY_COLOR_ARRAY_POINTER = 0x845d;
    public static final int GL_SECONDARY_COLOR_ARRAY = 0x845e;
    public static final int GL_BLEND_DST_RGB = 0x80c8;
    public static final int GL_BLEND_SRC_RGB = 0x80c9;
    public static final int GL_BLEND_DST_ALPHA = 0x80ca;
    public static final int GL_BLEND_SRC_ALPHA = 0x80cb;
    public static final int GL_INCR_WRAP = 0x8507;
    public static final int GL_DECR_WRAP = 0x8508;
    public static final int GL_TEXTURE_FILTER_CONTROL = 0x8500;
    public static final int GL_TEXTURE_LOD_BIAS = 0x8501;
    public static final int GL_MAX_TEXTURE_LOD_BIAS = 0x84fd;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_BLEND_COLOR = 0x8005;
    public static final int GL_BLEND_EQUATION = 0x8009;
    public static final int GL_FUNC_ADD = 0x8006;
    public static final int GL_FUNC_SUBTRACT = 0x800a;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800b;
    public static final int GL_MIN = 0x8007;
    public static final int GL_MAX = 0x8008;

    private GL14() {
    }

    public static void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        retro.gl.GLEmu.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    public static void glBlendEquation(int mode) { }

    public static void glBlendColor(float r, float g, float b, float a) { }

    public static void glWindowPos2f(float x, float y) { }
}
