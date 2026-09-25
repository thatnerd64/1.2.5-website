package org.lwjgl.opengl;

/** LWJGL GL12 for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class GL12 {
    public static final int GL_TEXTURE_BINDING_3D = 0x806a;
    public static final int GL_PACK_SKIP_IMAGES = 0x806b;
    public static final int GL_PACK_IMAGE_HEIGHT = 0x806c;
    public static final int GL_UNPACK_SKIP_IMAGES = 0x806d;
    public static final int GL_UNPACK_IMAGE_HEIGHT = 0x806e;
    public static final int GL_TEXTURE_3D = 0x806f;
    public static final int GL_PROXY_TEXTURE_3D = 0x8070;
    public static final int GL_TEXTURE_DEPTH = 0x8071;
    public static final int GL_TEXTURE_WRAP_R = 0x8072;
    public static final int GL_MAX_3D_TEXTURE_SIZE = 0x8073;
    public static final int GL_BGR = 0x80e0;
    public static final int GL_BGRA = 0x80e1;
    public static final int GL_UNSIGNED_BYTE_3_3_2 = 0x8032;
    public static final int GL_UNSIGNED_BYTE_2_3_3_REV = 0x8362;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
    public static final int GL_UNSIGNED_SHORT_5_6_5_REV = 0x8364;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4_REV = 0x8365;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
    public static final int GL_UNSIGNED_SHORT_1_5_5_5_REV = 0x8366;
    public static final int GL_UNSIGNED_INT_8_8_8_8 = 0x8035;
    public static final int GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367;
    public static final int GL_UNSIGNED_INT_10_10_10_2 = 0x8036;
    public static final int GL_UNSIGNED_INT_2_10_10_10_REV = 0x8368;
    public static final int GL_RESCALE_NORMAL = 0x803a;
    public static final int GL_LIGHT_MODEL_COLOR_CONTROL = 0x81f8;
    public static final int GL_SINGLE_COLOR = 0x81f9;
    public static final int GL_SEPARATE_SPECULAR_COLOR = 0x81fa;
    public static final int GL_CLAMP_TO_EDGE = 0x812f;
    public static final int GL_TEXTURE_MIN_LOD = 0x813a;
    public static final int GL_TEXTURE_MAX_LOD = 0x813b;
    public static final int GL_TEXTURE_BASE_LEVEL = 0x813c;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813d;
    public static final int GL_MAX_ELEMENTS_VERTICES = 0x80e8;
    public static final int GL_MAX_ELEMENTS_INDICES = 0x80e9;
    public static final int GL_ALIASED_POINT_SIZE_RANGE = 0x846d;
    public static final int GL_ALIASED_LINE_WIDTH_RANGE = 0x846e;
    public static final int GL_SMOOTH_POINT_SIZE_RANGE = 2834;
    public static final int GL_SMOOTH_POINT_SIZE_GRANULARITY = 2835;
    public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 2850;
    public static final int GL_SMOOTH_LINE_WIDTH_GRANULARITY = 2851;

    private GL12() {
    }

    public static void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type, java.nio.ByteBuffer pixels) { }

    public static void glDrawRangeElements(int mode, int start, int end, java.nio.IntBuffer indices) { GL11.glDrawElements(mode, indices); }
}
