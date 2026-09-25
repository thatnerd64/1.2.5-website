package org.teavm.classlib.java.awt;

import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.java.awt.event.TWindowEvent;
import org.teavm.classlib.java.awt.event.TWindowListener;

public class TWindow extends TContainer {
    final List<TWindowListener> windowListeners = new ArrayList<>();

    public TWindow() {
        visible = false;
    }

    public void pack() {
        TDimension d = getPreferredSize();
        setSize(d.width, d.height);
    }

    public void setLocationRelativeTo(TComponent c) {
    }

    public void toFront() {
    }

    public void toBack() {
    }

    public void dispose() {
        for (TWindowListener l : new ArrayList<>(windowListeners)) {
            l.windowClosed(new TWindowEvent(this, TWindowEvent.WINDOW_CLOSED));
        }
    }

    public void addWindowListener(TWindowListener l) {
        windowListeners.add(l);
    }

    public void removeWindowListener(TWindowListener l) {
        windowListeners.remove(l);
    }

    public void setIconImage(TImage image) {
    }

    public void setIconImages(List<? extends TImage> images) {
    }

    public void setAlwaysOnTop(boolean onTop) {
    }

    public boolean isActive() {
        return true;
    }

    public boolean isFocused() {
        return true;
    }
}
