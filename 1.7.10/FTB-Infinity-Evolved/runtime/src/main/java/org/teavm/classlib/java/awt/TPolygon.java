package org.teavm.classlib.java.awt;

/** java.awt.Polygon: the point list and the even-odd containment test (HQM's quest map hit testing). */
public class TPolygon {
    public int npoints;
    public int[] xpoints;
    public int[] ypoints;

    public TPolygon() {
        xpoints = new int[4];
        ypoints = new int[4];
    }

    public TPolygon(int[] xs, int[] ys, int n) {
        npoints = n;
        xpoints = java.util.Arrays.copyOf(xs, n);
        ypoints = java.util.Arrays.copyOf(ys, n);
    }

    public void reset() {
        npoints = 0;
    }

    public void addPoint(int x, int y) {
        if (npoints == xpoints.length) {
            xpoints = java.util.Arrays.copyOf(xpoints, npoints * 2 + 1);
            ypoints = java.util.Arrays.copyOf(ypoints, npoints * 2 + 1);
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
    }

    public boolean contains(int x, int y) {
        return contains((double) x, (double) y);
    }

    public boolean contains(TPoint p) {
        return contains(p.x, p.y);
    }

    public boolean contains(double x, double y) {
        boolean inside = false;
        for (int i = 0, j = npoints - 1; i < npoints; j = i++) {
            if ((ypoints[i] > y) != (ypoints[j] > y)
                    && x < (double) (xpoints[j] - xpoints[i]) * (y - ypoints[i]) / (ypoints[j] - ypoints[i]) + xpoints[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    public TRectangle getBounds() {
        if (npoints == 0) {
            return new TRectangle();
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < npoints; i++) {
            minX = Math.min(minX, xpoints[i]);
            maxX = Math.max(maxX, xpoints[i]);
            minY = Math.min(minY, ypoints[i]);
            maxY = Math.max(maxY, ypoints[i]);
        }
        return new TRectangle(minX, minY, maxX - minX, maxY - minY);
    }
}
