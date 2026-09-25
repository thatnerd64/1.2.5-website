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

    public static URL jarUrl(Class<?> cls) {
        String origin = load().get(cls.getName());
        File file = origin == null
                ? new File(retro.Runtime.minecraftDir(), "bin/minecraft.jar")
                : new File(retro.Runtime.minecraftDir(), "mods/" + origin);
        try {
            return new URL("file", "", -1, file.getAbsolutePath() + (file.isDirectory() ? "/" : ""));
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
