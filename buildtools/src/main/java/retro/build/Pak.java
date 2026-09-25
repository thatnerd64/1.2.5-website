package retro.build;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal archive format read by {@code retro.rt.Pak} in the browser.
 *
 * <pre>
 * "RPK2" int:count
 * count x { int:pathLength byte[pathLength]:utf8Path int:offset int:length }
 * data...
 * </pre>
 * Offsets are relative to the start of the file; all integers are big-endian. An offset of -1 marks a lazy entry:
 * its data is not in the pak but in a separate file ({@code <pakName>.d/<path>}), fetched on first use. Audio is
 * stored this way so it does not delay startup.
 */
final class Pak {
    private Pak() {
    }

    static boolean isLazy(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        return p.endsWith(".ogg") || p.endsWith(".wav") || p.endsWith(".mus") || p.endsWith(".mp3");
    }

    static void write(Path path, Map<String, byte[]> entries) throws IOException {
        Map<String, byte[]> inline = new LinkedHashMap<>();
        Path lazyDir = path.resolveSibling(path.getFileName() + ".d");
        int header = 8;
        for (String name : entries.keySet()) {
            header += 12 + name.getBytes(StandardCharsets.UTF_8).length;
        }
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(path))) {
            out.writeBytes("RPK2");
            out.writeInt(entries.size());
            int offset = header;
            for (var e : entries.entrySet()) {
                byte[] name = e.getKey().getBytes(StandardCharsets.UTF_8);
                out.writeInt(name.length);
                out.write(name);
                if (isLazy(e.getKey())) {
                    Path target = lazyDir.resolve(e.getKey());
                    Files.createDirectories(target.getParent());
                    Files.write(target, e.getValue());
                    out.writeInt(-1);
                } else {
                    inline.put(e.getKey(), e.getValue());
                    out.writeInt(offset);
                    offset += e.getValue().length;
                }
                out.writeInt(e.getValue().length);
            }
            for (byte[] data : inline.values()) {
                out.write(data);
            }
        }
    }
}
