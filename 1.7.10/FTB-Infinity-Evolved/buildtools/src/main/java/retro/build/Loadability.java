package retro.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.objectweb.asm.ClassReader;

/**
 * Decides which game classes can be defined in the browser. A class whose superclass or interfaces do not exist
 * (neither in the game/mods nor in TeaVM's class library or our shims) could never be loaded on a JVM either;
 * such classes are dropped, so references to them fail lazily with NoClassDefFoundError as on desktop.
 */
final class Loadability {
    private final Map<String, byte[]> classes;
    private final Set<String> provided = new HashSet<>();
    private final Map<String, Boolean> memo = new HashMap<>();

    Loadability(Map<String, byte[]> classes, List<Path> libraries) throws IOException {
        this.classes = classes;
        for (Path lib : libraries) {
            if (Files.isRegularFile(lib)) {
                try (ZipInputStream z = new ZipInputStream(Files.newInputStream(lib))) {
                    ZipEntry e;
                    while ((e = z.getNextEntry()) != null) {
                        addProvided(e.getName());
                    }
                }
            }
        }
    }

    private void addProvided(String entry) {
        if (!entry.endsWith(".class")) {
            return;
        }
        String name = entry.substring(0, entry.length() - 6);
        provided.add(name);
        String prefix = "org/teavm/classlib/";
        if (name.startsWith(prefix)) {
            String rest = name.substring(prefix.length());
            int slash = rest.lastIndexOf('/');
            String simple = rest.substring(slash + 1);
            if (simple.startsWith("T")) {
                provided.add(rest.substring(0, slash + 1) + simple.substring(1));
            }
        }
    }

    /** True if the class exists in the runtime (TeaVM class library or our shims). */
    boolean isProvided(String name) {
        return provided.contains(name);
    }

    boolean isLoadable(String name) {
        Boolean known = memo.get(name);
        if (known != null) {
            return known;
        }
        byte[] bytes = classes.get(name);
        if (bytes == null) {
            boolean ok = provided.contains(name) || Patches.REPLACED_CLASSES.contains(name);
            memo.put(name, ok);
            return ok;
        }
        memo.put(name, true); // guards against cycles in broken class files
        ClassReader reader = new ClassReader(bytes);
        boolean ok = reader.getSuperName() == null || isLoadable(reader.getSuperName());
        for (String itf : reader.getInterfaces()) {
            ok &= isLoadable(itf);
        }
        memo.put(name, ok);
        return ok;
    }

    /** The first superclass or interface (transitively) that does not exist, for diagnostics. */
    String missingSupertype(String name) {
        byte[] bytes = classes.get(name);
        if (bytes == null) {
            return provided.contains(name) || Patches.REPLACED_CLASSES.contains(name) ? null : name;
        }
        ClassReader reader = new ClassReader(bytes);
        java.util.List<String> parents = new java.util.ArrayList<>();
        if (reader.getSuperName() != null) {
            parents.add(reader.getSuperName());
        }
        parents.addAll(java.util.Arrays.asList(reader.getInterfaces()));
        for (String p : parents) {
            if (!isLoadable(p)) {
                String deeper = missingSupertype(p);
                return deeper != null ? deeper : p;
            }
        }
        return null;
    }
}
