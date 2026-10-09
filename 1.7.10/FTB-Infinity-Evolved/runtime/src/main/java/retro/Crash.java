package retro;

import org.teavm.jso.JSBody;

/** Shows Minecraft's crash report (normally an AWT panel) in the page. */
public final class Crash {
    private Crash() {
    }

    public static void show(String report) {
        error(report);
        showImpl(report);
    }

    @JSBody(params = "msg", script = "console.error(msg);")
    private static native void error(String msg);

    @JSBody(params = "report", script = "if (window.retroLoader) window.retroLoader.crash(report);")
    private static native void showImpl(String report);
}
