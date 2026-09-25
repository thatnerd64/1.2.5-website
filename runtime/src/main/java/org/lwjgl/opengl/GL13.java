package org.lwjgl.opengl;

/** LWJGL GL13 for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class GL13 {
    public static final int GL_TEXTURE0 = 0x84c0;
    public static final int GL_TEXTURE1 = 0x84c1;
    public static final int GL_TEXTURE2 = 0x84c2;
    public static final int GL_TEXTURE3 = 0x84c3;
    public static final int GL_TEXTURE4 = 0x84c4;
    public static final int GL_TEXTURE5 = 0x84c5;
    public static final int GL_TEXTURE6 = 0x84c6;
    public static final int GL_TEXTURE7 = 0x84c7;
    public static final int GL_TEXTURE8 = 0x84c8;
    public static final int GL_TEXTURE9 = 0x84c9;
    public static final int GL_TEXTURE10 = 0x84ca;
    public static final int GL_TEXTURE11 = 0x84cb;
    public static final int GL_TEXTURE12 = 0x84cc;
    public static final int GL_TEXTURE13 = 0x84cd;
    public static final int GL_TEXTURE14 = 0x84ce;
    public static final int GL_TEXTURE15 = 0x84cf;
    public static final int GL_TEXTURE16 = 0x84d0;
    public static final int GL_TEXTURE17 = 0x84d1;
    public static final int GL_TEXTURE18 = 0x84d2;
    public static final int GL_TEXTURE19 = 0x84d3;
    public static final int GL_TEXTURE20 = 0x84d4;
    public static final int GL_TEXTURE21 = 0x84d5;
    public static final int GL_TEXTURE22 = 0x84d6;
    public static final int GL_TEXTURE23 = 0x84d7;
    public static final int GL_TEXTURE24 = 0x84d8;
    public static final int GL_TEXTURE25 = 0x84d9;
    public static final int GL_TEXTURE26 = 0x84da;
    public static final int GL_TEXTURE27 = 0x84db;
    public static final int GL_TEXTURE28 = 0x84dc;
    public static final int GL_TEXTURE29 = 0x84dd;
    public static final int GL_TEXTURE30 = 0x84de;
    public static final int GL_TEXTURE31 = 0x84df;
    public static final int GL_ACTIVE_TEXTURE = 0x84e0;
    public static final int GL_CLIENT_ACTIVE_TEXTURE = 0x84e1;
    public static final int GL_MAX_TEXTURE_UNITS = 0x84e2;
    public static final int GL_NORMAL_MAP = 0x8511;
    public static final int GL_REFLECTION_MAP = 0x8512;
    public static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    public static final int GL_TEXTURE_BINDING_CUBE_MAP = 0x8514;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851a;
    public static final int GL_PROXY_TEXTURE_CUBE_MAP = 0x851b;
    public static final int GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851c;
    public static final int GL_COMPRESSED_ALPHA = 0x84e9;
    public static final int GL_COMPRESSED_LUMINANCE = 0x84ea;
    public static final int GL_COMPRESSED_LUMINANCE_ALPHA = 0x84eb;
    public static final int GL_COMPRESSED_INTENSITY = 0x84ec;
    public static final int GL_COMPRESSED_RGB = 0x84ed;
    public static final int GL_COMPRESSED_RGBA = 0x84ee;
    public static final int GL_TEXTURE_COMPRESSION_HINT = 0x84ef;
    public static final int GL_TEXTURE_COMPRESSED_IMAGE_SIZE = 0x86a0;
    public static final int GL_TEXTURE_COMPRESSED = 0x86a1;
    public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0x86a2;
    public static final int GL_COMPRESSED_TEXTURE_FORMATS = 0x86a3;
    public static final int GL_MULTISAMPLE = 0x809d;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0x809e;
    public static final int GL_SAMPLE_ALPHA_TO_ONE = 0x809f;
    public static final int GL_SAMPLE_COVERAGE = 0x80a0;
    public static final int GL_SAMPLE_BUFFERS = 0x80a8;
    public static final int GL_SAMPLES = 0x80a9;
    public static final int GL_SAMPLE_COVERAGE_VALUE = 0x80aa;
    public static final int GL_SAMPLE_COVERAGE_INVERT = 0x80ab;
    public static final int GL_MULTISAMPLE_BIT = 0x20000000;
    public static final int GL_TRANSPOSE_MODELVIEW_MATRIX = 0x84e3;
    public static final int GL_TRANSPOSE_PROJECTION_MATRIX = 0x84e4;
    public static final int GL_TRANSPOSE_TEXTURE_MATRIX = 0x84e5;
    public static final int GL_TRANSPOSE_COLOR_MATRIX = 0x84e6;
    public static final int GL_COMBINE = 0x8570;
    public static final int GL_COMBINE_RGB = 0x8571;
    public static final int GL_COMBINE_ALPHA = 0x8572;
    public static final int GL_SOURCE0_RGB = 0x8580;
    public static final int GL_SOURCE1_RGB = 0x8581;
    public static final int GL_SOURCE2_RGB = 0x8582;
    public static final int GL_SOURCE0_ALPHA = 0x8588;
    public static final int GL_SOURCE1_ALPHA = 0x8589;
    public static final int GL_SOURCE2_ALPHA = 0x858a;
    public static final int GL_OPERAND0_RGB = 0x8590;
    public static final int GL_OPERAND1_RGB = 0x8591;
    public static final int GL_OPERAND2_RGB = 0x8592;
    public static final int GL_OPERAND0_ALPHA = 0x8598;
    public static final int GL_OPERAND1_ALPHA = 0x8599;
    public static final int GL_OPERAND2_ALPHA = 0x859a;
    public static final int GL_RGB_SCALE = 0x8573;
    public static final int GL_ADD_SIGNED = 0x8574;
    public static final int GL_INTERPOLATE = 0x8575;
    public static final int GL_SUBTRACT = 0x84e7;
    public static final int GL_CONSTANT = 0x8576;
    public static final int GL_PRIMARY_COLOR = 0x8577;
    public static final int GL_PREVIOUS = 0x8578;
    public static final int GL_DOT3_RGB = 0x86ae;
    public static final int GL_DOT3_RGBA = 0x86af;
    public static final int GL_CLAMP_TO_BORDER = 0x812d;

    private GL13() {
    }

    public static void glActiveTexture(int texture) { retro.gl.GLEmu.activeTexture(texture); }
    public static void glClientActiveTexture(int texture) { retro.gl.GLEmu.clientActiveTexture(texture); }
    public static void glMultiTexCoord2f(int target, float s, float t) { retro.gl.GLEmu.multiTexCoord(target - GL_TEXTURE0, s, t); }
    public static void glMultiTexCoord2d(int target, double s, double t) { retro.gl.GLEmu.multiTexCoord(target - GL_TEXTURE0, (float) s, (float) t); }
    public static void glMultiTexCoord2i(int target, int s, int t) { retro.gl.GLEmu.multiTexCoord(target - GL_TEXTURE0, s, t); }
    public static void glMultiTexCoord2s(int target, short s, short t) { retro.gl.GLEmu.multiTexCoord(target - GL_TEXTURE0, s, t); }
    public static void glSampleCoverage(float value, boolean invert) { }
}
