package org.teavm.classlib.java.awt.geom;

public class TAffineTransform {
    private double m00 = 1;
    private double m10;
    private double m01;
    private double m11 = 1;
    private double m02;
    private double m12;

    public TAffineTransform() {
    }

    public void translate(double tx, double ty) {
        m02 += tx * m00 + ty * m01;
        m12 += tx * m10 + ty * m11;
    }

    public void scale(double sx, double sy) {
        m00 *= sx;
        m10 *= sx;
        m01 *= sy;
        m11 *= sy;
    }

    public void rotate(double theta) {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        double n00 = m00 * c + m01 * s;
        double n01 = -m00 * s + m01 * c;
        double n10 = m10 * c + m11 * s;
        double n11 = -m10 * s + m11 * c;
        m00 = n00;
        m01 = n01;
        m10 = n10;
        m11 = n11;
    }

    public static TAffineTransform getScaleInstance(double sx, double sy) {
        TAffineTransform t = new TAffineTransform();
        t.scale(sx, sy);
        return t;
    }

    public static TAffineTransform getTranslateInstance(double tx, double ty) {
        TAffineTransform t = new TAffineTransform();
        t.translate(tx, ty);
        return t;
    }

    public double getScaleX() {
        return m00;
    }

    public double getScaleY() {
        return m11;
    }

    public double getTranslateX() {
        return m02;
    }

    public double getTranslateY() {
        return m12;
    }
}
