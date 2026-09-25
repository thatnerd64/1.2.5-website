package org.teavm.classlib.java.awt;

public class TLabel extends TComponent {
    private String text;

    public TLabel() {
        this("");
    }

    public TLabel(String text) {
        this.text = text;
    }

    public TLabel(String text, int alignment) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
