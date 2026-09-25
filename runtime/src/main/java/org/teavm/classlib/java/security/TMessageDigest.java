package org.teavm.classlib.java.security;

import java.io.ByteArrayOutputStream;

/** MD5, SHA-1 and SHA-256 message digests. */
public abstract class TMessageDigest {
    private final String algorithm;
    private final ByteArrayOutputStream data = new ByteArrayOutputStream();

    protected TMessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    public static TMessageDigest getInstance(String algorithm) throws TNoSuchAlgorithmException {
        String a = algorithm.toUpperCase().replace("-", "");
        switch (a) {
            case "MD5":
            case "SHA1":
            case "SHA":
            case "SHA256":
                return new TMessageDigest(algorithm) {
                };
            default:
                throw new TNoSuchAlgorithmException(algorithm + " MessageDigest not available");
        }
    }

    public static TMessageDigest getInstance(String algorithm, String provider) throws TNoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public void update(byte input) {
        data.write(input);
    }

    public void update(byte[] input) {
        data.write(input, 0, input.length);
    }

    public void update(byte[] input, int offset, int len) {
        data.write(input, offset, len);
    }

    public void reset() {
        data.reset();
    }

    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    public int getDigestLength() {
        String a = algorithm.toUpperCase().replace("-", "");
        return a.equals("MD5") ? 16 : a.equals("SHA256") ? 32 : 20;
    }

    public byte[] digest() {
        byte[] message = data.toByteArray();
        data.reset();
        String a = algorithm.toUpperCase().replace("-", "");
        if (a.equals("MD5")) {
            return retro.compat.Digests.md5(message);
        } else if (a.equals("SHA256")) {
            return retro.compat.Digests.sha256(message);
        }
        return retro.compat.Digests.sha1(message);
    }

    public static boolean isEqual(byte[] a, byte[] b) {
        return java.util.Arrays.equals(a, b);
    }
}
