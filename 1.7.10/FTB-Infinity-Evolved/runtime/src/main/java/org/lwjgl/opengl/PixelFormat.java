package org.lwjgl.opengl;

public final class PixelFormat {
    private int bpp;
    private int alpha;
    private int depth = 8;
    private int stencil;
    private int samples;

    public PixelFormat() {
        this(0, 8, 0);
    }

    public PixelFormat(int alpha, int depth, int stencil) {
        this(alpha, depth, stencil, 0);
    }

    public PixelFormat(int alpha, int depth, int stencil, int samples) {
        this(0, alpha, depth, stencil, samples);
    }

    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples) {
        this.bpp = bpp;
        this.alpha = alpha;
        this.depth = depth;
        this.stencil = stencil;
        this.samples = samples;
    }

    private PixelFormat copy() {
        return new PixelFormat(bpp, alpha, depth, stencil, samples);
    }

    public PixelFormat withBitsPerPixel(int v) {
        PixelFormat p = copy();
        p.bpp = v;
        return p;
    }

    public PixelFormat withAlphaBits(int v) {
        PixelFormat p = copy();
        p.alpha = v;
        return p;
    }

    public PixelFormat withDepthBits(int v) {
        PixelFormat p = copy();
        p.depth = v;
        return p;
    }

    public PixelFormat withStencilBits(int v) {
        PixelFormat p = copy();
        p.stencil = v;
        return p;
    }

    public PixelFormat withSamples(int v) {
        PixelFormat p = copy();
        p.samples = v;
        return p;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public int getAlphaBits() {
        return alpha;
    }

    public int getDepthBits() {
        return depth;
    }

    public int getStencilBits() {
        return stencil;
    }

    public int getSamples() {
        return samples;
    }
}
