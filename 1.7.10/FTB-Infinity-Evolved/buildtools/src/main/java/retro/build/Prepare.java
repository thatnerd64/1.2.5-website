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
    /**
     * Annotations that say nothing about reflective use: {@code @SideOnly} is on thousands of methods, and counting them
     * as "found by reflection" makes the compiler's reachability analysis blow up.
     */
    static final Set<String> IGNORED_ANNOTATIONS = Set.of("Lcpw/mods/fml/relauncher/SideOnly;",
            "Ljava/lang/Deprecated;", "Lcpw/mods/fml/common/Optional$Method;", "Lcpw/mods/fml/common/Optional$Interface;",
            "Ljavax/annotation/Nonnull;", "Ljavax/annotation/Nullable;", "Ljavax/annotation/CheckForNull;");
    static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
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
        Path aot = Path.of(opts.getOrDefault("aot", "build/aot/classes.jar"));
        Path out = Path.of(opts.getOrDefault("out", "build/prepared"));
        List<Path> resourceJars = new ArrayList<>();
        for (String jar : opts.getOrDefault("resource-jars", "").split(java.io.File.pathSeparator)) {
            if (!jar.isBlank()) {
                resourceJars.add(Path.of(jar));
            }
        }
        List<Path> libraries = new ArrayList<>();
        for (String lib : opts.getOrDefault("libraries", "").split(java.io.File.pathSeparator)) {
            if (!lib.isBlank()) {
                libraries.add(Path.of(lib));
            }
        }
        if (!Files.isRegularFile(aot)) {
            System.err.println("Missing " + aot + ": run tools/aot.sh first (./gradlew aot).");
            System.exit(2);
        }
        Prepare prepare = new Prepare();
        prepare.modFilter = opts.getOrDefault("mod-filter", "");
        for (String jar : opts.getOrDefault("extra-classes", "").split(java.io.File.pathSeparator)) {
            if (!jar.isBlank()) {
                prepare.extraClassJars.add(Path.of(jar));
            }
        }
        prepare.run(aot, resourceJars, opts.containsKey("assets") ? Path.of(opts.get("assets")) : null,
                opts.containsKey("pack") && !opts.get("pack").isBlank() ? Path.of(opts.get("pack")) : null, out, libraries);
    }

    private String modFilter = "";
    /** Library jars whose classes the game uses but the AOT dump skips (lwjgl_util), minus what the runtime provides. */
    private final List<Path> extraClassJars = new ArrayList<>();
    private final Set<String> generatedClasses = new TreeSet<>();
    /** Jar classes the game redefines at run time (from the capture run), as they were in the jar. */
    private final Map<String, byte[]> redefinedOriginals = new HashMap<>();

    /** FNV-1a, 64 bits (the same as retro.compat.GeneratedClasses.hash). */
    static long fnv64(byte[] b) {
        long h = 0xcbf29ce484222325L;
        for (byte x : b) {
            h ^= x & 0xff;
            h *= 0x100000001b3L;
        }
        return h;
    }

    private void run(Path aot, List<Path> resourceJars, Path assets, Path pack, Path out, List<Path> libraries)
            throws IOException {
        Files.createDirectories(out.resolve("web"));

        // 1. Every class after Forge's and the coremods' transformers (see tools/aot.sh).
        for (var e : readZip(aot).entrySet()) {
            add(e.getKey(), e.getValue(), "aot");
        }

        // 1a. Library classes the AOT step leaves to the runtime (it skips all of org.lwjgl), for the parts the runtime
        //     does not implement itself: lwjgl_util's vectors, GLU quadrics, colours (plain Java over GL11)
        if (!extraClassJars.isEmpty()) {
            Set<String> provided = new java.util.HashSet<>();
            if (!libraries.isEmpty()) {
                provided.addAll(readZip(libraries.get(0)).keySet());
            }
            int count = 0;
            for (Path jar : extraClassJars) {
                for (var e : readZip(jar).entrySet()) {
                    if (e.getKey().endsWith(".class") && !provided.contains(e.getKey())
                            && !classes.containsKey(e.getKey().substring(0, e.getKey().length() - 6))) {
                        add(e.getKey(), e.getValue(), jar.getFileName().toString());
                        count++;
                    }
                }
            }
            log.add("library classes added (not provided by the runtime): " + count);
        }

        // 1b. Classes the game generates at run time, recorded by a capture run of the real client (tools/capture.sh).
        //     The browser's ClassLoader.defineClass finds them by a hash of their bytes (retro.compat.GeneratedClasses).
        Path generated = aot.getParent().resolveSibling("capture").resolve("generated.jar");
        if (Files.isRegularFile(generated)) {
            StringBuilder index = new StringBuilder();
            int count = 0;
            for (var e : new java.util.TreeMap<>(readZip(generated)).entrySet()) {
                String entry = e.getKey();
                if (!entry.endsWith(".class")
                        // FML's per-subscriber event handlers: our ASMEventHandler calls subscribers reflectively
                        || entry.substring(entry.lastIndexOf('/') + 1).startsWith("ASMEventHandler_")) {
                    continue;
                }
                String name = entry.substring(0, entry.length() - 6);
                byte[] existing = classes.get(name);
                if (existing != null) {
                    // A class of the same name in a jar: Minecraft's own (renamed at load time, so the capture saw no
                    // class file for it) or one the game redefines before loading it (ForgeMultipart rewrites its
                    // Java traits, e.g. JInventoryTile, into trait form): only the latter, with other bytes, counts
                    if (name.startsWith("net/minecraft/") || Arrays.equals(existing, e.getValue())) {
                        continue;
                    }
                    redefinedOriginals.put(name, existing);
                    classes.put(name, e.getValue());
                    classOrigin.put(name, "generated (redefines the jar's)");
                } else {
                    add(entry, e.getValue(), "generated");
                }
                index.append(String.format("%016x", fnv64(e.getValue()))).append('\t')
                        .append(name.replace('/', '.')).append('\n');
                byName.add(name.replace('/', '.'));
                generatedClasses.add(name.replace('/', '.'));
                count++;
            }
            resources.put("retro/generated.txt", index.toString().getBytes(StandardCharsets.UTF_8));
            log.add("classes generated at run time (from " + generated + "): " + count);
            // The classes whose bytecode mods read at run time (LaunchClassLoader.getClassBytes), by readers other
            // than Forge's own deobfuscation (which ran at build time)
            Path bytesRead = generated.resolveSibling("classbytes.txt");
            if (Files.isRegularFile(bytesRead)) {
                Map<String, String> obfToDeobf = new HashMap<>();
                Path renamed = aot.resolveSibling("renamed.txt");
                if (Files.isRegularFile(renamed)) {
                    for (String line : Files.readAllLines(renamed)) {
                        String[] r = line.split(" ");
                        if (r.length == 2) {
                            obfToDeobf.put(r[0], r[1]);
                        }
                    }
                }
                int packed = 0;
                for (String line : Files.readAllLines(bytesRead)) {
                    String[] f = line.split("\t");
                    String caller = f.length > 1 ? f[1] : "?";
                    // Forge's patcher and the coremods' transformers and loaders ran at build time. CodeChickenLib's
                    // ObfMapping must keep getting null (no class bytes means "obfuscated environment": SRG names).
                    if (caller.startsWith("cpw.mods.fml.") || caller.startsWith("net.minecraftforge.")
                            || caller.startsWith("codechicken.lib.asm.") || caller.contains(".launch.DepLoader")
                            || caller.startsWith("cofh.asm.") || caller.startsWith("chocohead.patcher.")
                            || caller.equals("logisticspipes.asm.LogisticsPipesCoreLoader")) {
                        continue;
                    }
                    // Minecraft's classes are asked for by their obfuscated names on desktop (ForgeMultipart unmaps
                    // the name first); here they carry their deobfuscated ones, which unmapping leaves as they are
                    String internal = obfToDeobf.getOrDefault(f[0], f[0]).replace('.', '/');
                    // (a class the game redefines is read in its jar form: that is what the generator rewrites)
                    byte[] bytes = redefinedOriginals.getOrDefault(internal, classes.get(internal));
                    if (bytes != null) {
                        resources.put("retro/classbytes/" + internal + ".class", bytes);
                        packed++;
                    }
                }
                log.add("class bytecode packed for run-time readers: " + packed);
            }
        } else {
            log.add("no capture run (" + generated + "): classes generated at run time will fail to define");
        }

        // What mod discovery found in each jar (see aot/.../Discovery.java), replayed by our JarDiscoverer.
        Path pluginList = aot.resolveSibling("plugins.txt");
        if (Files.isRegularFile(pluginList)) {
            resources.put("retro/plugins.txt", Files.readAllBytes(pluginList));
        }
        Path containerList = aot.resolveSibling("containers.txt");
        if (Files.isRegularFile(containerList)) {
            resources.put("retro/containers.txt", Files.readAllBytes(containerList));
        }
        Path discovery = aot.resolveSibling("discovery.json");
        if (Files.isRegularFile(discovery)) {
            resources.put("retro/discovery.json", Files.readAllBytes(discovery));
        }

        // 2. Resources the game reads through the class path (textures, lang files, FML's data...). Class files in
        //    these jars were already taken (transformed) from the AOT jar.
        for (Path jar : resourceJars) {
            for (var e : readZip(jar).entrySet()) {
                if (!e.getKey().endsWith(".class") && !e.getKey().startsWith("META-INF/")
                        || e.getKey().startsWith("META-INF/services/")) {
                    add(e.getKey(), e.getValue(), jar.getFileName().toString());
                }
            }
            log.add("resources from " + jar.getFileName());
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
            Map<String, String> missing = new HashMap<>();
            for (String name : unloadable) {
                missing.put(name, loadability.missingSupertype(name));
            }
            for (String name : unloadable) {
                classes.remove(name);
                byName.remove(name.replace('/', '.'));
                log.add("  unloadable (missing supertype " + missing.get(name) + "): " + name + " from "
                        + classOrigin.get(name));
            }
            log.add("unloadable classes dropped: " + unloadable.size());
            // On a JVM such a class still loads (a class literal naming it works: AE2's part list names its
            // OpenComputers P2P tunnel); linking it fails. So it stays, with a static initializer that throws
            // what the verifier would have, and Class.forName(name) throws it too (retro/linkfail.txt).
            StringBuilder linkFail = new StringBuilder();
            for (String name : rejected) {
                String reason = verification.reason(name);
                String type = reason.contains(" ") ? reason.substring(0, reason.indexOf(' ')) : reason;
                classes.put(name, failLinking(classes.get(name), type));
                linkFail.append(name.replace('/', '.')).append('\t').append(type).append('\n');
                log.add("  fails verification (needs " + reason + "): " + name + " from "
                        + classOrigin.get(name));
            }
            resources.put("retro/linkfail.txt", linkFail.toString().getBytes(StandardCharsets.UTF_8));
            log.add("classes failing verification (kept, fail on initialization): " + rejected.size());

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
            // (the compiler has to leave these out of generic reflection metadata: they get no definition in the output)
            java.util.List<String> stubNames = new ArrayList<>();
            for (String stub : stubs) {
                stubNames.add(stub.replace('/', '.'));
            }
            Files.write(out.resolve("stubs.txt"), stubNames);
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
            // Coremod and transformer classes only run at build time (the AOT step); the method names they spell out in
            // strings (to patch Minecraft's code) say nothing about run-time reflection.
            Set<String> sink = isBuildTimeOnly(e.getKey()) ? new java.util.HashSet<>() : strings;
            Set<String> nameSink = isBuildTimeOnly(e.getKey()) ? new java.util.HashSet<>() : byName;
            patched.put(e.getKey(),
                    Patches.transform(e.getKey(), e.getValue(), hierarchy, nameSink, classes.keySet(), sink));
        }
        // Diagnostics: an ItemStack of a null item means some mod's item was never created (a silent failure
        // elsewhere); report where such stacks are made (retro.compat.SystemCompat.checkItem)
        if (patched.containsKey("net/minecraft/item/ItemStack")) {
            patched.put("net/minecraft/item/ItemStack", reportNullItems(patched.get("net/minecraft/item/ItemStack")));
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

        // Every class of every mod jar can be loaded by name (the AOT step recorded them, see Discovery).
        Path modClassList = aot.resolveSibling("modclasses.txt");
        if (Files.isRegularFile(modClassList)) {
            for (String c : Files.readAllLines(modClassList)) {
                if (!c.isBlank() && classes.containsKey(c.replace('.', '/')) && !c.contains("package-info")
                        && !isBundledLibrary(c.replace('.', '/'))) {
                    byName.add(c);
                }
            }
        }
        // Forge and its mods are found by scanning and instantiated through Class.forName, and Forge itself looks
        // classes up by name (event handlers, containers, coremod data), so all of it can be loaded by name.
        for (String name : classes.keySet()) {
            if ((name.startsWith("cpw/mods/") || name.startsWith("net/minecraftforge/"))
                    && !name.contains("package-info")) {
                byName.add(name.replace('/', '.'));
            }
        }

        // Methods that may be looked up by name through reflection: those named by some string constant, and
        // custom serialization hooks (called by our ObjectOutputStream/ObjectInputStream). One- and two-letter
        // names (obfuscated ones such as "a") are so common as strings (recipe patterns, etc.) that they
        // would make most of the game reflectable; those count only near code that looks methods up, and only
        // for the classes that code refers to (and, for inherited methods, their superclasses).
        Set<String> lookupStrings = new java.util.HashSet<>();
        Map<String, Set<String>> lookupOwners = new HashMap<>();
        // Scope: a named package (mods keep obfuscated names in constant classes next to their reflection
        // helper, e.g. Mouse Tweaks), or the class itself in the default package (Minecraft, ModLoader mods).
        Map<String, Set<String>> scopeStrings = new HashMap<>();
        Map<String, Set<String>> scopeTargets = new HashMap<>();
        Map<String, Set<String>> scopeFieldTargets = new HashMap<>();
        Set<String> allFieldClasses = new java.util.HashSet<>();
        Set<String> allMethodClasses = new java.util.HashSet<>();
        // every class literal in game code (classes handed around as Class objects get instantiated reflectively)
        Set<String> classLiterals = new java.util.HashSet<>();
        // classes that call Gson, and per method: the class literals it loads and the classes it calls into
        Set<String> gsonUsers = new java.util.HashSet<>();
        List<String[][]> literalCalls = new ArrayList<>();
        Map<String, Set<String>> namedMemberStrings = new HashMap<>();
        // JDK class literals directly followed by a name (the shape of `X.class, "name"` method lookups; MineTweaker
        // binds script functions to Math.max etc. this way)
        Set<String> jdkPairs = new java.util.HashSet<>();
        for (var entry : classes.entrySet()) {
            byte[] bytes = entry.getValue();
            String cname = entry.getKey();
            int slash = cname.lastIndexOf('/');
            String scope = slash < 0 ? cname : cname.substring(0, slash);
            Set<String> shortStrings = scopeStrings.computeIfAbsent(scope, k -> new java.util.HashSet<>());
            Set<String> namedClasses = new java.util.HashSet<>();
            Set<String> classStrings = new java.util.HashSet<>();
            boolean[] looksUp = {false};
            boolean[] looksUpField = {false};
            new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(
                    org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                        String sig, String[] exc) {
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        String jdkLiteral;
                        // class literals in this method, and whether it looks fields up by name
                        final Set<String> literals = new java.util.HashSet<>();
                        final Set<String> invokedOwners = new java.util.HashSet<>();
                        boolean fieldLookup;
                        boolean methodListing;
                        boolean gson;

                        @Override
                        public void visitEnd() {
                            // a field name built at run time (Railcraft: EntityIDs.class.getField("CART_" + name()))
                            // matches no string constant, and a listing (getDeclaredFields) sees them all: every
                            // field of the classes the method names is reachable
                            if (fieldLookup || gson) {
                                allFieldClasses.addAll(literals);
                            }
                            // a method listing (IC2 finds StringTranslate's instance getter among its declared
                            // methods by return type): every method of the classes the method names
                            if (methodListing) {
                                allMethodClasses.addAll(literals);
                            }
                            // (Gson fills objects field by field: the classes handed to it, see allFieldClasses)
                            if (gson) {
                                gsonUsers.add(cname);
                            }
                            if (!literals.isEmpty() && !invokedOwners.isEmpty()) {
                                literalCalls.add(new String[][] {literals.toArray(new String[0]),
                                        invokedOwners.toArray(new String[0])});
                            }
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof org.objectweb.asm.Type t && t.getSort() == org.objectweb.asm.Type.OBJECT) {
                                literals.add(t.getInternalName());
                                classLiterals.add(t.getInternalName());
                            } else if (value instanceof org.objectweb.asm.Type t && t.getSort() == org.objectweb.asm.Type.ARRAY
                                    && t.getElementType().getSort() == org.objectweb.asm.Type.OBJECT) {
                                literals.add(t.getElementType().getInternalName());
                            }
                            // a class named by a string (Class.forName("tconstruct.TConstruct").getMethod(...)) is a
                            // lookup target like a class literal
                            if (value instanceof String str && str.indexOf('.') > 0 && str.indexOf(' ') < 0
                                    && classes.containsKey(str.replace('.', '/'))) {
                                namedClasses.add(str.replace('.', '/'));
                            }
                            if (value instanceof String str && str.length() <= 40 && IDENTIFIER.matcher(str).matches()) {
                                shortStrings.add(str);
                                classStrings.add(str);
                                if (jdkLiteral != null) {
                                    jdkPairs.add(jdkLiteral + " " + str);
                                }
                            }
                            jdkLiteral = value instanceof org.objectweb.asm.Type t
                                    && t.getSort() == org.objectweb.asm.Type.OBJECT
                                    && t.getInternalName().startsWith("java/") ? t.getInternalName() : null;
                        }

                        @Override
                        public void visitInsn(int op) {
                            jdkLiteral = null;
                        }

                        @Override
                        public void visitVarInsn(int op, int v) {
                            jdkLiteral = null;
                        }

                        @Override
                        public void visitIntInsn(int op, int v) {
                            jdkLiteral = null;
                        }

                        @Override
                        public void visitFieldInsn(int op, String o, String n, String d) {
                            jdkLiteral = null;
                        }

                        @Override
                        public void visitTypeInsn(int op, String t) {
                            jdkLiteral = null;
                        }

                        @Override
                        public void visitMethodInsn(int op, String owner, String mname, String mdesc,
                                boolean itf) {
                            jdkLiteral = null;
                            invokedOwners.add(owner);
                            if (owner.equals("java/lang/Class") && (mname.equals("getMethod")
                                    || mname.equals("getDeclaredMethod") || mname.equals("getMethods")
                                    || mname.equals("getDeclaredMethods"))) {
                                looksUp[0] = true;
                            }
                            if (owner.equals("java/lang/Class") && (mname.equals("getField")
                                    || mname.equals("getDeclaredField"))) {
                                looksUpField[0] = true;
                                fieldLookup = true;
                            }
                            if (owner.equals("java/lang/Class") && (mname.equals("getFields")
                                    || mname.equals("getDeclaredFields"))) {
                                fieldLookup = true;
                            }
                            if (owner.equals("java/lang/Class") && (mname.equals("getMethods")
                                    || mname.equals("getDeclaredMethods"))) {
                                methodListing = true;
                            }
                            if (owner.equals("com/google/gson/Gson") && (mname.equals("fromJson")
                                    || mname.equals("toJson") || mname.equals("toJsonTree"))) {
                                gson = true;
                            }
                        }
                    };
                }
            }, org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
            // A class naming another class in a string, next to names of its members, hands both to a reflection
            // helper somewhere (OpenMods' FieldAccess.create(ReflectionHelper.getClass("x.Y"), "field")): those
            // members of the named class and of its ancestors are reachable by name, whoever does the lookup
            // (only for classes naming a few classes: a registry listing dozens would make everything reachable)
            if (namedClasses.size() <= 8) {
                for (String named : namedClasses) {
                    namedMemberStrings.computeIfAbsent(named, k -> new java.util.HashSet<>()).addAll(classStrings);
                }
            }
            if (!looksUp[0] && !looksUpField[0]) {
                continue;
            }
            // The classes a lookup class refers to (constant-pool class entries): a reflective lookup by a
            // short name targets one of them, or a subclass whose method is declared in one of them.
            Set<String> targets = new java.util.HashSet<>();
            if (looksUp[0]) {
                targets = scopeTargets.computeIfAbsent(scope, k -> new java.util.HashSet<>());
            }
            Set<String> fieldTargets = looksUpField[0]
                    ? scopeFieldTargets.computeIfAbsent(scope, k -> new java.util.HashSet<>()) : new java.util.HashSet<>();
            org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytes);
            char[] buf = new char[cr.getMaxStringLength()];
            for (int i = 1; i < cr.getItemCount(); i++) {
                int offset = cr.getItem(i);
                if (offset != 0 && cr.readByte(offset - 1) == 7) {
                    String cls = cr.readUTF8(offset, buf);
                    if (cls != null && !cls.startsWith("[") && classes.containsKey(cls)) {
                        targets.add(cls);
                        fieldTargets.add(cls);
                    }
                }
            }
            targets.addAll(namedClasses);
            fieldTargets.addAll(namedClasses);
        }
        Map<String, Set<String>> memberNamesByDeclaring = new HashMap<>();
        for (var e : namedMemberStrings.entrySet()) {
            for (String ancestor : hierarchy.ancestors(e.getKey())) {
                memberNamesByDeclaring.computeIfAbsent(ancestor, k -> new java.util.HashSet<>()).addAll(e.getValue());
            }
        }
        for (var e : scopeTargets.entrySet()) {
            for (String str : scopeStrings.getOrDefault(e.getKey(), Set.of())) {
                lookupOwners.computeIfAbsent(str, k -> new java.util.HashSet<>()).addAll(e.getValue());
            }
        }
        // Same for fields: an ordinary name counts only near code that calls Class.getField/getDeclaredField, for the
        // classes that code refers to (a name such as "id" or "x" is a string constant in nearly every mod).
        Map<String, Set<String>> fieldOwners = new HashMap<>();
        for (var e : scopeFieldTargets.entrySet()) {
            for (String str : scopeStrings.getOrDefault(e.getKey(), Set.of())) {
                fieldOwners.computeIfAbsent(str, k -> new java.util.HashSet<>()).addAll(e.getValue());
            }
        }
        List<String> reflectMethods = new ArrayList<>();
        Map<String, Integer> reasonCounts = new java.util.TreeMap<>();
        for (var e : classes.entrySet()) {
            new org.objectweb.asm.ClassReader(e.getValue()).accept(new org.objectweb.asm.ClassVisitor(
                    org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                        String sig, String[] exc) {
                    boolean serialHook = (name.equals("writeObject") && desc.equals("(Ljava/io/ObjectOutputStream;)V"))
                            || (name.equals("readObject") && desc.equals("(Ljava/io/ObjectInputStream;)V"));
                    // SRG names (func_...) are handed to reflection helpers from anywhere, so any string naming one
                    // counts; ordinary names count only near code that looks methods up (scoped by package).
                    boolean named = name.startsWith("func_") && strings.contains(name);
                    // Scala's var setters (proxy_$eq): FML's Scala language adapter finds the setter of a @SidedProxy
                    // field among the class's methods
                    named |= name.endsWith("_$eq") && !e.getKey().startsWith("scala/");
                    // MineTweaker's script runtime looks its own methods up by name from other packages
                    named |= (access & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0
                            && (e.getKey().startsWith("stanhebben/zenscript/") || e.getKey().startsWith("minetweaker/")
                            // cglib finds the one method of its key interfaces by listing them
                            || e.getKey().startsWith("net/sf/cglib/"));
                    // classes generated at run time are driven reflectively by their generators (cglib's
                    // CGLIB$SET_THREAD_CALLBACKS, ...)
                    named |= generatedClasses.contains(e.getKey().replace('/', '.'));
                    named |= memberNamesByDeclaring.getOrDefault(e.getKey(), Set.of()).contains(name);
                    named |= allMethodClasses.contains(e.getKey());
                    for (String owner : lookupOwners.getOrDefault(name, Set.of())) {
                        // the method as inherited by the looked-up class, and its overrides in subclasses (getMethod
                        // on an object's runtime class: Railcraft calls a module's preInit only if the module
                        // overrides it)
                        named |= hierarchy.isAncestor(owner, e.getKey()) || hierarchy.isAncestor(e.getKey(), owner);
                    }
                    String entry = e.getKey().replace('/', '.') + " " + name;
                    if (!name.startsWith("<") && (named || serialHook)) {
                        reasonCounts.merge(serialHook ? "serialization hook" : name.startsWith("func_") ? "srg name in a string"
                                : "scoped name", 1, Integer::sum);
                        reflectMethods.add(entry);
                        return null;
                    }
                    // Methods with runtime-visible annotations are found reflectively (WorldEdit's @Command).
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        boolean added;

                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                            if (visible && !added && !name.startsWith("<") && !IGNORED_ANNOTATIONS.contains(adesc)) {
                                added = true;
                                reasonCounts.merge("annotation " + adesc, 1, Integer::sum);
                                reflectMethods.add(entry);
                            }
                            return null;
                        }
                    };
                }
            }, org.objectweb.asm.ClassReader.SKIP_CODE);
        }
        log.add("methods reflectable by name: " + reflectMethods.size());
        reasonCounts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(14)
                .forEach(en -> log.add("  " + en.getValue() + "  " + en.getKey()));

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
        // Every other class with generic information: Netty resolves the type arguments of its codecs and handlers
        // (class.getTypeParameters(), getGenericSuperclass()) from these at run time.
        for (var e : classes.entrySet()) {
            org.objectweb.asm.ClassReader r = new org.objectweb.asm.ClassReader(e.getValue());
            String superName = r.getSuperName();
            if (superName == null || superName.equals("java/lang/Enum") || superName.endsWith("/TypeToken")) {
                continue;
            }
            String[] signature = new String[1];
            r.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String sig, String sup, String[] itf) {
                    signature[0] = sig;
                }
            }, org.objectweb.asm.ClassReader.SKIP_CODE);
            if (signature[0] != null) {
                generics.append(e.getKey().replace('/', '.')).append('\t').append(signature[0]).append('\n');
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
        // Reflection (every field and constructor) is only enabled where code looks things up reflectively:
        // Minecraft, Forge and the mods. Enabling it for the bundled libraries makes the compiler's reachability
        // analysis blow up.
        java.util.TreeSet<String> reflective = new java.util.TreeSet<>();
        classes.keySet().stream().filter(Prepare::isReflective).forEach(n -> reflective.add(n.replace('/', '.')));
        // our replacements (gameglue) are found by name and constructed reflectively too (resource packs)
        Patches.REPLACED_CLASSES.forEach(n -> reflective.add(n.replace('/', '.')));
        // the boxed types' valueOf is looked up by name by script engines (MineTweaker's type conversions)
        for (String w : new String[] {"Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double", "Character"}) {
            reflective.add("java.lang." + w);
            reflectMethods.add("java.lang." + w + " valueOf");
            for (String unbox : new String[] {"booleanValue", "byteValue", "shortValue", "intValue", "longValue",
                    "floatValue", "doubleValue", "charValue"}) {
                reflectMethods.add("java.lang." + w + " " + unbox);
            }
        }
        // (and the other JDK members MineTweaker's script type conversions look up: Number.xxxValue, toString)
        for (String m : new String[] {"byteValue", "shortValue", "intValue", "longValue", "floatValue", "doubleValue"}) {
            reflectMethods.add("java.lang.Number " + m);
        }
        for (String w : new String[] {"Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double", "Character",
                "String", "Number", "Object"}) {
            for (String m : new String[] {"toString", "valueOf", "equals", "hashCode", "compareTo", "compare",
                    "parseBoolean", "parseByte", "parseShort", "parseInt", "parseLong", "parseFloat", "parseDouble"}) {
                reflectMethods.add("java.lang." + w + " " + m);
            }
        }
        reflective.add("java.lang.Number");
        reflective.add("java.lang.Object");
        reflective.add("java.lang.String");
        reflectMethods.add("java.lang.Object toString");
        reflectMethods.add("java.lang.String toString");
        reflectMethods.add("java.lang.String valueOf");
        // run-time class generators reach ClassLoader.defineClass (and LaunchWrapper's runTransformers) reflectively;
        // the generated classes themselves are constructed reflectively
        reflective.add("java.lang.ClassLoader");
        reflectMethods.add("java.lang.ClassLoader defineClass");
        reflectMethods.add("java.lang.ClassLoader findLoadedClass");
        reflectMethods.add("net.minecraft.launchwrapper.LaunchClassLoader runTransformers");
        reflective.addAll(generatedClasses);
        // (the stand-in for Field.modifiers, see retro.compat.ReflectCompat.getDeclaredField)
        reflective.add("retro.compat.ReflectCompat");
        // public JDK methods named right after their class literal (`Math.class, "max"`)
        java.util.TreeSet<String> jdkMethods = new java.util.TreeSet<>();
        for (String pair : jdkPairs) {
            String cls = pair.substring(0, pair.indexOf(' ')), name = pair.substring(pair.indexOf(' ') + 1);
            Class<?> c;
            try {
                c = Class.forName(cls.replace('/', '.'), false, Prepare.class.getClassLoader());
            } catch (ReflectiveOperationException | LinkageError ex) {
                continue;
            }
            for (java.lang.reflect.Method m : c.getMethods()) {
                if (m.getDeclaringClass() != Object.class && m.getName().equals(name)) {
                    String owner = m.getDeclaringClass().getName();
                    if (jdkMethods.add(owner + " " + name)) {
                        reflective.add(owner);
                        reflectMethods.add(owner + " " + name);
                    }
                }
            }
        }
        log.add("JDK methods reflectable by name: " + jdkMethods);
        // coremod plugins and their setup classes are instantiated by name at start-up
        if (Files.isRegularFile(pluginList)) {
            for (String l : Files.readAllLines(pluginList)) {
                for (String c : l.replace(',', '\t').split("\t")) {
                    if (c.contains(".") && !c.endsWith(".jar") && classes.containsKey(c.trim().replace('.', '/'))) {
                        byName.add(c.trim());
                        reflective.add(c.trim());
                    }
                }
            }
        }
        // the mod containers coremods contribute are instantiated by name (and their jars may not have been scanned)
        if (Files.isRegularFile(containerList)) {
            for (String c : Files.readAllLines(containerList)) {
                if (!c.isBlank() && classes.containsKey(c.trim().replace('.', '/'))) {
                    byName.add(c.trim());
                    reflective.add(c.trim());
                }
            }
        }
        Files.write(out.resolve("classes.txt"), reflective);
        Patches.REPLACED_CLASSES.forEach(n -> byName.add(n.replace('/', '.')));
        // the JDK collection/map types whose generic shape retro.compat.GenericSignatures supplies (Gson resolves
        // Set<X> -> Collection<X> through them by name)
        for (String n : new String[] {"java.lang.Iterable", "java.util.Collection", "java.util.Set", "java.util.List",
                "java.util.Queue", "java.util.Deque", "java.util.SortedSet", "java.util.NavigableSet",
                "java.util.AbstractCollection", "java.util.AbstractList", "java.util.AbstractSequentialList",
                "java.util.AbstractSet", "java.util.ArrayList", "java.util.LinkedList", "java.util.ArrayDeque",
                "java.util.Vector", "java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeSet",
                "java.util.Map", "java.util.SortedMap", "java.util.NavigableMap", "java.util.concurrent.ConcurrentMap",
                "java.util.AbstractMap", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap",
                "java.util.Hashtable", "java.util.concurrent.ConcurrentHashMap", "java.util.SequencedCollection",
                "java.util.SequencedSet", "java.util.SequencedMap", "java.util.Dictionary",
                "net.minecraft.entity.item.EntityPainting$EnumArt", "net.minecraft.block.BlockPressurePlate$Sensitivity"}) {
            byName.add(n);
        }
        Files.write(out.resolve("methods.txt"), reflectMethods);

        // Fields: all of Minecraft's, Forge's and FML's are reflectable (Gson, ObfuscationReflectionHelper...); for everything
        // else (mods, bundled libraries) only fields that carry a run-time annotation or whose name appears as a string.
        // A concrete class with a no-argument constructor that code passes around as a class literal is created with
        // Class.newInstance somewhere (registries, Aroma1997's match criteria): make it constructible by name
        int instantiable = 0;
        for (String literal : classLiterals) {
            byte[] bytes = classes.get(literal);
            if (bytes == null || isBuildTimeOnly(literal)) {
                continue;
            }
            org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytes);
            if ((cr.getAccess() & (org.objectweb.asm.Opcodes.ACC_ABSTRACT | org.objectweb.asm.Opcodes.ACC_INTERFACE)) != 0) {
                continue;
            }
            boolean[] noArg = {false};
            cr.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc, String sig,
                        String[] exc) {
                    if (name.equals("<init>") && desc.equals("()V")) {
                        noArg[0] = true;
                    }
                    return null;
                }
            }, org.objectweb.asm.ClassReader.SKIP_CODE);
            if (noArg[0] && byName.add(literal.replace('/', '.'))) {
                instantiable++;
            }
        }
        log.add("class literals made constructible by name: " + instantiable);
        Files.write(out.resolve("byname.txt"), byName.stream().sorted().toList());
        // A class literal handed to a class that calls Gson (EnderCore's JsonConfigReader(token, file, PlantInfo.class))
        // names a class Gson will fill
        for (String[][] call : literalCalls) {
            for (String owner : call[1]) {
                if (gsonUsers.contains(owner)) {
                    for (String literal : call[0]) {
                        if (classes.containsKey(literal) && !isCoreClass(literal)) {
                            allFieldClasses.add(literal);
                        }
                    }
                    break;
                }
            }
        }
        // Objects Gson fills hold other objects it fills: the game-class types of their fields too, transitively
        {
            java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>(allFieldClasses);
            while (!queue.isEmpty()) {
                byte[] bytes = classes.get(queue.poll());
                if (bytes == null) {
                    continue;
                }
                new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(
                        org.objectweb.asm.Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc,
                            String sig, Object value) {
                        if ((access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0) {
                            return null;
                        }
                        // the field's type, and the classes its generic signature names (a Map<String, Entry>
                        // field holds Entries Gson fills too)
                        java.util.List<String> types = new ArrayList<>();
                        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(desc);
                        if (t.getSort() == org.objectweb.asm.Type.ARRAY) {
                            t = t.getElementType();
                        }
                        if (t.getSort() == org.objectweb.asm.Type.OBJECT) {
                            types.add(t.getInternalName());
                        }
                        if (sig != null) {
                            java.util.regex.Matcher m = Pattern.compile("L([^;<]+)").matcher(sig);
                            while (m.find()) {
                                types.add(m.group(1));
                            }
                        }
                        for (String type : types) {
                            if (classes.containsKey(type) && !isCoreClass(type) && allFieldClasses.add(type)) {
                                queue.add(type);
                            }
                        }
                        return null;
                    }
                }, org.objectweb.asm.ClassReader.SKIP_CODE);
            }
        }
        List<String> reflectFields = new ArrayList<>();
        for (var e : classes.entrySet()) {
            String cname = e.getKey();
            if (isCoreClass(cname) || !isReflective(cname)) {
                continue;
            }
            String dotted = cname.replace('/', '.');
            new org.objectweb.asm.ClassReader(e.getValue()).accept(new org.objectweb.asm.ClassVisitor(
                    org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc, String sig,
                        Object value) {
                    // (MODULE$: the singleton of a Scala object, which FML's Scala language adapter reads by name)
                    boolean named = name.equals("MODULE$") || (name.startsWith("field_") && strings.contains(name));
                    // Public static fields named by a string anywhere: item/block holder classes are read through
                    // name-based APIs whose names come from other mods (IC2Items.getItem("cell") -> Ic2Items.cell)
                    named |= (access & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0
                            && (access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0 && name.length() > 2
                            && strings.contains(name);
                    named |= allFieldClasses.contains(cname);
                    named |= memberNamesByDeclaring.getOrDefault(cname, Set.of()).contains(name);
                    for (String owner : fieldOwners.getOrDefault(name, Set.of())) {
                        named |= hierarchy.isSubclass(owner, cname);
                    }
                    if (named) {
                        reflectFields.add(dotted + " " + name);
                        return null;
                    }
                    return new org.objectweb.asm.FieldVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        boolean added;

                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                            if (visible && !added && !IGNORED_ANNOTATIONS.contains(adesc)) {
                                added = true;
                                reflectFields.add(dotted + " " + name);
                            }
                            return null;
                        }
                    };
                }
            }, org.objectweb.asm.ClassReader.SKIP_CODE);
        }
        reflectFields.add("retro.compat.ReflectCompat modifiersStandIn");
        Files.write(out.resolve("fields.txt"), reflectFields);
        log.add("fields reflectable beyond the core classes: " + reflectFields.size());
        Pak.write(out.resolve("web/assets.pak"), resources);

        FsImage fs = new FsImage();
        if (assets != null && Files.isDirectory(assets)) {
            addAssets(fs, assets);
        }
        // Forge's splash screen draws on a second thread with a shared GL context; there is only one context here.
        fs.addFile("config/splash.properties", "enabled=false\n".getBytes(StandardCharsets.UTF_8));
        // Defaults for a browser: a short view distance, and no usage snooper (it posts to another origin)
        fs.addFile("options.txt", "snooperEnabled:false\nrenderDistance:6\n".getBytes(StandardCharsets.UTF_8));
        // Forge's update check would be an http request to another origin (blocked by CORS)
        fs.addFile("config/forge.cfg", ("# Configuration file\n\ngeneral {\n    B:disableVersionCheck=true\n}\n\n")
                .getBytes(StandardCharsets.UTF_8));
        if (pack != null && Files.isDirectory(pack)) {
            addPack(fs, pack);
        }
        fs.write(out.resolve("web"));

        log.add("classes: " + classes.size() + ", resources: " + resources.size() + ", by-name: " + byName.size());
        Files.write(out.resolve("prepare.log"), log);
        for (String line : log) {
            System.out.println(line);
        }
    }

    /**
     * Mojang's asset index and objects under {@code .minecraft/assets}. Sounds are lazy: the browser fetches a sound
     * the first time it is played.
     */
    private void addAssets(FsImage fs, Path assets) throws IOException {
        Path index = assets.resolve("indexes");
        if (!Files.isDirectory(index)) {
            return;
        }
        try (Stream<Path> s = Files.list(index)) {
            for (Path f : s.toList()) {
                fs.addFile("assets/indexes/" + f.getFileName(), Files.readAllBytes(f));
                String json = Files.readString(f);
                Matcher m = Pattern.compile("\"(minecraft/sounds/[^\"]*)\"\\s*:\\s*\\{\\s*\"hash\"\\s*:\\s*\"([0-9a-f]{40})\"").matcher(json);
                while (m.find()) {
                    Pak.LAZY_PATHS.add("assets/objects/" + m.group(2).substring(0, 2) + "/" + m.group(2));
                }
            }
        }
        Path objects = assets.resolve("objects");
        try (Stream<Path> s = Files.walk(objects)) {
            for (Path f : s.filter(Files::isRegularFile).sorted().toList()) {
                fs.addFile("assets/objects/" + objects.relativize(f).toString().replace('\\', '/'),
                        Files.readAllBytes(f));
            }
        }
        log.add("assets: " + Pak.LAZY_PATHS.size() + " lazy sounds");
    }

    /**
     * The modpack's own files in the game directory: its configs, game modes and scripts, resources, and one file per mod
     * in {@code mods/} (FML scans that folder; what it finds in each file was recorded at build time), empty unless
     * the mod reads jar files itself.
     */
    private void addPack(FsImage fs, Path pack) throws IOException {
        Path mods = pack.resolve("mods");
        if (Files.isDirectory(mods)) {
            try (Stream<Path> s = Files.list(mods)) {
                for (Path f : s.sorted().toList()) {
                    if (ZIP_JAR.matcher(f.getFileName().toString()).matches()
                            && !f.getFileName().toString().matches("(?i)(fastcraft|Patcher|CustomMainMenu).*")
                            && (modFilter.isEmpty() || Pattern.compile(modFilter, Pattern.CASE_INSENSITIVE)
                                    .matcher(f.getFileName().toString()).find())) {
                        // a mod whose code opens jar files (its own: Decocraft's CraftStudio packs, ComputerCraft's
                        // ROM) gets a stub listing its resources; the others an empty file
                        if (FsImage.opensJars(f)) {
                            fs.addListingJar("mods/" + f.getFileName(), f);
                        } else {
                            fs.addFile("mods/" + f.getFileName(), new byte[0]);
                        }
                    }
                }
            }
        }
        // (modpack/: FTBLib's game modes, normal and expert, each with its CraftTweaker scripts; resources/: the
        // pack's own textures, which ResourceLoader serves)
        for (String dir : new String[] { "config", "modpack", "resources" }) {
            Path base = pack.resolve(dir);
            if (!Files.isDirectory(base)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(base)) {
                for (Path f : s.filter(Files::isRegularFile).sorted().toList()) {
                    String rel = pack.relativize(f).toString().replace('\\', '/');
                    if (!rel.equals("config/splash.properties") && !rel.equals("config/forge.cfg")) {
                        fs.addFile(rel, Files.readAllBytes(f));
                    }
                }
            }
        }
        log.add("pack files added to the game directory");
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

    /** ItemStack(Item, int, int) with a call to SystemCompat.checkItem(item) on entry. */
    static byte[] reportNullItems(byte[] bytes) {
        var node = new org.objectweb.asm.tree.ClassNode();
        new org.objectweb.asm.ClassReader(bytes).accept(node, 0);
        for (var m : node.methods) {
            if (m.name.equals("<init>") && m.desc.equals("(Lnet/minecraft/item/Item;II)V")) {
                var call = new org.objectweb.asm.tree.InsnList();
                call.add(new org.objectweb.asm.tree.VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 1));
                call.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC,
                        "retro/compat/SystemCompat", "checkItem", "(Ljava/lang/Object;)V", false));
                m.instructions.insert(call);
                m.maxStack = Math.max(m.maxStack, 1);
            }
        }
        var writer = new org.objectweb.asm.ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    /** The class with a static initializer that throws NoClassDefFoundError(missing), replacing any it had. */
    static byte[] failLinking(byte[] bytes, String missing) {
        var node = new org.objectweb.asm.tree.ClassNode();
        new org.objectweb.asm.ClassReader(bytes).accept(node, 0);
        node.methods.removeIf(m -> m.name.equals("<clinit>"));
        var clinit = new org.objectweb.asm.tree.MethodNode(org.objectweb.asm.Opcodes.ACC_STATIC, "<clinit>", "()V",
                null, null);
        var code = clinit.instructions;
        code.add(new org.objectweb.asm.tree.TypeInsnNode(org.objectweb.asm.Opcodes.NEW, "java/lang/NoClassDefFoundError"));
        code.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.DUP));
        code.add(new org.objectweb.asm.tree.LdcInsnNode(missing));
        code.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                "java/lang/NoClassDefFoundError", "<init>", "(Ljava/lang/String;)V", false));
        code.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.ATHROW));
        clinit.maxStack = 3;
        clinit.maxLocals = 0;
        node.methods.add(clinit);
        var writer = new org.objectweb.asm.ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    static boolean isBuildTimeOnly(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        return n.contains("/asm/") || n.contains("transformer") || n.contains("coremod") || n.contains("preloader")
                || n.contains("loadingplugin") || n.contains("fmlplugin") || n.contains("corepl")
                || n.contains("deobf") || n.startsWith("cpw/mods/fml/relauncher/");
    }

    /** Minecraft, Forge, FML and our replacements: every field is reflectable. */
    static boolean isCoreClass(String name) {
        return name.startsWith("net/minecraft/") || name.startsWith("cpw/mods/fml/") || name.startsWith("net/minecraftforge/")
                || Patches.REPLACED_CLASSES.contains(name);
    }

    static boolean isReflective(String name) {
        for (String prefix : NON_REFLECTIVE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static final List<String> NON_REFLECTIVE_PREFIXES = List.of("com/google/common/",
            "org/apache/", "org/objectweb/", "gnu/trove/", "com/ibm/", "com/jcraft/", "tv/twitch/",
            "net/java/", "org/lwjgl/", "joptsimple/", "LZMA/", "lzma/", "javax/vecmath/");

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
