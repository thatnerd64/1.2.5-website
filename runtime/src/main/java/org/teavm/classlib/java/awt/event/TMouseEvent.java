package org.teavm.classlib.java.awt.event;

public class TMouseEvent extends TInputEvent {
    public static final int BUTTON1 = 1;
    public static final int BUTTON2 = 2;
    public static final int BUTTON3 = 3;
    private int x;
    private int y;
    private int button;
    private int clickCount;

    public TMouseEvent(Object source, int id, long when, int modifiers, int x, int y, int clickCount,
            boolean popupTrigger, int button) {
        super(source, id);
        this.modifiers = modifiers;
        this.x = x;
        this.y = y;
        this.clickCount = clickCount;
        this.button = button;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getButton() {
        return button;
    }

    public int getClickCount() {
        return clickCount;
    }
}
