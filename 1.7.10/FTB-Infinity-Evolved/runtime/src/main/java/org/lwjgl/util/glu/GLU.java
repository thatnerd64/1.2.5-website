package org.lwjgl.util.glu;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;

public class GLU {
    public static final int GLU_INVALID_ENUM = 100900;
    public static final int GLU_INVALID_VALUE = 100901;
    public static final int GLU_OUT_OF_MEMORY = 100902;

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        retro.gl.GLEmu.perspective(fovy, aspect, zNear, zFar);
    }

    public static void gluOrtho2D(float left, float right, float bottom, float top) {
        GL11.glOrtho(left, right, bottom, top, -1, 1);
    }

    public static void gluLookAt(float eyex, float eyey, float eyez, float centerx, float centery, float centerz,
            float upx, float upy, float upz) {
        float fx = centerx - eyex;
        float fy = centery - eyey;
        float fz = centerz - eyez;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl;
        fy /= fl;
        fz /= fl;
        float sx = fy * upz - fz * upy;
        float sy = fz * upx - fx * upz;
        float sz = fx * upy - fy * upx;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= sl;
        sy /= sl;
        sz /= sl;
        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;
        float[] m = { sx, ux, -fx, 0, sy, uy, -fy, 0, sz, uz, -fz, 0, 0, 0, 0, 1 };
        retro.gl.GLEmu.multMatrix(m);
        GL11.glTranslatef(-eyex, -eyey, -eyez);
    }

    private static float[] read16(FloatBuffer b) {
        float[] m = new float[16];
        int p = b.position();
        for (int i = 0; i < 16; i++) {
            m[i] = b.get(p + i);
        }
        return m;
    }

    private static void mulVec(float[] m, float[] in, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] = in[0] * m[i] + in[1] * m[4 + i] + in[2] * m[8 + i] + in[3] * m[12 + i];
        }
    }

    private static void mul(float[] a, float[] b, float[] r) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r[j * 4 + i] = a[i] * b[j * 4] + a[4 + i] * b[j * 4 + 1] + a[8 + i] * b[j * 4 + 2]
                        + a[12 + i] * b[j * 4 + 3];
            }
        }
    }

    private static boolean invert(float[] src, float[] inverse) {
        float[] temp = new float[16];
        System.arraycopy(src, 0, temp, 0, 16);
        for (int i = 0; i < 16; i++) {
            inverse[i] = (i % 5 == 0) ? 1 : 0;
        }
        for (int i = 0; i < 4; i++) {
            int swap = i;
            for (int j = i + 1; j < 4; j++) {
                if (Math.abs(temp[j * 4 + i]) > Math.abs(temp[i * 4 + i])) {
                    swap = j;
                }
            }
            if (swap != i) {
                for (int k = 0; k < 4; k++) {
                    float t = temp[i * 4 + k];
                    temp[i * 4 + k] = temp[swap * 4 + k];
                    temp[swap * 4 + k] = t;
                    t = inverse[i * 4 + k];
                    inverse[i * 4 + k] = inverse[swap * 4 + k];
                    inverse[swap * 4 + k] = t;
                }
            }
            if (temp[i * 4 + i] == 0) {
                return false;
            }
            float t = temp[i * 4 + i];
            for (int k = 0; k < 4; k++) {
                temp[i * 4 + k] /= t;
                inverse[i * 4 + k] /= t;
            }
            for (int j = 0; j < 4; j++) {
                if (j != i) {
                    t = temp[j * 4 + i];
                    for (int k = 0; k < 4; k++) {
                        temp[j * 4 + k] -= temp[i * 4 + k] * t;
                        inverse[j * 4 + k] -= inverse[i * 4 + k] * t;
                    }
                }
            }
        }
        return true;
    }

    public static boolean gluUnProject(float winx, float winy, float winz, FloatBuffer modelMatrix,
            FloatBuffer projMatrix, IntBuffer viewport, FloatBuffer objPos) {
        float[] final4 = new float[16];
        mul(read16(projMatrix), read16(modelMatrix), final4);
        float[] inv = new float[16];
        if (!invert(final4, inv)) {
            return false;
        }
        int vp = viewport.position();
        float[] in = { winx, winy, winz, 1 };
        in[0] = (in[0] - viewport.get(vp)) / viewport.get(vp + 2);
        in[1] = (in[1] - viewport.get(vp + 1)) / viewport.get(vp + 3);
        in[0] = in[0] * 2 - 1;
        in[1] = in[1] * 2 - 1;
        in[2] = in[2] * 2 - 1;
        float[] out = new float[4];
        mulVec(inv, in, out);
        if (out[3] == 0) {
            return false;
        }
        int p = objPos.position();
        objPos.put(p, out[0] / out[3]);
        objPos.put(p + 1, out[1] / out[3]);
        objPos.put(p + 2, out[2] / out[3]);
        return true;
    }

    public static boolean gluProject(float objx, float objy, float objz, FloatBuffer modelMatrix,
            FloatBuffer projMatrix, IntBuffer viewport, FloatBuffer winPos) {
        float[] in = { objx, objy, objz, 1 };
        float[] out = new float[4];
        mulVec(read16(modelMatrix), in, out);
        mulVec(read16(projMatrix), out, in);
        if (in[3] == 0) {
            return false;
        }
        in[0] /= in[3];
        in[1] /= in[3];
        in[2] /= in[3];
        int vp = viewport.position();
        int p = winPos.position();
        winPos.put(p, viewport.get(vp) + (1 + in[0]) * viewport.get(vp + 2) / 2);
        winPos.put(p + 1, viewport.get(vp + 1) + (1 + in[1]) * viewport.get(vp + 3) / 2);
        winPos.put(p + 2, (1 + in[2]) / 2);
        return true;
    }

    public static String gluErrorString(int errorCode) {
        return errorCode == 0 ? "No error" : "GL error " + errorCode;
    }

    public static int gluBuild2DMipmaps(int target, int components, int width, int height, int format, int type,
            java.nio.ByteBuffer data) {
        GL11.glTexImage2D(target, 0, components, width, height, 0, format, type, data);
        retro.gl.GLEmu.generateMipmap();
        return 0;
    }
}
