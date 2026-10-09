package org.teavm.classlib.java.awt.event;

public interface TComponentListener extends java.util.EventListener {
    void componentResized(TComponentEvent e);

    void componentMoved(TComponentEvent e);

    void componentShown(TComponentEvent e);

    void componentHidden(TComponentEvent e);
}
