package retro.rt;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Which jar each class came from, for mods that locate their own jar through
 * {@code getProtectionDomain().getCodeSource()}. Written by the build as {@code retro/origins.txt}.
 */
public final class Origins {
    private static Map<String, String> origins;

    private Origins() {
    }

    private static synchronized Map<String, String> load() {
        if (origins == null) {
            origins = new HashMap<>();
            byte[] data = Resources.read("retro/origins.txt");
            if (data != null) {
                for (String line : new String(data, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    int tab = line.indexOf('\t');
                    if (tab > 0) {
                        origins.put(line.substring(0, tab), line.substring(tab + 1));
                    }
                }
            }
        }
        return origins;
    }

    /** The (virtual) jar or folder a class was loaded from, or null for classes the game doesn't contain. */
    static File jarFile(String className) {
        String origin = load().get(className);
        if (origin == null) {
            return null;
        }
        if (origin.isEmpty()) {
            return new File(retro.Runtime.minecraftDir(), "bin/minecraft.jar");
        }
        // Libraries are recorded with their bin/ path; mods by their file name in mods/.
        return new File(retro.Runtime.minecraftDir(), origin.startsWith("bin/") ? origin : "mods/" + origin);
    }

    public static URL jarUrl(Class<?> cls) {
        File file = jarFile(cls.getName());
        if (file == null) {
            file = new File(retro.Runtime.minecraftDir(), "bin/minecraft.jar");
        }
        try {
            return new URL("file", "", -1, file.getAbsolutePath() + (file.isDirectory() ? "/" : ""));
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
