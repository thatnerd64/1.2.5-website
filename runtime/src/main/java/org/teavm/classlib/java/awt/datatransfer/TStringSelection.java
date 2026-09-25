package org.teavm.classlib.java.awt.datatransfer;

public class TStringSelection implements TTransferable, TClipboardOwner {
    private final String data;

    public TStringSelection(String data) {
        this.data = data;
    }

    @Override
    public TDataFlavor[] getTransferDataFlavors() {
        return new TDataFlavor[] { TDataFlavor.stringFlavor };
    }

    @Override
    public boolean isDataFlavorSupported(TDataFlavor flavor) {
        return TDataFlavor.stringFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(TDataFlavor flavor) throws TUnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new TUnsupportedFlavorException(flavor);
        }
        return data;
    }

    @Override
    public void lostOwnership(TClipboard clipboard, TTransferable contents) {
    }
}
