package net.minecraft.launchwrapper;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Replacement for LaunchWrapper's entry class. In the browser every class is compiled in, so there is nothing to
 * launch; this only holds the shared state that Forge and mods read ({@code Launch.blackboard}, the game directory).
 */
public class Launch {
    public static File minecraftHome;
    public static File assetsDir;
    public static Map<String, Object> blackboard = new HashMap<>();
    public static LaunchClassLoader classLoader = new LaunchClassLoader(new URL[0]);

    static {
        // what LaunchWrapper and FMLTweaker put there on desktop (mods read the deobfuscation flag unguarded:
        // FTBLib, EnderTech)
        blackboard.put("fml.deobfuscatedEnvironment", Boolean.FALSE);
        blackboard.put("Tweaks", new java.util.ArrayList<Object>());
        blackboard.put("TweakClasses", new java.util.ArrayList<String>());
        blackboard.put("ArgumentList", new java.util.ArrayList<String>());
        blackboard.put("launchArgs", new HashMap<String, String>());
    }

    private Launch() {
    }
}
