package retro.glue;

import net.minecraft.client.Minecraft;
import retro.JS;

/** Page entry point: sets up the browser runtime, then launches Minecraft (via FML) as the desktop jar does. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            retro.Runtime.boot();
        } catch (Throwable t) {
            t.printStackTrace();
            JS.fatal("Start-up failed: " + t);
            return;
        }
        Minecraft.main(new String[] { retro.Runtime.username(), "-" });
    }
}
