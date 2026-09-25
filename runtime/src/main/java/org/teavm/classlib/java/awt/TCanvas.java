package org.teavm.classlib.java.awt;

/** The game canvas. Its size tracks the page's canvas element, which is what Minecraft polls for resizes. */
public class TCanvas extends TComponent {
    public TCanvas() {
    }

    @Override
    public int getWidth() {
        return retro.gl.Display.canvasWidth();
    }

    @Override
    public int getHeight() {
        return retro.gl.Display.canvasHeight();
    }

    @Override
    public void setSize(int width, int height) {
    }

    public void createBufferStrategy(int n) {
    }

    public org.teavm.classlib.java.awt.image.TBufferStrategy getBufferStrategy() {
        return null;
    }
}
