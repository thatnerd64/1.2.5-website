package retro.net;

/**
 * Server addresses as typed into Minecraft's multiplayer screens. Besides {@code host[:port]}, which is reached
 * through the launcher's relay, an address may be a WebSocket URL ({@code wss://...} or {@code ws://...}) for a
 * server that runs a relay itself (tools/ws-proxy.js --target).
 *
 * <p>Two places in the game need help with URLs (see {@code retro.build.Patches}): GuiMultiplayer splits
 * addresses on ':', which cuts a URL apart, and Packet2Handshake sends {@code user;host:port}, which servers
 * reject when longer than 64 characters.
 */
public final class ServerAddress {
    private ServerAddress() {
    }

    public static boolean isUrl(String address) {
        if (address == null) {
            return false;
        }
        String a = address.trim();
        return a.regionMatches(true, 0, "ws://", 0, 5) || a.regionMatches(true, 0, "wss://", 0, 6);
    }

    /** {@code String.split} for GuiMultiplayer: a URL stays whole and reaches GuiConnecting as the host. */
    public static String[] split(String address, String regex) {
        if (":".equals(regex) && isUrl(address)) {
            return new String[] { address.trim() };
        }
        return address.split(regex);
    }

    /** The host to put in the handshake: a URL's host name, anything else unchanged. */
    public static String handshakeHost(String host) {
        if (!isUrl(host)) {
            return host;
        }
        String rest = host.trim();
        rest = rest.substring(rest.indexOf("//") + 2);
        int end = rest.length();
        for (char c : new char[] { '/', '?', '#' }) {
            int i = rest.indexOf(c);
            if (i >= 0 && i < end) {
                end = i;
            }
        }
        String authority = rest.substring(0, end);
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            authority = authority.substring(at + 1);
        }
        if (authority.startsWith("[")) {
            int close = authority.indexOf(']');
            return close > 0 ? authority.substring(1, close) : authority;
        }
        int colon = authority.indexOf(':');
        return colon >= 0 ? authority.substring(0, colon) : authority;
    }
}
