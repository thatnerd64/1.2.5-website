package org.teavm.classlib.java.net;

import java.io.IOException;

public class TMulticastSocket extends TDatagramSocket {
    public TMulticastSocket(int port) throws IOException {
        super(port);
    }

    public void joinGroup(TInetAddress group) throws IOException {
        throw new IOException("UDP is not available in a browser");
    }

    public void leaveGroup(TInetAddress group) throws IOException {
    }
}
