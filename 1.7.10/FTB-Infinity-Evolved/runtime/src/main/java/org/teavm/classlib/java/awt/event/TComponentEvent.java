package org.teavm.classlib.java.awt.event;

public class TComponentEvent extends TAWTEvent {
    public static final int COMPONENT_RESIZED = 101;

    public TComponentEvent(Object source, int id) {
        super(source, id);
    }

    public org.teavm.classlib.java.awt.TComponent getComponent() {
        return (org.teavm.classlib.java.awt.TComponent) getSource();
    }
}
