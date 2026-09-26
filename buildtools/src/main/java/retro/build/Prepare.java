package retro.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Turns the user's Minecraft jar plus the modpack into inputs for TeaVM.
 *
 * <p>Outputs (in the output directory):
 * <ul>
 *   <li>{@code game.jar} - merged, patched classes (Minecraft + jar mods + mods)</li>
 *   <li>{@code classes.txt} - every merged class (reflection is enabled for these)</li>
 *   <li>{@code byname.txt} - classes that must be loadable through {@code Class.forName}</li>
 *   <li>{@code web/assets.pak} - classpath resources, looked up by {@code getResourceAsStream}</li>
 *   <li>{@code web/fs.pak} - initial contents of the in-browser {@code .minecraft} directory</li>
 * </ul>
 */
public final class Prepare {
    /** Same pattern FML's Loader uses to discover ModLoader-style mods. */
    static final Pattern MOD_CLASS = Pattern.compile("(.+/|)(mod\\_[^\\s$]+).class$");
    static final Pattern ZIP_JAR = Pattern.compile("(.+).(zip|jar)$");

    /** Resource folders under .minecraft/resources that Minecraft 1.2.5 actually installs. */
    static final Set<String> SOUND_ROOTS = Set.of("sound", "newsound", "streaming", "music", "newmusic", "mod");

    final Map<String, byte[]> classes = new LinkedHashMap<>();
    final Map<String, String> classOrigin = new HashMap<>();
    final Map<String, byte[]> resources = new LinkedHashMap<>();
    final Set<String> byName = new TreeSet<>();
    final List<String> log = new ArrayList<>();

    private Prepare() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            opts.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        }
        Path jar = Path.of(opts.getOrDefault("jar", "input/minecraft.jar"));
        Path jarMods = Path.of(opts.getOrDefault("jarmods", "input/jarmods"));
        Path modpack = Path.of(opts.getOrDefault("modpack", "modpack"));
        Path userResources = Path.of(opts.getOrDefault("resources", "input/resources"));
        Path out = Path.of(opts.getOrDefault("out", "build/prepared"));
        List<Path> libraries = new ArrayList<>();
        for (String lib : opts.getOrDefault("libraries", "").split(java.io.File.pathSeparator)) {
            if (!lib.isBlank()) {
                libraries.add(Path.of(lib));
            }
        }
        Set<String> excluded = new TreeSet<>();
        for (String s : opts.getOrDefault("exclude-mods", "").split(",")) {
            if (!s.isBlank()) {
                excluded.add(s.trim());
            }
        }

        if (!Files.isRegularFile(jar)) {
            System.err.println();
            System.err.println("Missing " + jar + ".");
            System.err.println("Copy your own Minecraft 1.2.5 client jar (with Forge installed, as used by the");
            System.err.println("Full Retro pack - .minecraft/bin/minecraft.jar) to " + jar + " and build again.");
            System.err.println("See input/README.md.");
            System.exit(2);
        }

        new Prepare().run(jar, jarMods, modpack, userResources, out, excluded, libraries);
    }

    private void run(Path jar, Path jarMods, Path modpack, Path userResources, Path out, Set<String> excluded,
            List<Path> libraries) throws IOException {
        Files.createDirectories(out.resolve("web"));

        // 1. The game jar, with jar mods layered on top (like the classic "delete META-INF" install): first the
        // modpack's own (modpack/jarmods), then the user's extras (input/jarmods), each in name order.
        Map<String, byte[]> gameEntries = readZip(jar);
        Set<String> jarModClasses = new java.util.HashSet<>();
        for (Path dir : List.of(modpack.resolve("jarmods"), jarMods)) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            for (Path p : sortedChildren(dir)) {
                if (ZIP_JAR.matcher(p.getFileName().toString()).matches()) {
                    log.add("jar mod: " + p.getFileName());
                    Map<String, byte[]> entries = readZip(p);
                    gameEntries.putAll(entries);
                    for (String n : entries.keySet()) {
                        if (n.endsWith(".class")) {
                            jarModClasses.add(n.substring(0, n.length() - 6));
                        }
                    }
                }
            }
        }
        gameEntries.keySet().removeIf(n -> n.startsWith("META-INF/"));
        List<String> gameModClasses = new ArrayList<>();
        for (var e : gameEntries.entrySet()) {
            add(e.getKey(), e.getValue(), "minecraft.jar");
            Matcher m = MOD_CLASS.matcher(e.getKey());
            if (m.matches()) {
                gameModClasses.add(m.group(1).replace('/', '.') + m.group(2));
            }
        }

        // 2a. Libraries in .minecraft/bin besides minecraft.jar (e.g. WorldEdit.jar for Single Player Commands):
        //     on the classpath after minecraft.jar, not scanned by FML.
        List<ModSource> binLibraries = new ArrayList<>();
        Path binDir = modpack.resolve("bin");
        if (Files.isDirectory(binDir)) {
            for (Path p : sortedChildren(binDir)) {
                if (ZIP_JAR.matcher(p.getFileName().toString()).matches()) {
                    ModSource lib = ModSource.fromZip(p, "bin/" + p.getFileName());
                    binLibraries.add(lib);
                    for (var e : lib.entries.entrySet()) {
                        add(e.getKey(), e.getValue(), lib.name);
                    }
                    log.add("library: " + lib.name);
                }
            }
        }

        // 2b. Mods, in the order FML scans them (File.compareTo == path string order).
        Path modsDir = modpack.resolve("mods");
        List<ModSource> mods = new ArrayList<>();
        if (Files.isDirectory(modsDir)) {
            for (Path p : sortedChildren(modsDir)) {
                String name = p.getFileName().toString();
                if (excluded.contains(name)) {
                    log.add("excluded mod: " + name);
                    continue;
                }
                if (Files.isDirectory(p)) {
                    mods.add(ModSource.fromDirectory(p));
                } else if (ZIP_JAR.matcher(name).matches()) {
                    mods.add(ModSource.fromZip(p));
                }
            }
        }
        for (ModSource mod : mods) {
            for (var e : mod.entries.entrySet()) {
                add(e.getKey(), e.getValue(), mod.name);
            }
            byName.addAll(mod.modClasses);
            log.add("mod: " + mod.name + " " + mod.modClasses);
        }
        byName.addAll(gameModClasses);

        // 3a. Mods that redefine Minecraft classes at runtime (impossible in compiled JavaScript): apply their
        //     replacement classes now; their runtime patch method is turned into a no-op (see Patches.EMPTIED).
        for (var patch : Patches.RUNTIME_CLASS_PATCHES.entrySet()) {
            String prefix = patch.getValue();
            for (var e : resources.entrySet()) {
                String path = e.getKey();
                if (path.startsWith(prefix) && path.endsWith(".class")) {
                    String name = new org.objectweb.asm.ClassReader(e.getValue()).getClassName();
                    if (classes.containsKey(name)) {
                        classes.put(name, e.getValue());
                        log.add("runtime patch applied ahead of time (" + patch.getKey() + "): " + name);
                    }
                }
            }
        }

        // 3b. Our replacements for classes that cannot work in a browser as-is.
        for (String replaced : Patches.REPLACED_CLASSES) {
            if (classes.remove(replaced) != null) {
                log.add("replaced: " + replaced);
            }
        }

        // 4. Drop classes that could never be defined (missing superclass/interfaces), like a JVM would.
        if (!libraries.isEmpty()) {
            Loadability loadability = new Loadability(classes, libraries);
            List<String> unloadable = new ArrayList<>();
            for (String name : classes.keySet()) {
                if (!loadability.isLoadable(name)) {
                    unloadable.add(name);
                }
            }
            // Classes a JVM would reject at verification time (they need a missing class to type-check).
            // Checked before anything is dropped, so the verifier sees every class a desktop JVM would.
            Verification verification = new Verification(classes, loadability);
            Set<String> rejected = new TreeSet<>(verification.run());
            rejected.removeAll(unloadable);
            for (String name : unloadable) {
                classes.remove(name);
                byName.remove(name.replace('/', '.'));
                log.add("  unloadable (missing supertype): " + name + " from " + classOrigin.get(name));
            }
            log.add("unloadable classes dropped: " + unloadable.size());
            for (String name : rejected) {
                classes.remove(name);
                byName.remove(name.replace('/', '.'));
                log.add("  fails verification (needs " + verification.reason(name) + "): " + name + " from "
                        + classOrigin.get(name));
            }
            log.add("classes failing verification dropped: " + rejected.size());

            // Missing types used in member signatures, and missing enclosing classes of nested classes, get
            // empty stub classes: class metadata (all fields are reflectable; nested classes name their
            // declaring class) must be able to name them. Using them still fails lazily, as on a JVM.
            Set<String> stubs = new TreeSet<>();
            for (byte[] bytes : classes.values()) {
                new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(
                        org.objectweb.asm.Opcodes.ASM9) {
                    String self;

                    @Override
                    public void visit(int version, int access, String name, String sig, String superName,
                            String[] itfs) {
                        self = name;
                    }

                    @Override
                    public void visitOuterClass(String owner, String name, String desc) {
                        check(org.objectweb.asm.Type.getObjectType(owner));
                    }

                    @Override
                    public void visitInnerClass(String name, String outerName, String innerName, int access) {
                        if (name.equals(self) && outerName != null) {
                            check(org.objectweb.asm.Type.getObjectType(outerName));
                        }
                    }

                    void check(org.objectweb.asm.Type t) {
                        while (t.getSort() == org.objectweb.asm.Type.ARRAY) {
                            t = t.getElementType();
                        }
                        if (t.getSort() == org.objectweb.asm.Type.OBJECT) {
                            String n = t.getInternalName();
                            if (!classes.containsKey(n) && !loadability.isProvided(n)
                                    && !Patches.REPLACED_CLASSES.contains(n)) {
                                stubs.add(n);
                            }
                        }
                    }

                    @Override
                    public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc,
                            String sig, Object value) {
                        check(org.objectweb.asm.Type.getType(desc));
                        return null;
                    }

                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                            String sig, String[] exc) {
                        check(org.objectweb.asm.Type.getReturnType(desc));
                        for (org.objectweb.asm.Type t : org.objectweb.asm.Type.getArgumentTypes(desc)) {
                            check(t);
                        }
                        return null;
                    }
                }, org.objectweb.asm.ClassReader.SKIP_CODE);
            }
            for (String stub : stubs) {
                // JDK types must use the class library's naming (java.io.Foo -> org.teavm.classlib.java.io.TFoo).
                String stubName = stub;
                if (stub.startsWith("java/") || stub.startsWith("javax/")) {
                    int slash = stub.lastIndexOf('/');
                    stubName = "org/teavm/classlib/" + stub.substring(0, slash + 1) + "T" + stub.substring(slash + 1);
                }
                org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
                cw.visit(org.objectweb.asm.Opcodes.V1_6, org.objectweb.asm.Opcodes.ACC_PUBLIC, stubName, null,
                        "java/lang/Object", null);
                cw.visitEnd();
                classes.put(stubName, cw.toByteArray());
                classOrigin.put(stub, "(stub)");
                log.add("  stub for missing signature type: " + stub);
            }
        }

        // 5. Patch bytecode and collect names used with Class.forName-style lookups.
        Hierarchy hierarchy = new Hierarchy(classes);
        Map<String, byte[]> patched = new LinkedHashMap<>();
        Set<String> strings = new java.util.HashSet<>();
        for (var e : classes.entrySet()) {
            patched.put(e.getKey(),
                    Patches.transform(e.getKey(), e.getValue(), hierarchy, byName, classes.keySet(), strings));
        }
        // ChunkProvider: int-keyed front cache for chunk lookups (see RetroChunkCache in gameglue).
        if (patched.containsKey("ko")) {
            byte[] fast = Patches.chunkProviderCache(patched.get("ko"));
            if (fast != null) {
                patched.put("ko", fast);
                log.add("ChunkProvider: chunk lookups go through RetroChunkCache");
            } else {
                log.add("ChunkProvider: unexpected shape, chunk cache not installed");
            }
        }
        // Class names built at runtime from a package plus a constant suffix (IC2: getPackage() + ".common.X"):
        // classes whose dotted name ends with a string constant of the form ".a.B" or "a.B" become loadable by name.
        Map<String, List<String>> bySuffix = new HashMap<>();
        for (String cls : classes.keySet()) {
            String dotted = cls.replace('/', '.');
            for (int i = dotted.indexOf('.'); i > 0; i = dotted.indexOf('.', i + 1)) {
                bySuffix.computeIfAbsent(dotted.substring(i), k -> new ArrayList<>()).add(dotted);
                bySuffix.computeIfAbsent(dotted.substring(i + 1), k -> new ArrayList<>()).add(dotted);
            }
        }
        for (String str : strings) {
            if (str.length() > 4 && str.indexOf('.') >= 0 && str.indexOf(' ') < 0) {
                List<String> matches = bySuffix.get(str);
                if (matches != null && matches.size() <= 4) {
                    byName.addAll(matches);
                }
            }
        }

        // As on a JVM, any class in a mod can be loaded by name (Forestry, for example, discovers its plugins by
        // listing its own jar). Minecraft's own classes are only loadable by name when referenced as above (jar
        // mods' classes are, though).
        // That includes jars FML finds no mod in: add-ons such as NEI's plugins and Forestry's IC2 crops are
        // discovered by their host mod listing the mods folder. Excluded: third-party libraries bundled in mods.
        Set<String> modSources = new java.util.HashSet<>();
        for (ModSource mod : mods) {
            modSources.add(mod.name);
        }
        for (ModSource lib : binLibraries) {
            modSources.add(lib.name);
        }
        for (var e : classOrigin.entrySet()) {
            String name = e.getKey();
            // Jar mods' own classes count as mod classes (NEI finds its configuration classes by scanning).
            boolean fromMod = modSources.contains(e.getValue()) || jarModClasses.contains(name);
            if (fromMod && classes.containsKey(name) && !name.contains("package-info")
                    && !isBundledLibrary(name)) {
                byName.add(name.replace('/', '.'));
            }
        }

        // Methods that may be looked up by name through reflection: those named by some string constant, and
        // custom serialization hooks (called by our ObjectOutputStream/ObjectInputStream). Short names (up to 3
        // characters, i.e. obfuscated ones such as "a") are so common as strings (recipe patterns, etc.) that they
        // would make most of the game reflectable; those count only near code that looks methods up, and only
        // for the classes that code refers to (and, for inherited methods, their superclasses).
        Set<String> lookupStrings = new java.util.HashSet<>();
        Map<String, Set<String>> lookupOwners = new HashMap<>();
        // Scope: a named package (mods keep obfuscated names in constant classes next to their reflection
        // helper, e.g. Mouse Tweaks), or the class itself in the default package (Minecraft, ModLoader mods).
        Map<String, Set<String>> scopeStrings = new HashMap<>();
        Map<String, Set<String>> scopeTargets = new HashMap<>();
        for (var entry : classes.entrySet()) {
            byte[] bytes = entry.getValue();
            String cname = entry.getKey();
            int slash = cname.lastIndexOf('/');
            String scope = slash < 0 ? cname : cname.substring(0, slash);
            Set<String> shortStrings = scopeStrings.computeIfAbsent(scope, k -> new java.util.HashSet<>());
            boolean[] looksUp = {false};
            new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(
                    org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                        String sig, String[] exc) {
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof String str && str.length() <= 3) {
                                shortStrings.add(str);
                            }
                        }

                        @Override
                        public void visitMethodInsn(int op, String owner, String mname, String mdesc,
                                boolean itf) {
                            if (owner.equals("java/lang/Class") && (mname.equals("getMethod")
                                    || mname.equals("getDeclaredMethod") || mname.equals("getMethods")
                                    || mname.equals("getDeclaredMethods"))) {
                                looksUp[0] = true;
                            }
                        }
                    };
                }
            }, org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
            if (!looksUp[0]) {
                continue;
            }
            // The classes a lookup class refers to (constant-pool class entries): a reflective lookup by a
            // short name targets one of them, or a subclass whose method is declared in one of them.
            Set<String> targets = scopeTargets.computeIfAbsent(scope, k -> new java.util.HashSet<>());
            org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytes);
            char[] buf = new char[cr.getMaxStringLength()];
            for (int i = 1; i < cr.getItemCount(); i++) {
                int offset = cr.getItem(i);
                if (offset != 0 && cr.readByte(offset - 1) == 7) {
                    String cls = cr.readUTF8(offset, buf);
                    if (cls != null && !cls.startsWith("[") && classes.containsKey(cls)) {
                        targets.add(cls);
                    }
                }
            }
        }
        for (var e : scopeTargets.entrySet()) {
            for (String str : scopeStrings.getOrDefault(e.getKey(), Set.of())) {
                lookupOwners.computeIfAbsent(str, k -> new java.util.HashSet<>()).addAll(e.getValue());
            }
        }
        List<String> reflectMethods = new ArrayList<>();
        for (var e : classes.entrySet()) {
            new org.objectweb.asm.ClassReader(e.getValue()).accept(new org.objectweb.asm.ClassVisitor(
                    org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                        String sig, String[] exc) {
                    boolean serialHook = (name.equals("writeObject") && desc.equals("(Ljava/io/ObjectOutputStream;)V"))
                            || (name.equals("readObject") && desc.equals("(Ljava/io/ObjectInputStream;)V"));
                    boolean named;
                    if (name.length() > 3) {
                        named = strings.contains(name);
                    } else {
                        named = lookupStrings.contains(name);
                        for (String owner : lookupOwners.getOrDefault(name, Set.of())) {
                            named |= hierarchy.isSubclass(owner, e.getKey());
                        }
                    }
                    String entry = e.getKey().replace('/', '.') + " " + name;
                    if (!name.startsWith("<") && (named || serialHook)) {
                        reflectMethods.add(entry);
                        return null;
                    }
                    // Methods with runtime-visible annotations are found reflectively (WorldEdit's @Command).
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        boolean added;

                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                            if (visible && !added && !name.startsWith("<")) {
                                added = true;
                                reflectMethods.add(entry);
                            }
                            return null;
                        }
                    };
                }
            }, org.objectweb.asm.ClassReader.SKIP_CODE);
        }
        log.add("methods reflectable by name: " + reflectMethods.size());

        // Generic superclass signatures of TypeToken subclasses (Gson/Guava read them via getGenericSuperclass).
        StringBuilder generics = new StringBuilder();
        for (var e : classes.entrySet()) {
            org.objectweb.asm.ClassReader r = new org.objectweb.asm.ClassReader(e.getValue());
            String superName = r.getSuperName();
            if (superName != null && superName.endsWith("/TypeToken")) {
                String[] signature = new String[1];
                r.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                    @Override
                    public void visit(int version, int access, String name, String sig, String sup, String[] itf) {
                        signature[0] = sig;
                    }
                }, org.objectweb.asm.ClassReader.SKIP_CODE);
                if (signature[0] != null) {
                    generics.append(e.getKey().replace('/', '.')).append('\t').append(signature[0]).append('\n');
                    byName.add(e.getKey().replace('/', '.'));
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("L([^<;]+)").matcher(signature[0]);
                    while (m.find()) {
                        String ref = m.group(1);
                        if (classes.containsKey(ref) || ref.startsWith("java/")) {
                            byName.add(ref.replace('/', '.'));
                        }
                    }
                }
            }
        }
        resources.put("retro/generic-supers.txt", generics.toString().getBytes(StandardCharsets.UTF_8));

        // Which jar each class came from (for mods that look up their own code source, or list their own jar
        // through getResource("X.class")). Classes from minecraft.jar and jar mods have an empty origin.
        StringBuilder origins = new StringBuilder();
        for (var e : classOrigin.entrySet()) {
            if (e.getValue().equals("(stub)") || !patched.containsKey(e.getKey())) {
                continue;
            }
            String origin = e.getValue().equals("minecraft.jar") ? "" : e.getValue();
            origins.append(e.getKey().replace('/', '.')).append('\t').append(origin).append('\n');
        }
        resources.put("retro/origins.txt", origins.toString().getBytes(StandardCharsets.UTF_8));

        // 6. Write everything.
        writeJar(out.resolve("game.jar"), patched);
        Files.write(out.resolve("classes.txt"),
                classes.keySet().stream().map(n -> n.replace('/', '.')).sorted().toList());
        Files.write(out.resolve("byname.txt"), byName.stream().sorted().toList());
        Files.write(out.resolve("methods.txt"), reflectMethods);
        Pak.write(out.resolve("web/assets.pak"), resources);

        FsImage fs = new FsImage();
        fs.addModpack(modpack);
        fs.addStubJar("bin/minecraft.jar", gameEntries);
        for (ModSource lib : binLibraries) {
            fs.addStubJar(lib.name, lib.entries);
        }
        for (ModSource mod : mods) {
            fs.addMod(mod);
        }
        fs.addSounds(userResources, modpack.resolve("resources"), SOUND_ROOTS);
        fs.write(out.resolve("web"));

        log.add("classes: " + classes.size() + ", resources: " + resources.size() + ", by-name: " + byName.size());
        Files.write(out.resolve("prepare.log"), log);
        for (String line : log) {
            System.out.println(line);
        }
    }

    private void add(String entry, byte[] data, String origin) {
        if (entry.endsWith("/")) {
            return;
        }
        if (entry.endsWith(".class") && !isClassAtPath(entry, data)) {
            // A class file stored under another path (e.g. patched classes a mod applies at runtime): a resource.
            if (!resources.containsKey(entry)) {
                resources.put(entry, data);
            }
        } else if (entry.endsWith(".class")) {
            String name = entry.substring(0, entry.length() - 6);
            if (!classes.containsKey(name)) {
                classes.put(name, data);
                classOrigin.put(name, origin);
            } else if (!origin.equals(classOrigin.get(name))) {
                log.add("  shadowed class " + name + " from " + origin + " (using " + classOrigin.get(name) + ")");
            }
        } else if (isNativeLibrary(entry)) {
            return;
        } else if (!resources.containsKey(entry)) {
            resources.put(entry, data);
        }
    }

    private static final List<String> LIBRARY_PREFIXES = List.of("com/google/", "org/apache/", "org/yaml/",
            "org/json/", "org/newsclub/", "external/", "org/slf4j/", "org/objectweb/", "javax/", "com/sun/",
            "org/bukkit/", "org/spout/", "de/schlichtherle/", "io/netty/", "kotlin/", "org/intellij/",
            "org/jetbrains/", "com/jagrosh/", "club/minnced/", "org/mozilla/", "net/java/");

    static boolean isBundledLibrary(String name) {
        for (String prefix : LIBRARY_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    static boolean isClassAtPath(String entry, byte[] data) {
        try {
            return new org.objectweb.asm.ClassReader(data).getClassName().equals(entry.substring(0, entry.length() - 6));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Desktop native libraries bundled by some mods (e.g. Discord IPC) are useless in a browser. */
    static boolean isNativeLibrary(String entry) {
        String e = entry.toLowerCase(java.util.Locale.ROOT);
        return e.endsWith(".dll") || e.endsWith(".so") || e.endsWith(".dylib") || e.endsWith(".jnilib")
                || e.endsWith(".exe");
    }

    static List<Path> sortedChildren(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.sorted(Comparator.comparing(Path::toString)).toList();
        }
    }

    static Map<String, byte[]> readZip(Path p) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (ZipInputStream z = new ZipInputStream(Files.newInputStream(p))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (!e.isDirectory()) {
                    result.put(e.getName().replace('\\', '/'), z.readAllBytes());
                }
            }
        }
        return result;
    }

    static void writeJar(Path path, Map<String, byte[]> classes) throws IOException {
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(path))) {
            for (var e : classes.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey() + ".class"));
                z.write(e.getValue());
                z.closeEntry();
            }
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.transferTo(out);
        return out.toByteArray();
    }

    /** A mod file or directory from the mods folder. */
    static final class ModSource {
        final String name;
        final boolean directory;
        final Map<String, byte[]> entries = new LinkedHashMap<>();
        final List<String> modClasses = new ArrayList<>();
        /** True when the mod's code opens jar/zip files or its code source, so it may read its own jar. */
        boolean readsOwnJar;

        private ModSource(String name, boolean directory) {
            this.name = name;
            this.directory = directory;
        }

        static ModSource fromZip(Path p) throws IOException {
            return fromZip(p, p.getFileName().toString());
        }

        static ModSource fromZip(Path p, String name) throws IOException {
            ModSource m = new ModSource(name, false);
            m.entries.putAll(readZip(p));
            m.detectSelfReading();
            for (String n : m.entries.keySet()) {
                Matcher matcher = MOD_CLASS.matcher(n);
                if (matcher.matches()) {
                    m.modClasses.add(matcher.group(1).replace('/', '.') + matcher.group(2));
                }
            }
            return m;
        }

        static ModSource fromDirectory(Path p) throws IOException {
            ModSource m = new ModSource(p.getFileName().toString(), true);
            try (Stream<Path> s = Files.walk(p)) {
                for (Path f : s.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList()) {
                    String rel = p.relativize(f).toString().replace(File.separatorChar, '/');
                    m.entries.put(rel, Files.readAllBytes(f));
                    int slash = rel.lastIndexOf('/');
                    Matcher matcher = MOD_CLASS.matcher(rel.substring(slash + 1));
                    if (matcher.find()) {
                        String pkg = slash < 0 ? "" : rel.substring(0, slash + 1).replace('/', '.');
                        m.modClasses.add(pkg + matcher.group(2));
                    }
                }
            }
            return m;
        }

        private void detectSelfReading() {
            for (var e : entries.entrySet()) {
                if (e.getKey().endsWith(".class")) {
                    String text = new String(e.getValue(), StandardCharsets.ISO_8859_1);
                    if (text.contains("java/util/zip/ZipFile") || text.contains("java/util/zip/ZipInputStream")
                            || text.contains("java/util/jar/JarFile") || text.contains("getProtectionDomain")) {
                        readsOwnJar = true;
                        return;
                    }
                }
            }
        }
    }

    static String utf8(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    static <T> List<T> list(T[] a) {
        return a == null ? List.of() : Arrays.asList(a);
    }
}
