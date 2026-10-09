package org.teavm.classlib.java.security;

import org.teavm.classlib.java.security.cert.TCertificate;

public class TCodeSource {
    private final java.net.URL location;

    public TCodeSource(java.net.URL location, TCertificate[] certificates) {
        this.location = location;
    }

    public final java.net.URL getLocation() {
        return location;
    }

    public final TCertificate[] getCertificates() {
        return null;
    }
}
