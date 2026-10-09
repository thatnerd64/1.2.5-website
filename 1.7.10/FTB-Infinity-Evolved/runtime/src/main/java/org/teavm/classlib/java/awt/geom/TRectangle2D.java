package org.teavm.classlib.java.awt.geom;

/** java.awt.geom.Rectangle2D with its Double form (JourneyMap's map and dialog layout). */
public abstract class TRectangle2D {
    public abstract double getX();

    public abstract double getY();

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract void setRect(double x, double y, double w, double h);

    public double getMinX() {
        return getX();
    }

    public double getMinY() {
        return getY();
    }

    public double getMaxX() {
        return getX() + getWidth();
    }

    public double getMaxY() {
        return getY() + getHeight();
    }

    public double getCenterX() {
        return getX() + getWidth() / 2;
    }

    public double getCenterY() {
        return getY() + getHeight() / 2;
    }

    public boolean isEmpty() {
        return getWidth() <= 0 || getHeight() <= 0;
    }

    public boolean contains(double x, double y) {
        double x0 = getX();
        double y0 = getY();
        return x >= x0 && y >= y0 && x < x0 + getWidth() && y < y0 + getHeight();
    }

    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return x + w > x0 && y + h > y0 && x < x0 + getWidth() && y < y0 + getHeight();
    }

    public static class Double extends TRectangle2D {
        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            setRect(x, y, w, h);
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

        @Override
        public void setRect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }
}
