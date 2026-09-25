package org.teavm.classlib.java.security;

public class TProtectionDomain {
    private final TCodeSource codeSource;

    public TProtectionDomain(TCodeSource codeSource, TPermissionCollection permissions) {
        this.codeSource = codeSource;
    }

    public final TCodeSource getCodeSource() {
        return codeSource;
    }
}
