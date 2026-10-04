package org.teavm.classlib.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import retro.net.WebSocketConnection;

/**
 * TCP sockets carried over WebSockets (see {@link WebSocketConnection}): through the launcher's relay for
 * {@code host:port}, or straight to a {@code ws://} / {@code wss://} URL given as the host. Minecraft
 * multiplayer and server list pings use this. Connection errors, read timeouts and closes raise the same
 * exceptions as on a JVM.
 */
public class TSocket implements java.io.Closeable {
    private WebSocketConnection connection;
    private TInetSocketAddress remote;
    private int soTimeout;
    private boolean closed;

    public TSocket() {
    }

    public TSocket(String host, int port) throws IOException {
        connect(new TInetSocketAddress(host, port), 0);
    }

    public TSocket(TInetAddress address, int port) throws IOException {
        connect(new TInetSocketAddress(address, port), 0);
    }

    public void connect(TSocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    public void connect(TSocketAddress endpoint, int timeout) throws IOException {
        if (closed) {
            throw new TSocketException("Socket is closed");
        }
        if (connection != null) {
            throw new TSocketException("already connected");
        }
        if (!(endpoint instanceof TInetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
        remote = (TInetSocketAddress) endpoint;
        connection = WebSocketConnection.open(remote.getHostString(), remote.getPort(), timeout);
        connection.setReadTimeout(soTimeout);
        if (closed) {
            connection.close();
        }
    }

    public InputStream getInputStream() throws IOException {
        checkOpen();
        return connection.inputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        checkOpen();
        return connection.outputStream();
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new TSocketException("Socket is closed");
        }
        if (connection == null) {
            throw new TSocketException("Socket is not connected");
        }
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    /** Read timeout in milliseconds (0: none); a read that waits longer throws SocketTimeoutException. */
    public void setSoTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        soTimeout = timeout;
        if (connection != null) {
            connection.setReadTimeout(timeout);
        }
    }

    public void setTcpNoDelay(boolean on) {
    }

    public boolean getTcpNoDelay() {
        return true;
    }

    public void setTrafficClass(int tc) {
    }

    public int getTrafficClass() {
        return 0;
    }

    public void setKeepAlive(boolean on) {
    }

    public boolean getKeepAlive() {
        return false;
    }

    public void setReceiveBufferSize(int size) {
    }

    public int getReceiveBufferSize() {
        return 65536;
    }

    public void setSendBufferSize(int size) {
    }

    public int getSendBufferSize() {
        return 65536;
    }

    public void setSoLinger(boolean on, int linger) {
    }

    public int getSoLinger() {
        return -1;
    }

    public void setReuseAddress(boolean on) {
    }

    public boolean isConnected() {
        return connection != null;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isBound() {
        return connection != null;
    }

    public boolean isInputShutdown() {
        return closed;
    }

    public boolean isOutputShutdown() {
        return closed;
    }

    public void shutdownInput() {
    }

    public void shutdownOutput() {
    }

    public TSocketAddress getRemoteSocketAddress() {
        return remote;
    }

    public TSocketAddress getLocalSocketAddress() {
        return new TInetSocketAddress("127.0.0.1", 0);
    }

    public TInetAddress getInetAddress() {
        return remote == null ? null : remote.getAddress();
    }

    public TInetAddress getLocalAddress() {
        return TInetAddress.getLoopbackAddress();
    }

    public int getPort() {
        return remote == null ? 0 : remote.getPort();
    }

    public int getLocalPort() {
        return connection == null ? -1 : 0;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public String toString() {
        return remote == null ? "Socket[unconnected]" : "Socket[addr=" + remote + "]";
    }
}
