package org.teavm.classlib.java.awt;

/** Only used by Minecraft's crash report panel: the text is shown in the page instead. */
public class TTextArea extends TTextComponent {
    public static final int SCROLLBARS_BOTH = 0;
    public static final int SCROLLBARS_VERTICAL_ONLY = 1;
    public static final int SCROLLBARS_HORIZONTAL_ONLY = 2;
    public static final int SCROLLBARS_NONE = 3;

    public TTextArea() {
    }

    public TTextArea(String text) {
        setText(text);
    }

    public TTextArea(String text, int rows, int columns) {
        setText(text);
    }

    public TTextArea(String text, int rows, int columns, int scrollbars) {
        setText(text);
        retro.Crash.show(text);
    }

    public void append(String s) {
        setText(getText() + s);
    }
}
