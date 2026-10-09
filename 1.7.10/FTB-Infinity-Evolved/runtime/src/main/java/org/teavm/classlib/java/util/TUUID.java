package org.teavm.classlib.java.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** java.util.UUID in full (TeaVM's stand-in only holds a string). */
public final class TUUID implements java.io.Serializable, Comparable<TUUID> {
    private static final java.util.Random RANDOM = new java.util.Random();

    private final long mostSigBits;
    private final long leastSigBits;

    public TUUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    private TUUID(byte[] data) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (data[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (data[i] & 0xff);
        }
        this.mostSigBits = msb;
        this.leastSigBits = lsb;
    }

    public static TUUID randomUUID() {
        byte[] data = new byte[16];
        RANDOM.nextBytes(data);
        data[6] &= 0x0f;
        data[6] |= 0x40;
        data[8] &= 0x3f;
        data[8] |= 0x80;
        return new TUUID(data);
    }

    public static TUUID nameUUIDFromBytes(byte[] name) {
        byte[] md5;
        try {
            md5 = MessageDigest.getInstance("MD5").digest(name);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("MD5 not supported");
        }
        md5[6] &= 0x0f;
        md5[6] |= 0x30;
        md5[8] &= 0x3f;
        md5[8] |= 0x80;
        return new TUUID(md5);
    }

    public static TUUID fromString(String name) {
        String[] parts = name.split("-");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid UUID string: " + name);
        }
        long msb = Long.parseLong(parts[0], 16);
        msb = (msb << 16) | Long.parseLong(parts[1], 16);
        msb = (msb << 16) | Long.parseLong(parts[2], 16);
        long lsb = Long.parseLong(parts[3], 16);
        lsb = (lsb << 48) | Long.parseLong(parts[4], 16);
        return new TUUID(msb, lsb);
    }

    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    public long getMostSignificantBits() {
        return mostSigBits;
    }

    public int version() {
        return (int) ((mostSigBits >> 12) & 0x0f);
    }

    public int variant() {
        return (int) ((leastSigBits >>> (64 - (leastSigBits >>> 62))) & (leastSigBits >> 63));
    }

    @Override
    public String toString() {
        return digits(mostSigBits >> 32, 8) + "-" + digits(mostSigBits >> 16, 4) + "-" + digits(mostSigBits, 4) + "-"
                + digits(leastSigBits >> 48, 4) + "-" + digits(leastSigBits, 12);
    }

    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    @Override
    public int hashCode() {
        long hilo = mostSigBits ^ leastSigBits;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TUUID)) {
            return false;
        }
        TUUID id = (TUUID) obj;
        return mostSigBits == id.mostSigBits && leastSigBits == id.leastSigBits;
    }

    @Override
    public int compareTo(TUUID val) {
        return mostSigBits < val.mostSigBits ? -1 : mostSigBits > val.mostSigBits ? 1
                : leastSigBits < val.leastSigBits ? -1 : leastSigBits > val.leastSigBits ? 1 : 0;
    }
}
