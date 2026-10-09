package retro.gl;

import org.teavm.jso.JSObject;
import retro.JS;

/**
 * GLSL programs emulating the OpenGL 1.x fixed-function pipeline. One program is compiled per combination of
 * enabled features; everything else (colors, normals, texture coordinates when no array is enabled) comes
 * from constant vertex attributes, so the combinations stay few.
 */
final class Shaders {
    static final int F_TEX0 = 1;
    static final int F_TEX1 = 2;
    static final int F_ALPHA = 4;
    static final int F_FOG = 8;
    static final int F_LIGHT = 16;
    static final int F_TEXGEN = 32;
    static final int COUNT = 64;

    static final int A_POS = 0;
    static final int A_COLOR = 1;
    static final int A_NORMAL = 2;
    static final int A_TEX0 = 3;
    static final int A_TEX1 = 4;

    /** A compiled variant with its uniform locations and the state versions last uploaded to it. */
    static final class Program {
        JSObject program;
        JSObject uMv;
        JSObject uProj;
        JSObject uTex0Mat;
        JSObject uTex1Mat;
        JSObject uAlphaRef;
        JSObject uAlphaFunc;
        JSObject uFogColor;
        JSObject uFogParams;
        JSObject uL0Dir;
        JSObject uL1Dir;
        JSObject uL0Diff;
        JSObject uL1Diff;
        JSObject uAmbient;
        JSObject uTgMode;
        JSObject[] uTgPlane = new JSObject[4];
        int mvVersion = -1;
        int projVersion = -1;
        int tex0Version = -1;
        int tex1Version = -1;
        int alphaVersion = -1;
        int fogVersion = -1;
        int lightVersion = -1;
        int texgenVersion = -1;
    }

    private static final Program[] programs = new Program[COUNT];

    private Shaders() {
    }

    static Program get(WebGL gl, int features) {
        Program p = programs[features];
        if (p == null) {
            p = compile(gl, features);
            programs[features] = p;
        }
        return p;
    }

    private static String defines(int f) {
        StringBuilder sb = new StringBuilder("#version 300 es\n");
        if ((f & F_TEX0) != 0) {
            sb.append("#define TEX0\n");
        }
        if ((f & F_TEX1) != 0) {
            sb.append("#define TEX1\n");
        }
        if ((f & F_ALPHA) != 0) {
            sb.append("#define ALPHA_TEST\n");
        }
        if ((f & F_FOG) != 0) {
            sb.append("#define FOG\n");
        }
        if ((f & F_LIGHT) != 0) {
            sb.append("#define LIGHTING\n");
        }
        if ((f & F_TEXGEN) != 0) {
            sb.append("#define TEXGEN\n");
        }
        return sb.toString();
    }

    private static final String VERTEX = ""
            + "precision highp float;\n"
            + "layout(location = 0) in vec4 a_pos;\n"
            + "layout(location = 1) in vec4 a_color;\n"
            + "layout(location = 2) in vec3 a_normal;\n"
            + "layout(location = 3) in vec4 a_tex0;\n"
            + "layout(location = 4) in vec2 a_tex1;\n"
            + "uniform mat4 u_mv;\n"
            + "uniform mat4 u_proj;\n"
            + "uniform mat4 u_tex0Mat;\n"
            + "uniform mat4 u_tex1Mat;\n"
            + "out vec4 v_color;\n"
            + "out vec4 v_tex0;\n"
            + "out vec2 v_tex1;\n"
            + "out float v_fogDist;\n"
            + "#ifdef LIGHTING\n"
            + "uniform vec3 u_l0Dir;\n"
            + "uniform vec3 u_l1Dir;\n"
            + "uniform vec4 u_l0Diff;\n"
            + "uniform vec4 u_l1Diff;\n"
            + "uniform vec4 u_ambient;\n"
            + "#endif\n"
            + "#ifdef TEXGEN\n"
            + "uniform ivec4 u_tgMode;\n"
            + "uniform vec4 u_tgPlane0;\n"
            + "uniform vec4 u_tgPlane1;\n"
            + "uniform vec4 u_tgPlane2;\n"
            + "uniform vec4 u_tgPlane3;\n"
            + "float texgen(int mode, vec4 plane, float original, vec4 obj, vec4 eye) {\n"
            + "  if (mode == 1) return dot(obj, plane);\n"
            + "  if (mode == 2) return dot(eye, plane);\n"
            + "  return original;\n"
            + "}\n"
            + "#endif\n"
            + "void main() {\n"
            + "  vec4 eye = u_mv * a_pos;\n"
            + "  gl_Position = u_proj * eye;\n"
            + "  vec4 c = a_color;\n"
            + "#ifdef LIGHTING\n"
            + "  vec3 n = normalize(mat3(u_mv) * a_normal);\n"
            + "  vec3 lit = u_ambient.rgb + max(dot(n, u_l0Dir), 0.0) * u_l0Diff.rgb"
            + " + max(dot(n, u_l1Dir), 0.0) * u_l1Diff.rgb;\n"
            + "  c = vec4(clamp(lit, 0.0, 1.0) * c.rgb, c.a);\n"
            + "#endif\n"
            + "  v_color = c;\n"
            + "  vec4 t0 = a_tex0;\n"
            + "#ifdef TEXGEN\n"
            + "  t0 = vec4(texgen(u_tgMode.x, u_tgPlane0, t0.x, a_pos, eye),\n"
            + "            texgen(u_tgMode.y, u_tgPlane1, t0.y, a_pos, eye),\n"
            + "            texgen(u_tgMode.z, u_tgPlane2, t0.z, a_pos, eye),\n"
            + "            texgen(u_tgMode.w, u_tgPlane3, t0.w, a_pos, eye));\n"
            + "#endif\n"
            + "  v_tex0 = u_tex0Mat * t0;\n"
            + "  v_tex1 = (u_tex1Mat * vec4(a_tex1, 0.0, 1.0)).xy;\n"
            + "  v_fogDist = abs(eye.z / eye.w);\n"
            + "}\n";

    private static final String FRAGMENT = ""
            + "precision highp float;\n"
            + "in vec4 v_color;\n"
            + "in vec4 v_tex0;\n"
            + "in vec2 v_tex1;\n"
            + "in float v_fogDist;\n"
            + "uniform sampler2D u_tex0;\n"
            + "uniform sampler2D u_tex1;\n"
            + "uniform float u_alphaRef;\n"
            + "uniform int u_alphaFunc;\n"
            + "uniform vec4 u_fogColor;\n"
            + "uniform vec4 u_fogParams;\n"
            + "out vec4 fragColor;\n"
            + "void main() {\n"
            + "  vec4 c = v_color;\n"
            + "#ifdef TEX0\n"
            + "#ifdef TEXGEN\n"
            + "  c *= texture(u_tex0, v_tex0.xy / v_tex0.w);\n"
            + "#else\n"
            + "  c *= texture(u_tex0, v_tex0.xy);\n"
            + "#endif\n"
            + "#endif\n"
            + "#ifdef TEX1\n"
            + "  c *= texture(u_tex1, v_tex1);\n"
            + "#endif\n"
            + "#ifdef ALPHA_TEST\n"
            + "  bool pass;\n"
            + "  if (u_alphaFunc == 516) pass = c.a > u_alphaRef;\n"
            + "  else if (u_alphaFunc == 518) pass = c.a >= u_alphaRef;\n"
            + "  else if (u_alphaFunc == 513) pass = c.a < u_alphaRef;\n"
            + "  else if (u_alphaFunc == 515) pass = c.a <= u_alphaRef;\n"
            + "  else if (u_alphaFunc == 514) pass = c.a == u_alphaRef;\n"
            + "  else if (u_alphaFunc == 517) pass = c.a != u_alphaRef;\n"
            + "  else if (u_alphaFunc == 512) pass = false;\n"
            + "  else pass = true;\n"
            + "  if (!pass) discard;\n"
            + "#endif\n"
            + "#ifdef FOG\n"
            + "  float f;\n"
            + "  if (u_fogParams.w == 0.0) f = (u_fogParams.y - v_fogDist) / (u_fogParams.y - u_fogParams.x);\n"
            + "  else if (u_fogParams.w == 1.0) f = exp(-u_fogParams.z * v_fogDist);\n"
            + "  else { float d = u_fogParams.z * v_fogDist; f = exp(-d * d); }\n"
            + "  c.rgb = mix(u_fogColor.rgb, c.rgb, clamp(f, 0.0, 1.0));\n"
            + "#endif\n"
            + "  fragColor = c;\n"
            + "}\n";

    private static Program compile(WebGL gl, int features) {
        String defs = defines(features);
        JSObject vs = shader(gl, 0x8B31, defs + VERTEX);
        JSObject fs = shader(gl, 0x8B30, defs + FRAGMENT);
        JSObject program = gl.createProgram();
        gl.attachShader(program, vs);
        gl.attachShader(program, fs);
        gl.linkProgram(program);
        if (!gl.getProgramParameter(program, 0x8B82)) {
            throw new IllegalStateException("Shader link failed: " + gl.getProgramInfoLog(program));
        }
        Program p = new Program();
        p.program = program;
        gl.useProgram(program);
        p.uMv = gl.getUniformLocation(program, "u_mv");
        p.uProj = gl.getUniformLocation(program, "u_proj");
        p.uTex0Mat = gl.getUniformLocation(program, "u_tex0Mat");
        p.uTex1Mat = gl.getUniformLocation(program, "u_tex1Mat");
        p.uAlphaRef = gl.getUniformLocation(program, "u_alphaRef");
        p.uAlphaFunc = gl.getUniformLocation(program, "u_alphaFunc");
        p.uFogColor = gl.getUniformLocation(program, "u_fogColor");
        p.uFogParams = gl.getUniformLocation(program, "u_fogParams");
        p.uL0Dir = gl.getUniformLocation(program, "u_l0Dir");
        p.uL1Dir = gl.getUniformLocation(program, "u_l1Dir");
        p.uL0Diff = gl.getUniformLocation(program, "u_l0Diff");
        p.uL1Diff = gl.getUniformLocation(program, "u_l1Diff");
        p.uAmbient = gl.getUniformLocation(program, "u_ambient");
        p.uTgMode = gl.getUniformLocation(program, "u_tgMode");
        for (int i = 0; i < 4; i++) {
            p.uTgPlane[i] = gl.getUniformLocation(program, "u_tgPlane" + i);
        }
        JSObject tex0 = gl.getUniformLocation(program, "u_tex0");
        if (tex0 != null) {
            gl.uniform1i(tex0, 0);
        }
        JSObject tex1 = gl.getUniformLocation(program, "u_tex1");
        if (tex1 != null) {
            gl.uniform1i(tex1, 1);
        }
        return p;
    }

    private static JSObject shader(WebGL gl, int type, String source) {
        JSObject s = gl.createShader(type);
        gl.shaderSource(s, source);
        gl.compileShader(s);
        if (!gl.getShaderParameter(s, 0x8B81)) {
            String log = gl.getShaderInfoLog(s);
            JS.error(source);
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        return s;
    }
}
