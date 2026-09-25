package org.lwjgl.opengl;

/** LWJGL ARBMultitexture for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class ARBMultitexture {
    public static final int GL_TEXTURE0_ARB = 0x84c0;
    public static final int GL_TEXTURE1_ARB = 0x84c1;
    public static final int GL_TEXTURE2_ARB = 0x84c2;
    public static final int GL_TEXTURE3_ARB = 0x84c3;
    public static final int GL_TEXTURE4_ARB = 0x84c4;
    public static final int GL_TEXTURE5_ARB = 0x84c5;
    public static final int GL_TEXTURE6_ARB = 0x84c6;
    public static final int GL_TEXTURE7_ARB = 0x84c7;
    public static final int GL_TEXTURE8_ARB = 0x84c8;
    public static final int GL_TEXTURE9_ARB = 0x84c9;
    public static final int GL_TEXTURE10_ARB = 0x84ca;
    public static final int GL_TEXTURE11_ARB = 0x84cb;
    public static final int GL_TEXTURE12_ARB = 0x84cc;
    public static final int GL_TEXTURE13_ARB = 0x84cd;
    public static final int GL_TEXTURE14_ARB = 0x84ce;
    public static final int GL_TEXTURE15_ARB = 0x84cf;
    public static final int GL_TEXTURE16_ARB = 0x84d0;
    public static final int GL_TEXTURE17_ARB = 0x84d1;
    public static final int GL_TEXTURE18_ARB = 0x84d2;
    public static final int GL_TEXTURE19_ARB = 0x84d3;
    public static final int GL_TEXTURE20_ARB = 0x84d4;
    public static final int GL_TEXTURE21_ARB = 0x84d5;
    public static final int GL_TEXTURE22_ARB = 0x84d6;
    public static final int GL_TEXTURE23_ARB = 0x84d7;
    public static final int GL_TEXTURE24_ARB = 0x84d8;
    public static final int GL_TEXTURE25_ARB = 0x84d9;
    public static final int GL_TEXTURE26_ARB = 0x84da;
    public static final int GL_TEXTURE27_ARB = 0x84db;
    public static final int GL_TEXTURE28_ARB = 0x84dc;
    public static final int GL_TEXTURE29_ARB = 0x84dd;
    public static final int GL_TEXTURE30_ARB = 0x84de;
    public static final int GL_TEXTURE31_ARB = 0x84df;
    public static final int GL_ACTIVE_TEXTURE_ARB = 0x84e0;
    public static final int GL_CLIENT_ACTIVE_TEXTURE_ARB = 0x84e1;
    public static final int GL_MAX_TEXTURE_UNITS_ARB = 0x84e2;

    private ARBMultitexture() {
    }

    public static void glActiveTextureARB(int texture) { retro.gl.GLEmu.activeTexture(texture); }
    public static void glClientActiveTextureARB(int texture) { retro.gl.GLEmu.clientActiveTexture(texture); }
    public static void glMultiTexCoord2fARB(int target, float s, float t) { retro.gl.GLEmu.multiTexCoord(target - GL_TEXTURE0_ARB, s, t); }
    public static void glMultiTexCoord2dARB(int target, double s, double t) { retro.gl.GLEmu.multiTexCoord(target - GL_TEXTURE0_ARB, (float) s, (float) t); }
}
