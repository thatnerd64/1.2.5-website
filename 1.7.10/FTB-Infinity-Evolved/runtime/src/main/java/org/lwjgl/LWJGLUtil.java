package org.lwjgl;

public final class LWJGLUtil {
    public static final int PLATFORM_LINUX = 1;
    public static final int PLATFORM_MACOSX = 2;
    public static final int PLATFORM_WINDOWS = 3;
    public static final String PLATFORM_LINUX_NAME = "linux";
    public static final String PLATFORM_MACOSX_NAME = "macosx";
    public static final String PLATFORM_WINDOWS_NAME = "windows";
    public static final boolean DEBUG = false;
    public static final boolean CHECKS = false;

    private LWJGLUtil() {
    }

    public static int getPlatform() {
        return PLATFORM_LINUX;
    }

    public static String getPlatformName() {
        return PLATFORM_LINUX_NAME;
    }

    public static void log(CharSequence msg) {
    }

    public static boolean isMacOSXEqualsOrBetterThan(int major, int minor) {
        return false;
    }
}
