package org.lwjgl.opengl;

/** LWJGL GL15 for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class GL15 {
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;
    public static final int GL_VERTEX_ARRAY_BUFFER_BINDING = 0x8896;
    public static final int GL_NORMAL_ARRAY_BUFFER_BINDING = 0x8897;
    public static final int GL_COLOR_ARRAY_BUFFER_BINDING = 0x8898;
    public static final int GL_INDEX_ARRAY_BUFFER_BINDING = 0x8899;
    public static final int GL_TEXTURE_COORD_ARRAY_BUFFER_BINDING = 0x889a;
    public static final int GL_EDGE_FLAG_ARRAY_BUFFER_BINDING = 0x889b;
    public static final int GL_SECONDARY_COLOR_ARRAY_BUFFER_BINDING = 0x889c;
    public static final int GL_FOG_COORDINATE_ARRAY_BUFFER_BINDING = 0x889d;
    public static final int GL_WEIGHT_ARRAY_BUFFER_BINDING = 0x889e;
    public static final int GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0x889f;
    public static final int GL_STREAM_DRAW = 0x88e0;
    public static final int GL_STREAM_READ = 0x88e1;
    public static final int GL_STREAM_COPY = 0x88e2;
    public static final int GL_STATIC_DRAW = 0x88e4;
    public static final int GL_STATIC_READ = 0x88e5;
    public static final int GL_STATIC_COPY = 0x88e6;
    public static final int GL_DYNAMIC_DRAW = 0x88e8;
    public static final int GL_DYNAMIC_READ = 0x88e9;
    public static final int GL_DYNAMIC_COPY = 0x88ea;
    public static final int GL_READ_ONLY = 0x88b8;
    public static final int GL_WRITE_ONLY = 0x88b9;
    public static final int GL_READ_WRITE = 0x88ba;
    public static final int GL_BUFFER_SIZE = 0x8764;
    public static final int GL_BUFFER_USAGE = 0x8765;
    public static final int GL_BUFFER_ACCESS = 0x88bb;
    public static final int GL_BUFFER_MAPPED = 0x88bc;
    public static final int GL_BUFFER_MAP_POINTER = 0x88bd;
    public static final int GL_FOG_COORD_SRC = 0x8450;
    public static final int GL_FOG_COORD = 0x8451;
    public static final int GL_CURRENT_FOG_COORD = 0x8453;
    public static final int GL_FOG_COORD_ARRAY_TYPE = 0x8454;
    public static final int GL_FOG_COORD_ARRAY_STRIDE = 0x8455;
    public static final int GL_FOG_COORD_ARRAY_POINTER = 0x8456;
    public static final int GL_FOG_COORD_ARRAY = 0x8457;
    public static final int GL_FOG_COORD_ARRAY_BUFFER_BINDING = 0x889d;
    public static final int GL_SRC0_RGB = 0x8580;
    public static final int GL_SRC1_RGB = 0x8581;
    public static final int GL_SRC2_RGB = 0x8582;
    public static final int GL_SRC0_ALPHA = 0x8588;
    public static final int GL_SRC1_ALPHA = 0x8589;
    public static final int GL_SRC2_ALPHA = 0x858a;
    public static final int GL_SAMPLES_PASSED = 0x8914;
    public static final int GL_QUERY_COUNTER_BITS = 0x8864;
    public static final int GL_CURRENT_QUERY = 0x8865;
    public static final int GL_QUERY_RESULT = 0x8866;
    public static final int GL_QUERY_RESULT_AVAILABLE = 0x8867;

    private GL15() {
    }

    public static int glGenBuffers() { return retro.gl.GLEmu.genBuffer(); }
    public static void glGenBuffers(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { ids.put(i, retro.gl.GLEmu.genBuffer()); }
    }
    public static void glDeleteBuffers(int id) { retro.gl.GLEmu.deleteBuffer(id); }
    public static void glDeleteBuffers(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { retro.gl.GLEmu.deleteBuffer(ids.get(i)); }
    }
    public static void glBindBuffer(int target, int id) { retro.gl.GLEmu.bindBuffer(target, id); }
    public static void glBufferData(int target, java.nio.ByteBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferData(int target, java.nio.FloatBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferData(int target, java.nio.IntBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferData(int target, java.nio.ShortBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferData(int target, long size, int usage) { retro.gl.GLEmu.bufferData(target, null, (int) size); }
    public static boolean glIsBuffer(int id) { return id > 0; }

    public static int glGenQueries() { return 0; }
    public static void glBeginQuery(int target, int id) { }
    public static void glEndQuery(int target) { }
    public static int glGetQueryObjecti(int id, int pname) { return 1; }
}
