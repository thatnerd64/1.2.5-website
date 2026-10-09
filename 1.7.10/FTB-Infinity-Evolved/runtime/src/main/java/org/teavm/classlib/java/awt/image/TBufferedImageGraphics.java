package org.teavm.classlib.java.awt.image;

import java.util.HashMap;
import java.util.Map;
import org.teavm.classlib.java.awt.TColor;
import org.teavm.classlib.java.awt.TFont;
import org.teavm.classlib.java.awt.TGraphics;
import org.teavm.classlib.java.awt.TGraphics2D;
import org.teavm.classlib.java.awt.TImage;
import org.teavm.classlib.java.awt.TRenderingHints;

/** Software Graphics2D for BufferedImage: SrcOver compositing, nearest or bilinear scaling. */
final class TBufferedImageGraphics extends TGraphics2D {
    private final TBufferedImage target;
    private TColor color = TColor.WHITE;
    private TColor background = TColor.BLACK;
    private TFont font = new TFont("Dialog", TFont.PLAIN, 12);
    private final Map<Object, Object> hints = new HashMap<>();
    private int tx;
    private int ty;
    private int clipX;
    private int clipY;
    private int clipW;
    private int clipH;

    TBufferedImageGraphics(TBufferedImage target) {
        this.target = target;
        clipW = target.getWidth();
        clipH = target.getHeight();
    }

    @Override
    public TGraphics create() {
        TBufferedImageGraphics g = new TBufferedImageGraphics(target);
        g.color = color;
        g.font = font;
        g.tx = tx;
        g.ty = ty;
        g.hints.putAll(hints);
        return g;
    }

    @Override
    public TColor getColor() {
        return color;
    }

    @Override
    public void setColor(TColor c) {
        if (c != null) {
            color = c;
        }
    }

    @Override
    public void setBackground(TColor c) {
        background = c;
    }

    @Override
    public TFont getFont() {
        return font;
    }

    @Override
    public void setFont(TFont font) {
        this.font = font;
    }

    @Override
    public void translate(int x, int y) {
        tx += x;
        ty += y;
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        clipX = x + tx;
        clipY = y + ty;
        clipW = width;
        clipH = height;
    }

    @Override
    public void setRenderingHint(TRenderingHints.Key key, Object value) {
        hints.put(key, value);
    }

    @Override
    public Object getRenderingHint(TRenderingHints.Key key) {
        return hints.get(key);
    }

    private boolean inClip(int x, int y) {
        return x >= clipX && y >= clipY && x < clipX + clipW && y < clipY + clipH
                && x >= 0 && y >= 0 && x < target.getWidth() && y < target.getHeight();
    }

    private void blend(int x, int y, int src) {
        int[] px = target.pixels;
        int i = y * target.getWidth() + x;
        int sa = src >>> 24;
        if (sa == 255) {
            px[i] = src;
            return;
        }
        if (sa == 0) {
            return;
        }
        int dst = px[i];
        int da = dst >>> 24;
        int outA = sa + da * (255 - sa) / 255;
        if (outA == 0) {
            px[i] = 0;
            return;
        }
        int r = (((src >> 16) & 0xFF) * sa + ((dst >> 16) & 0xFF) * da * (255 - sa) / 255) / outA;
        int g = (((src >> 8) & 0xFF) * sa + ((dst >> 8) & 0xFF) * da * (255 - sa) / 255) / outA;
        int b = ((src & 0xFF) * sa + (dst & 0xFF) * da * (255 - sa) / 255) / outA;
        px[i] = (outA << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        int argb = color.getRGB();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int px = x + i + tx;
                int py = y + j + ty;
                if (inClip(px, py)) {
                    blend(px, py, argb);
                }
            }
        }
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        int argb = background.getRGB();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int px = x + i + tx;
                int py = y + j + ty;
                if (inClip(px, py)) {
                    target.pixels[py * target.getWidth() + px] = argb;
                }
            }
        }
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        int argb = color.getRGB();
        for (int s = 0; s <= steps; s++) {
            int x = steps == 0 ? x1 : x1 + (x2 - x1) * s / steps;
            int y = steps == 0 ? y1 : y1 + (y2 - y1) * s / steps;
            if (inClip(x + tx, y + ty)) {
                blend(x + tx, y + ty, argb);
            }
        }
    }

    @Override
    public void drawString(String str, int x, int y) {
        // Text rasterisation is only used for Minecraft's "missing texture" placeholder; skipped.
    }

    @Override
    public boolean drawImage(TImage img, int x, int y, TImageObserver observer) {
        if (img == null) {
            return true;
        }
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        return drawImage(img, x, y, x + w, y + h, 0, 0, w, h, observer);
    }

    @Override
    public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {
        if (img == null) {
            return true;
        }
        return drawImage(img, x, y, x + width, y + height, 0, 0, img.getWidth(null), img.getHeight(null),
                observer);
    }

    @Override
    public boolean drawImage(TImage img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            TImageObserver observer) {
        if (!(img instanceof TBufferedImage)) {
            return true;
        }
        TBufferedImage src = (TBufferedImage) img;
        int dw = dx2 - dx1;
        int dh = dy2 - dy1;
        int sw = sx2 - sx1;
        int sh = sy2 - sy1;
        if (dw == 0 || dh == 0 || sw == 0 || sh == 0) {
            return true;
        }
        boolean bilinear = hints.get(TRenderingHints.KEY_INTERPOLATION) == TRenderingHints.VALUE_INTERPOLATION_BILINEAR
                || hints.get(TRenderingHints.KEY_INTERPOLATION) == TRenderingHints.VALUE_INTERPOLATION_BICUBIC;
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int[] sp = src.argbPixels();
        boolean srcAlpha = src.hasAlpha();
        int adw = Math.abs(dw);
        int adh = Math.abs(dh);
        for (int j = 0; j < adh; j++) {
            int py = (dh > 0 ? dy1 + j : dy1 - 1 - j) + ty;
            for (int i = 0; i < adw; i++) {
                int px = (dw > 0 ? dx1 + i : dx1 - 1 - i) + tx;
                if (!inClip(px, py)) {
                    continue;
                }
                int argb;
                if (bilinear && (adw != Math.abs(sw) || adh != Math.abs(sh))) {
                    float fx = sx1 + (i + 0.5f) * sw / adw - 0.5f;
                    float fy = sy1 + (j + 0.5f) * sh / adh - 0.5f;
                    argb = sampleBilinear(sp, srcW, srcH, fx, fy);
                } else {
                    int sx = sx1 + (int) ((long) i * sw / adw);
                    int sy = sy1 + (int) ((long) j * sh / adh);
                    if (sw < 0) {
                        sx--;
                    }
                    if (sh < 0) {
                        sy--;
                    }
                    if (sx < 0 || sy < 0 || sx >= srcW || sy >= srcH) {
                        continue;
                    }
                    argb = sp[sy * srcW + sx];
                }
                if (!srcAlpha) {
                    argb |= 0xFF000000;
                }
                blend(px, py, argb);
            }
        }
        return true;
    }

    private static int sampleBilinear(int[] p, int w, int h, float fx, float fy) {
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        float ax = fx - x0;
        float ay = fy - y0;
        int c00 = get(p, w, h, x0, y0);
        int c10 = get(p, w, h, x0 + 1, y0);
        int c01 = get(p, w, h, x0, y0 + 1);
        int c11 = get(p, w, h, x0 + 1, y0 + 1);
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            float top = ((c00 >>> shift) & 0xFF) * (1 - ax) + ((c10 >>> shift) & 0xFF) * ax;
            float bottom = ((c01 >>> shift) & 0xFF) * (1 - ax) + ((c11 >>> shift) & 0xFF) * ax;
            int v = Math.round(top * (1 - ay) + bottom * ay);
            result |= (Math.max(0, Math.min(255, v))) << shift;
        }
        return result;
    }

    private static int get(int[] p, int w, int h, int x, int y) {
        x = Math.max(0, Math.min(w - 1, x));
        y = Math.max(0, Math.min(h - 1, y));
        return p[y * w + x];
    }

    @Override
    public void dispose() {
        // Byte-raster images: publish what was drawn to the raster's bytes.
        target.pushBytes();
    }
}
