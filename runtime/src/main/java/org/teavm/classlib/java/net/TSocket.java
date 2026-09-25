package org.teavm.classlib.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import retro.net.WebSocketConnection;

/**
 * TCP sockets tunnelled over a WebSocket proxy (configured by the page as {@code retroConfig.proxy}). Minecraft
 * multiplayer and server list pings use this.
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
        if (!(endpoint instanceof TInetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        remote = (TInetSocketAddress) endpoint;
        try {
            connection = WebSocketConnection.open(remote.getHostName(), remote.getPort(), timeout);
        } catch (IOException e) {
            throw new TConnectException(e.getMessage());
        }
    }

    public InputStream getInputStream() throws IOException {
        if (connection == null) {
            throw new TSocketException("Socket is not connected");
        }
        return connection.inputStream(this);
    }

    public OutputStream getOutputStream() throws IOException {
        if (connection == null) {
            throw new TSocketException("Socket is not connected");
        }
        return connection.outputStream();
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int timeout) {
        soTimeout = timeout;
    }

    public void setTcpNoDelay(boolean on) {
    }

    public boolean getTcpNoDelay() {
        return true;
    }

    public void setTrafficClass(int tc) {
    }

    public void setKeepAlive(boolean on) {
    }

    public void setReceiveBufferSize(int size) {
    }

    public void setSendBufferSize(int size) {
    }

    public void setSoLinger(boolean on, int linger) {
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
        return 0;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (connection != null) {
            connection.close();
        }
    }
}
