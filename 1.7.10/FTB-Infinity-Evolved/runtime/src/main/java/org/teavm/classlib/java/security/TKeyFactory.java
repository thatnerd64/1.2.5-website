package org.teavm.classlib.java.security;

import java.security.NoSuchAlgorithmException;
import org.teavm.classlib.java.security.spec.TInvalidKeySpecException;
import org.teavm.classlib.java.security.spec.TKeySpec;
import org.teavm.classlib.java.security.spec.TX509EncodedKeySpec;

/** Keys can be held but not used: there is no public-key cryptography in the browser build. */
public class TKeyFactory {
    private final String algorithm;

    private TKeyFactory(String algorithm) {
        this.algorithm = algorithm;
    }

    public static TKeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        return new TKeyFactory(algorithm);
    }

    public final TPublicKey generatePublic(TKeySpec spec) throws TInvalidKeySpecException {
        if (!(spec instanceof TX509EncodedKeySpec)) {
            throw new TInvalidKeySpecException("unsupported key spec");
        }
        byte[] encoded = ((TX509EncodedKeySpec) spec).getEncoded();
        String alg = algorithm;
        return new TPublicKey() {
            @Override
            public String getAlgorithm() {
                return alg;
            }

            @Override
            public String getFormat() {
                return "X.509";
            }

            @Override
            public byte[] getEncoded() {
                return encoded.clone();
            }
        };
    }

    public final String getAlgorithm() {
        return algorithm;
    }
}
