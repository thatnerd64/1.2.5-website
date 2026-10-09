package org.teavm.classlib.java.net;

import java.io.IOException;

/** No UDP in a browser (LAN discovery and pings simply fail). */
public class TDatagramSocket implements java.io.Closeable {
    public TDatagramSocket() throws TSocketException {
        throw new TSocketException("UDP is not available in a browser");
    }

    public TDatagramSocket(int port) throws TSocketException {
        throw new TSocketException("UDP is not available in a browser");
    }

    public void send(TDatagramPacket p) throws IOException {
        throw new IOException("UDP is not available in a browser");
    }

    public void receive(TDatagramPacket p) throws IOException {
        throw new IOException("UDP is not available in a browser");
    }

    public void setSoTimeout(int timeout) throws TSocketException {
    }

    @Override
    public void close() {
    }
}
