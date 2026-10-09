package retro.input;

import org.teavm.jso.JSBody;

/**
 * Browser clipboard access. Reading the clipboard is asynchronous and permission-gated in browsers, so the last
 * text pasted into the page (Ctrl+V) is cached and returned to the game.
 */
public final class ClipboardBridge {
    private ClipboardBridge() {
    }

    @JSBody(script = "return window.__retroClipboard || '';")
    public static native String read();

    @JSBody(params = "text", script = ""
            + "window.__retroClipboard = text;"
            + "try { if (navigator.clipboard) navigator.clipboard.writeText(text).catch(function() {}); }"
            + "catch (e) {}")
    public static native void write(String text);

    @JSBody(script = ""
            + "window.addEventListener('paste', function(e) {"
            + "  var t = e.clipboardData && e.clipboardData.getData('text');"
            + "  if (t) window.__retroClipboard = t;"
            + "});")
    public static native void install();
}
