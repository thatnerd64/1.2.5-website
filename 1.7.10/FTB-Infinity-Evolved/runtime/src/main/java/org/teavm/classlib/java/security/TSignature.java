package org.teavm.classlib.java.security;

import java.security.NoSuchAlgorithmException;

/** Signature checks always fail: nothing here can verify Mojang's signatures. */
public abstract class TSignature {
    private TSignature() {
    }

    public static TSignature getInstance(String algorithm) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException(algorithm + " is not available in the browser build");
    }

    public final void initVerify(TPublicKey publicKey) throws TInvalidKeyException {
    }

    public final void update(byte[] data) throws TSignatureException {
    }

    public final boolean verify(byte[] signature) throws TSignatureException {
        return false;
    }
}
