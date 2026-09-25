package org.teavm.classlib.java.awt.event;

public class TInputEvent extends TComponentEvent {
    public static final int SHIFT_MASK = 1;
    public static final int CTRL_MASK = 2;
    public static final int META_MASK = 4;
    public static final int ALT_MASK = 8;
    protected int modifiers;
    private boolean consumed;

    public TInputEvent(Object source, int id) {
        super(source, id);
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isShiftDown() {
        return (modifiers & SHIFT_MASK) != 0;
    }

    public boolean isControlDown() {
        return (modifiers & CTRL_MASK) != 0;
    }

    public boolean isAltDown() {
        return (modifiers & ALT_MASK) != 0;
    }

    public boolean isMetaDown() {
        return (modifiers & META_MASK) != 0;
    }

    public void consume() {
        consumed = true;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public long getWhen() {
        return System.currentTimeMillis();
    }
}
