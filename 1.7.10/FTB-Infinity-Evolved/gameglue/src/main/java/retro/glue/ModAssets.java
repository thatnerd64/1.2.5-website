package retro.glue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.minecraft.client.resources.ResourcePackFileNotFoundException;
import retro.rt.Resources;

/**
 * What a mod's resource pack serves. All mod assets live in the shared packed classpath store (assets.pak); a mod's
 * pack answers only for the asset domains found in its own jar at build time.
 */
public final class ModAssets {
    private final Set<String> domains;
    private final File source;
    private final String modName;

    public ModAssets(File source, String modName, Set<String> domains) {
        this.source = source;
        this.modName = modName;
        this.domains = domains;
    }

    public Set<String> domains() {
        return domains;
    }

    private boolean owns(String name) {
        if (!name.startsWith("assets/")) {
            return false;
        }
        int slash = name.indexOf('/', 7);
        return slash > 7 && domains.contains(name.substring(7, slash));
    }

    public boolean has(String name) {
        return name.equals("pack.mcmeta") || owns(name) && Resources.exists(name);
    }

    public InputStream open(String name) throws IOException {
        if (name.equals("pack.mcmeta")) {
            return new ByteArrayInputStream(("{\n \"pack\": {\n   \"description\": \"dummy FML pack for " + modName
                    + "\",\n   \"pack_format\": 1\n}\n}").getBytes(StandardCharsets.UTF_8));
        }
        InputStream in = owns(name) ? Resources.open(name) : null;
        if (in == null) {
            throw new ResourcePackFileNotFoundException(source, name);
        }
        return in;
    }
}
