package org.teavm.classlib.java.security;

import java.security.NoSuchAlgorithmException;

public abstract class TKeyPairGenerator {
    private TKeyPairGenerator() {
    }

    public static TKeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException(algorithm + " is not available in the browser build");
    }

    public void initialize(int keysize) {
    }

    public final TKeyPair generateKeyPair() {
        throw new UnsupportedOperationException();
    }
}
