package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Never selected (no shader support is reported); present so OpenGlHelper's ARB branch links. */
public final class ARBShaderObjects {
    public static final int GL_OBJECT_COMPILE_STATUS_ARB = 0x8b81;
    public static final int GL_OBJECT_LINK_STATUS_ARB = 0x8b82;
    public static final int GL_OBJECT_INFO_LOG_LENGTH_ARB = 0x8b84;
    public static final int GL_FRAGMENT_SHADER_ARB = 0x8b30;

    private ARBShaderObjects() {
    }

    public static int glCreateShaderObjectARB(int type) { return 0; }
    public static void glShaderSourceARB(int shader, ByteBuffer source) { }
    public static void glCompileShaderARB(int shader) { }
    public static int glGetObjectParameteriARB(int object, int pname) { return 0; }
    public static String glGetInfoLogARB(int object, int maxLength) { return ""; }
    public static void glDeleteObjectARB(int object) { }
    public static int glCreateProgramObjectARB() { return 0; }
    public static void glAttachObjectARB(int program, int shader) { }
    public static void glLinkProgramARB(int program) { }
    public static void glUseProgramObjectARB(int program) { }
    public static int glGetUniformLocationARB(int program, CharSequence name) { return -1; }
    public static void glUniform1iARB(int location, int v) { }
    public static void glUniform1ARB(int location, FloatBuffer v) { }
    public static void glUniform2ARB(int location, FloatBuffer v) { }
    public static void glUniform3ARB(int location, FloatBuffer v) { }
    public static void glUniform4ARB(int location, FloatBuffer v) { }
    public static void glUniform1ARB(int location, IntBuffer v) { }
    public static void glUniform2ARB(int location, IntBuffer v) { }
    public static void glUniform3ARB(int location, IntBuffer v) { }
    public static void glUniform4ARB(int location, IntBuffer v) { }
    public static void glUniformMatrix2ARB(int location, boolean transpose, FloatBuffer v) { }
    public static void glUniformMatrix3ARB(int location, boolean transpose, FloatBuffer v) { }
    public static void glUniformMatrix4ARB(int location, boolean transpose, FloatBuffer v) { }
}
