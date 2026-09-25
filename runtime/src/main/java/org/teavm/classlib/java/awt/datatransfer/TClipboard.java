package org.teavm.classlib.java.awt.datatransfer;

/** The system clipboard, backed by the browser clipboard (see {@code retro.input.ClipboardBridge}). */
public class TClipboard {
    private final String name;

    public TClipboard(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public TTransferable getContents(Object requestor) {
        return new TStringSelection(retro.input.ClipboardBridge.read());
    }

    public void setContents(TTransferable contents, TClipboardOwner owner) {
        try {
            Object data = contents.getTransferData(TDataFlavor.stringFlavor);
            retro.input.ClipboardBridge.write(String.valueOf(data));
        } catch (Exception e) {
            // not text
        }
    }

    public boolean isDataFlavorAvailable(TDataFlavor flavor) {
        return TDataFlavor.stringFlavor.equals(flavor);
    }

    public Object getData(TDataFlavor flavor) throws TUnsupportedFlavorException {
        if (!isDataFlavorAvailable(flavor)) {
            throw new TUnsupportedFlavorException(flavor);
        }
        return retro.input.ClipboardBridge.read();
    }
}
