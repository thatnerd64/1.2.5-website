package org.lwjgl.opengl;

public final class DisplayMode {
    private final int width;
    private final int height;
    private final int bpp;
    private final int freq;
    private final boolean fullscreen;

    public DisplayMode(int width, int height) {
        this(width, height, 32, 60, false);
    }

    DisplayMode(int width, int height, int bpp, int freq, boolean fullscreen) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.freq = freq;
        this.fullscreen = fullscreen;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public int getFrequency() {
        return freq;
    }

    public boolean isFullscreenCapable() {
        return fullscreen;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DisplayMode && ((DisplayMode) o).width == width && ((DisplayMode) o).height == height
                && ((DisplayMode) o).bpp == bpp && ((DisplayMode) o).freq == freq;
    }

    @Override
    public int hashCode() {
        return width ^ height ^ freq ^ bpp;
    }

    @Override
    public String toString() {
        return width + " x " + height + " x " + bpp + " @" + freq + "Hz";
    }
}
