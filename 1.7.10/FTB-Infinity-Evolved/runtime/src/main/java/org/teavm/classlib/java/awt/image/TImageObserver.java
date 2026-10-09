package org.teavm.classlib.java.awt.image;

import org.teavm.classlib.java.awt.TImage;

public interface TImageObserver {
    int WIDTH = 1;
    int HEIGHT = 2;
    int PROPERTIES = 4;
    int SOMEBITS = 8;
    int FRAMEBITS = 16;
    int ALLBITS = 32;
    int ERROR = 64;
    int ABORT = 128;

    boolean imageUpdate(TImage img, int infoflags, int x, int y, int width, int height);
}
