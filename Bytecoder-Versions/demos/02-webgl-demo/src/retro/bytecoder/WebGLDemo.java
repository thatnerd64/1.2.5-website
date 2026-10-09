package retro.bytecoder;

import de.mirkosertic.bytecoder.api.web.Window;
import de.mirkosertic.bytecoder.api.web.HTMLDocument;
import de.mirkosertic.bytecoder.api.web.HTMLWebGLCanvasElement;
import de.mirkosertic.bytecoder.api.web.AnimationFrameCallback;
import de.mirkosertic.bytecoder.api.web.webgl.WebGLRenderingContext;

public class WebGLDemo implements AnimationFrameCallback {
    private final WebGLRenderingContext gl;
    private float hue = 0.0f;

    public WebGLDemo(WebGLRenderingContext gl) {
        this.gl = gl;
    }

    public static void main(String[] args) {
        System.out.println("Initializing Bytecoder WebGL Demo...");
        Window window = Window.window();
        HTMLDocument doc = window.document();
        HTMLWebGLCanvasElement canvas = doc.getElementById("webgl-canvas");
        if (canvas == null) {
            System.out.println("Error: Canvas element 'webgl-canvas' not found!");
            return;
        }

        WebGLRenderingContext gl = canvas.getContext("webgl");
        if (gl == null) {
            System.out.println("Warning: 'webgl' context null, trying 'experimental-webgl'...");
            gl = canvas.getContext("experimental-webgl");
        }
        if (gl == null) {
            System.out.println("Error: Failed to obtain WebGL context!");
            return;
        }

        System.out.println("WebGL context acquired! Viewport: " + canvas.width() + "x" + canvas.height());
        gl.viewport(0, 0, canvas.width(), canvas.height());
        gl.clearColor(0.2f, 0.4f, 0.8f, 1.0f);
        gl.clear(16384); // 0x4000 = COLOR_BUFFER_BIT

        WebGLDemo demo = new WebGLDemo(gl);
        window.requestAnimationFrame(demo);
        System.out.println("WebGL Animation Loop started via Bytecoder WasmGC!");
    }

    @Override
    public void run(int timestamp) {
        hue += 0.008f;
        if (hue > 1.0f) hue = 0.0f;

        float r = (float) (0.5 + 0.5 * Math.sin(hue * 6.2831853));
        float g = (float) (0.5 + 0.5 * Math.sin((hue + 0.333) * 6.2831853));
        float b = (float) (0.5 + 0.5 * Math.sin((hue + 0.666) * 6.2831853));

        gl.clearColor(r, g, b, 1.0f);
        gl.clear(16384); // COLOR_BUFFER_BIT

        Window.window().requestAnimationFrame(this);
    }
}
