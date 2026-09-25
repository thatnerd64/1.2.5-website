package org.teavm.classlib.java.util.logging;

public class TErrorManager {
    public static final int GENERIC_FAILURE = 0;
    public static final int WRITE_FAILURE = 1;
    public static final int FLUSH_FAILURE = 2;
    public static final int CLOSE_FAILURE = 3;
    public static final int OPEN_FAILURE = 4;
    public static final int FORMAT_FAILURE = 5;

    public synchronized void error(String msg, Exception ex, int code) {
        retro.JS.error("java.util.logging.ErrorManager: " + code + (msg != null ? ": " + msg : ""));
    }
}
