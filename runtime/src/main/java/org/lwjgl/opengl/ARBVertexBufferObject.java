package org.lwjgl.opengl;

/** LWJGL ARBVertexBufferObject for the browser (see retro.gl.GLEmu). Constants from LWJGL 2.9.3. */
public final class ARBVertexBufferObject {
    public static final int GL_ARRAY_BUFFER_ARB = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER_ARB = 0x8893;
    public static final int GL_ARRAY_BUFFER_BINDING_ARB = 0x8894;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING_ARB = 0x8895;
    public static final int GL_VERTEX_ARRAY_BUFFER_BINDING_ARB = 0x8896;
    public static final int GL_NORMAL_ARRAY_BUFFER_BINDING_ARB = 0x8897;
    public static final int GL_COLOR_ARRAY_BUFFER_BINDING_ARB = 0x8898;
    public static final int GL_INDEX_ARRAY_BUFFER_BINDING_ARB = 0x8899;
    public static final int GL_TEXTURE_COORD_ARRAY_BUFFER_BINDING_ARB = 0x889a;
    public static final int GL_EDGE_FLAG_ARRAY_BUFFER_BINDING_ARB = 0x889b;
    public static final int GL_SECONDARY_COLOR_ARRAY_BUFFER_BINDING_ARB = 0x889c;
    public static final int GL_FOG_COORDINATE_ARRAY_BUFFER_BINDING_ARB = 0x889d;
    public static final int GL_WEIGHT_ARRAY_BUFFER_BINDING_ARB = 0x889e;
    public static final int GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING_ARB = 0x889f;
    public static final int GL_STREAM_DRAW_ARB = 0x88e0;
    public static final int GL_STREAM_READ_ARB = 0x88e1;
    public static final int GL_STREAM_COPY_ARB = 0x88e2;
    public static final int GL_STATIC_DRAW_ARB = 0x88e4;
    public static final int GL_STATIC_READ_ARB = 0x88e5;
    public static final int GL_STATIC_COPY_ARB = 0x88e6;
    public static final int GL_DYNAMIC_DRAW_ARB = 0x88e8;
    public static final int GL_DYNAMIC_READ_ARB = 0x88e9;
    public static final int GL_DYNAMIC_COPY_ARB = 0x88ea;
    public static final int GL_READ_ONLY_ARB = 0x88b8;
    public static final int GL_WRITE_ONLY_ARB = 0x88b9;
    public static final int GL_READ_WRITE_ARB = 0x88ba;
    public static final int GL_BUFFER_SIZE_ARB = 0x8764;
    public static final int GL_BUFFER_USAGE_ARB = 0x8765;
    public static final int GL_BUFFER_ACCESS_ARB = 0x88bb;
    public static final int GL_BUFFER_MAPPED_ARB = 0x88bc;
    public static final int GL_BUFFER_MAP_POINTER_ARB = 0x88bd;

    private ARBVertexBufferObject() {
    }

    public static int glGenBuffersARB() { return retro.gl.GLEmu.genBuffer(); }
    public static void glGenBuffersARB(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { ids.put(i, retro.gl.GLEmu.genBuffer()); }
    }
    public static void glDeleteBuffersARB(int id) { retro.gl.GLEmu.deleteBuffer(id); }
    public static void glDeleteBuffersARB(java.nio.IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) { retro.gl.GLEmu.deleteBuffer(ids.get(i)); }
    }
    public static void glBindBufferARB(int target, int id) { retro.gl.GLEmu.bindBuffer(target, id); }
    public static void glBufferDataARB(int target, java.nio.ByteBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferDataARB(int target, java.nio.FloatBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferDataARB(int target, java.nio.IntBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferDataARB(int target, java.nio.ShortBuffer data, int usage) { retro.gl.GLEmu.bufferData(target, data, 0); }
    public static void glBufferDataARB(int target, long size, int usage) { retro.gl.GLEmu.bufferData(target, null, (int) size); }
    public static boolean glIsBufferARB(int id) { return id > 0; }
}
