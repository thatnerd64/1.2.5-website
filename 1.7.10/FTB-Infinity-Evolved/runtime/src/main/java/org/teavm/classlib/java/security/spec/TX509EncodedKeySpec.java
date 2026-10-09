package org.teavm.classlib.java.security.spec;

public class TX509EncodedKeySpec implements TKeySpec {
    private final byte[] encoded;

    public TX509EncodedKeySpec(byte[] encoded) {
        this.encoded = encoded.clone();
    }

    public byte[] getEncoded() {
        return encoded.clone();
    }

    public final String getFormat() {
        return "X.509";
    }
}
