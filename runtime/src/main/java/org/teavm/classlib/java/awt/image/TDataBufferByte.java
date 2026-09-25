package org.teavm.classlib.java.awt.image;

public final class TDataBufferByte extends TDataBuffer {
    private final byte[] data;

    public TDataBufferByte(int size) {
        this(new byte[size], size);
    }

    public TDataBufferByte(byte[] data, int size) {
        super(TYPE_BYTE, size);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getData(int bank) {
        return data;
    }

    @Override
    public int getElem(int bank, int i) {
        return data[i] & 0xFF;
    }

    @Override
    public void setElem(int bank, int i, int val) {
        data[i] = (byte) val;
    }
}
