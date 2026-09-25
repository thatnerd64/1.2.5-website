package retro.rt;

import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Int8Array;

final class Utf8 {
    private Utf8() {
    }

    @JSBody(params = { "bytes", "offset", "length" }, script = ""
            + "return (window.__retroTd || (window.__retroTd = new TextDecoder('utf-8'))).decode(new Uint8Array(bytes.buffer, bytes.byteOffset + offset, length));")
    static native String decode(Int8Array bytes, int offset, int length);
}
