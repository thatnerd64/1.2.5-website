package org.teavm.classlib.java.awt.image;

public final class TDataBufferInt extends TDataBuffer {
    private final int[] data;

    public TDataBufferInt(int size) {
        this(new int[size], size);
    }

    public TDataBufferInt(int[] data, int size) {
        super(TYPE_INT, size);
        this.data = data;
    }

    public int[] getData() {
        return data;
    }

    public int[] getData(int bank) {
        return data;
    }

    @Override
    public int getElem(int bank, int i) {
        return data[i];
    }

    @Override
    public void setElem(int bank, int i, int val) {
        data[i] = val;
    }
}
