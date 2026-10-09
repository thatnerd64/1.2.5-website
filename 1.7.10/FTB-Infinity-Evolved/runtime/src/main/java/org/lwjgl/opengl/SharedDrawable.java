package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

/** A second GL context sharing objects with the display; the browser has a single context, so this is the display. */
public final class SharedDrawable implements Drawable {
    public SharedDrawable(Drawable drawable) throws LWJGLException {
    }

    public boolean isCurrent() {
        return true;
    }

    public void makeCurrent() {
    }

    public void releaseContext() {
    }

    public void destroy() {
    }
}
