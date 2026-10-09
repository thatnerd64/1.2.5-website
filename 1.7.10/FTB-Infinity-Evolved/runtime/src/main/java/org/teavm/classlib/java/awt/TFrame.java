package org.teavm.classlib.java.awt;

public class TFrame extends TWindow {
    public static final int NORMAL = 0;
    public static final int MAXIMIZED_BOTH = 6;
    private String title;

    public TFrame() {
        this("");
    }

    public TFrame(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setResizable(boolean resizable) {
    }

    public boolean isResizable() {
        return true;
    }

    public void setExtendedState(int state) {
    }

    public int getExtendedState() {
        return NORMAL;
    }

    public void setUndecorated(boolean undecorated) {
    }

    public void setDefaultCloseOperation(int op) {
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            retro.gl.Display.setTitleFromFrame(title);
        }
    }
}
