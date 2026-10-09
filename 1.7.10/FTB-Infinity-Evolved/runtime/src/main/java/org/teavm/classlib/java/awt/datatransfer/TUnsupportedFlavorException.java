package org.teavm.classlib.java.awt.datatransfer;

public class TUnsupportedFlavorException extends Exception {
    public TUnsupportedFlavorException(TDataFlavor flavor) {
        super(flavor != null ? flavor.getHumanPresentableName() : null);
    }
}
