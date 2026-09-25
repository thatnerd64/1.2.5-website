package org.lwjgl.opengl;

/** LWJGL ARBOcclusionQuery for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class ARBOcclusionQuery {
    public static final int GL_SAMPLES_PASSED_ARB = 0x8914;
    public static final int GL_QUERY_COUNTER_BITS_ARB = 0x8864;
    public static final int GL_CURRENT_QUERY_ARB = 0x8865;
    public static final int GL_QUERY_RESULT_ARB = 0x8866;
    public static final int GL_QUERY_RESULT_AVAILABLE_ARB = 0x8867;

    private ARBOcclusionQuery() {
    }

    public static void glGenQueriesARB(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { ids.put(i, i + 1); }
    }
    public static int glGenQueriesARB() { return 1; }
    public static void glDeleteQueriesARB(java.nio.IntBuffer ids) { }
    public static void glBeginQueryARB(int target, int id) { }
    public static void glEndQueryARB(int target) { }
    public static void glGetQueryObjectuARB(int id, int pname, java.nio.IntBuffer params) { params.put(params.position(), 1); }
    public static void glGetQueryObjectiARB(int id, int pname, java.nio.IntBuffer params) { params.put(params.position(), 1); }
    public static int glGetQueryObjectuiARB(int id, int pname) { return 1; }
}
