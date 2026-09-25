package org.teavm.classlib.java.applet;

import org.teavm.classlib.java.awt.TPanel;

public class TApplet extends TPanel {
    private TAppletStub stub;

    public TApplet() {
    }

    public final void setStub(TAppletStub stub) {
        this.stub = stub;
    }

    public void init() {
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
    }

    public boolean isActive() {
        return stub == null || stub.isActive();
    }

    public String getParameter(String name) {
        return stub != null ? stub.getParameter(name) : null;
    }

    public java.net.URL getDocumentBase() {
        return stub != null ? stub.getDocumentBase() : null;
    }

    public java.net.URL getCodeBase() {
        return stub != null ? stub.getCodeBase() : null;
    }

    public TAppletContext getAppletContext() {
        return stub != null ? stub.getAppletContext() : null;
    }

    public void showStatus(String msg) {
    }

    public String getAppletInfo() {
        return null;
    }

    public void resize(int width, int height) {
        setSize(width, height);
    }
}
