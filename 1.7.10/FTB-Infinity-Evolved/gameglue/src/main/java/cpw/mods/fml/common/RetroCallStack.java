package cpw.mods.fml.common;

/**
 * LoadController's FMLSecurityManager reads the classes on the call stack (SecurityManager.getClassContext) to guess
 * which mod is calling outside a mod event; calls to its getStackClasses() are redirected here (see
 * retro.build.Patches). There is no such stack in the browser: an empty one makes FML attribute the call to
 * Minecraft, as it does when no mod class is on the stack.
 */
public final class RetroCallStack {
    private RetroCallStack() {
    }

    public static Class<?>[] getStackClasses(LoadController.FMLSecurityManager manager) {
        return new Class<?>[0];
    }
}
