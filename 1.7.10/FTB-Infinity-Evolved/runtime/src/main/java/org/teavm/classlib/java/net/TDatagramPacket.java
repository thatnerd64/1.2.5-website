package org.teavm.classlib.java.net;

public final class TDatagramPacket {
    private byte[] buf;
    private int offset;
    private int length;

    public TDatagramPacket(byte[] buf, int length) {
        this(buf, 0, length);
    }

    public TDatagramPacket(byte[] buf, int offset, int length) {
        this.buf = buf;
        this.offset = offset;
        this.length = length;
    }

    public TDatagramPacket(byte[] buf, int length, TInetAddress address, int port) {
        this(buf, 0, length);
    }

    public byte[] getData() {
        return buf;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public TInetAddress getAddress() {
        return null;
    }

    public int getPort() {
        return -1;
    }
}
