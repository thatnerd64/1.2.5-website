package retro.gl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 * OpenGL 1.3-era fixed-function emulation on WebGL 2: everything LWJGL's GL11/GL12/GL13 and the ARB/EXT
 * extension classes do for Minecraft 1.2.5 and its mods.
 *
 * <p>Key points:
 * <ul>
 *   <li>Client-side vertex arrays (Tessellator) are uploaded as-is into a streaming buffer; offsets and strides
 *   are passed straight to {@code vertexAttribPointer}.</li>
 *   <li>Disabled arrays read the current color/normal/texcoord through constant vertex attributes.</li>
 *   <li>Display lists record state changes as closures and vertex data as static VBO+VAO draws, so compiled
 *   chunks are replayed without re-uploading.</li>
 *   <li>Quads, quad strips and polygons are converted to triangles.</li>
 * </ul>
 */
public final class GLEmu {
    // ---- GL constants used internally ----
    static final int GL_POINTS = 0;
    static final int GL_LINES = 1;
    static final int GL_TRIANGLES = 4;
    static final int GL_TRIANGLE_STRIP = 5;
    static final int GL_TRIANGLE_FAN = 6;
    static final int GL_QUADS = 7;
    static final int GL_QUAD_STRIP = 8;
    static final int GL_POLYGON = 9;
    static final int GL_BYTE = 0x1400;
    static final int GL_UNSIGNED_BYTE = 0x1401;
    static final int GL_SHORT = 0x1402;
    static final int GL_UNSIGNED_SHORT = 0x1403;
    static final int GL_INT = 0x1404;
    static final int GL_UNSIGNED_INT = 0x1405;
    static final int GL_FLOAT = 0x1406;
    static final int GL_DOUBLE = 0x140A;
    static final int GL_TEXTURE_2D = 0xDE1;
    static final int GL_MODELVIEW = 0x1700;
    static final int GL_PROJECTION = 0x1701;
    static final int GL_TEXTURE = 0x1702;
    static final int GL_TEXTURE0 = 0x84C0;
    static final int GL_ARRAY_BUFFER = 0x8892;
    static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;

    static WebGL gl;
    private static int counter;

    // ---- Matrices ----
    private static final float[][] modelview = new float[64][16];
    private static final float[][] projection = new float[16][16];
    private static final float[][][] textureMatrix = new float[4][16][16];
    private static int mvTop;
    private static int projTop;
    private static final int[] texTop = new int[4];
    private static int matrixMode = GL_MODELVIEW;
    static int mvVersion;
    static int projVersion;
    private static final int[] texMatVersion = new int[4];

    // ---- Textures ----
    static final class Tex {
        JSObject tex;
        int width;
        int height;
        int maxLevel;
        /** True once re-specified as RGB8 to receive copies from the (alpha-less) default framebuffer. */
        boolean rgb;
        /** Filter and wrap parameters last set (0: not yet), so repeated identical calls cost nothing. */
        int minFilter;
        int magFilter;
        int wrapS;
        int wrapT;
    }

    private static final List<Tex> textures = new ArrayList<>();
    private static int activeUnit;
    private static final int[] boundTexture = new int[4];
    private static final boolean[] textureEnabled = new boolean[4];

    // ---- Vertex arrays ----
    static final class Arr {
        boolean enabled;
        int size = 4;
        int type = GL_FLOAT;
        boolean normalized;
        int stride;
        Buffer buffer;
        /** Buffer position when the pointer was set (LWJGL passes buffer address + position at call time). */
        int position;
        ArrayBuffer base;
        int baseOffset;
        int vbo;
        int vboOffset;
    }

    private static final Arr vertexArray = new Arr();
    private static final Arr colorArray = new Arr();
    private static final Arr normalArray = new Arr();
    private static final Arr[] texCoordArray = { new Arr(), new Arr(), new Arr(), new Arr() };
    private static int clientActiveUnit;

    /** Emulated buffer objects (ARB_vertex_buffer_object / GL15): client-side copies. */
    private static final List<ArrayBuffer> userBuffers = new ArrayList<>();
    private static int arrayBufferBinding;
    private static int elementBufferBinding;

    // ---- Current attributes ----
    private static final float[] color = { 1, 1, 1, 1 };
    private static final float[] normal = { 0, 0, 1 };
    private static final float[][] texCoord = { { 0, 0, 0, 1 }, { 0, 0, 0, 1 }, { 0, 0, 0, 1 }, { 0, 0, 0, 1 } };
    private static final float[] sentColor = { -1, -1, -1, -1 };
    private static final float[] sentNormal = { -9, -9, -9 };
    private static final float[] sentTex0 = { -9, -9, -9, -9 };
    private static final float[] sentTex1 = { -9, -9 };

    // ---- Fixed-function state ----
    private static boolean alphaTest;
    private static int alphaFunc = 519;
    private static float alphaRef;
    private static int alphaVersion;
    private static boolean fog;
    private static int fogMode = 0x800;
    private static float fogStart;
    private static float fogEnd = 1;
    private static float fogDensity = 1;
    private static final float[] fogColor = new float[4];
    private static int fogVersion;
    private static boolean lighting;
    private static final boolean[] lightEnabled = new boolean[8];
    private static final float[][] lightDiffuse = { { 1, 1, 1, 1 }, { 0, 0, 0, 1 }, { 0, 0, 0, 1 }, { 0, 0, 0, 1 },
            { 0, 0, 0, 1 }, { 0, 0, 0, 1 }, { 0, 0, 0, 1 }, { 0, 0, 0, 1 } };
    private static final float[][] lightAmbient = new float[8][4];
    private static final float[][] lightDir = new float[8][3];
    private static final float[] lightModelAmbient = { 0.2f, 0.2f, 0.2f, 1 };
    private static int lightVersion;
    private static final boolean[] texGenEnabled = new boolean[4];
    private static final int[] texGenMode = { 0x2400, 0x2400, 0x2400, 0x2400 };
    private static final float[][] texGenObjectPlane = { { 1, 0, 0, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
    private static final float[][] texGenEyePlane = { { 1, 0, 0, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
    private static int texGenVersion;
    private static final java.util.Map<Integer, Boolean> otherCaps = new java.util.HashMap<>();

    // ---- WebGL state caches ----
    private static boolean blend;
    private static boolean depthTest;
    private static boolean cullFace;
    private static boolean polygonOffsetFill;
    private static boolean scissorTest;
    private static boolean stencilTest;
    private static int blendSrc = 1;
    private static int blendDst;
    private static int depthFunc = 0x201;
    private static boolean depthMask = true;
    private static boolean[] colorMask = { true, true, true, true };
    private static int cullFaceMode = 0x405;
    private static float lineWidth = 1;
    private static final int[] viewport = new int[4];
    private static final float[] clearColor = new float[4];

    // ---- Draw resources ----
    private static JSObject streamVao;
    private static final JSObject[] streamVbo = new JSObject[5];
    // scratch for submit(), kept here so a streamed draw allocates nothing
    private static final ArrayBuffer[] streamGroupBase = new ArrayBuffer[5];
    private static final int[] streamGroupMin = new int[5];
    private static final int[] streamGroupMax = new int[5];
    private static final int[] streamAttribGroup = new int[5];
    private static JSObject quadIndexBuffer;
    private static int quadIndexCapacity;
    private static Shaders.Program currentProgram;
    private static int streamEnabledMask;
    private static JSObject boundVao;

    // ---- Small-draw batching ----
    // Most of a frame's draw calls are tiny: a quad per text character, heart, slot or icon. Draws of up to
    // BATCH_MAX_DRAW vertices are transformed by the modelview matrix on the CPU and appended to one batch,
    // drawn with an identity modelview when anything that affects rendering is about to change: every state
    // setter flushes first (only when its value really changes), as does every draw that is not batched.
    private static final int BATCH_MAX_DRAW = 64;
    private static final int BATCH_FLOATS = 14; // x y z, r g b a, nx ny nz, s0 t0, s1 t1 (as immediate mode)
    private static final int BATCH_CAPACITY = 6144; // vertices
    private static final float[] IDENTITY = { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
    private static int batchVerts;
    private static JSObject batchVbo;
    private static JSObject batchState;

    // ---- Display lists ----
    static final class DrawOp {
        JSObject vao;
        JSObject[] vbos;
        int mode;
        int count;
        int attribMask;
        /** For small draws: a CPU copy of the vertex data (one Src per attribute, null if unused). */
        Src[] cpu;
    }

    static final class DisplayList {
        final List<Object> ops = new ArrayList<>();

        void free() {
            for (Object op : ops) {
                if (op instanceof DrawOp) {
                    DrawOp d = (DrawOp) op;
                    if (JSUtil.same(boundVao, d.vao)) {
                        boundVao = null;
                    }
                    gl.deleteVertexArray(d.vao);
                    for (JSObject vbo : d.vbos) {
                        gl.deleteBuffer(vbo);
                    }
                }
            }
            ops.clear();
        }
    }

    private static final List<DisplayList> displayLists = new ArrayList<>();
    private static DisplayList compiling;
    private static int compilingId;
    private static boolean compileAndExecute;
    private static int callDepth;

    // ---- Immediate mode ----
    private static int beginMode = -1;
    private static float[] immediate = new float[14 * 1024];
    private static int immediateCount;
    private static boolean immediateColor;
    private static boolean immediateNormal;
    private static boolean immediateTex0;
    private static boolean immediateTex1;

    // ---- Framebuffers ----
    private static final List<JSObject> framebuffers = new ArrayList<>();
    private static final List<JSObject> renderbuffers = new ArrayList<>();
    private static int boundFramebuffer;
    private static JSObject boundRenderbuffer;
    private static JSObject screenFramebuffer;
    private static JSObject screenColor;
    private static JSObject screenDepth;
    private static int screenWidth;
    private static int screenHeight;

    private GLEmu() {
    }

    // =====================================================================================================
    // Setup
    // =====================================================================================================

    static void init(WebGL context) {
        gl = context;
        textures.add(null);
        displayLists.add(null);
        userBuffers.add(null);
        framebuffers.add(null);
        renderbuffers.add(null);
        Mat4.identity(modelview[0]);
        Mat4.identity(projection[0]);
        for (int i = 0; i < 4; i++) {
            Mat4.identity(textureMatrix[i][0]);
        }
        lightDir[0][2] = 1;
        lightDir[1][2] = 1;
        streamVao = gl.createVertexArray();
        bindVao(streamVao);
        for (int i = 0; i < streamVbo.length; i++) {
            streamVbo[i] = gl.createBuffer();
        }
        batchVbo = gl.createBuffer();
        batchState = createBatchState(BATCH_CAPACITY * BATCH_FLOATS);
        quadIndexBuffer = gl.createBuffer();
        gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer);
        ensureQuadIndices(65536);
        gl.pixelStorei(0x0CF5, 1);
        gl.depthFunc(depthFunc);
        gl.blendFunc(blendSrc, blendDst);
        gl.cullFace(cullFaceMode);
        gl.disable(0xB71);
        gl.disable(0xBE2);
        gl.disable(0xB44);
        mvVersion = ++counter;
        projVersion = ++counter;
    }

    private static void ensureQuadIndices(int vertexCount) {
        int quads = (vertexCount + 3) / 4;
        if (quads <= quadIndexCapacity) {
            return;
        }
        int capacity = Math.max(quads, quadIndexCapacity * 2);
        int[] indices = new int[capacity * 6];
        for (int q = 0, i = 0; q < capacity; q++) {
            int v = q * 4;
            indices[i++] = v;
            indices[i++] = v + 1;
            indices[i++] = v + 2;
            indices[i++] = v;
            indices[i++] = v + 2;
            indices[i++] = v + 3;
        }
        gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer);
        gl.bufferData(GL_ELEMENT_ARRAY_BUFFER, org.teavm.jso.typedarrays.Int32Array.fromJavaArray(indices), 0x88E4);
        quadIndexCapacity = capacity;
    }

    private static void bindVao(JSObject vao) {
        if (boundVao == null || !JSUtil.same(boundVao, vao)) {
            gl.bindVertexArray(vao);
            boundVao = vao;
        }
    }

    private static boolean record(Runnable op) {
        if (compiling == null) {
            return false;
        }
        compiling.ops.add(op);
        return !compileAndExecute;
    }

    // =====================================================================================================
    // Matrices
    // =====================================================================================================

    private static float[] current() {
        switch (matrixMode) {
            case GL_PROJECTION:
                return projection[projTop];
            case GL_TEXTURE:
                return textureMatrix[activeUnit][texTop[activeUnit]];
            default:
                return modelview[mvTop];
        }
    }

    /** Before the current matrix changes: batched vertices carry the modelview, but no other matrix. */
    private static void matrixChanging() {
        if (matrixMode != GL_MODELVIEW) {
            flushBatch();
        }
    }

    private static void changed() {
        int v = ++counter;
        switch (matrixMode) {
            case GL_PROJECTION:
                projVersion = v;
                break;
            case GL_TEXTURE:
                texMatVersion[activeUnit] = v;
                break;
            default:
                mvVersion = v;
                break;
        }
    }

    public static void matrixMode(int mode) {
        if (record(() -> matrixModeImpl(mode))) {
            return;
        }
        matrixModeImpl(mode);
    }

    private static void matrixModeImpl(int mode) {
        matrixMode = mode;
    }

    public static int getMatrixMode() {
        return matrixMode;
    }

    public static void pushMatrix() {
        if (record(GLEmu::pushMatrixImpl)) {
            return;
        }
        pushMatrixImpl();
    }

    private static void pushMatrixImpl() {
        switch (matrixMode) {
            case GL_PROJECTION:
                if (projTop + 1 < projection.length) {
                    System.arraycopy(projection[projTop], 0, projection[projTop + 1], 0, 16);
                    projTop++;
                }
                break;
            case GL_TEXTURE: {
                int u = activeUnit;
                if (texTop[u] + 1 < textureMatrix[u].length) {
                    System.arraycopy(textureMatrix[u][texTop[u]], 0, textureMatrix[u][texTop[u] + 1], 0, 16);
                    texTop[u]++;
                }
                break;
            }
            default:
                if (mvTop + 1 < modelview.length) {
                    System.arraycopy(modelview[mvTop], 0, modelview[mvTop + 1], 0, 16);
                    mvTop++;
                }
                break;
        }
    }

    public static void popMatrix() {
        if (record(GLEmu::popMatrixImpl)) {
            return;
        }
        popMatrixImpl();
    }

    private static void popMatrixImpl() {
        matrixChanging();
        switch (matrixMode) {
            case GL_PROJECTION:
                if (projTop > 0) {
                    projTop--;
                }
                break;
            case GL_TEXTURE:
                if (texTop[activeUnit] > 0) {
                    texTop[activeUnit]--;
                }
                break;
            default:
                if (mvTop > 0) {
                    mvTop--;
                }
                break;
        }
        changed();
    }

    public static void loadIdentity() {
        if (record(GLEmu::loadIdentityImpl)) {
            return;
        }
        loadIdentityImpl();
    }

    private static void loadIdentityImpl() {
        matrixChanging();
        Mat4.identity(current());
        changed();
    }

    public static void translate(float x, float y, float z) {
        if (record(() -> translateImpl(x, y, z))) {
            return;
        }
        translateImpl(x, y, z);
    }

    private static void translateImpl(float x, float y, float z) {
        matrixChanging();
        Mat4.translate(current(), x, y, z);
        changed();
    }

    public static void scale(float x, float y, float z) {
        if (record(() -> scaleImpl(x, y, z))) {
            return;
        }
        scaleImpl(x, y, z);
    }

    private static void scaleImpl(float x, float y, float z) {
        matrixChanging();
        Mat4.scale(current(), x, y, z);
        changed();
    }

    public static void rotate(float angle, float x, float y, float z) {
        if (record(() -> rotateImpl(angle, x, y, z))) {
            return;
        }
        rotateImpl(angle, x, y, z);
    }

    private static void rotateImpl(float angle, float x, float y, float z) {
        matrixChanging();
        Mat4.rotate(current(), angle, x, y, z);
        changed();
    }

    public static void ortho(double l, double r, double b, double t, double n, double f) {
        if (record(() -> orthoImpl(l, r, b, t, n, f))) {
            return;
        }
        orthoImpl(l, r, b, t, n, f);
    }

    private static void orthoImpl(double l, double r, double b, double t, double n, double f) {
        matrixChanging();
        Mat4.ortho(current(), l, r, b, t, n, f);
        changed();
    }

    public static void frustum(double l, double r, double b, double t, double n, double f) {
        if (record(() -> frustumImpl(l, r, b, t, n, f))) {
            return;
        }
        frustumImpl(l, r, b, t, n, f);
    }

    private static void frustumImpl(double l, double r, double b, double t, double n, double f) {
        matrixChanging();
        Mat4.frustum(current(), l, r, b, t, n, f);
        changed();
    }

    public static void perspective(float fovy, float aspect, float near, float far) {
        if (record(() -> perspectiveImpl(fovy, aspect, near, far))) {
            return;
        }
        perspectiveImpl(fovy, aspect, near, far);
    }

    private static void perspectiveImpl(float fovy, float aspect, float near, float far) {
        matrixChanging();
        Mat4.perspective(current(), fovy, aspect, near, far);
        changed();
    }

    public static void multMatrix(float[] m) {
        float[] copy = m.clone();
        if (record(() -> multMatrixImpl(copy))) {
            return;
        }
        multMatrixImpl(copy);
    }

    private static void multMatrixImpl(float[] m) {
        matrixChanging();
        Mat4.mul(current(), m);
        changed();
    }

    public static void loadMatrix(float[] m) {
        float[] copy = m.clone();
        if (record(() -> loadMatrixImpl(copy))) {
            return;
        }
        loadMatrixImpl(copy);
    }

    private static void loadMatrixImpl(float[] m) {
        matrixChanging();
        System.arraycopy(m, 0, current(), 0, 16);
        changed();
    }

    public static float[] modelviewMatrix() {
        return modelview[mvTop];
    }

    public static float[] projectionMatrix() {
        return projection[projTop];
    }

    // =====================================================================================================
    // Enable / disable
    // =====================================================================================================

    public static void enable(int cap) {
        if (record(() -> setCap(cap, true))) {
            return;
        }
        setCap(cap, true);
    }

    public static void disable(int cap) {
        if (record(() -> setCap(cap, false))) {
            return;
        }
        setCap(cap, false);
    }

    private static void setCap(int cap, boolean on) {
        if (isEnabled(cap) == on) {
            return;
        }
        flushBatch();
        switch (cap) {
            case GL_TEXTURE_2D:
                textureEnabled[activeUnit] = on;
                break;
            case 0xBC0:
                alphaTest = on;
                break;
            case 0xB60:
                fog = on;
                break;
            case 0xB50:
                lighting = on;
                break;
            case 0x4000:
            case 0x4001:
            case 0x4002:
            case 0x4003:
            case 0x4004:
            case 0x4005:
            case 0x4006:
            case 0x4007:
                lightEnabled[cap - 0x4000] = on;
                lightVersion = ++counter;
                break;
            case 0xC60:
            case 0xC61:
            case 0xC62:
            case 0xC63:
                texGenEnabled[cap - 0xC60] = on;
                texGenVersion = ++counter;
                break;
            case 0xBE2:
                if (blend != on) {
                    blend = on;
                    toggle(cap, on);
                }
                break;
            case 0xB71:
                if (depthTest != on) {
                    depthTest = on;
                    toggle(cap, on);
                }
                break;
            case 0xB44:
                if (cullFace != on) {
                    cullFace = on;
                    toggle(cap, on);
                }
                break;
            case 0x8037:
                if (polygonOffsetFill != on) {
                    polygonOffsetFill = on;
                    toggle(cap, on);
                }
                break;
            case 0xC11:
                if (scissorTest != on) {
                    scissorTest = on;
                    toggle(cap, on);
                }
                break;
            case 0xB90:
                if (stencilTest != on) {
                    stencilTest = on;
                    toggle(cap, on);
                }
                break;
            default:
                otherCaps.put(cap, on);
                break;
        }
    }

    private static void toggle(int cap, boolean on) {
        if (on) {
            gl.enable(cap);
        } else {
            gl.disable(cap);
        }
    }

    public static boolean isEnabled(int cap) {
        switch (cap) {
            case GL_TEXTURE_2D:
                return textureEnabled[activeUnit];
            case 0xBC0:
                return alphaTest;
            case 0xB60:
                return fog;
            case 0xB50:
                return lighting;
            case 0xBE2:
                return blend;
            case 0xB71:
                return depthTest;
            case 0xB44:
                return cullFace;
            case 0x8037:
                return polygonOffsetFill;
            case 0xC11:
                return scissorTest;
            case 0xB90:
                return stencilTest;
            default:
                if (cap >= 0x4000 && cap < 0x4008) {
                    return lightEnabled[cap - 0x4000];
                }
                if (cap >= 0xC60 && cap <= 0xC63) {
                    return texGenEnabled[cap - 0xC60];
                }
                return Boolean.TRUE.equals(otherCaps.get(cap));
        }
    }

    // =====================================================================================================
    // Simple state
    // =====================================================================================================

    public static void blendFunc(int src, int dst) {
        if (record(() -> blendFuncImpl(src, dst))) {
            return;
        }
        blendFuncImpl(src, dst);
    }

    private static void blendFuncImpl(int src, int dst) {
        if (src != blendSrc || dst != blendDst) {
            flushBatch();
            blendSrc = src;
            blendDst = dst;
            gl.blendFunc(src, dst);
        }
    }

    public static void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        if (record(() -> blendFuncSeparateImpl(srcRgb, dstRgb, srcAlpha, dstAlpha))) {
            return;
        }
        blendFuncSeparateImpl(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    private static void blendFuncSeparateImpl(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        flushBatch();
        blendSrc = -1;
        blendDst = -1;
        gl.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    public static void alphaFunc(int func, float ref) {
        if (record(() -> alphaFuncImpl(func, ref))) {
            return;
        }
        alphaFuncImpl(func, ref);
    }

    private static void alphaFuncImpl(int func, float ref) {
        if (func == alphaFunc && ref == alphaRef) {
            return;
        }
        flushBatch();
        alphaFunc = func;
        alphaRef = ref;
        alphaVersion = ++counter;
    }

    public static void depthFunc(int func) {
        if (record(() -> depthFuncImpl(func))) {
            return;
        }
        depthFuncImpl(func);
    }

    private static void depthFuncImpl(int func) {
        if (func != depthFunc) {
            flushBatch();
            depthFunc = func;
            gl.depthFunc(func);
        }
    }

    public static void depthMask(boolean flag) {
        if (record(() -> depthMaskImpl(flag))) {
            return;
        }
        depthMaskImpl(flag);
    }

    private static void depthMaskImpl(boolean flag) {
        if (flag != depthMask) {
            flushBatch();
            depthMask = flag;
            gl.depthMask(flag);
        }
    }

    public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
        if (record(() -> colorMaskImpl(r, g, b, a))) {
            return;
        }
        colorMaskImpl(r, g, b, a);
    }

    private static void colorMaskImpl(boolean r, boolean g, boolean b, boolean a) {
        if (colorMask[0] == r && colorMask[1] == g && colorMask[2] == b && colorMask[3] == a) {
            return;
        }
        flushBatch();
        colorMask = new boolean[] { r, g, b, a };
        gl.colorMask(r, g, b, a);
    }

    public static void cullFace(int mode) {
        if (record(() -> cullFaceImpl(mode))) {
            return;
        }
        cullFaceImpl(mode);
    }

    private static void cullFaceImpl(int mode) {
        if (mode != cullFaceMode) {
            flushBatch();
            cullFaceMode = mode;
            gl.cullFace(mode);
        }
    }

    public static void frontFace(int mode) {
        flushBatch();
        gl.frontFace(mode);
    }

    public static void polygonOffset(float factor, float units) {
        if (record(() -> polygonOffsetImpl(factor, units))) {
            return;
        }
        polygonOffsetImpl(factor, units);
    }

    private static void polygonOffsetImpl(float factor, float units) {
        flushBatch();
        gl.polygonOffset(factor, units);
    }

    public static void lineWidth(float width) {
        if (record(() -> lineWidthImpl(width))) {
            return;
        }
        lineWidthImpl(width);
    }

    private static void lineWidthImpl(float width) {
        if (width == lineWidth) {
            return;
        }
        flushBatch();
        lineWidth = width;
        gl.lineWidth(width);
    }

    public static void viewport(int x, int y, int w, int h) {
        flushBatch();
        viewport[0] = x;
        viewport[1] = y;
        viewport[2] = w;
        viewport[3] = h;
        gl.viewport(x, y, w, h);
    }

    public static void scissor(int x, int y, int w, int h) {
        flushBatch();
        gl.scissor(x, y, w, h);
    }

    public static void clear(int mask) {
        if (record(() -> clearImpl(mask))) {
            return;
        }
        clearImpl(mask);
    }

    private static void clearImpl(int mask) {
        flushBatch();
        gl.clear(mask);
    }

    public static void clearColor(float r, float g, float b, float a) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
        clearColor[3] = a;
        gl.clearColor(r, g, b, a);
    }

    public static void clearDepth(double d) {
        gl.clearDepth((float) d);
    }

    public static void clearStencil(int s) {
        gl.clearStencil(s);
    }

    public static void stencilFunc(int func, int ref, int mask) {
        if (record(() -> stencilFuncImpl(func, ref, mask))) {
            return;
        }
        stencilFuncImpl(func, ref, mask);
    }

    private static void stencilFuncImpl(int func, int ref, int mask) {
        flushBatch();
        gl.stencilFunc(func, ref, mask);
    }

    public static void stencilOp(int fail, int zfail, int zpass) {
        if (record(() -> stencilOpImpl(fail, zfail, zpass))) {
            return;
        }
        stencilOpImpl(fail, zfail, zpass);
    }

    private static void stencilOpImpl(int fail, int zfail, int zpass) {
        flushBatch();
        gl.stencilOp(fail, zfail, zpass);
    }

    public static void stencilMask(int mask) {
        flushBatch();
        gl.stencilMask(mask);
    }

    public static void flush() {
        flushBatch();
        gl.flush();
    }

    public static void finish() {
        flushBatch();
        gl.flush();
    }

    // =====================================================================================================
    // Current attributes
    // =====================================================================================================

    public static void color(float r, float g, float b, float a) {
        if (compiling != null && beginMode < 0 && record(() -> colorImpl(r, g, b, a))) {
            return;
        }
        colorImpl(r, g, b, a);
    }

    private static void colorImpl(float r, float g, float b, float a) {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
        if (beginMode >= 0) {
            immediateColor = true;
        }
    }

    public static void normal(float x, float y, float z) {
        if (compiling != null && beginMode < 0 && record(() -> normalImpl(x, y, z))) {
            return;
        }
        normalImpl(x, y, z);
    }

    private static void normalImpl(float x, float y, float z) {
        normal[0] = x;
        normal[1] = y;
        normal[2] = z;
        if (beginMode >= 0) {
            immediateNormal = true;
        }
    }

    public static void texCoord(float s, float t) {
        multiTexCoord(0, s, t);
    }

    public static void multiTexCoord(int unit, float s, float t) {
        if (compiling != null && beginMode < 0 && record(() -> multiTexCoordImpl(unit, s, t))) {
            return;
        }
        multiTexCoordImpl(unit, s, t);
    }

    private static void multiTexCoordImpl(int unit, float s, float t) {
        float[] tc = texCoord[unit & 3];
        tc[0] = s;
        tc[1] = t;
        tc[2] = 0;
        tc[3] = 1;
        if (beginMode >= 0) {
            if (unit == 0) {
                immediateTex0 = true;
            } else {
                immediateTex1 = true;
            }
        }
    }

    public static float[] currentColor() {
        return color;
    }

    // =====================================================================================================
    // Lighting, fog, texgen
    // =====================================================================================================

    public static void light(int light, int pname, float[] v) {
        float[] copy = v.clone();
        if (record(() -> lightImpl(light, pname, copy))) {
            return;
        }
        lightImpl(light, pname, copy);
    }

    private static void lightImpl(int light, int pname, float[] v) {
        flushBatch();
        int i = light - 0x4000;
        if (i < 0 || i >= 8) {
            return;
        }
        switch (pname) {
            case 0x1200: // AMBIENT
                System.arraycopy(v, 0, lightAmbient[i], 0, 4);
                break;
            case 0x1201: // DIFFUSE
                System.arraycopy(v, 0, lightDiffuse[i], 0, 4);
                break;
            case 0x1203: { // POSITION (directional lights only)
                float[] m = modelview[mvTop];
                float[] out = new float[4];
                Mat4.transform(m, v[0], v[1], v[2], 0, out);
                float len = (float) Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
                if (len > 0) {
                    lightDir[i][0] = out[0] / len;
                    lightDir[i][1] = out[1] / len;
                    lightDir[i][2] = out[2] / len;
                }
                break;
            }
            default:
                break;
        }
        lightVersion = ++counter;
    }

    public static void lightModel(int pname, float[] v) {
        float[] copy = v.clone();
        if (record(() -> lightModelImpl(pname, copy))) {
            return;
        }
        lightModelImpl(pname, copy);
    }

    private static void lightModelImpl(int pname, float[] v) {
        flushBatch();
        if (pname == 0xB53) {
            System.arraycopy(v, 0, lightModelAmbient, 0, 4);
            lightVersion = ++counter;
        }
    }

    public static void fogi(int pname, int value) {
        if (record(() -> fogiImpl(pname, value))) {
            return;
        }
        fogiImpl(pname, value);
    }

    private static void fogiImpl(int pname, int value) {
        flushBatch();
        if (pname == 0xB65) {
            fogMode = value;
            fogVersion = ++counter;
        } else {
            fogfImpl(pname, value);
        }
    }

    public static void fogf(int pname, float value) {
        if (record(() -> fogfImpl(pname, value))) {
            return;
        }
        fogfImpl(pname, value);
    }

    private static void fogfImpl(int pname, float value) {
        flushBatch();
        switch (pname) {
            case 0xB62:
                fogDensity = value;
                break;
            case 0xB63:
                fogStart = value;
                break;
            case 0xB64:
                fogEnd = value;
                break;
            case 0xB65:
                fogMode = (int) value;
                break;
            default:
                return;
        }
        fogVersion = ++counter;
    }

    public static void fogv(int pname, float[] v) {
        float[] copy = v.clone();
        if (record(() -> fogvImpl(pname, copy))) {
            return;
        }
        fogvImpl(pname, copy);
    }

    private static void fogvImpl(int pname, float[] v) {
        flushBatch();
        if (pname == 0xB66) {
            System.arraycopy(v, 0, fogColor, 0, Math.min(4, v.length));
            fogVersion = ++counter;
        } else if (v.length > 0) {
            fogfImpl(pname, v[0]);
        }
    }

    public static void texGeni(int coord, int pname, int mode) {
        if (record(() -> texGeniImpl(coord, pname, mode))) {
            return;
        }
        texGeniImpl(coord, pname, mode);
    }

    private static void texGeniImpl(int coord, int pname, int mode) {
        int i = coord - 0x2000;
        if (i >= 0 && i < 4 && pname == 0x2500) {
            texGenMode[i] = mode;
            texGenVersion = ++counter;
        }
    }

    public static void texGen(int coord, int pname, float[] v) {
        float[] copy = v.clone();
        if (record(() -> texGenImpl(coord, pname, copy))) {
            return;
        }
        texGenImpl(coord, pname, copy);
    }

    private static void texGenImpl(int coord, int pname, float[] p) {
        int i = coord - 0x2000;
        if (i < 0 || i >= 4) {
            return;
        }
        if (pname == 0x2501) {
            System.arraycopy(p, 0, texGenObjectPlane[i], 0, 4);
        } else if (pname == 0x2502) {
            float[] inv = new float[16];
            if (Mat4.invert(modelview[mvTop], inv)) {
                for (int j = 0; j < 4; j++) {
                    texGenEyePlane[i][j] = p[0] * inv[j * 4] + p[1] * inv[j * 4 + 1] + p[2] * inv[j * 4 + 2]
                            + p[3] * inv[j * 4 + 3];
                }
            }
        } else if (pname == 0x2500) {
            texGenMode[i] = (int) p[0];
        }
        texGenVersion = ++counter;
    }

    // =====================================================================================================
    // Textures
    // =====================================================================================================

    /** A new texture object with the defaults the emulation assumes; it is left bound to the active unit. */
    private static Tex newTex() {
        Tex t = new Tex();
        t.tex = gl.createTexture();
        gl.bindTexture(GL_TEXTURE_2D, t.tex);
        gl.texParameteri(GL_TEXTURE_2D, 0x813D, 0);
        gl.texParameteri(GL_TEXTURE_2D, 0x2801, 0x2601);
        t.minFilter = 0x2601; // as just set; the rest are WebGL's defaults
        t.magFilter = 0x2601;
        t.wrapS = 0x2901;
        t.wrapT = 0x2901;
        return t;
    }

    public static int genTexture() {
        Tex t = newTex();
        textures.add(t);
        int id = textures.size() - 1;
        gl.bindTexture(GL_TEXTURE_2D, texObject(boundTexture[activeUnit]));
        return id;
    }

    public static void deleteTexture(int id) {
        if (id > 0 && id < textures.size() && textures.get(id) != null) {
            flushBatch();
            gl.deleteTexture(textures.get(id).tex);
            textures.set(id, null);
            for (int u = 0; u < 4; u++) {
                if (boundTexture[u] == id) {
                    boundTexture[u] = 0;
                }
            }
        }
    }

    private static JSObject texObject(int id) {
        Tex t = id > 0 && id < textures.size() ? textures.get(id) : null;
        return t != null ? t.tex : null;
    }

    private static Tex tex(int id) {
        return id > 0 && id < textures.size() ? textures.get(id) : null;
    }

    public static void bindTexture(int target, int id) {
        if (record(() -> bindTextureImpl(id))) {
            return;
        }
        bindTextureImpl(id);
    }

    private static void bindTextureImpl(int id) {
        if (id > 0 && (id >= textures.size() || textures.get(id) == null)) {
            // GL lets a deleted (or never generated) name be bound again, which creates a new texture under that
            // name; Minecraft 1.7 deletes a texture and re-fills the same name.
            flushBatch();
            while (textures.size() <= id) {
                textures.add(null);
            }
            textures.set(id, newTex());
            boundTexture[activeUnit] = id;
            return;
        }
        if (boundTexture[activeUnit] != id) {
            flushBatch();
            boundTexture[activeUnit] = id;
            gl.bindTexture(GL_TEXTURE_2D, texObject(id));
        }
    }

    public static int boundTexture() {
        return boundTexture[activeUnit];
    }

    public static void activeTexture(int unit) {
        if (record(() -> activeTextureImpl(unit))) {
            return;
        }
        activeTextureImpl(unit);
    }

    private static void activeTextureImpl(int unit) {
        int u = (unit - GL_TEXTURE0) & 3;
        if (u != activeUnit) {
            activeUnit = u;
            gl.activeTexture(GL_TEXTURE0 + u);
        }
    }

    public static void clientActiveTexture(int unit) {
        clientActiveUnit = (unit - GL_TEXTURE0) & 3;
    }

    public static void texParameteri(int target, int pname, int param) {
        if (record(() -> texParameteriImpl(pname, param))) {
            return;
        }
        texParameteriImpl(pname, param);
    }

    private static void texParameteriImpl(int pname, int param) {
        Tex bound = tex(boundTexture[activeUnit]);
        switch (pname) {
            case 0x2801: // MIN_FILTER
            case 0x2800: // MAG_FILTER
            case 0x2802: // WRAP_S
            case 0x2803: { // WRAP_T
                int value = pname >= 0x2802 && (param == 0x2900 || param == 0x812D) ? 0x812F : param;
                if (bound != null) {
                    // EntityRenderer sets the lightmap's filters and wrapping every time it enables it
                    int last = pname == 0x2801 ? bound.minFilter : pname == 0x2800 ? bound.magFilter
                            : pname == 0x2802 ? bound.wrapS : bound.wrapT;
                    if (last == value) {
                        return;
                    }
                    if (pname == 0x2801) {
                        bound.minFilter = value;
                    } else if (pname == 0x2800) {
                        bound.magFilter = value;
                    } else if (pname == 0x2802) {
                        bound.wrapS = value;
                    } else {
                        bound.wrapT = value;
                    }
                }
                flushBatch();
                gl.texParameteri(GL_TEXTURE_2D, pname, value);
                return;
            }
            default:
                break;
        }
        flushBatch();
        switch (pname) {
            case 0x813D: { // MAX_LEVEL
                Tex t = tex(boundTexture[activeUnit]);
                if (t != null) {
                    t.maxLevel = Math.min(param, 16);
                    gl.texParameteri(GL_TEXTURE_2D, pname, t.maxLevel);
                }
                break;
            }
            case 0x813C: // BASE_LEVEL
            case 0x813A: // MIN_LOD
            case 0x813B: // MAX_LOD
                gl.texParameteri(GL_TEXTURE_2D, pname, param);
                break;
            default:
                break;
        }
    }

    /** GL_PROXY_TEXTURE_2D bookkeeping: Minecraft 1.7 finds the largest texture the driver accepts by probing it. */
    private static int proxyWidth;
    private static int proxyHeight;

    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border,
            int format, int type, Buffer pixels) {
        if (target == 0x8064) {
            int max = getIntegers(0x0D33)[0];
            boolean fits = width <= max && height <= max;
            proxyWidth = fits ? width : 0;
            proxyHeight = fits ? height : 0;
            return;
        }
        Tex t = tex(boundTexture[activeUnit]);
        if (t == null || target != GL_TEXTURE_2D) {
            return;
        }
        flushBatch();
        ArrayBufferView data = pixels == null ? null : convertPixels(format, type, width, height, pixels);
        gl.texImage2D(GL_TEXTURE_2D, level, 0x8058, width, height, 0, 0x1908, GL_UNSIGNED_BYTE, data);
        if (level == 0) {
            t.width = width;
            t.height = height;
            t.rgb = false;
        }
        if (level > t.maxLevel) {
            t.maxLevel = level;
            gl.texParameteri(GL_TEXTURE_2D, 0x813D, level);
        }
    }

    public static void texSubImage2D(int target, int level, int x, int y, int width, int height, int format,
            int type, Buffer pixels) {
        Tex t = tex(boundTexture[activeUnit]);
        if (t == null || pixels == null || width <= 0 || height <= 0) {
            return;
        }
        flushBatch();
        ArrayBufferView data = convertPixels(format, type, width, height, pixels);
        gl.texSubImage2D(GL_TEXTURE_2D, level, x, y, width, height, 0x1908, GL_UNSIGNED_BYTE, data);
    }

    public static void copyTexSubImage2D(int target, int level, int xoff, int yoff, int x, int y, int w, int h) {
        Tex t = tex(boundTexture[activeUnit]);
        if (t == null) {
            return;
        }
        flushBatch();
        if (boundFramebuffer == 0 && !t.rgb && t.width > 0) {
            // The canvas has no alpha channel and WebGL cannot copy RGB into RGBA. Desktop GL fills alpha with 1
            // in that case, which is what an RGB8 texture samples as.
            gl.texImage2D(GL_TEXTURE_2D, 0, 0x8051, t.width, t.height, 0, 0x1907, GL_UNSIGNED_BYTE, null);
            gl.texParameteri(GL_TEXTURE_2D, 0x813D, 0);
            t.maxLevel = 0;
            t.rgb = true;
        }
        gl.copyTexSubImage2D(GL_TEXTURE_2D, level, xoff, yoff, x, y, w, h);
    }

    public static int getTexLevelParameteri(int target, int level, int pname) {
        if (target == 0x8064) {
            return pname == 0x1000 ? proxyWidth : pname == 0x1001 ? proxyHeight : 0;
        }
        Tex t = tex(boundTexture[activeUnit]);
        if (t == null) {
            return 0;
        }
        int w = Math.max(1, t.width >> level);
        int h = Math.max(1, t.height >> level);
        switch (pname) {
            case 0x1000:
                return w;
            case 0x1001:
                return h;
            case 0x1003:
                return 0x8058;
            default:
                return 0;
        }
    }

    public static void generateMipmap() {
        Tex t = tex(boundTexture[activeUnit]);
        if (t != null && t.width > 0) {
            flushBatch();
            gl.texParameteri(GL_TEXTURE_2D, 0x813D, 1000);
            t.maxLevel = 1000;
            gl.generateMipmap(GL_TEXTURE_2D);
        }
    }

    /** Converts client pixel data to tightly packed RGBA bytes. */
    private static ArrayBufferView convertPixels(int format, int type, int width, int height, Buffer pixels) {
        Src src = new Src();
        resolve(pixels, src);
        int bytes = width * height * pixelSize(format, type);
        return convert(src.base, src.offset, bytes, format, type, width * height);
    }

    private static int pixelSize(int format, int type) {
        if (type == 0x8367 || type == 0x8035 || type == GL_UNSIGNED_INT || type == GL_INT || type == GL_FLOAT) {
            return type == GL_FLOAT ? 4 * components(format) : 4;
        }
        return components(format);
    }

    private static int components(int format) {
        switch (format) {
            case 0x1907: // RGB
            case 0x80E0: // BGR
                return 3;
            case 0x1909: // LUMINANCE
            case 0x1906: // ALPHA
            case 0x1903: // RED
                return 1;
            case 0x190A: // LUMINANCE_ALPHA
                return 2;
            default:
                return 4;
        }
    }

    @JSBody(params = { "buffer", "offset", "length", "format", "type", "pixels" }, script = ""
            + "var src = new Uint8Array(buffer, offset, Math.min(length, buffer.byteLength - offset));"
            + "if (format === 0x1908 && (type === 0x1401 || type === 0x8367)) return src;"
            + "var out = new Uint8Array(pixels * 4);"
            + "var i, j;"
            + "if (format === 0x80E1 && (type === 0x1401 || type === 0x8367)) {"
            + "  for (i = 0; i < pixels; i++) { j = i * 4;"
            + "    out[j] = src[j + 2]; out[j + 1] = src[j + 1]; out[j + 2] = src[j]; out[j + 3] = src[j + 3]; }"
            + "} else if (format === 0x1908 && type === 0x8035) {"
            + "  for (i = 0; i < pixels; i++) { j = i * 4;"
            + "    out[j] = src[j + 3]; out[j + 1] = src[j + 2]; out[j + 2] = src[j + 1]; out[j + 3] = src[j]; }"
            + "} else if (format === 0x80E1 && type === 0x8035) {"
            + "  for (i = 0; i < pixels; i++) { j = i * 4;"
            + "    out[j] = src[j + 1]; out[j + 1] = src[j + 2]; out[j + 2] = src[j + 3]; out[j + 3] = src[j]; }"
            + "} else if (format === 0x1907) {"
            + "  for (i = 0; i < pixels; i++) {"
            + "    out[i*4] = src[i*3]; out[i*4+1] = src[i*3+1]; out[i*4+2] = src[i*3+2]; out[i*4+3] = 255; }"
            + "} else if (format === 0x80E0) {"
            + "  for (i = 0; i < pixels; i++) {"
            + "    out[i*4] = src[i*3+2]; out[i*4+1] = src[i*3+1]; out[i*4+2] = src[i*3]; out[i*4+3] = 255; }"
            + "} else if (format === 0x1909 || format === 0x1903) {"
            + "  for (i = 0; i < pixels; i++) { out[i*4] = out[i*4+1] = out[i*4+2] = src[i]; out[i*4+3] = 255; }"
            + "} else if (format === 0x1906) {"
            + "  for (i = 0; i < pixels; i++) { out[i*4] = out[i*4+1] = out[i*4+2] = 255; out[i*4+3] = src[i]; }"
            + "} else if (format === 0x190A) {"
            + "  for (i = 0; i < pixels; i++) { out[i*4] = out[i*4+1] = out[i*4+2] = src[i*2]; out[i*4+3] = src[i*2+1]; }"
            + "} else { out.set(src.subarray(0, Math.min(src.length, out.length))); }"
            + "return out;")
    private static native ArrayBufferView convert(ArrayBuffer buffer, int offset, int length, int format, int type,
            int pixels);

    public static void pixelStorei(int pname, int param) {
        switch (pname) {
            case 0x0CF5: // UNPACK_ALIGNMENT
            case 0x0D05: // PACK_ALIGNMENT
            case 0x0CF2: // UNPACK_ROW_LENGTH
            case 0x0CF3: // UNPACK_SKIP_ROWS
            case 0x0CF4: // UNPACK_SKIP_PIXELS
            case 0x0D02: // PACK_ROW_LENGTH
                gl.pixelStorei(pname, param);
                break;
            default:
                break;
        }
    }

    public static void readPixels(int x, int y, int width, int height, int format, int type, Buffer pixels) {
        flushBatch();
        Uint8Array rgba = new Uint8Array(width * height * 4);
        gl.readPixels(x, y, width, height, 0x1908, GL_UNSIGNED_BYTE, rgba);
        Src dst = new Src();
        resolve(pixels, dst);
        writePixels(rgba, dst.base, dst.offset, format, width * height);
    }

    @JSBody(params = { "rgba", "buffer", "offset", "format", "pixels" }, script = ""
            + "var out = new Uint8Array(buffer, offset);"
            + "var i;"
            + "if (format === 0x1907) { for (i = 0; i < pixels; i++) {"
            + "  out[i*3] = rgba[i*4]; out[i*3+1] = rgba[i*4+1]; out[i*3+2] = rgba[i*4+2]; } }"
            + "else if (format === 0x80E0) { for (i = 0; i < pixels; i++) {"
            + "  out[i*3] = rgba[i*4+2]; out[i*3+1] = rgba[i*4+1]; out[i*3+2] = rgba[i*4]; } }"
            + "else if (format === 0x80E1) { for (i = 0; i < pixels; i++) {"
            + "  out[i*4] = rgba[i*4+2]; out[i*4+1] = rgba[i*4+1]; out[i*4+2] = rgba[i*4]; out[i*4+3] = rgba[i*4+3]; } }"
            + "else { out.set(rgba.subarray(0, Math.min(rgba.length, out.length))); }")
    private static native void writePixels(Uint8Array rgba, ArrayBuffer buffer, int offset, int format, int pixels);

    // =====================================================================================================
    // Vertex arrays and buffer objects
    // =====================================================================================================

    private static Arr arrayFor(int cap) {
        switch (cap) {
            case 0x8074:
                return vertexArray;
            case 0x8076:
                return colorArray;
            case 0x8075:
                return normalArray;
            case 0x8078:
                return texCoordArray[clientActiveUnit];
            default:
                return null;
        }
    }

    public static void enableClientState(int cap) {
        Arr a = arrayFor(cap);
        if (a != null) {
            a.enabled = true;
        }
    }

    public static void disableClientState(int cap) {
        Arr a = arrayFor(cap);
        if (a != null) {
            a.enabled = false;
        }
    }

    private static void pointer(Arr a, int size, int type, boolean normalized, int stride, Buffer buffer) {
        a.size = size;
        a.type = type;
        a.normalized = normalized;
        a.stride = stride;
        a.buffer = buffer;
        a.vbo = 0;
        if (buffer != null) {
            a.position = buffer.position();
            Src s = new Src();
            resolve(buffer, s);
            a.base = s.base;
            a.baseOffset = s.offset;
        } else {
            a.base = null;
        }
    }

    private static void pointer(Arr a, int size, int type, boolean normalized, int stride, long offset) {
        a.size = size;
        a.type = type;
        a.normalized = normalized;
        a.stride = stride;
        a.buffer = null;
        a.vbo = arrayBufferBinding;
        a.vboOffset = (int) offset;
    }

    public static void vertexPointer(int size, int type, int stride, Buffer buffer) {
        pointer(vertexArray, size, type, false, stride, buffer);
    }

    public static void vertexPointer(int size, int type, int stride, long offset) {
        pointer(vertexArray, size, type, false, stride, offset);
    }

    public static void colorPointer(int size, int type, int stride, Buffer buffer) {
        pointer(colorArray, size, type, type != GL_FLOAT, stride, buffer);
    }

    public static void colorPointer(int size, int type, int stride, long offset) {
        pointer(colorArray, size, type, type != GL_FLOAT, stride, offset);
    }

    public static void normalPointer(int type, int stride, Buffer buffer) {
        pointer(normalArray, 3, type, type != GL_FLOAT, stride, buffer);
    }

    public static void normalPointer(int type, int stride, long offset) {
        pointer(normalArray, 3, type, type != GL_FLOAT, stride, offset);
    }

    public static void texCoordPointer(int size, int type, int stride, Buffer buffer) {
        pointer(texCoordArray[clientActiveUnit], size, type, false, stride, buffer);
    }

    public static void texCoordPointer(int size, int type, int stride, long offset) {
        pointer(texCoordArray[clientActiveUnit], size, type, false, stride, offset);
    }

    public static int genBuffer() {
        userBuffers.add(new ArrayBuffer(0));
        return userBuffers.size() - 1;
    }

    public static void deleteBuffer(int id) {
        if (id > 0 && id < userBuffers.size()) {
            userBuffers.set(id, null);
        }
    }

    public static void bindBuffer(int target, int id) {
        if (target == GL_ARRAY_BUFFER) {
            arrayBufferBinding = id;
        } else if (target == GL_ELEMENT_ARRAY_BUFFER) {
            elementBufferBinding = id;
        }
    }

    public static void bufferData(int target, Buffer data, int size) {
        int id = target == GL_ARRAY_BUFFER ? arrayBufferBinding : elementBufferBinding;
        if (id <= 0 || id >= userBuffers.size()) {
            return;
        }
        if (data == null) {
            userBuffers.set(id, new ArrayBuffer(size));
            return;
        }
        Src src = new Src();
        resolve(data, src);
        int length = data.remaining() * elementSize(data);
        userBuffers.set(id, JSUtil.slice(src.base, src.offset, length));
    }

    /** Where an attribute's data lives: a JS ArrayBuffer and a byte offset. */
    static final class Src {
        ArrayBuffer base;
        int offset;
        int size;
        int type;
        boolean normalized;
        int stride;
        int end;
    }

    private static int elementSize(Buffer b) {
        if (b instanceof ByteBuffer) {
            return 1;
        } else if (b instanceof ShortBuffer || b instanceof CharBuffer) {
            return 2;
        } else if (b instanceof DoubleBuffer || b instanceof LongBuffer) {
            return 8;
        }
        return 4;
    }

    static int typeSize(int type) {
        switch (type) {
            case GL_BYTE:
            case GL_UNSIGNED_BYTE:
                return 1;
            case GL_SHORT:
            case GL_UNSIGNED_SHORT:
                return 2;
            case GL_DOUBLE:
                return 8;
            default:
                return 4;
        }
    }

    /** Resolves a Java NIO buffer (at its current position) to its backing JS memory. */
    static void resolve(Buffer buffer, Src out) {
        ArrayBufferView view;
        try {
            view = Int8Array.fromJavaBuffer(buffer);
        } catch (RuntimeException e) {
            view = copyToTypedArray(buffer);
            out.base = view.getBuffer();
            out.offset = view.getByteOffset();
            return;
        }
        int esize = elementSize(buffer);
        if (esize > 1 && isBigEndian(buffer)) {
            view = swapCopy(buffer);
            out.base = view.getBuffer();
            out.offset = view.getByteOffset();
            return;
        }
        out.base = view.getBuffer();
        out.offset = view.getByteOffset() + buffer.position() * esize;
    }

    private static boolean isBigEndian(Buffer b) {
        ByteOrder order;
        if (b instanceof FloatBuffer) {
            order = ((FloatBuffer) b).order();
        } else if (b instanceof IntBuffer) {
            order = ((IntBuffer) b).order();
        } else if (b instanceof ShortBuffer) {
            order = ((ShortBuffer) b).order();
        } else if (b instanceof DoubleBuffer) {
            order = ((DoubleBuffer) b).order();
        } else {
            return false;
        }
        return order == ByteOrder.BIG_ENDIAN;
    }

    /** Copies a buffer's remaining elements into a new little-endian typed array. */
    private static ArrayBufferView copyToTypedArray(Buffer b) {
        int n = b.remaining();
        int p = b.position();
        if (b instanceof FloatBuffer) {
            float[] a = new float[n];
            for (int i = 0; i < n; i++) {
                a[i] = ((FloatBuffer) b).get(p + i);
            }
            return Float32Array.fromJavaArray(a);
        } else if (b instanceof IntBuffer) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = ((IntBuffer) b).get(p + i);
            }
            return org.teavm.jso.typedarrays.Int32Array.fromJavaArray(a);
        } else if (b instanceof ShortBuffer) {
            short[] a = new short[n];
            for (int i = 0; i < n; i++) {
                a[i] = ((ShortBuffer) b).get(p + i);
            }
            return org.teavm.jso.typedarrays.Int16Array.fromJavaArray(a);
        } else if (b instanceof ByteBuffer) {
            byte[] a = new byte[n];
            for (int i = 0; i < n; i++) {
                a[i] = ((ByteBuffer) b).get(p + i);
            }
            return Int8Array.fromJavaArray(a);
        } else if (b instanceof DoubleBuffer) {
            float[] a = new float[n];
            for (int i = 0; i < n; i++) {
                a[i] = (float) ((DoubleBuffer) b).get(p + i);
            }
            return Float32Array.fromJavaArray(a);
        }
        throw new IllegalArgumentException("Unsupported buffer " + b);
    }

    private static ArrayBufferView swapCopy(Buffer b) {
        return copyToTypedArray(b);
    }

    private static final Src[] srcs = { new Src(), new Src(), new Src(), new Src(), new Src() };
    private static final boolean[] srcUsed = new boolean[5];

    private static boolean resolveArray(Arr a, int first, Src s) {
        if (!a.enabled) {
            return false;
        }
        int tsize = typeSize(a.type);
        int stride = a.stride != 0 ? a.stride : a.size * tsize;
        if (a.vbo != 0) {
            ArrayBuffer data = a.vbo < userBuffers.size() ? userBuffers.get(a.vbo) : null;
            if (data == null) {
                return false;
            }
            s.base = data;
            s.offset = a.vboOffset;
        } else if (a.base != null) {
            s.base = a.base;
            s.offset = a.baseOffset;
        } else {
            return false;
        }
        s.offset += first * stride;
        s.size = a.size;
        s.type = a.type;
        s.normalized = a.normalized;
        s.stride = stride;
        return true;
    }

    public static void drawArrays(int mode, int first, int count) {
        if (count <= 0 || beginMode >= 0) {
            return;
        }
        srcUsed[Shaders.A_POS] = resolveArray(vertexArray, first, srcs[Shaders.A_POS]);
        if (!srcUsed[Shaders.A_POS]) {
            return;
        }
        srcUsed[Shaders.A_COLOR] = resolveArray(colorArray, first, srcs[Shaders.A_COLOR]);
        srcUsed[Shaders.A_NORMAL] = resolveArray(normalArray, first, srcs[Shaders.A_NORMAL]);
        srcUsed[Shaders.A_TEX0] = resolveArray(texCoordArray[0], first, srcs[Shaders.A_TEX0]);
        srcUsed[Shaders.A_TEX1] = resolveArray(texCoordArray[1], first, srcs[Shaders.A_TEX1]);
        submit(mode, count);
    }

    /** Draws (or records, inside a display list) the vertices described by {@link #srcs}. */
    private static void submit(int mode, int count) {
        if (compiling != null) {
            DrawOp op = capture(mode, count);
            compiling.ops.add(op);
            if (compileAndExecute) {
                drawOp(op);
            }
            return;
        }
        if (batchable(mode, count) && appendToBatch(mode, count, srcs, srcUsed)) {
            return;
        }
        flushBatch();
        bindVao(streamVao);
        int mask = 0;
        for (int i = 0; i < 5; i++) {
            if (srcUsed[i]) {
                mask |= 1 << i;
            }
        }
        // Group attributes by the JS buffer they live in; usually everything is one interleaved buffer.
        int groups = 0;
        ArrayBuffer[] groupBase = streamGroupBase;
        int[] groupMin = streamGroupMin;
        int[] groupMax = streamGroupMax;
        int[] attribGroup = streamAttribGroup;
        for (int i = 0; i < 5; i++) {
            if (!srcUsed[i]) {
                continue;
            }
            Src s = srcs[i];
            int end = s.offset + (count - 1) * s.stride + s.size * typeSize(s.type);
            int g = -1;
            for (int k = 0; k < groups; k++) {
                if (JSUtil.same(groupBase[k], s.base)) {
                    g = k;
                    break;
                }
            }
            if (g < 0) {
                g = groups++;
                groupBase[g] = s.base;
                groupMin[g] = s.offset;
                groupMax[g] = end;
            } else {
                groupMin[g] = Math.min(groupMin[g], s.offset);
                groupMax[g] = Math.max(groupMax[g], end);
            }
            attribGroup[i] = g;
        }
        for (int g = 0; g < groups; g++) {
            int length = Math.min(groupMax[g], groupBase[g].getByteLength()) - groupMin[g];
            gl.bindBuffer(GL_ARRAY_BUFFER, streamVbo[g]);
            gl.bufferData(GL_ARRAY_BUFFER, JSUtil.bytes(groupBase[g], groupMin[g], Math.max(0, length)), 0x88E0);
        }
        for (int i = 0; i < 5; i++) {
            if (!srcUsed[i]) {
                continue;
            }
            Src s = srcs[i];
            int g = attribGroup[i];
            gl.bindBuffer(GL_ARRAY_BUFFER, streamVbo[g]);
            gl.vertexAttribPointer(i, s.size, s.type, s.normalized, s.stride, s.offset - groupMin[g]);
        }
        if (mask != streamEnabledMask) {
            for (int i = 0; i < 5; i++) {
                boolean on = (mask & (1 << i)) != 0;
                if (on != ((streamEnabledMask & (1 << i)) != 0)) {
                    if (on) {
                        gl.enableVertexAttribArray(i);
                    } else {
                        gl.disableVertexAttribArray(i);
                    }
                }
            }
            streamEnabledMask = mask;
        }
        prepare(mask, false);
        issue(mode, count);
    }

    private static DrawOp capture(int mode, int count) {
        DrawOp op = new DrawOp();
        op.mode = mode;
        op.count = count;
        if (count <= BATCH_MAX_DRAW) {
            // small list draws (e.g. a font character) are batched when replayed: keep the data on the CPU
            op.cpu = new Src[5];
            for (int i = 0; i < 5; i++) {
                if (srcUsed[i]) {
                    Src from = srcs[i];
                    Src c = new Src();
                    int extent = (count - 1) * from.stride + from.size * typeSize(from.type);
                    c.base = JSUtil.slice(from.base, from.offset,
                            Math.max(0, Math.min(extent, from.base.getByteLength() - from.offset)));
                    c.offset = 0;
                    c.size = from.size;
                    c.type = from.type;
                    c.normalized = from.normalized;
                    c.stride = from.stride;
                    op.cpu[i] = c;
                }
            }
        }
        op.vao = gl.createVertexArray();
        bindVao(op.vao);
        gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer);
        List<JSObject> vbos = new ArrayList<>();
        List<ArrayBuffer> bases = new ArrayList<>();
        List<int[]> ranges = new ArrayList<>();
        int[] attribGroup = new int[5];
        for (int i = 0; i < 5; i++) {
            if (!srcUsed[i]) {
                continue;
            }
            Src s = srcs[i];
            int end = s.offset + (count - 1) * s.stride + s.size * typeSize(s.type);
            int g = -1;
            for (int k = 0; k < bases.size(); k++) {
                if (JSUtil.same(bases.get(k), s.base)) {
                    g = k;
                    break;
                }
            }
            if (g < 0) {
                g = bases.size();
                bases.add(s.base);
                ranges.add(new int[] { s.offset, end });
            } else {
                int[] r = ranges.get(g);
                r[0] = Math.min(r[0], s.offset);
                r[1] = Math.max(r[1], end);
            }
            attribGroup[i] = g;
            op.attribMask |= 1 << i;
        }
        for (int g = 0; g < bases.size(); g++) {
            int[] r = ranges.get(g);
            int length = Math.min(r[1], bases.get(g).getByteLength()) - r[0];
            JSObject vbo = gl.createBuffer();
            gl.bindBuffer(GL_ARRAY_BUFFER, vbo);
            gl.bufferData(GL_ARRAY_BUFFER, JSUtil.bytes(bases.get(g), r[0], Math.max(0, length)), 0x88E4);
            vbos.add(vbo);
        }
        for (int i = 0; i < 5; i++) {
            if (!srcUsed[i]) {
                continue;
            }
            Src s = srcs[i];
            gl.bindBuffer(GL_ARRAY_BUFFER, vbos.get(attribGroup[i]));
            gl.vertexAttribPointer(i, s.size, s.type, s.normalized, s.stride, s.offset - ranges.get(attribGroup[i])[0]);
            gl.enableVertexAttribArray(i);
        }
        op.vbos = vbos.toArray(new JSObject[0]);
        bindVao(streamVao);
        if (mode == GL_QUADS) {
            ensureQuadIndices(count);
        }
        return op;
    }

    private static void drawOp(DrawOp op) {
        if (op.cpu != null && batchable(op.mode, op.count) && appendToBatch(op.mode, op.count, op.cpu, null)) {
            return;
        }
        flushBatch();
        bindVao(op.vao);
        prepare(op.attribMask, false);
        issue(op.mode, op.count);
    }

    // ---- Small-draw batching (see BATCH_MAX_DRAW) ----

    /** Whether a draw may join the batch: small, a polygon mode, no texgen, an affine modelview. */
    private static boolean batchable(int mode, int count) {
        if (count > BATCH_MAX_DRAW || texGenEnabled[0] || texGenEnabled[1] || texGenEnabled[2]
                || texGenEnabled[3]) {
            return false;
        }
        switch (mode) {
            case GL_TRIANGLES:
            case GL_TRIANGLE_STRIP:
            case GL_TRIANGLE_FAN:
            case GL_QUADS:
            case GL_QUAD_STRIP:
            case GL_POLYGON:
                break;
            default:
                return false;
        }
        float[] m = modelview[mvTop];
        return m[3] == 0 && m[7] == 0 && m[11] == 0 && m[15] == 1;
    }

    /**
     * Appends a draw to the batch as triangles in eye space (positions and normals; the shader normalizes the
     * latter), with every attribute per vertex (current values where no array is enabled). {@code used} null means attributes are present where {@code s[i]} is not null.
     * False if the vertex layout is not supported, the draw is then issued normally.
     */
    private static boolean appendToBatch(int mode, int count, Src[] s, boolean[] used) {
        Src pos = batchSource(s, used, Shaders.A_POS);
        Src tex0 = batchSource(s, used, Shaders.A_TEX0);
        Src tex1 = batchSource(s, used, Shaders.A_TEX1);
        if (pos == null || pos.size > 3 || tex0 != null && tex0.size > 2 || tex1 != null && tex1.size > 2) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            Src a = batchSource(s, used, i);
            if (a != null && a.type == GL_DOUBLE) {
                return false;
            }
        }
        int triangles = mode == GL_TRIANGLES ? count / 3 : mode == GL_QUADS ? count / 4 * 2
                : mode == GL_QUAD_STRIP ? Math.max(0, (count - 2) / 2 * 2) : Math.max(0, count - 2);
        if (batchVerts + triangles * 3 > BATCH_CAPACITY) {
            flushBatch();
        }
        for (int i = 0; i < 5; i++) {
            Src a = batchSource(s, used, i);
            if (a == null) {
                setBatchAttr(batchState, i, null, 0, 0, 0, 0, false);
            } else {
                setBatchAttr(batchState, i, a.base, a.offset, a.stride, a.type, a.size, a.normalized);
            }
        }
        float[] t0 = texCoord[0];
        float[] t1 = texCoord[1];
        batchVerts = appendBatch(batchState, batchVerts, mode, count, Float32Array.fromJavaArray(modelview[mvTop]),
                color[0], color[1], color[2], color[3], normal[0], normal[1], normal[2], t0[0], t0[1], t1[0], t1[1]);
        return true;
    }

    private static Src batchSource(Src[] s, boolean[] used, int i) {
        return (used == null || used[i]) ? s[i] : null;
    }

    /** Draws the batched vertices, with the (unchanged since) current state and an identity modelview. */
    static void flushBatch() {
        if (batchVerts == 0) {
            return;
        }
        int n = batchVerts;
        batchVerts = 0;
        bindVao(streamVao);
        gl.bindBuffer(GL_ARRAY_BUFFER, batchVbo);
        gl.bufferData(GL_ARRAY_BUFFER, batchData(batchState, n * BATCH_FLOATS), 0x88E0);
        int stride = BATCH_FLOATS * 4;
        gl.vertexAttribPointer(Shaders.A_POS, 3, GL_FLOAT, false, stride, 0);
        gl.vertexAttribPointer(Shaders.A_COLOR, 4, GL_FLOAT, false, stride, 12);
        gl.vertexAttribPointer(Shaders.A_NORMAL, 3, GL_FLOAT, false, stride, 28);
        gl.vertexAttribPointer(Shaders.A_TEX0, 2, GL_FLOAT, false, stride, 40);
        gl.vertexAttribPointer(Shaders.A_TEX1, 2, GL_FLOAT, false, stride, 48);
        for (int i = 0; i < 5; i++) {
            if ((streamEnabledMask & (1 << i)) == 0) {
                gl.enableVertexAttribArray(i);
            }
        }
        streamEnabledMask = 0x1F;
        prepare(0x1F, true);
        gl.drawArrays(GL_TRIANGLES, 0, n);
    }

    @JSBody(params = "capacity", script = ""
            + "var attribs = [];"
            + "for (var slot = 0; slot < 5; slot++) {"
            + "  attribs.push({ base: null, view: null, viewBase: null, off: 0, stride: 0, type: 0, size: 0, norm: false });"
            + "}"
            + "return { out: new Float32Array(capacity), attribs: attribs, order: new Int32Array(512) };")
    private static native JSObject createBatchState(int capacity);

    @JSBody(params = { "S", "slot", "base", "off", "stride", "type", "size", "norm" }, script = ""
            + "var desc = S.attribs[slot];"
            + "desc.base = base;"
            + "if (base && desc.viewBase !== base) { desc.view = new DataView(base); desc.viewBase = base; }"
            + "desc.off = off; desc.stride = stride; desc.type = type; desc.size = size; desc.norm = norm;")
    private static native void setBatchAttr(JSObject state, int slot, ArrayBuffer base, int off, int stride,
            int type, int size, boolean norm);

    @JSBody(params = { "S", "floats" }, script = "return S.out.subarray(0, floats);")
    private static native ArrayBufferView batchData(JSObject state, int floats);

    /** Appends a draw's vertices as triangles; returns the new vertex count of the batch. */
    @JSBody(params = { "S", "start", "mode", "count", "mv", "cr", "cg", "cb", "ca", "nx", "ny", "nz",
            "s0", "t0", "s1", "t1" }, script = ""
            + "function read(desc, vertex, comp) {"
            + "  var at = desc.off + vertex * desc.stride;"
            + "  var dv = desc.view;"
            + "  switch (desc.type) {"
            + "    case 0x1406: return dv.getFloat32(at + comp * 4, true);"
            + "    case 0x1401: return desc.norm ? dv.getUint8(at + comp) / 255 : dv.getUint8(at + comp);"
            + "    case 0x1400: return desc.norm ? Math.max(dv.getInt8(at + comp) / 127, -1) : dv.getInt8(at + comp);"
            + "    case 0x1402: return desc.norm ? Math.max(dv.getInt16(at + comp * 2, true) / 32767, -1)"
            + "        : dv.getInt16(at + comp * 2, true);"
            + "    case 0x1403: return desc.norm ? dv.getUint16(at + comp * 2, true) / 65535"
            + "        : dv.getUint16(at + comp * 2, true);"
            + "    case 0x1404: return dv.getInt32(at + comp * 4, true);"
            + "    case 0x1405: return dv.getUint32(at + comp * 4, true);"
            + "    default: return 0;"
            + "  }"
            + "}"
            + "var order = S.order, len = 0, idx;"
            + "if (mode === 4) {"
            + "  for (idx = 0; idx + 2 < count; idx += 3) { order[len++] = idx; order[len++] = idx + 1; order[len++] = idx + 2; }"
            + "} else if (mode === 7) {"
            + "  for (idx = 0; idx + 3 < count; idx += 4) {"
            + "    order[len++] = idx; order[len++] = idx + 1; order[len++] = idx + 2;"
            + "    order[len++] = idx; order[len++] = idx + 2; order[len++] = idx + 3;"
            + "  }"
            + "} else if (mode === 5) {"
            + "  for (idx = 0; idx + 2 < count; idx++) {"
            + "    if (idx & 1) { order[len++] = idx + 1; order[len++] = idx; }"
            + "    else { order[len++] = idx; order[len++] = idx + 1; }"
            + "    order[len++] = idx + 2;"
            + "  }"
            + "} else if (mode === 8) {"
            + "  for (idx = 0; idx + 3 < count; idx += 2) {"
            + "    order[len++] = idx; order[len++] = idx + 1; order[len++] = idx + 3;"
            + "    order[len++] = idx; order[len++] = idx + 3; order[len++] = idx + 2;"
            + "  }"
            + "} else {"
            + "  for (idx = 1; idx + 1 < count; idx++) { order[len++] = 0; order[len++] = idx; order[len++] = idx + 1; }"
            + "}"
            + "var out = S.out, pos = S.attribs[0], col = S.attribs[1], nor = S.attribs[2];"
            + "var tx0 = S.attribs[3], tx1 = S.attribs[4];"
            + "for (var num = 0; num < len; num++) {"
            + "  var vertex = order[num], dst = (start + num) * 14;"
            + "  var px = read(pos, vertex, 0), py = pos.size > 1 ? read(pos, vertex, 1) : 0;"
            + "  var pz = pos.size > 2 ? read(pos, vertex, 2) : 0;"
            + "  out[dst] = mv[0] * px + mv[4] * py + mv[8] * pz + mv[12];"
            + "  out[dst + 1] = mv[1] * px + mv[5] * py + mv[9] * pz + mv[13];"
            + "  out[dst + 2] = mv[2] * px + mv[6] * py + mv[10] * pz + mv[14];"
            + "  if (col.base) {"
            + "    out[dst + 3] = read(col, vertex, 0); out[dst + 4] = read(col, vertex, 1);"
            + "    out[dst + 5] = read(col, vertex, 2); out[dst + 6] = col.size > 3 ? read(col, vertex, 3) : 1;"
            + "  } else { out[dst + 3] = cr; out[dst + 4] = cg; out[dst + 5] = cb; out[dst + 6] = ca; }"
            + "  var qx = nx, qy = ny, qz = nz;"
            + "  if (nor.base) { qx = read(nor, vertex, 0); qy = read(nor, vertex, 1); qz = read(nor, vertex, 2); }"
            + "  out[dst + 7] = mv[0] * qx + mv[4] * qy + mv[8] * qz;"
            + "  out[dst + 8] = mv[1] * qx + mv[5] * qy + mv[9] * qz;"
            + "  out[dst + 9] = mv[2] * qx + mv[6] * qy + mv[10] * qz;"
            + "  if (tx0.base) {"
            + "    out[dst + 10] = read(tx0, vertex, 0); out[dst + 11] = tx0.size > 1 ? read(tx0, vertex, 1) : 0;"
            + "  } else { out[dst + 10] = s0; out[dst + 11] = t0; }"
            + "  if (tx1.base) {"
            + "    out[dst + 12] = read(tx1, vertex, 0); out[dst + 13] = tx1.size > 1 ? read(tx1, vertex, 1) : 0;"
            + "  } else { out[dst + 12] = s1; out[dst + 13] = t1; }"
            + "}"
            + "return start + len;")
    private static native int appendBatch(JSObject state, int start, int mode, int count, Float32Array mv,
            float cr, float cg, float cb, float ca, float nx, float ny, float nz, float s0, float t0, float s1,
            float t1);

    private static void issue(int mode, int count) {
        switch (mode) {
            case GL_QUADS:
                ensureQuadIndices(count);
                gl.drawElements(GL_TRIANGLES, count / 4 * 6, GL_UNSIGNED_INT, 0);
                break;
            case GL_QUAD_STRIP:
                gl.drawArrays(GL_TRIANGLE_STRIP, 0, count);
                break;
            case GL_POLYGON:
                gl.drawArrays(GL_TRIANGLE_FAN, 0, count);
                break;
            default:
                gl.drawArrays(mode, 0, count);
                break;
        }
    }

    /** Selects the shader variant and uploads whatever state changed since it was last used. */
    private static void prepare(int attribMask, boolean identityModelview) {
        int f = 0;
        Tex t0 = textureEnabled[0] ? tex(boundTexture[0]) : null;
        if (t0 != null && t0.width > 0) {
            f |= Shaders.F_TEX0;
            if (texGenEnabled[0] || texGenEnabled[1] || texGenEnabled[2] || texGenEnabled[3]) {
                f |= Shaders.F_TEXGEN;
            }
        }
        Tex t1 = textureEnabled[1] ? tex(boundTexture[1]) : null;
        if (t1 != null && t1.width > 0) {
            f |= Shaders.F_TEX1;
        }
        if (alphaTest) {
            f |= Shaders.F_ALPHA;
        }
        if (fog) {
            f |= Shaders.F_FOG;
        }
        if (lighting) {
            f |= Shaders.F_LIGHT;
        }
        Shaders.Program p = Shaders.get(gl, f);
        if (p != currentProgram) {
            gl.useProgram(p.program);
            currentProgram = p;
        }
        if (identityModelview) {
            if (p.mvVersion != -2) { // batched vertices are already in eye space
                gl.uniformMatrix4fv(p.uMv, false, IDENTITY);
                p.mvVersion = -2;
            }
        } else if (p.mvVersion != mvVersion) {
            gl.uniformMatrix4fv(p.uMv, false, modelview[mvTop]);
            p.mvVersion = mvVersion;
        }
        if (p.projVersion != projVersion) {
            gl.uniformMatrix4fv(p.uProj, false, projection[projTop]);
            p.projVersion = projVersion;
        }
        if (p.tex0Version != texMatVersion[0]) {
            gl.uniformMatrix4fv(p.uTex0Mat, false, textureMatrix[0][texTop[0]]);
            p.tex0Version = texMatVersion[0];
        }
        if ((f & Shaders.F_TEX1) != 0 && p.tex1Version != texMatVersion[1]) {
            gl.uniformMatrix4fv(p.uTex1Mat, false, textureMatrix[1][texTop[1]]);
            p.tex1Version = texMatVersion[1];
        }
        if ((f & Shaders.F_ALPHA) != 0 && p.alphaVersion != alphaVersion) {
            gl.uniform1f(p.uAlphaRef, alphaRef);
            gl.uniform1i(p.uAlphaFunc, alphaFunc);
            p.alphaVersion = alphaVersion;
        }
        if ((f & Shaders.F_FOG) != 0 && p.fogVersion != fogVersion) {
            gl.uniform4f(p.uFogColor, fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            float mode = fogMode == 0x2601 ? 0 : fogMode == 0x800 ? 1 : 2;
            gl.uniform4f(p.uFogParams, fogStart, fogEnd, fogDensity, mode);
            p.fogVersion = fogVersion;
        }
        if ((f & Shaders.F_LIGHT) != 0 && p.lightVersion != lightVersion) {
            float ar = lightModelAmbient[0];
            float ag = lightModelAmbient[1];
            float ab = lightModelAmbient[2];
            for (int i = 0; i < 2; i++) {
                if (lightEnabled[i]) {
                    ar += lightAmbient[i][0];
                    ag += lightAmbient[i][1];
                    ab += lightAmbient[i][2];
                }
            }
            gl.uniform4f(p.uAmbient, ar, ag, ab, 1);
            gl.uniform3f(p.uL0Dir, lightDir[0][0], lightDir[0][1], lightDir[0][2]);
            gl.uniform3f(p.uL1Dir, lightDir[1][0], lightDir[1][1], lightDir[1][2]);
            float[] d0 = lightEnabled[0] ? lightDiffuse[0] : new float[4];
            float[] d1 = lightEnabled[1] ? lightDiffuse[1] : new float[4];
            gl.uniform4f(p.uL0Diff, d0[0], d0[1], d0[2], d0[3]);
            gl.uniform4f(p.uL1Diff, d1[0], d1[1], d1[2], d1[3]);
            p.lightVersion = lightVersion;
        }
        if ((f & Shaders.F_TEXGEN) != 0 && p.texgenVersion != texGenVersion) {
            int[] modes = new int[4];
            for (int i = 0; i < 4; i++) {
                if (texGenEnabled[i]) {
                    modes[i] = texGenMode[i] == 0x2401 ? 1 : texGenMode[i] == 0x2400 ? 2 : 0;
                }
                float[] plane = modes[i] == 1 ? texGenObjectPlane[i] : texGenEyePlane[i];
                gl.uniform4f(p.uTgPlane[i], plane[0], plane[1], plane[2], plane[3]);
            }
            gl.uniform4i(p.uTgMode, modes[0], modes[1], modes[2], modes[3]);
            p.texgenVersion = texGenVersion;
        }
        // Constant attributes for disabled arrays
        if ((attribMask & (1 << Shaders.A_COLOR)) == 0 && !same4(sentColor, color)) {
            gl.vertexAttrib4f(Shaders.A_COLOR, color[0], color[1], color[2], color[3]);
            System.arraycopy(color, 0, sentColor, 0, 4);
        }
        if ((attribMask & (1 << Shaders.A_NORMAL)) == 0 && (f & Shaders.F_LIGHT) != 0
                && (sentNormal[0] != normal[0] || sentNormal[1] != normal[1] || sentNormal[2] != normal[2])) {
            gl.vertexAttrib4f(Shaders.A_NORMAL, normal[0], normal[1], normal[2], 0);
            System.arraycopy(normal, 0, sentNormal, 0, 3);
        }
        if ((attribMask & (1 << Shaders.A_TEX0)) == 0 && !same4(sentTex0, texCoord[0])) {
            float[] t = texCoord[0];
            gl.vertexAttrib4f(Shaders.A_TEX0, t[0], t[1], t[2], t[3]);
            System.arraycopy(t, 0, sentTex0, 0, 4);
        }
        if ((attribMask & (1 << Shaders.A_TEX1)) == 0 && (f & Shaders.F_TEX1) != 0
                && (sentTex1[0] != texCoord[1][0] || sentTex1[1] != texCoord[1][1])) {
            gl.vertexAttrib4f(Shaders.A_TEX1, texCoord[1][0], texCoord[1][1], 0, 1);
            sentTex1[0] = texCoord[1][0];
            sentTex1[1] = texCoord[1][1];
        }
    }

    private static boolean same4(float[] a, float[] b) {
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2] && a[3] == b[3];
    }

    public static void drawElements(int mode, int count, int type, Buffer indices) {
        if (count <= 0) {
            return;
        }
        int[] idx = new int[count];
        int p = indices.position();
        for (int i = 0; i < count; i++) {
            if (indices instanceof IntBuffer) {
                idx[i] = ((IntBuffer) indices).get(p + i);
            } else if (indices instanceof ShortBuffer) {
                idx[i] = ((ShortBuffer) indices).get(p + i) & 0xFFFF;
            } else {
                idx[i] = ((ByteBuffer) indices).get(p + i) & 0xFF;
            }
        }
        drawIndexed(mode, idx);
    }

    /** Expands indexed geometry into immediate-mode vertices (rare path, used by a few mods). */
    private static void drawIndexed(int mode, int[] idx) {
        begin(mode);
        float[] v = new float[4];
        for (int i : idx) {
            fetch(texCoordArray[0], i, v, 0, 0, 0, 1);
            if (texCoordArray[0].enabled) {
                texCoord[0][0] = v[0];
                texCoord[0][1] = v[1];
                immediateTex0 = true;
            }
            fetch(colorArray, i, v, 1, 1, 1, 1);
            if (colorArray.enabled) {
                System.arraycopy(v, 0, color, 0, 4);
                immediateColor = true;
            }
            fetch(normalArray, i, v, 0, 0, 1, 0);
            if (normalArray.enabled) {
                System.arraycopy(v, 0, normal, 0, 3);
                immediateNormal = true;
            }
            fetch(vertexArray, i, v, 0, 0, 0, 1);
            vertex(v[0], v[1], v[2]);
        }
        end();
    }

    private static void fetch(Arr a, int index, float[] out, float d0, float d1, float d2, float d3) {
        out[0] = d0;
        out[1] = d1;
        out[2] = d2;
        out[3] = d3;
        if (!a.enabled || a.buffer == null) {
            return;
        }
        int tsize = typeSize(a.type);
        int stride = a.stride != 0 ? a.stride : a.size * tsize;
        int esize = elementSize(a.buffer);
        int byteOffset = a.position * esize + index * stride;
        for (int c = 0; c < a.size && c < 4; c++) {
            int at = byteOffset + c * tsize;
            float value;
            if (a.buffer instanceof FloatBuffer && a.type == GL_FLOAT) {
                value = ((FloatBuffer) a.buffer).get(at / 4);
            } else if (a.buffer instanceof ByteBuffer) {
                ByteBuffer b = (ByteBuffer) a.buffer;
                switch (a.type) {
                    case GL_FLOAT:
                        value = b.getFloat(at);
                        break;
                    case GL_UNSIGNED_BYTE:
                        value = (b.get(at) & 0xFF) / (a.normalized ? 255f : 1f);
                        break;
                    case GL_BYTE:
                        value = b.get(at) / (a.normalized ? 127f : 1f);
                        break;
                    case GL_SHORT:
                        value = b.getShort(at);
                        break;
                    default:
                        value = b.getInt(at);
                        break;
                }
            } else if (a.buffer instanceof ShortBuffer) {
                value = ((ShortBuffer) a.buffer).get(at / 2);
            } else if (a.buffer instanceof IntBuffer) {
                value = ((IntBuffer) a.buffer).get(at / 4);
            } else {
                value = 0;
            }
            out[c] = value;
        }
    }

    // =====================================================================================================
    // Immediate mode
    // =====================================================================================================

    public static void begin(int mode) {
        beginMode = mode;
        immediateCount = 0;
        immediateColor = false;
        immediateNormal = false;
        immediateTex0 = false;
        immediateTex1 = false;
    }

    public static void vertex(float x, float y, float z) {
        if (beginMode < 0) {
            return;
        }
        int base = immediateCount * 14;
        if (base + 14 > immediate.length) {
            float[] grown = new float[immediate.length * 2];
            System.arraycopy(immediate, 0, grown, 0, immediate.length);
            immediate = grown;
        }
        float[] d = immediate;
        d[base] = x;
        d[base + 1] = y;
        d[base + 2] = z;
        d[base + 3] = color[0];
        d[base + 4] = color[1];
        d[base + 5] = color[2];
        d[base + 6] = color[3];
        d[base + 7] = normal[0];
        d[base + 8] = normal[1];
        d[base + 9] = normal[2];
        d[base + 10] = texCoord[0][0];
        d[base + 11] = texCoord[0][1];
        d[base + 12] = texCoord[1][0];
        d[base + 13] = texCoord[1][1];
        immediateCount++;
    }

    public static void end() {
        int mode = beginMode;
        beginMode = -1;
        if (immediateCount == 0) {
            return;
        }
        ArrayBuffer base = Float32Array.fromJavaArray(immediate).getBuffer();
        int byteOffset = Float32Array.fromJavaArray(immediate).getByteOffset();
        setSrc(Shaders.A_POS, true, base, byteOffset, 3);
        setSrc(Shaders.A_COLOR, immediateColor, base, byteOffset + 12, 4);
        setSrc(Shaders.A_NORMAL, immediateNormal, base, byteOffset + 28, 3);
        setSrc(Shaders.A_TEX0, immediateTex0, base, byteOffset + 40, 2);
        setSrc(Shaders.A_TEX1, immediateTex1, base, byteOffset + 48, 2);
        submit(mode, immediateCount);
    }

    private static void setSrc(int attrib, boolean used, ArrayBuffer base, int offset, int size) {
        srcUsed[attrib] = used;
        if (used) {
            Src s = srcs[attrib];
            s.base = base;
            s.offset = offset;
            s.size = size;
            s.type = GL_FLOAT;
            s.normalized = false;
            s.stride = 56;
        }
    }

    // =====================================================================================================
    // Display lists
    // =====================================================================================================

    public static int genLists(int range) {
        int base = displayLists.size();
        for (int i = 0; i < range; i++) {
            displayLists.add(null);
        }
        return base;
    }

    public static void newList(int list, int mode) {
        compiling = new DisplayList();
        compilingId = list;
        compileAndExecute = mode == 0x1301;
    }

    public static void endList() {
        if (compiling == null) {
            return;
        }
        DisplayList done = compiling;
        compiling = null;
        while (displayLists.size() <= compilingId) {
            displayLists.add(null);
        }
        DisplayList old = displayLists.get(compilingId);
        if (old != null) {
            old.free();
        }
        displayLists.set(compilingId, done);
    }

    public static void callList(int list) {
        if (record(() -> callListImpl(list))) {
            return;
        }
        callListImpl(list);
    }

    private static void callListImpl(int list) {
        if (list <= 0 || list >= displayLists.size() || callDepth > 64) {
            return;
        }
        DisplayList dl = displayLists.get(list);
        if (dl == null) {
            return;
        }
        callDepth++;
        try {
            List<Object> ops = dl.ops;
            for (int i = 0, n = ops.size(); i < n; i++) {
                Object op = ops.get(i);
                if (op instanceof DrawOp) {
                    drawOp((DrawOp) op);
                } else {
                    ((Runnable) op).run();
                }
            }
        } finally {
            callDepth--;
        }
    }

    public static void deleteLists(int list, int range) {
        for (int i = list; i < list + range && i < displayLists.size(); i++) {
            if (i > 0) {
                DisplayList dl = displayLists.get(i);
                if (dl != null) {
                    dl.free();
                    displayLists.set(i, null);
                }
            }
        }
    }

    public static boolean isList(int list) {
        return list > 0 && list < displayLists.size() && displayLists.get(list) != null;
    }

    // =====================================================================================================
    // glPushAttrib / glPopAttrib (all tracked state is saved regardless of the mask)
    // =====================================================================================================

    private static final class Attribs {
        boolean alphaTest;
        boolean fog;
        boolean lighting;
        boolean blend;
        boolean depthTest;
        boolean cullFace;
        boolean polygonOffsetFill;
        boolean scissorTest;
        boolean stencilTest;
        boolean[] textureEnabled;
        boolean[] lightEnabled;
        boolean[] texGenEnabled;
        int blendSrc;
        int blendDst;
        int depthFunc;
        boolean depthMask;
        boolean[] colorMask;
        int alphaFunc;
        float alphaRef;
        float[] color;
        int cullFaceMode;
        float lineWidth;
        int[] boundTexture;
        int activeUnit;
        int fogMode;
        float fogStart;
        float fogEnd;
        float fogDensity;
        float[] fogColor;
    }

    private static final List<Attribs> attribStack = new ArrayList<>();

    public static void pushAttrib(int mask) {
        if (record(() -> pushAttribImpl())) {
            return;
        }
        pushAttribImpl();
    }

    private static void pushAttribImpl() {
        Attribs a = new Attribs();
        a.alphaTest = alphaTest;
        a.fog = fog;
        a.lighting = lighting;
        a.blend = blend;
        a.depthTest = depthTest;
        a.cullFace = cullFace;
        a.polygonOffsetFill = polygonOffsetFill;
        a.scissorTest = scissorTest;
        a.stencilTest = stencilTest;
        a.textureEnabled = textureEnabled.clone();
        a.lightEnabled = lightEnabled.clone();
        a.texGenEnabled = texGenEnabled.clone();
        a.blendSrc = blendSrc;
        a.blendDst = blendDst;
        a.depthFunc = depthFunc;
        a.depthMask = depthMask;
        a.colorMask = colorMask.clone();
        a.alphaFunc = alphaFunc;
        a.alphaRef = alphaRef;
        a.color = color.clone();
        a.cullFaceMode = cullFaceMode;
        a.lineWidth = lineWidth;
        a.boundTexture = boundTexture.clone();
        a.activeUnit = activeUnit;
        a.fogMode = fogMode;
        a.fogStart = fogStart;
        a.fogEnd = fogEnd;
        a.fogDensity = fogDensity;
        a.fogColor = fogColor.clone();
        attribStack.add(a);
    }

    public static void popAttrib() {
        if (record(() -> popAttribImpl())) {
            return;
        }
        popAttribImpl();
    }

    private static void popAttribImpl() {
        if (attribStack.isEmpty()) {
            return;
        }
        flushBatch();
        Attribs a = attribStack.remove(attribStack.size() - 1);
        alphaTest = a.alphaTest;
        fog = a.fog;
        lighting = a.lighting;
        setCap(0xBE2, a.blend);
        setCap(0xB71, a.depthTest);
        setCap(0xB44, a.cullFace);
        setCap(0x8037, a.polygonOffsetFill);
        setCap(0xC11, a.scissorTest);
        setCap(0xB90, a.stencilTest);
        System.arraycopy(a.textureEnabled, 0, textureEnabled, 0, 4);
        System.arraycopy(a.lightEnabled, 0, lightEnabled, 0, 8);
        System.arraycopy(a.texGenEnabled, 0, texGenEnabled, 0, 4);
        lightVersion = ++counter;
        texGenVersion = ++counter;
        blendFuncImpl(a.blendSrc, a.blendDst);
        depthFuncImpl(a.depthFunc);
        depthMaskImpl(a.depthMask);
        colorMaskImpl(a.colorMask[0], a.colorMask[1], a.colorMask[2], a.colorMask[3]);
        alphaFuncImpl(a.alphaFunc, a.alphaRef);
        System.arraycopy(a.color, 0, color, 0, 4);
        cullFaceImpl(a.cullFaceMode);
        lineWidthImpl(a.lineWidth);
        for (int u = 3; u >= 0; u--) {
            activeTextureImpl(GL_TEXTURE0 + u);
            bindTextureImpl(a.boundTexture[u]);
        }
        activeTextureImpl(GL_TEXTURE0 + a.activeUnit);
        fogMode = a.fogMode;
        fogStart = a.fogStart;
        fogEnd = a.fogEnd;
        fogDensity = a.fogDensity;
        System.arraycopy(a.fogColor, 0, fogColor, 0, 4);
        fogVersion = ++counter;
    }

    // =====================================================================================================
    // Queries
    // =====================================================================================================

    public static float[] getFloats(int pname) {
        switch (pname) {
            case 0xBA6:
                return modelview[mvTop];
            case 0xBA7:
                return projection[projTop];
            case 0xBA8:
                return textureMatrix[activeUnit][texTop[activeUnit]];
            case 0xB00:
                return color;
            case 0xB66:
                return fogColor;
            case 0xB21:
                return new float[] { lineWidth };
            case 0xC22:
                return clearColor;
            case 0xBC2:
                return new float[] { alphaRef };
            default: {
                int[] ints = getIntegers(pname);
                float[] out = new float[ints.length];
                for (int i = 0; i < ints.length; i++) {
                    out[i] = ints[i];
                }
                return out;
            }
        }
    }

    public static int[] getIntegers(int pname) {
        switch (pname) {
            case 0xBA2:
                return viewport.clone();
            case 0xD33:
                // the driver's limit, capped: bigger atlases than this only cost memory here
                return new int[] { Math.min(8192, Math.max(2048, gl.getParameteri(0xD33))) };
            case 0x8069:
                return new int[] { boundTexture[activeUnit] };
            case 0xBA0:
                return new int[] { matrixMode };
            case 0x84E0:
                return new int[] { GL_TEXTURE0 + activeUnit };
            case 0x84E1:
                return new int[] { GL_TEXTURE0 + clientActiveUnit };
            case 0x84E2:
                return new int[] { 4 };
            case 0xBA3:
                return new int[] { mvTop + 1 };
            case 0xBA4:
                return new int[] { projTop + 1 };
            case 0xD36:
                return new int[] { modelview.length };
            case 0xD38:
                return new int[] { projection.length };
            case 0xBE1:
                return new int[] { blendSrc };
            case 0xBE0:
                return new int[] { blendDst };
            case 0xB74:
                return new int[] { depthFunc };
            case 0xBC1:
                return new int[] { alphaFunc };
            case 0x8CA6:
                return new int[] { boundFramebuffer };
            case 0x8894:
                return new int[] { arrayBufferBinding };
            case 0xB45:
                return new int[] { cullFaceMode };
            case 0xB65:
                return new int[] { fogMode };
            case 0xD32:
                return new int[] { 8 };
            case 0xD50:
            case 0xD52:
            case 0xD53:
            case 0xD54:
            case 0xD55:
                return new int[] { 8 };
            case 0xD56:
                return new int[] { 24 };
            case 0xD57:
                return new int[] { 8 };
            default:
                return new int[] { 0 };
        }
    }

    public static boolean getBoolean(int pname) {
        switch (pname) {
            case 0xB72:
                return depthMask;
            default:
                return isEnabled(pname);
        }
    }

    public static String getString(int name) {
        switch (name) {
            case 0x1F00:
                return "Minecraft Web";
            case 0x1F01:
                return "WebGL 2 (" + Display.rendererName() + ")";
            case 0x1F02:
                return "2.1.0 WebGL";
            case 0x1F03:
                return "GL_ARB_multitexture GL_EXT_framebuffer_object GL_ARB_texture_non_power_of_two";
            case 0x8B8C:
                return "1.20";
            default:
                return "";
        }
    }

    // =====================================================================================================
    // Framebuffer objects
    // =====================================================================================================

    public static int genFramebuffer() {
        framebuffers.add(gl.createFramebuffer());
        return framebuffers.size() - 1;
    }

    public static void deleteFramebuffer(int id) {
        flushBatch();
        if (id > 0 && id < framebuffers.size() && framebuffers.get(id) != null) {
            gl.deleteFramebuffer(framebuffers.get(id));
            framebuffers.set(id, null);
        }
    }

    public static void bindFramebuffer(int target, int id) {
        flushBatch();
        boundFramebuffer = id;
        gl.bindFramebuffer(0x8D40, id > 0 && id < framebuffers.size() ? framebuffers.get(id) : screenFramebuffer);
    }

    public static void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        flushBatch();
        gl.framebufferTexture2D(0x8D40, attachment, GL_TEXTURE_2D, texObject(texture), level);
    }

    public static int genRenderbuffer() {
        renderbuffers.add(gl.createRenderbuffer());
        return renderbuffers.size() - 1;
    }

    public static void deleteRenderbuffer(int id) {
        if (id > 0 && id < renderbuffers.size() && renderbuffers.get(id) != null) {
            gl.deleteRenderbuffer(renderbuffers.get(id));
            renderbuffers.set(id, null);
        }
    }

    public static void bindRenderbuffer(int target, int id) {
        boundRenderbuffer = id > 0 && id < renderbuffers.size() ? renderbuffers.get(id) : null;
        gl.bindRenderbuffer(0x8D41, boundRenderbuffer);
    }

    public static void renderbufferStorage(int target, int internalFormat, int width, int height) {
        flushBatch();
        int format = internalFormat;
        switch (internalFormat) {
            case 0x1902:
            case 0x81A6:
            case 0x81A7:
                format = 0x81A6;
                break;
            case 0x84F9:
            case 0x88F0:
                format = 0x88F0;
                break;
            case 0x1908:
            case 0x8058:
                format = 0x8058;
                break;
            default:
                break;
        }
        gl.renderbufferStorage(0x8D41, format, width, height);
    }

    public static void framebufferRenderbuffer(int target, int attachment, int rbTarget, int renderbuffer) {
        flushBatch();
        gl.framebufferRenderbuffer(0x8D40, attachment, 0x8D41,
                renderbuffer > 0 && renderbuffer < renderbuffers.size() ? renderbuffers.get(renderbuffer) : null);
    }

    public static int checkFramebufferStatus(int target) {
        return gl.checkFramebufferStatus(0x8D40);
    }

    /**
     * The game's "default framebuffer" (id 0) is an offscreen framebuffer, copied to the canvas once per frame by
     * {@link #present()}. Drawing straight to the canvas would let the browser show half-drawn frames, because a
     * game frame can span several browser tasks (green threads yield while it is being drawn).
     */
    static void resizeScreen(int width, int height) {
        flushBatch();
        if (screenFramebuffer == null) {
            screenFramebuffer = gl.createFramebuffer();
            screenColor = gl.createRenderbuffer();
            screenDepth = gl.createRenderbuffer();
        }
        screenWidth = width;
        screenHeight = height;
        gl.bindRenderbuffer(0x8D41, screenColor);
        gl.renderbufferStorage(0x8D41, 0x8051, width, height);
        gl.bindRenderbuffer(0x8D41, screenDepth);
        gl.renderbufferStorage(0x8D41, 0x88F0, width, height);
        gl.bindRenderbuffer(0x8D41, boundRenderbuffer);
        gl.bindFramebuffer(0x8D40, screenFramebuffer);
        gl.framebufferRenderbuffer(0x8D40, 0x8CE0, 0x8D41, screenColor);
        gl.framebufferRenderbuffer(0x8D40, 0x821A, 0x8D41, screenDepth);
        gl.bindFramebuffer(0x8D40, boundFramebuffer > 0 && boundFramebuffer < framebuffers.size()
                ? framebuffers.get(boundFramebuffer) : screenFramebuffer);
    }

    /** Copies the finished frame to the canvas; the browser presents it when the current task ends. */
    static void present() {
        flushBatch();
        if (screenFramebuffer == null) {
            return;
        }
        if (scissorTest) {
            gl.disable(0xC11);
        }
        gl.bindFramebuffer(0x8CA8, screenFramebuffer);
        gl.bindFramebuffer(0x8CA9, null);
        gl.blitFramebuffer(0, 0, screenWidth, screenHeight, 0, 0, screenWidth, screenHeight, 0x4000, 0x2600);
        gl.bindFramebuffer(0x8D40, boundFramebuffer > 0 && boundFramebuffer < framebuffers.size()
                ? framebuffers.get(boundFramebuffer) : screenFramebuffer);
        if (scissorTest) {
            gl.enable(0xC11);
        }
    }
}
