package org.teavm.classlib.java.awt.geom;

/** java.awt.geom.Point2D with its Double and Float implementations. */
public abstract class TPoint2D implements Cloneable {
    protected TPoint2D() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract void setLocation(double x, double y);

    public void setLocation(TPoint2D p) {
        setLocation(p.getX(), p.getY());
    }

    public static double distanceSq(double x1, double y1, double x2, double y2) {
        x1 -= x2;
        y1 -= y2;
        return x1 * x1 + y1 * y1;
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distanceSq(x1, y1, x2, y2));
    }

    public double distanceSq(double px, double py) {
        return distanceSq(getX(), getY(), px, py);
    }

    public double distanceSq(TPoint2D pt) {
        return distanceSq(getX(), getY(), pt.getX(), pt.getY());
    }

    public double distance(double px, double py) {
        return Math.sqrt(distanceSq(px, py));
    }

    public double distance(TPoint2D pt) {
        return Math.sqrt(distanceSq(pt));
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits ^= java.lang.Double.doubleToLongBits(getY()) * 31;
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TPoint2D) {
            TPoint2D p = (TPoint2D) obj;
            return getX() == p.getX() && getY() == p.getY();
        }
        return false;
    }

    public static class Double extends TPoint2D {
        public double x;
        public double y;

        public Double() {
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point2D.Double[" + x + ", " + y + "]";
        }
    }

    public static class Float extends TPoint2D {
        public float x;
        public float y;

        public Float() {
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point2D.Float[" + x + ", " + y + "]";
        }
    }
}
