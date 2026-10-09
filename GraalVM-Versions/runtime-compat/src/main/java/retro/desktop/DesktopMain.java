package retro.desktop;

import net.minecraft.client.Minecraft;
import retro.Runtime;

/** Entry point for Minecraft 1.2.5 Full Retro on GraalVM Native Image. */
public final class DesktopMain {
    private DesktopMain() {
    }

    public static void main(String[] args) {
        try {
            Runtime.boot();
        } catch (Throwable t) {
            System.err.println("[DesktopMain] Warning during Runtime.boot(): " + t);
            t.printStackTrace();
        }
        String user = (args != null && args.length > 0) ? args[0] : Runtime.username();
        String session = (args != null && args.length > 1) ? args[1] : "-";
        System.out.println("[DesktopMain] Launching Minecraft with user: " + user);
        Minecraft.main(new String[] { user, session });
    }
}
