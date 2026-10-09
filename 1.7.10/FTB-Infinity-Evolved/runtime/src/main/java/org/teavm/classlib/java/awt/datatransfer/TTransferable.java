package org.teavm.classlib.java.awt.datatransfer;

public interface TTransferable {
    TDataFlavor[] getTransferDataFlavors();

    boolean isDataFlavorSupported(TDataFlavor flavor);

    Object getTransferData(TDataFlavor flavor) throws TUnsupportedFlavorException, java.io.IOException;
}
