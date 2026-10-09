package org.lwjgl;

/** LWJGL Sys for the browser. */
public final class Sys {
    private Sys() {
    }

    public static void initialize() {
    }

    public static String getVersion() {
        return "2.9.3";
    }

    public static long getTime() {
        return (long) retro.JS.now();
    }

    public static long getTimerResolution() {
        return 1000;
    }

    public static boolean openURL(String url) {
        retro.JS.openUrl(url);
        return true;
    }

    public static void alert(String title, String message) {
        retro.JS.log(title + ": " + message);
    }

    public static boolean is64Bit() {
        return false;
    }

    public static String getClipboard() {
        return retro.input.ClipboardBridge.read();
    }
}
