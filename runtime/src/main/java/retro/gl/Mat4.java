package retro.gl;

/** Column-major 4x4 matrix helpers (OpenGL conventions). */
final class Mat4 {
    private static final float[] TMP = new float[16];
    private static final float[] TMP2 = new float[16];

    private Mat4() {
    }

    static void identity(float[] m) {
        for (int i = 0; i < 16; i++) {
            m[i] = (i % 5 == 0) ? 1 : 0;
        }
    }

    static boolean isIdentity(float[] m) {
        for (int i = 0; i < 16; i++) {
            if (m[i] != ((i % 5 == 0) ? 1 : 0)) {
                return false;
            }
        }
        return true;
    }

    /** m = m * b */
    static void mul(float[] m, float[] b) {
        float[] r = TMP2;
        for (int c = 0; c < 4; c++) {
            float b0 = b[c * 4];
            float b1 = b[c * 4 + 1];
            float b2 = b[c * 4 + 2];
            float b3 = b[c * 4 + 3];
            r[c * 4] = m[0] * b0 + m[4] * b1 + m[8] * b2 + m[12] * b3;
            r[c * 4 + 1] = m[1] * b0 + m[5] * b1 + m[9] * b2 + m[13] * b3;
            r[c * 4 + 2] = m[2] * b0 + m[6] * b1 + m[10] * b2 + m[14] * b3;
            r[c * 4 + 3] = m[3] * b0 + m[7] * b1 + m[11] * b2 + m[15] * b3;
        }
        System.arraycopy(r, 0, m, 0, 16);
    }

    static void translate(float[] m, float x, float y, float z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
        m[15] += m[3] * x + m[7] * y + m[11] * z;
    }

    static void scale(float[] m, float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
    }

    static void rotate(float[] m, float angleDeg, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) {
            return;
        }
        x /= len;
        y /= len;
        z /= len;
        double a = Math.toRadians(angleDeg);
        float c = (float) Math.cos(a);
        float s = (float) Math.sin(a);
        float t = 1 - c;
        float[] r = TMP;
        r[0] = x * x * t + c;
        r[1] = y * x * t + z * s;
        r[2] = x * z * t - y * s;
        r[3] = 0;
        r[4] = x * y * t - z * s;
        r[5] = y * y * t + c;
        r[6] = y * z * t + x * s;
        r[7] = 0;
        r[8] = x * z * t + y * s;
        r[9] = y * z * t - x * s;
        r[10] = z * z * t + c;
        r[11] = 0;
        r[12] = 0;
        r[13] = 0;
        r[14] = 0;
        r[15] = 1;
        mul(m, r);
    }

    static void ortho(float[] m, double l, double r, double b, double t, double n, double f) {
        float[] o = new float[16];
        o[0] = (float) (2 / (r - l));
        o[5] = (float) (2 / (t - b));
        o[10] = (float) (-2 / (f - n));
        o[12] = (float) (-(r + l) / (r - l));
        o[13] = (float) (-(t + b) / (t - b));
        o[14] = (float) (-(f + n) / (f - n));
        o[15] = 1;
        mul(m, o);
    }

    static void frustum(float[] m, double l, double r, double b, double t, double n, double f) {
        float[] o = new float[16];
        o[0] = (float) (2 * n / (r - l));
        o[5] = (float) (2 * n / (t - b));
        o[8] = (float) ((r + l) / (r - l));
        o[9] = (float) ((t + b) / (t - b));
        o[10] = (float) (-(f + n) / (f - n));
        o[11] = -1;
        o[14] = (float) (-2 * f * n / (f - n));
        mul(m, o);
    }

    static void perspective(float[] m, float fovy, float aspect, float zNear, float zFar) {
        double radians = Math.toRadians(fovy / 2);
        double deltaZ = zFar - zNear;
        double sine = Math.sin(radians);
        if (deltaZ == 0 || sine == 0 || aspect == 0) {
            return;
        }
        double cotangent = Math.cos(radians) / sine;
        float[] o = new float[16];
        o[0] = (float) (cotangent / aspect);
        o[5] = (float) cotangent;
        o[10] = (float) (-(zFar + zNear) / deltaZ);
        o[11] = -1;
        o[14] = (float) (-2 * zNear * zFar / deltaZ);
        mul(m, o);
    }

    /** out = inverse(m); returns false if singular. */
    static boolean invert(float[] m, float[] out) {
        float[] inv = new float[16];
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14]
                + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14]
                - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13]
                + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13]
                - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14]
                - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14]
                + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13]
                - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13]
                + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14]
                + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14]
                - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13]
                + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13]
                - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10]
                - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10]
                + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9]
                - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9]
                + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
        float det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (det == 0) {
            return false;
        }
        det = 1.0f / det;
        for (int i = 0; i < 16; i++) {
            out[i] = inv[i] * det;
        }
        return true;
    }

    /** out = m * v (column vector). */
    static void transform(float[] m, float x, float y, float z, float w, float[] out) {
        out[0] = m[0] * x + m[4] * y + m[8] * z + m[12] * w;
        out[1] = m[1] * x + m[5] * y + m[9] * z + m[13] * w;
        out[2] = m[2] * x + m[6] * y + m[10] * z + m[14] * w;
        out[3] = m[3] * x + m[7] * y + m[11] * z + m[15] * w;
    }
}
