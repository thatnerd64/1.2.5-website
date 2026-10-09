package org.teavm.classlib.java.util;

/**
 * java.util.Random, bit-for-bit compatible with the JDK. Replaces TeaVM's version for two reasons:
 * <ul>
 *   <li>TeaVM's {@code nextInt(bound)} uses a different algorithm, so a world seed would generate different
 *       terrain than on desktop (and a desktop world continued here would get seams at new chunks);</li>
 *   <li>its 48-bit LCG runs on 64-bit longs, which are BigInts in JavaScript and slow. Here the state is two
 *       24-bit halves held in doubles, where every intermediate product is exact.</li>
 * </ul>
 */
public class TRandom implements java.util.random.RandomGenerator, java.io.Serializable {
    private static final long serialVersionUID = 3905348978240129619L;
    private static final double TWO_24 = 16777216.0;
    private static final double MUL_HI = 0x5DE;
    private static final double MUL_LO = 0xECE66D;
    private static final double[] SHIFT = new double[49];
    private static int uniquifier;

    static {
        double p = 1;
        for (int i = 0; i <= 48; i++) {
            SHIFT[i] = p;
            p *= 2;
        }
    }

    private double hi;
    private double lo;
    private double nextNextGaussian;
    private boolean haveNextNextGaussian;

    public TRandom() {
        this((long) (Math.random() * 281474976710656.0) ^ System.currentTimeMillis() ^ ((long) ++uniquifier << 32));
    }

    public TRandom(long seed) {
        scramble(seed);
    }

    private void scramble(long seed) {
        long s = seed ^ 0x5DEECE66DL;
        lo = (int) s & 0xFFFFFF;
        hi = (int) (s >>> 24) & 0xFFFFFF;
        haveNextNextGaussian = false;
    }

    public void setSeed(long seed) {
        scramble(seed);
    }

    protected int next(int bits) {
        double l = lo * MUL_LO + 0xB;
        double carry = Math.floor(l / TWO_24);
        double newLo = l - carry * TWO_24;
        double h = hi * MUL_LO + lo * MUL_HI + carry;
        double newHi = h - Math.floor(h / TWO_24) * TWO_24;
        hi = newHi;
        lo = newLo;
        double v = Math.floor((newHi * TWO_24 + newLo) / SHIFT[48 - bits]);
        return v >= 2147483648.0 ? (int) (v - 4294967296.0) : (int) v;
    }

    public void nextBytes(byte[] bytes) {
        for (int i = 0, len = bytes.length; i < len;) {
            for (int rnd = nextInt(), n = Math.min(len - i, 4); n-- > 0; rnd >>= 8) {
                bytes[i++] = (byte) rnd;
            }
        }
    }

    @Override
    public int nextInt() {
        return next(32);
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0) {
            // (int) ((bound * (long) r) >> 31) for a power of two
            r >>= 31 - Integer.numberOfTrailingZeros(bound);
        } else {
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31)) {
                // retry
            }
        }
        return r;
    }

    @Override
    public int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        int r = nextInt();
        int n = bound - origin;
        int m = n - 1;
        if ((n & m) == 0) {
            r = (r & m) + origin;
        } else if (n > 0) {
            for (int u = r >>> 1; u + m - (r = u % n) < 0; u = nextInt() >>> 1) {
                // retry
            }
            r += origin;
        } else {
            while (r < origin || r >= bound) {
                r = nextInt();
            }
        }
        return r;
    }

    @Override
    public long nextLong() {
        return ((long) next(32) << 32) + next(32);
    }

    @Override
    public boolean nextBoolean() {
        return next(1) != 0;
    }

    @Override
    public float nextFloat() {
        return next(24) / (float) (1 << 24);
    }

    @Override
    public double nextDouble() {
        return (next(26) * 134217728.0 + next(27)) * 0x1.0p-53;
    }

    @Override
    public double nextGaussian() {
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        }
        double v1;
        double v2;
        double s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        double multiplier = Math.sqrt(-2 * Math.log(s) / s);
        nextNextGaussian = v2 * multiplier;
        haveNextNextGaussian = true;
        return v1 * multiplier;
    }
}
