package cpw.mods.fml.relauncher;

/**
 * Ends the AOT dump's JVM. FML's security manager traps System.exit unless the caller is in a cpw.mods.fml package,
 * and mods leave non-daemon threads running that would otherwise keep the build waiting forever.
 */
public final class RetroExit {
    private RetroExit() {
    }

    public static void exit() {
        System.exit(0);
    }
}
