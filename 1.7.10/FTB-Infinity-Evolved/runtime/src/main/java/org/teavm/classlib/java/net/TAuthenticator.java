package org.teavm.classlib.java.net;

public abstract class TAuthenticator {
    private static TAuthenticator theAuthenticator;

    public static synchronized void setDefault(TAuthenticator a) {
        theAuthenticator = a;
    }

    protected TPasswordAuthentication getPasswordAuthentication() {
        return null;
    }
}
