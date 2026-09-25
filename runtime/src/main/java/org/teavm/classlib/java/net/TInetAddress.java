package org.teavm.classlib.java.net;

/** Host names are not resolved in the browser; the proxy does that when connecting. */
public class TInetAddress implements java.io.Serializable {
    private final String host;

    TInetAddress(String host) {
        this.host = host;
    }

    public static TInetAddress getByName(String host) throws TUnknownHostException {
        if (host == null || host.isEmpty()) {
            return new TInetAddress("localhost");
        }
        return new TInetAddress(host);
    }

    public static TInetAddress[] getAllByName(String host) throws TUnknownHostException {
        return new TInetAddress[] { getByName(host) };
    }

    public static TInetAddress getLocalHost() {
        return new TInetAddress("localhost");
    }

    public static TInetAddress getLoopbackAddress() {
        return new TInetAddress("127.0.0.1");
    }

    public String getHostName() {
        return host;
    }

    public String getCanonicalHostName() {
        return host;
    }

    public String getHostAddress() {
        return host;
    }

    public byte[] getAddress() {
        String[] parts = host.split("\\.");
        byte[] out = new byte[4];
        if (parts.length == 4) {
            try {
                for (int i = 0; i < 4; i++) {
                    out[i] = (byte) Integer.parseInt(parts[i]);
                }
            } catch (NumberFormatException e) {
                // not numeric
            }
        }
        return out;
    }

    public boolean isLoopbackAddress() {
        return host.equals("localhost") || host.startsWith("127.");
    }

    public boolean isAnyLocalAddress() {
        return host.equals("0.0.0.0");
    }

    @Override
    public String toString() {
        return host + "/" + host;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TInetAddress && ((TInetAddress) o).host.equals(host);
    }

    @Override
    public int hashCode() {
        return host.hashCode();
    }
}
