package org.teavm.classlib.java.awt;

public class TTextComponent extends TComponent {
    String text = "";
    boolean editable = true;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setCaretPosition(int pos) {
    }
}
