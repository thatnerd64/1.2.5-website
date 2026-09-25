package retro.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int8Array;
import retro.JS;

/**
 * A TCP connection tunnelled through a WebSocket proxy (see tools/ws-proxy.js). The proxy URL comes from the
 * launcher ({@code retroConfig.proxy}); the target is passed as {@code ?host=...&port=...}.
 */
public final class WebSocketConnection {
    @JSFunctor
    interface Done extends JSObject {
        void done(String error);
    }

    private final JSObject state;
    private byte[] chunk;
    private int chunkPos;

    private WebSocketConnection(JSObject state) {
        this.state = state;
    }

    public static WebSocketConnection open(String host, int port, int timeoutMs) throws IOException {
        String proxy = JS.config("proxy");
        if (proxy == null || proxy.isBlank()) {
            throw new IOException("Multiplayer needs a WebSocket proxy (set one in the launcher)");
        }
        JSObject state = createState();
        String url = proxy + (proxy.contains("?") ? "&" : "?") + "host=" + encode(host) + "&port=" + port;
        String error = connect(state, url, timeoutMs > 0 ? timeoutMs : 15000);
        if (error != null) {
            throw new IOException(error);
        }
        return new WebSocketConnection(state);
    }

    @JSBody(params = "s", script = "return encodeURIComponent(s);")
    private static native String encode(String s);

    @JSBody(script = "return { ws: null, queue: [], closed: false, waiter: null, error: null };")
    private static native JSObject createState();

    @Async
    private static native String connect(JSObject state, String url, int timeoutMs);

    private static void connect(JSObject state, String url, int timeoutMs, AsyncCallback<String> callback) {
        connectImpl(state, url, timeoutMs, callback::complete);
    }

    @JSBody(params = { "S", "url", "timeout", "done" }, script = ""
            + "var finished = false;"
            + "function finish(err) { if (!finished) { finished = true; done(err); } }"
            + "var ws;"
            + "try { ws = new WebSocket(url); } catch (e) { finish(String(e)); return; }"
            + "ws.binaryType = 'arraybuffer';"
            + "S.ws = ws;"
            + "var timer = setTimeout(function() { finish('Connection timed out'); try { ws.close(); } catch (e) {} },"
            + "  timeout);"
            + "ws.onopen = function() { clearTimeout(timer); finish(null); };"
            + "ws.onmessage = function(e) {"
            + "  S.queue.push(new Int8Array(e.data));"
            + "  if (S.waiter) { var w = S.waiter; S.waiter = null; w(); }"
            + "};"
            + "ws.onerror = function() { S.error = 'Connection error'; };"
            + "ws.onclose = function(e) {"
            + "  clearTimeout(timer); S.closed = true;"
            + "  finish(e.reason || 'Connection refused');"
            + "  if (S.waiter) { var w = S.waiter; S.waiter = null; w(); }"
            + "};")
    private static native void connectImpl(JSObject state, String url, int timeout, Done done);

    @JSBody(params = "S", script = "return S.queue.length ? S.queue.shift() : null;")
    private static native Int8Array poll(JSObject state);

    @JSBody(params = "S", script = "return S.closed;")
    private static native boolean isClosed(JSObject state);

    @Async
    private static native Void waitForData(JSObject state);

    private static void waitForData(JSObject state, AsyncCallback<Void> callback) {
        waitImpl(state, () -> callback.complete(null));
    }

    @JSFunctor
    interface Waiter extends JSObject {
        void run();
    }

    @JSBody(params = { "S", "w" }, script = "if (S.queue.length || S.closed) w(); else S.waiter = w;")
    private static native void waitImpl(JSObject state, Waiter w);

    @JSBody(params = { "S", "data" }, script = ""
            + "if (S.ws && S.ws.readyState === 1) S.ws.send(data.slice().buffer);")
    private static native void send(JSObject state, Int8Array data);

    @JSBody(params = "S", script = "S.closed = true; if (S.ws) try { S.ws.close(); } catch (e) {}")
    private static native void closeImpl(JSObject state);

    private boolean fill() {
        while (chunk == null || chunkPos >= chunk.length) {
            Int8Array next = poll(state);
            if (next != null) {
                chunk = next.copyToJavaArray();
                chunkPos = 0;
                continue;
            }
            if (isClosed(state)) {
                return false;
            }
            waitForData(state);
        }
        return true;
    }

    public InputStream inputStream(Object owner) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                if (!fill()) {
                    return -1;
                }
                return chunk[chunkPos++] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                if (!fill()) {
                    return -1;
                }
                int n = Math.min(len, chunk.length - chunkPos);
                System.arraycopy(chunk, chunkPos, b, off, n);
                chunkPos += n;
                return n;
            }

            @Override
            public int available() {
                return chunk == null ? 0 : chunk.length - chunkPos;
            }

            @Override
            public void close() {
                WebSocketConnection.this.close();
            }
        };
    }

    public OutputStream outputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                write(new byte[] { (byte) b }, 0, 1);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (isClosed(state)) {
                    throw new IOException("Socket closed");
                }
                if (len > 0) {
                    send(state, new Int8Array(Int8Array.fromJavaArray(b).getBuffer(),
                            Int8Array.fromJavaArray(b).getByteOffset() + off, len));
                }
            }

            @Override
            public void close() {
                WebSocketConnection.this.close();
            }
        };
    }

    public void close() {
        closeImpl(state);
    }
}
