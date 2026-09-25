package org.teavm.classlib.java.awt.event;

public class TMouseWheelEvent extends TMouseEvent {
    private int rotation;

    public TMouseWheelEvent(Object source, int id, long when, int modifiers, int x, int y, int clickCount,
            boolean popupTrigger, int scrollType, int scrollAmount, int wheelRotation) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger, 0);
        this.rotation = wheelRotation;
    }

    public int getWheelRotation() {
        return rotation;
    }
}
