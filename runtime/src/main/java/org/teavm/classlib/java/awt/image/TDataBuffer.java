package org.teavm.classlib.java.awt.image;

public abstract class TDataBuffer {
    public static final int TYPE_BYTE = 0;
    public static final int TYPE_USHORT = 1;
    public static final int TYPE_SHORT = 2;
    public static final int TYPE_INT = 3;
    protected int dataType;
    protected int size;

    protected TDataBuffer(int dataType, int size) {
        this.dataType = dataType;
        this.size = size;
    }

    public int getDataType() {
        return dataType;
    }

    public int getSize() {
        return size;
    }

    public int getNumBanks() {
        return 1;
    }

    public abstract int getElem(int bank, int i);

    public abstract void setElem(int bank, int i, int val);

    public int getElem(int i) {
        return getElem(0, i);
    }

    public void setElem(int i, int val) {
        setElem(0, i, val);
    }
}
