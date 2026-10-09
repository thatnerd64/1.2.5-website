package org.lwjgl.opengl;

public final class EXTBlendFuncSeparate {
    public static final int GL_BLEND_DST_RGB_EXT = 0x80c8;
    public static final int GL_BLEND_SRC_RGB_EXT = 0x80c9;
    public static final int GL_BLEND_DST_ALPHA_EXT = 0x80ca;
    public static final int GL_BLEND_SRC_ALPHA_EXT = 0x80cb;

    private EXTBlendFuncSeparate() {
    }

    public static void glBlendFuncSeparateEXT(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GL14.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }
}
