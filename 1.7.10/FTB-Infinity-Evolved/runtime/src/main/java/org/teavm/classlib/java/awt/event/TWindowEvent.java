package org.teavm.classlib.java.awt.event;

public class TWindowEvent extends TComponentEvent {
    public static final int WINDOW_OPENED = 200;
    public static final int WINDOW_CLOSING = 201;
    public static final int WINDOW_CLOSED = 202;

    public TWindowEvent(Object source, int id) {
        super(source, id);
    }

    public org.teavm.classlib.java.awt.TWindow getWindow() {
        return (org.teavm.classlib.java.awt.TWindow) getSource();
    }
}
