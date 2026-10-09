package org.teavm.classlib.java.net;

import java.io.IOException;

/** A browser cannot listen on a port: binding always fails, as on a machine with no free port. */
public class TServerSocket implements java.io.Closeable {
    private boolean closed;

    public TServerSocket() throws IOException {
    }

    public TServerSocket(int port) throws IOException {
        throw new IOException("Cannot listen on a port in a browser");
    }

    public void bind(TSocketAddress endpoint) throws IOException {
        throw new IOException("Cannot listen on a port in a browser");
    }

    public void bind(TSocketAddress endpoint, int backlog) throws IOException {
        throw new IOException("Cannot listen on a port in a browser");
    }

    public TSocket accept() throws IOException {
        throw new IOException("Cannot listen on a port in a browser");
    }

    public void setReuseAddress(boolean on) throws TSocketException {
    }

    public boolean getReuseAddress() throws TSocketException {
        return false;
    }

    public void setSoTimeout(int timeout) throws TSocketException {
    }

    public int getSoTimeout() throws IOException {
        return 0;
    }

    public int getLocalPort() {
        return -1;
    }

    public boolean isBound() {
        return false;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
