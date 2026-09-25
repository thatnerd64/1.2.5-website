package org.teavm.classlib.java.awt;

public class TRectangle {
    public int x;
    public int y;
    public int width;
    public int height;

    public TRectangle() {
    }

    public TRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public TRectangle(int width, int height) {
        this(0, 0, width, height);
    }

    public TRectangle(TDimension d) {
        this(0, 0, d.width, d.height);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }

    public boolean intersects(TRectangle r) {
        return r.x < x + width && r.x + r.width > x && r.y < y + height && r.y + r.height > y;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
