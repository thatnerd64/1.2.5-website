package org.teavm.classlib.java.awt.event;

public class TAWTEvent extends java.util.EventObject {
    protected int id;

    public TAWTEvent(Object source, int id) {
        super(source);
        this.id = id;
    }

    public int getID() {
        return id;
    }
}
