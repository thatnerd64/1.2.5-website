package retro;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import retro.rt.Pak;
import retro.rt.Resources;

/** Desktop GraalVM native runtime: initializes file system and assets for Minecraft 1.2.5. */
public final class Runtime {
    private static String username = "Player";
    private static File minecraftDir;

    private Runtime() {
    }

    public static File minecraftDir() {
        if (minecraftDir == null) {
            // Priority 1: .minecraft in current working directory
            File local = new File(".minecraft");
            if (local.exists() && local.isDirectory()) {
                minecraftDir = local.getAbsoluteFile();
            } else {
                // Priority 2: ~/.minecraft
                String home = System.getProperty("user.home", ".");
                minecraftDir = new File(home, ".minecraft");
            }
        }
        return minecraftDir;
    }

    public static String username() {
        return username;
    }

    public static void setUsername(String name) {
        username = name;
    }

    public static void boot() {
        System.out.println("[Runtime] Initializing Retro Minecraft 1.2.5 Desktop GraalVM runtime...");
        File mc = minecraftDir();
        if (!mc.exists()) {
            mc.mkdirs();
        }
        System.out.println("[Runtime] Minecraft data dir: " + mc.getAbsolutePath());

        // Set Minecraft.aj to mc so Minecraft.b() and FML use our modpack directory
        try {
            java.lang.reflect.Field f = Class.forName("net.minecraft.client.Minecraft").getDeclaredField("aj");
            f.setAccessible(true);
            f.set(null, mc);
            System.out.println("[Runtime] Successfully bound Minecraft.aj data directory to: " + mc.getAbsolutePath());
        } catch (Throwable t) {
            System.err.println("[Runtime] Note: Could not set Minecraft.aj: " + t);
        }

        // Set java.home if not set
        if (System.getProperty("java.home") == null) {
            String javaHome = System.getenv("JAVA_HOME");
            if (javaHome != null && !javaHome.isEmpty()) {
                System.setProperty("java.home", javaHome);
            } else if (new File("/opt/graalvm").exists()) {
                System.setProperty("java.home", "/opt/graalvm");
            }
        }

        // Set LWJGL and JInput native paths if available
        File[] searchDirs = new File[] {
            new File("build/graalvm/lwjgl/natives"),
            new File("lwjgl/natives"),
            new File("../build/graalvm/lwjgl/natives")
        };
        for (File dir : searchDirs) {
            if (dir.exists() && dir.isDirectory()) {
                String abs = dir.getAbsolutePath();
                if (System.getProperty("org.lwjgl.librarypath") == null) {
                    System.setProperty("org.lwjgl.librarypath", abs);
                }
                if (System.getProperty("net.java.games.input.librarypath") == null) {
                    System.setProperty("net.java.games.input.librarypath", abs);
                }
                break;
            }
        }

        // Find and load assets.pak
        Path[] assetPaths = new Path[] {
            Paths.get("assets.pak"),
            Paths.get("build/prepared/web/assets.pak"),
            Paths.get("../build/prepared/web/assets.pak"),
            Paths.get("../../build/prepared/web/assets.pak")
        };
        for (Path p : assetPaths) {
            if (java.nio.file.Files.exists(p)) {
                try {
                    Pak pak = new Pak(p.toAbsolutePath());
                    Resources.init(pak);
                    System.out.println("[Runtime] Successfully mounted assets from: " + p.toAbsolutePath());
                    break;
                } catch (Exception e) {
                    System.err.println("[Runtime] Warning: Could not open assets from " + p + ": " + e);
                }
            }
        }
    }
}
