package org.lwjgl.opengl;

/** LWJGL GL20 for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class GL20 {
    public static final int GL_SHADING_LANGUAGE_VERSION = 0x8b8c;
    public static final int GL_CURRENT_PROGRAM = 0x8b8d;
    public static final int GL_SHADER_TYPE = 0x8b4f;
    public static final int GL_DELETE_STATUS = 0x8b80;
    public static final int GL_COMPILE_STATUS = 0x8b81;
    public static final int GL_LINK_STATUS = 0x8b82;
    public static final int GL_VALIDATE_STATUS = 0x8b83;
    public static final int GL_INFO_LOG_LENGTH = 0x8b84;
    public static final int GL_ATTACHED_SHADERS = 0x8b85;
    public static final int GL_ACTIVE_UNIFORMS = 0x8b86;
    public static final int GL_ACTIVE_UNIFORM_MAX_LENGTH = 0x8b87;
    public static final int GL_ACTIVE_ATTRIBUTES = 0x8b89;
    public static final int GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 0x8b8a;
    public static final int GL_SHADER_SOURCE_LENGTH = 0x8b88;
    public static final int GL_SHADER_OBJECT = 0x8b48;
    public static final int GL_FLOAT_VEC2 = 0x8b50;
    public static final int GL_FLOAT_VEC3 = 0x8b51;
    public static final int GL_FLOAT_VEC4 = 0x8b52;
    public static final int GL_INT_VEC2 = 0x8b53;
    public static final int GL_INT_VEC3 = 0x8b54;
    public static final int GL_INT_VEC4 = 0x8b55;
    public static final int GL_BOOL = 0x8b56;
    public static final int GL_BOOL_VEC2 = 0x8b57;
    public static final int GL_BOOL_VEC3 = 0x8b58;
    public static final int GL_BOOL_VEC4 = 0x8b59;
    public static final int GL_FLOAT_MAT2 = 0x8b5a;
    public static final int GL_FLOAT_MAT3 = 0x8b5b;
    public static final int GL_FLOAT_MAT4 = 0x8b5c;
    public static final int GL_SAMPLER_1D = 0x8b5d;
    public static final int GL_SAMPLER_2D = 0x8b5e;
    public static final int GL_SAMPLER_3D = 0x8b5f;
    public static final int GL_SAMPLER_CUBE = 0x8b60;
    public static final int GL_SAMPLER_1D_SHADOW = 0x8b61;
    public static final int GL_SAMPLER_2D_SHADOW = 0x8b62;
    public static final int GL_VERTEX_SHADER = 0x8b31;
    public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8b4a;
    public static final int GL_MAX_VARYING_FLOATS = 0x8b4b;
    public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
    public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8b4c;
    public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8b4d;
    public static final int GL_MAX_TEXTURE_COORDS = 0x8871;
    public static final int GL_VERTEX_PROGRAM_POINT_SIZE = 0x8642;
    public static final int GL_VERTEX_PROGRAM_TWO_SIDE = 0x8643;
    public static final int GL_VERTEX_ATTRIB_ARRAY_ENABLED = 0x8622;
    public static final int GL_VERTEX_ATTRIB_ARRAY_SIZE = 0x8623;
    public static final int GL_VERTEX_ATTRIB_ARRAY_STRIDE = 0x8624;
    public static final int GL_VERTEX_ATTRIB_ARRAY_TYPE = 0x8625;
    public static final int GL_VERTEX_ATTRIB_ARRAY_NORMALIZED = 0x886a;
    public static final int GL_CURRENT_VERTEX_ATTRIB = 0x8626;
    public static final int GL_VERTEX_ATTRIB_ARRAY_POINTER = 0x8645;
    public static final int GL_FRAGMENT_SHADER = 0x8b30;
    public static final int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8b49;
    public static final int GL_FRAGMENT_SHADER_DERIVATIVE_HINT = 0x8b8b;
    public static final int GL_MAX_DRAW_BUFFERS = 0x8824;
    public static final int GL_DRAW_BUFFER0 = 0x8825;
    public static final int GL_DRAW_BUFFER1 = 0x8826;
    public static final int GL_DRAW_BUFFER2 = 0x8827;
    public static final int GL_DRAW_BUFFER3 = 0x8828;
    public static final int GL_DRAW_BUFFER4 = 0x8829;
    public static final int GL_DRAW_BUFFER5 = 0x882a;
    public static final int GL_DRAW_BUFFER6 = 0x882b;
    public static final int GL_DRAW_BUFFER7 = 0x882c;
    public static final int GL_DRAW_BUFFER8 = 0x882d;
    public static final int GL_DRAW_BUFFER9 = 0x882e;
    public static final int GL_DRAW_BUFFER10 = 0x882f;
    public static final int GL_DRAW_BUFFER11 = 0x8830;
    public static final int GL_DRAW_BUFFER12 = 0x8831;
    public static final int GL_DRAW_BUFFER13 = 0x8832;
    public static final int GL_DRAW_BUFFER14 = 0x8833;
    public static final int GL_DRAW_BUFFER15 = 0x8834;
    public static final int GL_POINT_SPRITE = 0x8861;
    public static final int GL_COORD_REPLACE = 0x8862;
    public static final int GL_POINT_SPRITE_COORD_ORIGIN = 0x8ca0;
    public static final int GL_LOWER_LEFT = 0x8ca1;
    public static final int GL_UPPER_LEFT = 0x8ca2;
    public static final int GL_STENCIL_BACK_FUNC = 0x8800;
    public static final int GL_STENCIL_BACK_FAIL = 0x8801;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_FAIL = 0x8802;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_PASS = 0x8803;
    public static final int GL_STENCIL_BACK_REF = 0x8ca3;
    public static final int GL_STENCIL_BACK_VALUE_MASK = 0x8ca4;
    public static final int GL_STENCIL_BACK_WRITEMASK = 0x8ca5;
    public static final int GL_BLEND_EQUATION_RGB = 0x8009;
    public static final int GL_BLEND_EQUATION_ALPHA = 0x883d;

    private GL20() {
    }

    public static int glCreateShader(int type) { return 0; }
    public static void glShaderSource(int shader, java.nio.ByteBuffer source) { }
    public static void glShaderSource(int shader, CharSequence source) { }
    public static void glCompileShader(int shader) { }
    public static int glGetShaderi(int shader, int pname) { return 0; }
    public static String glGetShaderInfoLog(int shader, int maxLength) { return ""; }
    public static void glDeleteShader(int shader) { }
    public static int glCreateProgram() { return 0; }
    public static void glAttachShader(int program, int shader) { }
    public static void glLinkProgram(int program) { }
    public static int glGetProgrami(int program, int pname) { return 0; }
    public static String glGetProgramInfoLog(int program, int maxLength) { return ""; }
    public static void glUseProgram(int program) { }
    public static void glDeleteProgram(int program) { }
    public static int glGetUniformLocation(int program, CharSequence name) { return -1; }
    public static int glGetAttribLocation(int program, CharSequence name) { return -1; }
    public static void glUniform1i(int location, int v) { }
    public static void glUniform1(int location, java.nio.FloatBuffer v) { }
    public static void glUniform2(int location, java.nio.FloatBuffer v) { }
    public static void glUniform3(int location, java.nio.FloatBuffer v) { }
    public static void glUniform4(int location, java.nio.FloatBuffer v) { }
    public static void glUniform1(int location, java.nio.IntBuffer v) { }
    public static void glUniform2(int location, java.nio.IntBuffer v) { }
    public static void glUniform3(int location, java.nio.IntBuffer v) { }
    public static void glUniform4(int location, java.nio.IntBuffer v) { }
    public static void glUniformMatrix2(int location, boolean transpose, java.nio.FloatBuffer v) { }
    public static void glUniformMatrix3(int location, boolean transpose, java.nio.FloatBuffer v) { }
    public static void glUniformMatrix4(int location, boolean transpose, java.nio.FloatBuffer v) { }
}
