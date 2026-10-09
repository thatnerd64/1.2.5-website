package org.teavm.classlib.java.awt.event;

public interface TWindowListener extends java.util.EventListener {
    void windowOpened(TWindowEvent e);

    void windowClosing(TWindowEvent e);

    void windowClosed(TWindowEvent e);

    void windowIconified(TWindowEvent e);

    void windowDeiconified(TWindowEvent e);

    void windowActivated(TWindowEvent e);

    void windowDeactivated(TWindowEvent e);
}
