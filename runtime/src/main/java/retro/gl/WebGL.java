package retro.gl;

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** The subset of WebGL 2 used by the emulator (bound directly to the browser's context object). */
public interface WebGL extends JSObject {
    void activeTexture(int texture);

    void bindTexture(int target, JSObject texture);

    JSObject createTexture();

    void deleteTexture(JSObject texture);

    void texParameteri(int target, int pname, int param);

    void texImage2D(int target, int level, int internalformat, int width, int height, int border, int format,
            int type, ArrayBufferView pixels);

    void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type,
            ArrayBufferView pixels);

    void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);

    void generateMipmap(int target);

    void pixelStorei(int pname, int param);

    JSObject createBuffer();

    void deleteBuffer(JSObject buffer);

    void bindBuffer(int target, JSObject buffer);

    void bufferData(int target, int size, int usage);

    void bufferData(int target, ArrayBufferView data, int usage);

    void bufferData(int target, ArrayBufferView data, int usage, int srcOffset, int length);

    void bufferSubData(int target, int dstByteOffset, ArrayBufferView data, int srcOffset, int length);

    JSObject createVertexArray();

    void deleteVertexArray(JSObject vao);

    void bindVertexArray(JSObject vao);

    void enableVertexAttribArray(int index);

    void disableVertexAttribArray(int index);

    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset);

    void vertexAttrib4f(int index, float x, float y, float z, float w);

    void drawArrays(int mode, int first, int count);

    void drawElements(int mode, int count, int type, int offset);

    void enable(int cap);

    void disable(int cap);

    void blendFunc(int sfactor, int dfactor);

    void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

    void depthFunc(int func);

    void depthMask(boolean flag);

    void colorMask(boolean r, boolean g, boolean b, boolean a);

    void cullFace(int mode);

    void frontFace(int mode);

    void polygonOffset(float factor, float units);

    void lineWidth(float width);

    void viewport(int x, int y, int width, int height);

    void scissor(int x, int y, int width, int height);

    void clear(int mask);

    void clearColor(float r, float g, float b, float a);

    void clearDepth(float depth);

    void clearStencil(int s);

    void stencilFunc(int func, int ref, int mask);

    void stencilOp(int fail, int zfail, int zpass);

    void stencilMask(int mask);

    void readPixels(int x, int y, int width, int height, int format, int type, ArrayBufferView pixels);

    void flush();

    void finish();

    int getError();

    JSObject createShader(int type);

    void shaderSource(JSObject shader, String source);

    void compileShader(JSObject shader);

    boolean getShaderParameter(JSObject shader, int pname);

    String getShaderInfoLog(JSObject shader);

    JSObject createProgram();

    void attachShader(JSObject program, JSObject shader);

    void bindAttribLocation(JSObject program, int index, String name);

    void linkProgram(JSObject program);

    boolean getProgramParameter(JSObject program, int pname);

    String getProgramInfoLog(JSObject program);

    void useProgram(JSObject program);

    JSObject getUniformLocation(JSObject program, String name);

    void uniform1i(JSObject location, int x);

    void uniform1f(JSObject location, float x);

    void uniform3f(JSObject location, float x, float y, float z);

    void uniform4f(JSObject location, float x, float y, float z, float w);

    void uniform4i(JSObject location, int x, int y, int z, int w);

    void uniformMatrix4fv(JSObject location, boolean transpose, @JSByRef float[] value);

    JSObject createFramebuffer();

    void deleteFramebuffer(JSObject fb);

    void bindFramebuffer(int target, JSObject fb);

    void framebufferTexture2D(int target, int attachment, int textarget, JSObject texture, int level);

    void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, JSObject renderbuffer);

    int checkFramebufferStatus(int target);

    JSObject createRenderbuffer();

    void deleteRenderbuffer(JSObject rb);

    void bindRenderbuffer(int target, JSObject rb);

    void renderbufferStorage(int target, int internalformat, int width, int height);

    int getParameteri(int pname);
}
