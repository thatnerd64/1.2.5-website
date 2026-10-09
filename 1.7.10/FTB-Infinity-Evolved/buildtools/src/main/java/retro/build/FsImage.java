package retro.build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The initial contents of {@code .minecraft} in the browser's virtual file system.
 *
 * <p>Mod jars are not shipped as-is: their classes are compiled into the JavaScript bundle. FML still scans the
 * mods folder to discover and order mods, so each mod is represented by a stub jar with the same entry names
 * (class entries are empty; resources keep their data for mods that read their own jar).
 */
final class FsImage {
    private static final Set<String> SKIPPED_MODPACK_ENTRIES = Set.of("mods", "jarmods", "bin", "resources", "README.md", "README-mods.md");

    private final Map<String, byte[]> files = new LinkedHashMap<>();

    void addModpack(Path modpack) throws IOException {
        if (!Files.isDirectory(modpack)) {
            return;
        }
        try (Stream<Path> s = Files.walk(modpack)) {
            for (Path f : s.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList()) {
                String rel = modpack.relativize(f).toString().replace('\\', '/');
                String top = rel.contains("/") ? rel.substring(0, rel.indexOf('/')) : rel;
                if (!SKIPPED_MODPACK_ENTRIES.contains(top)) {
                    files.put(rel, Files.readAllBytes(f));
                }
            }
        }
    }

    void addFile(String path, byte[] data) {
        files.put(path, data);
    }

    void addStubJar(String path, Map<String, byte[]> entries) throws IOException {
        files.put(path, stubZip(entries, false));
    }

    void addMod(Prepare.ModSource mod) throws IOException {
        if (mod.directory) {
            for (var e : mod.entries.entrySet()) {
                boolean isClass = e.getKey().endsWith(".class");
                files.put("mods/" + mod.name + "/" + e.getKey(), isClass ? new byte[0] : e.getValue());
            }
        } else {
            files.put("mods/" + mod.name, stubZip(mod.entries, mod.readsOwnJar));
        }
    }

    /** True if some class in the jar opens jar or zip files (java.util.zip.ZipFile, java.util.jar.JarFile). */
    static boolean opensJars(Path jar) throws IOException {
        byte[] zipFile = "java/util/zip/ZipFile".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        byte[] jarFile = "java/util/jar/JarFile".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            for (var it = zip.entries(); it.hasMoreElements();) {
                ZipEntry e = it.nextElement();
                if (e.getName().endsWith(".class")) {
                    byte[] data = zip.getInputStream(e).readAllBytes();
                    if (contains(data, zipFile) || contains(data, jarFile)) {
                        return true;
                    }
                }
            }
        } catch (java.util.zip.ZipException e) {
            return false;
        }
        return false;
    }

    private static boolean contains(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= data.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * A stub of a mod jar for code that lists it with ZipFile/JarFile: every resource entry, keeping the contents of
     * small text-like files outside {@code assets/} (manifests, mcmod.info, ComputerCraft's Lua ROM, which is under
     * assets but read from the jar) and empty otherwise (textures and sounds are served from the asset packs).
     */
    void addListingJar(String path, Path jar) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (java.util.zip.ZipFile in = new java.util.zip.ZipFile(jar.toFile());
                ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.setLevel(9);
            for (var it = in.entries(); it.hasMoreElements();) {
                ZipEntry e = it.nextElement();
                String name = e.getName();
                if (name.endsWith(".class")) {
                    continue;
                }
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                boolean keep = !e.isDirectory() && e.getSize() < 256 * 1024
                        && (!lower.startsWith("assets/") || lower.contains("/lua/"))
                        && !lower.matches(".*\\.(ogg|wav|mp3|png|gif|jpg|jpeg|zip|jar|sf|rsa|dsa|dll|so|dylib|jnilib)$");
                zip.putNextEntry(new ZipEntry(name));
                if (keep) {
                    zip.write(in.getInputStream(e).readAllBytes());
                }
                zip.closeEntry();
            }
        }
        files.put(path, bytes.toByteArray());
    }

    /** Minecraft's sound folders (from the user's .minecraft/resources plus mod-supplied sounds). */
    void addSounds(Path userResources, Path modpackResources, Set<String> roots) throws IOException {
        for (Path base : List.of(userResources, modpackResources)) {
            if (!Files.isDirectory(base)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(base)) {
                for (Path f : s.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList()) {
                    String rel = base.relativize(f).toString().replace('\\', '/');
                    if (rel.contains("/") && roots.contains(rel.substring(0, rel.indexOf('/')))) {
                        files.put("resources/" + rel, Files.readAllBytes(f));
                    }
                }
            }
        }
    }

    void write(Path web) throws IOException {
        Pak.write(web.resolve("fs.pak"), files);
    }

    private static byte[] stubZip(Map<String, byte[]> entries, boolean keepResources) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.setLevel(9);
            for (var e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                if (keepResources && !e.getKey().endsWith(".class") && !Pak.isLazy(e.getKey())
                        && !Prepare.isNativeLibrary(e.getKey())) {
                    zip.write(e.getValue());
                }
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
