package org.teavm.classlib.java.awt;

import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.java.awt.event.TComponentListener;
import org.teavm.classlib.java.awt.event.TFocusListener;
import org.teavm.classlib.java.awt.event.TKeyListener;
import org.teavm.classlib.java.awt.event.TMouseListener;
import org.teavm.classlib.java.awt.event.TMouseMotionListener;
import org.teavm.classlib.java.awt.event.TMouseWheelListener;
import org.teavm.classlib.java.awt.image.TImageObserver;

/**
 * java.awt.Component. There is no real widget tree in the browser: the game renders to the page's canvas
 * through the LWJGL shims, so components only keep their properties.
 */
public abstract class TComponent implements TImageObserver {
    public static final float CENTER_ALIGNMENT = 0.5f;
    TContainer parent;
    int x;
    int y;
    int width;
    int height;
    boolean visible = true;
    boolean enabled = true;
    TColor background;
    TColor foreground;
    TFont font;
    String name;
    TDimension preferredSize;
    TDimension minimumSize;
    TDimension maximumSize;
    final List<Object> listeners = new ArrayList<>();

    protected TComponent() {
    }

    public TContainer getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TDimension getSize() {
        return new TDimension(getWidth(), getHeight());
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setSize(TDimension d) {
        setSize(d.width, d.height);
    }

    public TRectangle getBounds() {
        return new TRectangle(x, y, getWidth(), getHeight());
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        setSize(width, height);
    }

    public void setBounds(TRectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public TPoint getLocation() {
        return new TPoint(x, y);
    }

    public TPoint getLocationOnScreen() {
        return new TPoint(x, y);
    }

    public TDimension getPreferredSize() {
        return preferredSize != null ? preferredSize : getSize();
    }

    public void setPreferredSize(TDimension d) {
        preferredSize = d;
        if (d != null && width == 0 && height == 0) {
            setSize(d.width, d.height);
        }
    }

    public TDimension getMinimumSize() {
        return minimumSize != null ? minimumSize : new TDimension(0, 0);
    }

    public void setMinimumSize(TDimension d) {
        minimumSize = d;
    }

    public TDimension getMaximumSize() {
        return maximumSize != null ? maximumSize : new TDimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    public void setMaximumSize(TDimension d) {
        maximumSize = d;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isShowing() {
        return visible;
    }

    public boolean isDisplayable() {
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFocusable() {
        return true;
    }

    public void setFocusable(boolean focusable) {
    }

    public boolean hasFocus() {
        return true;
    }

    public boolean isFocusOwner() {
        return true;
    }

    public void requestFocus() {
    }

    public boolean requestFocusInWindow() {
        return true;
    }

    public void setIgnoreRepaint(boolean ignore) {
    }

    public TColor getBackground() {
        return background;
    }

    public void setBackground(TColor c) {
        background = c;
    }

    public TColor getForeground() {
        return foreground;
    }

    public void setForeground(TColor c) {
        foreground = c;
    }

    public TFont getFont() {
        return font;
    }

    public void setFont(TFont f) {
        font = f;
    }

    public TFontMetrics getFontMetrics(TFont f) {
        return new TFontMetrics(f) {
        };
    }

    public void setCursor(TCursor cursor) {
    }

    public TCursor getCursor() {
        return TCursor.getDefaultCursor();
    }

    public TGraphics getGraphics() {
        return null;
    }

    public TToolkit getToolkit() {
        return TToolkit.getDefaultToolkit();
    }

    public void paint(TGraphics g) {
    }

    public void update(TGraphics g) {
        paint(g);
    }

    public void repaint() {
    }

    public void repaint(long tm) {
    }

    public void repaint(int x, int y, int w, int h) {
    }

    public void invalidate() {
    }

    public void validate() {
    }

    public void revalidate() {
    }

    public void doLayout() {
    }

    public void addNotify() {
    }

    public void removeNotify() {
    }

    public void addKeyListener(TKeyListener l) {
        listeners.add(l);
    }

    public void removeKeyListener(TKeyListener l) {
        listeners.remove(l);
    }

    public void addMouseListener(TMouseListener l) {
        listeners.add(l);
    }

    public void removeMouseListener(TMouseListener l) {
        listeners.remove(l);
    }

    public void addMouseMotionListener(TMouseMotionListener l) {
        listeners.add(l);
    }

    public void removeMouseMotionListener(TMouseMotionListener l) {
        listeners.remove(l);
    }

    public void addMouseWheelListener(TMouseWheelListener l) {
        listeners.add(l);
    }

    public void addFocusListener(TFocusListener l) {
        listeners.add(l);
    }

    public void removeFocusListener(TFocusListener l) {
        listeners.remove(l);
    }

    public void addComponentListener(TComponentListener l) {
        listeners.add(l);
    }

    public void removeComponentListener(TComponentListener l) {
        listeners.remove(l);
    }

    public TImage createImage(int width, int height) {
        return new org.teavm.classlib.java.awt.image.TBufferedImage(width, height,
                org.teavm.classlib.java.awt.image.TBufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public boolean imageUpdate(TImage img, int infoflags, int x, int y, int w, int h) {
        return false;
    }
}
