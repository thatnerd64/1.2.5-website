package org.lwjgl.input;

import retro.input.Input;

/** LWJGL Mouse backed by page mouse events and pointer lock (see retro.input.Input). */
public final class Mouse {
    public static final int EVENT_SIZE = 22;
    private static boolean created;
    private static final int[] event = new int[7];
    private static Cursor nativeCursor;

    private Mouse() {
    }

    public static void create() {
        created = true;
    }

    public static boolean isCreated() {
        return created;
    }

    public static void destroy() {
        created = false;
    }

    public static void poll() {
    }

    public static boolean next() {
        return Input.nextMouse(event);
    }

    public static int getEventButton() {
        return event[0];
    }

    public static boolean getEventButtonState() {
        return event[1] != 0;
    }

    public static int getEventX() {
        return event[2];
    }

    public static int getEventY() {
        return event[3];
    }

    public static int getEventDWheel() {
        return event[4];
    }

    public static int getEventDX() {
        return event[5];
    }

    public static int getEventDY() {
        return event[6];
    }

    public static long getEventNanoseconds() {
        return System.nanoTime();
    }

    public static int getX() {
        return Input.mouseX();
    }

    public static int getY() {
        return Input.mouseY();
    }

    public static int getDX() {
        return Input.takeDX();
    }

    public static int getDY() {
        return Input.takeDY();
    }

    public static int getDWheel() {
        return Input.takeWheel();
    }

    public static boolean isButtonDown(int button) {
        return Input.isButtonDown(button);
    }

    public static int getButtonCount() {
        return 3;
    }

    public static String getButtonName(int button) {
        return "BUTTON" + button;
    }

    public static int getButtonIndex(String name) {
        return name.startsWith("BUTTON") ? Integer.parseInt(name.substring(6)) : -1;
    }

    public static boolean hasWheel() {
        return true;
    }

    public static void setGrabbed(boolean grab) {
        Input.setGrabbed(grab);
    }

    public static boolean isGrabbed() {
        return Input.isGrabbed();
    }

    public static void setCursorPosition(int x, int y) {
        Input.setCursorPosition(x, y);
    }

    public static boolean isInsideWindow() {
        return Input.isInsideWindow();
    }

    public static Cursor setNativeCursor(Cursor cursor) {
        Cursor old = nativeCursor;
        nativeCursor = cursor;
        return old;
    }

    public static Cursor getNativeCursor() {
        return nativeCursor;
    }

    public static void setClipMouseCoordinatesToWindow(boolean clip) {
    }

    public static void updateCursor() {
    }
}
