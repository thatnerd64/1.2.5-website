package org.teavm.classlib.java.awt.geom;

/** java.awt.geom.Ellipse2D with its Double form (Tinkers' Construct shapes its slime islands with one). */
public abstract class TEllipse2D {
    public abstract double getX();

    public abstract double getY();

    public abstract double getWidth();

    public abstract double getHeight();

    public boolean contains(double x, double y) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return false;
        }
        double nx = (x - getX()) / w - 0.5;
        double ny = (y - getY()) / h - 0.5;
        return nx * nx + ny * ny < 0.25;
    }

    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }

    public static class Double extends TEllipse2D {
        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        public void setFrame(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }
}
