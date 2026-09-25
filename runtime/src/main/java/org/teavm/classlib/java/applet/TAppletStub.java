package org.teavm.classlib.java.applet;

public interface TAppletStub {
    boolean isActive();

    java.net.URL getDocumentBase();

    java.net.URL getCodeBase();

    String getParameter(String name);

    TAppletContext getAppletContext();

    void appletResize(int width, int height);
}
