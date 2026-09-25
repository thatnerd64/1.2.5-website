package org.teavm.classlib.java.awt.event;

public interface TMouseListener extends java.util.EventListener {
    void mouseClicked(TMouseEvent e);

    void mousePressed(TMouseEvent e);

    void mouseReleased(TMouseEvent e);

    void mouseEntered(TMouseEvent e);

    void mouseExited(TMouseEvent e);
}
