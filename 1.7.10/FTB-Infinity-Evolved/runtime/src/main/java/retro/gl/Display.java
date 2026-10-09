package retro.gl;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import retro.JS;

/** The page's game canvas and its WebGL 2 context. */
public final class Display {
    private static JSObject canvas;
    private static WebGL gl;
    private static int width = 854;
    private static int height = 480;
    private static double scale = 1;
    private static String title = "Minecraft";
    private static boolean created;

    private Display() {
    }

    @JSFunctor
    interface FrameCallback extends JSObject {
        void run();
    }

    public static void create() {
        if (created) {
            return;
        }
        canvas = findCanvas();
        if (canvas == null) {
            throw new IllegalStateException("No <canvas id=\"game\"> in the page");
        }
        String s = JS.config("resolutionScale");
        if (s != null) {
            try {
                scale = Math.max(0.25, Math.min(4, Double.parseDouble(s)));
            } catch (NumberFormatException e) {
                // keep default
            }
        }
        gl = createContext(canvas);
        if (gl == null) {
            throw new IllegalStateException("WebGL 2 is not available in this browser");
        }
        GLEmu.init(gl);
        updateSize();
        created = true;
    }

    public static boolean isCreated() {
        return created;
    }

    @JSBody(script = "return document.getElementById('game');")
    private static native JSObject findCanvas();

    @JSBody(params = "canvas", script = ""
            + "var gl = canvas.getContext('webgl2', { alpha: false, depth: true, stencil: true, antialias: false,"
            + "  premultipliedAlpha: false, preserveDrawingBuffer: false, powerPreference: 'high-performance' });"
            + "if (gl) window.__retroGl = gl;"
            + "return gl;")
    private static native WebGL createContext(JSObject canvas);

    @JSBody(params = { "canvas", "scale" }, script = ""
            + "var w = Math.max(1, Math.round(canvas.clientWidth * scale));"
            + "var h = Math.max(1, Math.round(canvas.clientHeight * scale));"
            + "if (canvas.width !== w) canvas.width = w;"
            + "if (canvas.height !== h) canvas.height = h;"
            + "return w * 65536 + h;")
    private static native int syncSize(JSObject canvas, double scale);

    private static void updateSize() {
        int packed = syncSize(canvas, scale);
        int w = packed >>> 16;
        int h = packed & 0xFFFF;
        if (w != width || h != height || !created) {
            width = w;
            height = h;
            GLEmu.resizeScreen(w, h);
        }
    }

    public static int canvasWidth() {
        return width;
    }

    public static int canvasHeight() {
        return height;
    }

    @JSBody(script = "return screen.width;")
    public static native int screenWidth();

    @JSBody(script = "return screen.height;")
    public static native int screenHeight();

    /** Ends the frame: lets the browser present it and handle events, then continues on the next frame. */
    public static void update() {
        if (created) {
            GLEmu.present();
        }
        retro.input.Input.endFrame();
        nextFrame();
        if (created) {
            updateSize();
        }
    }

    @Async
    private static native void nextFrame();

    private static void nextFrame(AsyncCallback<Void> callback) {
        if (isHidden()) {
            // no animation frames in a hidden tab; keep ticking (multiplayer servers drop silent clients)
            retro.rt.Wakeup.schedule(50, () -> callback.complete(null));
        } else {
            requestFrame(() -> callback.complete(null));
        }
    }

    @JSBody(params = "cb", script = "requestAnimationFrame(function() { cb(); });")
    private static native void requestFrame(FrameCallback cb);

    public static boolean isActive() {
        return hasFocus();
    }

    @JSBody(script = "return document.hasFocus() && !document.hidden;")
    private static native boolean hasFocus();

    public static boolean isVisible() {
        return !isHidden();
    }

    @JSBody(script = "return !!document.hidden;")
    private static native boolean isHidden();

    public static void setTitle(String t) {
        title = t;
        setDocumentTitle(t);
    }

    public static void setTitleFromFrame(String t) {
        if (t != null && !t.isEmpty()) {
            setTitle(t);
        }
    }

    public static String getTitle() {
        return title;
    }

    @JSBody(params = "t", script = "document.title = t;")
    private static native void setDocumentTitle(String t);

    @JSBody(script = "return !!document.fullscreenElement;")
    public static native boolean isFullscreen();

    @JSBody(params = "on", script = ""
            + "try {"
            + "  if (on && !document.fullscreenElement) document.documentElement.requestFullscreen().catch(function(){});"
            + "  else if (!on && document.fullscreenElement) document.exitFullscreen().catch(function(){});"
            + "} catch (e) {}")
    public static native void setFullscreen(boolean on);

    public static String rendererName() {
        return rendererNameImpl(gl);
    }

    @JSBody(params = "gl", script = ""
            + "try { var ext = gl.getExtension('WEBGL_debug_renderer_info');"
            + "  return ext ? String(gl.getParameter(ext.UNMASKED_RENDERER_WEBGL)) : String(gl.getParameter(gl.RENDERER)); }"
            + "catch (e) { return 'unknown'; }")
    private static native String rendererNameImpl(WebGL gl);

    static JSObject canvas() {
        return canvas;
    }
}
