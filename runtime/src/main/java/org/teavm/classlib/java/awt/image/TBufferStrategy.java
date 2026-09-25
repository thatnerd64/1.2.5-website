package org.teavm.classlib.java.awt.image;

import org.teavm.classlib.java.awt.TGraphics;

public abstract class TBufferStrategy {
    public abstract TGraphics getDrawGraphics();

    public abstract void show();

    public boolean contentsLost() {
        return false;
    }

    public void dispose() {
    }
}
