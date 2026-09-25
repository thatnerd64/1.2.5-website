package org.lwjgl.input;

/** No gamepad support: reports zero controllers. */
public final class Controllers {
    private static boolean created;

    private Controllers() {
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

    public static int getControllerCount() {
        return 0;
    }

    public static Controller getController(int index) {
        throw new IndexOutOfBoundsException();
    }

    public static void poll() {
    }

    public static boolean next() {
        return false;
    }

    public static void clearEvents() {
    }
}
