package org.teavm.classlib.java.awt.event;

public class TKeyEvent extends TInputEvent {
    public static final int KEY_PRESSED = 401;
    public static final int KEY_RELEASED = 402;
    public static final int KEY_TYPED = 400;
    public static final char CHAR_UNDEFINED = 0xFFFF;
    private int keyCode;
    private char keyChar;

    public TKeyEvent(Object source, int id, long when, int modifiers, int keyCode, char keyChar) {
        super(source, id);
        this.modifiers = modifiers;
        this.keyCode = keyCode;
        this.keyChar = keyChar;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public char getKeyChar() {
        return keyChar;
    }

    public static String getKeyText(int keyCode) {
        return "Key " + keyCode;
    }
}
