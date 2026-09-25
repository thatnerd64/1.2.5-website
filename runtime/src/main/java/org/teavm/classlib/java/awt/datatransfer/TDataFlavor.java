package org.teavm.classlib.java.awt.datatransfer;

public class TDataFlavor {
    public static final TDataFlavor stringFlavor = new TDataFlavor("text/plain", "Unicode String");
    private final String mimeType;
    private final String name;

    public TDataFlavor(String mimeType, String name) {
        this.mimeType = mimeType;
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getHumanPresentableName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TDataFlavor && ((TDataFlavor) o).mimeType.equals(mimeType);
    }

    @Override
    public int hashCode() {
        return mimeType.hashCode();
    }
}
