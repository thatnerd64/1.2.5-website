package org.teavm.classlib.java.net;

public class TInetSocketAddress extends TSocketAddress {
    private final String host;
    private final int port;

    public TInetSocketAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public TInetSocketAddress(TInetAddress addr, int port) {
        this(addr == null ? "0.0.0.0" : addr.getHostName(), port);
    }

    public TInetSocketAddress(int port) {
        this("0.0.0.0", port);
    }

    public static TInetSocketAddress createUnresolved(String host, int port) {
        return new TInetSocketAddress(host, port);
    }

    public final int getPort() {
        return port;
    }

    public final String getHostName() {
        return host;
    }

    public final String getHostString() {
        return host;
    }

    public final TInetAddress getAddress() {
        return new TInetAddress(host);
    }

    public final boolean isUnresolved() {
        return false;
    }

    @Override
    public String toString() {
        return host + "/" + host + ":" + port;
    }
}
