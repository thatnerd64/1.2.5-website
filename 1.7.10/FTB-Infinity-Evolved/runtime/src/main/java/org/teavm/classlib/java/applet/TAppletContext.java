package org.teavm.classlib.java.applet;

public interface TAppletContext {
    void showDocument(java.net.URL url);

    void showDocument(java.net.URL url, String target);

    void showStatus(String status);
}
