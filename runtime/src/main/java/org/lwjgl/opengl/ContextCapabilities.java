package org.lwjgl.opengl;

/** What the WebGL emulation provides (checked by Minecraft and mods to choose rendering paths). */
public class ContextCapabilities {
    public final boolean OpenGL11 = true;
    public final boolean OpenGL12 = true;
    public final boolean OpenGL13 = true;
    public final boolean OpenGL14 = true;
    public final boolean OpenGL15 = true;
    public final boolean OpenGL20 = false;
    public final boolean OpenGL21 = false;
    public final boolean OpenGL30 = false;
    public final boolean OpenGL31 = false;
    public final boolean OpenGL32 = false;
    public final boolean GL_ARB_multitexture = true;
    public final boolean GL_ARB_vertex_buffer_object = false;
    public final boolean GL_ARB_occlusion_query = false;
    public final boolean GL_ARB_texture_non_power_of_two = true;
    public final boolean GL_ARB_shader_objects = false;
    public final boolean GL_ARB_vertex_shader = false;
    public final boolean GL_ARB_fragment_shader = false;
    public final boolean GL_ARB_framebuffer_object = false;
    public final boolean GL_ARB_depth_texture = false;
    public final boolean GL_ARB_texture_env_combine = false;
    public final boolean GL_ARB_imaging = false;
    public final boolean GL_EXT_framebuffer_object = true;
    public final boolean GL_EXT_blend_func_separate = true;
    public final boolean GL_EXT_texture_filter_anisotropic = false;
    public final boolean GL_EXT_texture_env_combine = false;
    public final boolean GL_EXT_abgr = false;
    public final boolean GL_EXT_bgra = true;
    public final boolean GL_EXT_packed_pixels = true;
    public final boolean GL_EXT_rescale_normal = true;
    public final boolean GL_NV_fog_distance = false;
    public final boolean GL_ATI_texture_float = false;

    ContextCapabilities() {
    }
}
