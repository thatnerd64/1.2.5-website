package retro;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

/** Small helpers for talking to the page. */
public final class JS {
    private JS() {
    }

    @JSFunctor
    public interface BufferCallback extends JSObject {
        void accept(ArrayBuffer buffer);
    }

    @JSFunctor
    public interface ErrorCallback extends JSObject {
        void accept(String message);
    }

    @JSFunctor
    public interface ProgressCallback extends JSObject {
        void accept(double loaded, double total);
    }

    @JSBody(params = { "url", "onOk", "onError", "onProgress" }, script = ""
            + "var xhr = new XMLHttpRequest();"
            + "xhr.open('GET', url, true);"
            + "xhr.responseType = 'arraybuffer';"
            + "xhr.onload = function() {"
            + "  if (xhr.status >= 200 && xhr.status < 300 || xhr.status === 0 && xhr.response) onOk(xhr.response);"
            + "  else onError('HTTP ' + xhr.status + ' for ' + url);"
            + "};"
            + "xhr.onerror = function() { onError('Network error loading ' + url); };"
            + "if (onProgress) xhr.onprogress = function(e) { onProgress(e.loaded, e.lengthComputable ? e.total : 0); };"
            + "xhr.send();")
    private static native void fetchImpl(String url, BufferCallback onOk, ErrorCallback onError,
            ProgressCallback onProgress);

    /** Downloads a file, suspending the calling (green) thread. Returns null on failure. */
    @Async
    public static native ArrayBuffer fetch(String url, ProgressCallback progress);

    private static void fetch(String url, ProgressCallback progress, AsyncCallback<ArrayBuffer> callback) {
        fetchImpl(url, callback::complete, message -> {
            log(message);
            callback.complete(null);
        }, progress);
    }

    public static ArrayBuffer fetch(String url) {
        return fetch(url, null);
    }

    @JSBody(params = "msg", script = "console.log(msg);")
    public static native void log(String msg);

    @JSBody(params = "msg", script = "console.error(msg);")
    public static native void error(String msg);

    @JSBody(params = { "stage", "detail", "fraction" }, script = ""
            + "if (window.retroLoader) window.retroLoader.progress(stage, detail, fraction);")
    public static native void progress(String stage, String detail, double fraction);

    @JSBody(params = "message", script = "if (window.retroLoader) window.retroLoader.fail(message);"
            + "else console.error(message);")
    public static native void fatal(String message);

    @JSBody(script = "if (window.retroLoader) window.retroLoader.started();")
    public static native void started();

    @JSBody(params = "name", script = "var v = window.retroConfig && window.retroConfig[name];"
            + "return v === undefined || v === null ? null : String(v);")
    public static native String config(String name);

    @JSBody(script = "return performance.now();")
    public static native double now();

    public static byte[] toBytes(ArrayBuffer buffer, int offset, int length) {
        return new Int8Array(buffer, offset, length).copyToJavaArray();
    }

    public static byte[] toBytes(ArrayBuffer buffer) {
        return new Int8Array(buffer).copyToJavaArray();
    }

    /** Copies the first {@code length} bytes of a Java array into a new ArrayBuffer. */
    @JSBody(params = { "array", "length" }, script = "return array.slice(0, length).buffer;")
    public static native ArrayBuffer copyPrefix(@org.teavm.jso.JSByRef byte[] array, int length);

    @JSBody(params = "url", script = "window.open(url, '_blank');")
    public static native void openUrl(String url);

    @JSBody(params = { "name", "data", "mime" }, script = ""
            + "var blob = new Blob([data], { type: mime });"
            + "var a = document.createElement('a');"
            + "a.href = URL.createObjectURL(blob); a.download = name;"
            + "document.body.appendChild(a); a.click();"
            + "setTimeout(function() { URL.revokeObjectURL(a.href); a.remove(); }, 1000);")
    public static native void download(String name, Int8Array data, String mime);
}
