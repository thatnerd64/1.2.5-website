package org.lwjgl.opengl;

/** Never selected (the page reports GL_EXT_framebuffer_object); present so OpenGlHelper's other branch links. */
public final class ARBFramebufferObject {
    private ARBFramebufferObject() {
    }

    public static int glGenFramebuffers() { return EXTFramebufferObject.glGenFramebuffersEXT(); }
    public static void glDeleteFramebuffers(int id) { EXTFramebufferObject.glDeleteFramebuffersEXT(id); }
    public static void glBindFramebuffer(int target, int id) { EXTFramebufferObject.glBindFramebufferEXT(target, id); }
    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
    }
    public static int glGenRenderbuffers() { return EXTFramebufferObject.glGenRenderbuffersEXT(); }
    public static void glDeleteRenderbuffers(int id) { EXTFramebufferObject.glDeleteRenderbuffersEXT(id); }
    public static void glBindRenderbuffer(int target, int id) { EXTFramebufferObject.glBindRenderbufferEXT(target, id); }
    public static void glRenderbufferStorage(int target, int internalFormat, int width, int height) {
        EXTFramebufferObject.glRenderbufferStorageEXT(target, internalFormat, width, height);
    }
    public static void glFramebufferRenderbuffer(int target, int attachment, int rbTarget, int renderbuffer) {
        EXTFramebufferObject.glFramebufferRenderbufferEXT(target, attachment, rbTarget, renderbuffer);
    }
    public static int glCheckFramebufferStatus(int target) { return EXTFramebufferObject.glCheckFramebufferStatusEXT(target); }
}
