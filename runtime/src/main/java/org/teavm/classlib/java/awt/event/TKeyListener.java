package org.teavm.classlib.java.awt.event;

public interface TKeyListener extends java.util.EventListener {
    void keyTyped(TKeyEvent e);

    void keyPressed(TKeyEvent e);

    void keyReleased(TKeyEvent e);
}
