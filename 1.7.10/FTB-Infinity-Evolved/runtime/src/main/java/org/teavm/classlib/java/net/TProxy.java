package org.teavm.classlib.java.net;

/** java.net.Proxy: the browser cannot use proxies; this only lets code that mentions them link and run. */
public class TProxy {
    public enum Type {
        DIRECT, HTTP, SOCKS
    }

    public static final TProxy NO_PROXY = new TProxy();

    private final Type type;
    private final TSocketAddress address;

    private TProxy() {
        this.type = Type.DIRECT;
        this.address = null;
    }

    public TProxy(Type type, TSocketAddress address) {
        this.type = type;
        this.address = address;
    }

    public Type type() {
        return type;
    }

    public TSocketAddress address() {
        return address;
    }

    @Override
    public String toString() {
        return type == Type.DIRECT ? "DIRECT" : type + " @ " + address;
    }
}
