package org.lwjgl.opengl;

/** LWJGL EXTFramebufferObject for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class EXTFramebufferObject {
    public static final int GL_FRAMEBUFFER_EXT = 0x8d40;
    public static final int GL_RENDERBUFFER_EXT = 0x8d41;
    public static final int GL_STENCIL_INDEX1_EXT = 0x8d46;
    public static final int GL_STENCIL_INDEX4_EXT = 0x8d47;
    public static final int GL_STENCIL_INDEX8_EXT = 0x8d48;
    public static final int GL_STENCIL_INDEX16_EXT = 0x8d49;
    public static final int GL_RENDERBUFFER_WIDTH_EXT = 0x8d42;
    public static final int GL_RENDERBUFFER_HEIGHT_EXT = 0x8d43;
    public static final int GL_RENDERBUFFER_INTERNAL_FORMAT_EXT = 0x8d44;
    public static final int GL_RENDERBUFFER_RED_SIZE_EXT = 0x8d50;
    public static final int GL_RENDERBUFFER_GREEN_SIZE_EXT = 0x8d51;
    public static final int GL_RENDERBUFFER_BLUE_SIZE_EXT = 0x8d52;
    public static final int GL_RENDERBUFFER_ALPHA_SIZE_EXT = 0x8d53;
    public static final int GL_RENDERBUFFER_DEPTH_SIZE_EXT = 0x8d54;
    public static final int GL_RENDERBUFFER_STENCIL_SIZE_EXT = 0x8d55;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE_EXT = 0x8cd0;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME_EXT = 0x8cd1;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL_EXT = 0x8cd2;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE_EXT = 0x8cd3;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_3D_ZOFFSET_EXT = 0x8cd4;
    public static final int GL_COLOR_ATTACHMENT0_EXT = 0x8ce0;
    public static final int GL_COLOR_ATTACHMENT1_EXT = 0x8ce1;
    public static final int GL_COLOR_ATTACHMENT2_EXT = 0x8ce2;
    public static final int GL_COLOR_ATTACHMENT3_EXT = 0x8ce3;
    public static final int GL_COLOR_ATTACHMENT4_EXT = 0x8ce4;
    public static final int GL_COLOR_ATTACHMENT5_EXT = 0x8ce5;
    public static final int GL_COLOR_ATTACHMENT6_EXT = 0x8ce6;
    public static final int GL_COLOR_ATTACHMENT7_EXT = 0x8ce7;
    public static final int GL_COLOR_ATTACHMENT8_EXT = 0x8ce8;
    public static final int GL_COLOR_ATTACHMENT9_EXT = 0x8ce9;
    public static final int GL_COLOR_ATTACHMENT10_EXT = 0x8cea;
    public static final int GL_COLOR_ATTACHMENT11_EXT = 0x8ceb;
    public static final int GL_COLOR_ATTACHMENT12_EXT = 0x8cec;
    public static final int GL_COLOR_ATTACHMENT13_EXT = 0x8ced;
    public static final int GL_COLOR_ATTACHMENT14_EXT = 0x8cee;
    public static final int GL_COLOR_ATTACHMENT15_EXT = 0x8cef;
    public static final int GL_DEPTH_ATTACHMENT_EXT = 0x8d00;
    public static final int GL_STENCIL_ATTACHMENT_EXT = 0x8d20;
    public static final int GL_FRAMEBUFFER_COMPLETE_EXT = 0x8cd5;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT = 0x8cd6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT = 0x8cd7;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT = 0x8cd9;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT = 0x8cda;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT = 0x8cdb;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT = 0x8cdc;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED_EXT = 0x8cdd;
    public static final int GL_FRAMEBUFFER_BINDING_EXT = 0x8ca6;
    public static final int GL_RENDERBUFFER_BINDING_EXT = 0x8ca7;
    public static final int GL_MAX_COLOR_ATTACHMENTS_EXT = 0x8cdf;
    public static final int GL_MAX_RENDERBUFFER_SIZE_EXT = 0x84e8;
    public static final int GL_INVALID_FRAMEBUFFER_OPERATION_EXT = 1286;

    private EXTFramebufferObject() {
    }

    public static int glGenFramebuffersEXT() { return retro.gl.GLEmu.genFramebuffer(); }
    public static void glGenFramebuffersEXT(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { ids.put(i, retro.gl.GLEmu.genFramebuffer()); }
    }
    public static void glDeleteFramebuffersEXT(int id) { retro.gl.GLEmu.deleteFramebuffer(id); }
    public static void glDeleteFramebuffersEXT(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { retro.gl.GLEmu.deleteFramebuffer(ids.get(i)); }
    }
    public static void glBindFramebufferEXT(int target, int id) { retro.gl.GLEmu.bindFramebuffer(target, id); }
    public static void glFramebufferTexture2DEXT(int target, int attachment, int textarget, int texture, int level) {
        retro.gl.GLEmu.framebufferTexture2D(target, attachment, textarget, texture, level);
    }
    public static int glGenRenderbuffersEXT() { return retro.gl.GLEmu.genRenderbuffer(); }
    public static void glGenRenderbuffersEXT(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { ids.put(i, retro.gl.GLEmu.genRenderbuffer()); }
    }
    public static void glDeleteRenderbuffersEXT(int id) { retro.gl.GLEmu.deleteRenderbuffer(id); }
    public static void glDeleteRenderbuffersEXT(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { retro.gl.GLEmu.deleteRenderbuffer(ids.get(i)); }
    }
    public static void glBindRenderbufferEXT(int target, int id) { retro.gl.GLEmu.bindRenderbuffer(target, id); }
    public static void glRenderbufferStorageEXT(int target, int internalFormat, int width, int height) {
        retro.gl.GLEmu.renderbufferStorage(target, internalFormat, width, height);
    }
    public static void glFramebufferRenderbufferEXT(int target, int attachment, int rbTarget, int renderbuffer) {
        retro.gl.GLEmu.framebufferRenderbuffer(target, attachment, rbTarget, renderbuffer);
    }
    public static int glCheckFramebufferStatusEXT(int target) { return retro.gl.GLEmu.checkFramebufferStatus(target); }
    public static void glGenerateMipmapEXT(int target) { retro.gl.GLEmu.generateMipmap(); }
}
