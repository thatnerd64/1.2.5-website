package retro.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;

final class JSUtil {
    private JSUtil() {
    }

    @JSBody(params = { "a", "b" }, script = "return a === b;")
    static native boolean same(JSObject a, JSObject b);

    @JSBody(params = { "buffer", "offset", "length" }, script = "return new Uint8Array(buffer, offset, length);")
    static native ArrayBufferView bytes(ArrayBuffer buffer, int offset, int length);

    @JSBody(params = { "buffer", "offset", "length" }, script = "return buffer.slice(offset, offset + length);")
    static native ArrayBuffer slice(ArrayBuffer buffer, int offset, int length);

    @JSBody(params = { "gl", "pname" }, script = "var v = gl.getParameter(pname); return typeof v === 'number' ? v : 0;")
    static native int getInt(WebGL gl, int pname);
}
