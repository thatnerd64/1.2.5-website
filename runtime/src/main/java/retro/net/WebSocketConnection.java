package retro.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int8Array;
import retro.JS;

/**
 * A TCP connection carried over a WebSocket, behind {@code java.net.Socket}.
 *
 * <p>The endpoint is either
 * <ul>
 *   <li>{@code host:port}, reached through the relay set in the launcher (tools/ws-proxy.js) as
 *       {@code <relay>?host=...&port=...}, or</li>
 *   <li>a {@code ws://} / {@code wss://} URL typed as the server address: a relay in front of one server, or
 *       any other WebSocket-to-TCP bridge (websockify, ...).</li>
 * </ul>
 * A relay that selects the {@value #RELAY_PROTOCOL} subprotocol (ws-proxy.js does) sends a "connected" text
 * message once its TCP connection is up and reports failures as close codes 4000-4005, so connection errors
 * become the exceptions a JVM would throw (see {@link #failure}). Other bridges count as connected once open.
 */
public final class WebSocketConnection {
    public static final String RELAY_PROTOCOL = "mc-relay.v1";
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    @JSFunctor
    interface ResultCallback extends JSObject {
        void done(int result);
    }

    @JSFunctor
    interface WaitCallback extends JSObject {
        void done(boolean ready);
    }

    private final JSObject state;
    private byte[] chunk;
    private int chunkPos;
    private int readTimeout;
    private boolean closedLocally;

    private WebSocketConnection(JSObject state) {
        this.state = state;
    }

    /** Connects to {@code host:port} (or to the URL in {@code host}); {@code timeoutMs} 0 means the default. */
    public static WebSocketConnection open(String host, int port, int timeoutMs) throws IOException {
        String url;
        String peer;
        if (ServerAddress.isUrl(host)) {
            url = host.trim();
            peer = url;
        } else {
            String relay = relayUrl();
            if (relay == null) {
                throw new ConnectException("No multiplayer relay set (see the launcher)");
            }
            url = relay + (relay.indexOf('?') >= 0 ? "&" : "?") + "host=" + encode(host) + "&port=" + port;
            peer = "the relay at " + relay;
        }
        JSObject state = createState();
        int result = connect(state, url, timeoutMs > 0 ? timeoutMs : DEFAULT_CONNECT_TIMEOUT);
        if (result != 0) {
            closeImpl(state);
            throw failure(result, state, host, peer);
        }
        return new WebSocketConnection(state);
    }

    /** The launcher's relay setting as a URL: {@code localhost:25566} becomes {@code ws://localhost:25566}. */
    static String relayUrl() {
        String relay = JS.config("proxy");
        if (relay == null || relay.trim().isEmpty()) {
            return null;
        }
        relay = relay.trim();
        if (!ServerAddress.isUrl(relay)) {
            String lower = relay.toLowerCase();
            boolean local = lower.startsWith("localhost") || lower.startsWith("127.") || lower.startsWith("[::1]");
            relay = (local ? "ws://" : "wss://") + relay;
        }
        return relay;
    }

    /**
     * The exception for a failed connection. {@code result}: -1 the WebSocket could not be created, -2 timed out,
     * otherwise the close code: 4001 unknown host, 4002 refused, 4003 timed out, 4004 unreachable, 4000 and 4005
     * (refused by the relay's settings) carry their message as the close reason.
     */
    private static IOException failure(int result, JSObject state, String host, String peer) {
        String reason = closeReason(state);
        switch (result) {
            case -1:
                return new ConnectException(reason);
            case -2:
            case 4003:
                return new SocketTimeoutException("connect timed out");
            case 4001:
                return new UnknownHostException(host);
            case 4002:
                return new ConnectException("Connection refused");
            case 4004:
                return new NoRouteToHostException("No route to host");
            default:
                if (!wasOpened(state)) {
                    return new ConnectException("Can't reach " + peer);
                }
                return new ConnectException(reason.isEmpty() ? "Connection closed by " + peer : reason);
        }
    }

    public void setReadTimeout(int millis) {
        readTimeout = Math.max(0, millis);
    }

    @JSBody(params = "s", script = "return encodeURIComponent(s);")
    private static native String encode(String s);

    @JSBody(script = "return { ws: null, queue: [], queued: 0, opened: false, connected: false, closed: false,"
            + " code: 0, reason: '', waiter: null };")
    private static native JSObject createState();

    @Async
    private static native Integer connect(JSObject state, String url, int timeoutMs);

    private static void connect(JSObject state, String url, int timeoutMs, AsyncCallback<Integer> callback) {
        connectImpl(state, url, timeoutMs, callback::complete);
    }

    @JSBody(params = { "S", "url", "timeout", "done" }, script = ""
            + "var finished = false;"
            + "var timer = 0;"
            + "function finish(result) { if (!finished) { finished = true; clearTimeout(timer); done(result); } }"
            + "var sock;"
            + "try {"
            + "  sock = new WebSocket(url, ['" + RELAY_PROTOCOL + "', 'binary']);"
            + "} catch (err) {"
            + "  S.reason = err && err.name === 'SecurityError' && location.protocol === 'https:'"
            + "    ? 'This page uses https: use a wss:// address' : String(err && err.message || err);"
            + "  finish(-1);"
            + "  return;"
            + "}"
            + "sock.binaryType = 'arraybuffer';"
            + "S.ws = sock;"
            + "timer = setTimeout(function() { finish(-2); try { sock.close(); } catch (err) {} }, timeout);"
            + "sock.onopen = function() {"
            + "  S.opened = true;"
            + "  if (sock.protocol !== '" + RELAY_PROTOCOL + "') { S.connected = true; finish(0); }"
            + "};"
            + "sock.onmessage = function(evt) {"
            + "  if (typeof evt.data === 'string') {"
            + "    if (!S.connected && evt.data === 'connected') { S.connected = true; finish(0); }"
            + "    return;"
            + "  }"
            + "  S.queue.push(new Int8Array(evt.data));"
            + "  S.queued += evt.data.byteLength;"
            + "  if (S.waiter) { var wake = S.waiter; S.waiter = null; wake(true); }"
            + "};"
            + "sock.onclose = function(evt) {"
            + "  S.closed = true;"
            + "  S.code = evt.code;"
            + "  S.reason = evt.reason || '';"
            + "  finish(S.connected ? 0 : (evt.code || 1006));"
            + "  if (S.waiter) { var wake = S.waiter; S.waiter = null; wake(true); }"
            + "};")
    private static native void connectImpl(JSObject state, String url, int timeout, ResultCallback done);

    @JSBody(params = "S", script = ""
            + "if (!S.queue.length) return null;"
            + "var item = S.queue.shift();"
            + "S.queued -= item.length;"
            + "return item;")
    private static native Int8Array poll(JSObject state);

    @JSBody(params = "S", script = "return S.queued;")
    private static native int queued(JSObject state);

    @JSBody(params = "S", script = "return S.closed;")
    private static native boolean isClosed(JSObject state);

    @JSBody(params = "S", script = "return S.opened;")
    private static native boolean wasOpened(JSObject state);

    @JSBody(params = "S", script = "return S.code;")
    private static native int closeCode(JSObject state);

    @JSBody(params = "S", script = "return S.reason || '';")
    private static native String closeReason(JSObject state);

    /** Waits until data arrives or the connection closes (true), or {@code timeout} ms pass (false; 0: none). */
    @Async
    private static native Boolean waitForData(JSObject state, int timeout);

    private static void waitForData(JSObject state, int timeout, AsyncCallback<Boolean> callback) {
        waitImpl(state, timeout, callback::complete);
    }

    @JSBody(params = { "S", "timeout", "done" }, script = ""
            + "if (S.queue.length || S.closed) { done(true); return; }"
            + "var timer = 0;"
            + "var waiter = function(ready) { if (timer) clearTimeout(timer); done(ready); };"
            + "S.waiter = waiter;"
            + "if (timeout > 0) timer = setTimeout(function() {"
            + "  if (S.waiter === waiter) { S.waiter = null; done(false); }"
            + "}, timeout);")
    private static native void waitImpl(JSObject state, int timeout, WaitCallback done);

    @JSBody(params = { "S", "data", "off", "len" }, script = ""
            + "if (S.ws && S.ws.readyState === 1) S.ws.send(data.slice(off, off + len));")
    private static native void send(JSObject state, Int8Array data, int off, int len);

    @JSBody(params = "S", script = ""
            + "if (S.ws) { try { S.ws.close(1000); } catch (err) {} }"
            + "if (S.waiter) { var wake = S.waiter; S.waiter = null; wake(true); }")
    private static native void closeImpl(JSObject state);

    private boolean fill() throws IOException {
        while (chunk == null || chunkPos >= chunk.length) {
            if (closedLocally) {
                throw new SocketException("Socket closed");
            }
            Int8Array next = poll(state);
            if (next != null) {
                chunk = next.copyToJavaArray();
                chunkPos = 0;
                continue;
            }
            if (isClosed(state)) {
                int code = closeCode(state);
                if (code == 1000 || code == 1001 || code == 1005) {
                    return false;
                }
                String reason = closeReason(state);
                throw new SocketException(reason.isEmpty() ? "Connection reset" : reason);
            }
            if (!waitForData(state, readTimeout)) {
                throw new SocketTimeoutException("Read timed out");
            }
        }
        return true;
    }

    public InputStream inputStream() {
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
                if (closedLocally) {
                    return 0;
                }
                return (chunk == null ? 0 : chunk.length - chunkPos) + queued(state);
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
                if (closedLocally) {
                    throw new SocketException("Socket closed");
                }
                if (isClosed(state)) {
                    throw new SocketException("Broken pipe");
                }
                if (len > 0) {
                    send(state, Int8Array.fromJavaArray(b), off, len);
                }
            }

            @Override
            public void close() {
                WebSocketConnection.this.close();
            }
        };
    }

    public void close() {
        if (!closedLocally) {
            closedLocally = true;
            closeImpl(state);
        }
    }
}
