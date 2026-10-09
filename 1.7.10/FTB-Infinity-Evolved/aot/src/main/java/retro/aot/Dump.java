package retro.aot;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Build-time stand-in for the JVM class loader. Runs inside LaunchWrapper after Forge and every coremod have
 * registered their transformers, brings FML up as far as mod construction (so the transformers that depend on mod
 * discovery have their data), and then pushes every class of every jar through the real transformer chain, writing
 * the result to a jar. The browser has no class loader to transform classes at load time; it gets the result of that
 * transformation instead.
 *
 * <p>System properties: {@code aot.out} (output directory), {@code aot.skip} (comma separated class name prefixes
 * to leave out of the output).
 */
public final class Dump {
    private Dump() {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(System.getProperty("aot.out", "build/aot"));
        out.mkdirs();
        LaunchClassLoader loader = Launch.classLoader;
        PrintWriter log = new PrintWriter(new File(out, "dump.log"), "UTF-8");

        initializeFml(log);
        try {
            Discovery.write(new File(out, "discovery.json"), log);
        } catch (Throwable t) {
            log.println("Discovery FAILED: " + t);
            t.printStackTrace(log);
        }

        Set<String> classLoaderExceptions = stringSet(loader, "classLoaderExceptions");
        Set<String> transformerExceptions = stringSet(loader, "transformerExceptions");
        Method transformName = LaunchClassLoader.class.getDeclaredMethod("transformName", String.class);
        Method runTransformers = LaunchClassLoader.class.getDeclaredMethod("runTransformers", String.class, String.class, byte[].class);
        transformName.setAccessible(true);
        runTransformers.setAccessible(true);

        List<String> skip = new ArrayList<>();
        for (String s : System.getProperty("aot.skip", "").split(",")) {
            if (!s.trim().isEmpty()) {
                skip.add(s.trim());
            }
        }

        log.println("transformers:");
        for (Object t : loader.getTransformers()) {
            log.println("  " + t.getClass().getName());
        }

        List<URL> sources = new ArrayList<>(loader.getSources());
        // FML adds a mod's jar to the class loader when it constructs the mod. Mods that were not constructed here (a
        // mod they depend on errored: IC2's signature check, say) would be missing from the dump, so add every jar.
        File[] modJars = new File(Launch.minecraftHome, "mods").listFiles((dir, n) -> n.endsWith(".jar"));
        if (modJars != null) {
            java.util.Arrays.sort(modJars);
            Set<String> known = new HashSet<>();
            for (URL u : sources) {
                known.add(new File(u.getPath()).getName());
            }
            for (File jar : modJars) {
                if (known.add(jar.getName())) {
                    URL url = jar.toURI().toURL();
                    loader.addURL(url);
                    sources.add(url);
                    log.println("added mod jar that FML had not loaded: " + jar.getName());
                }
            }
        }
        Set<String> done = new HashSet<>();
        TreeMap<String, String> renamed = new TreeMap<>();
        int[] counts = new int[2]; // written, failed
        try (JarOutputStream jar = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(new File(out, "classes.jar")), 1 << 16))) {
            Emitter emit = new Emitter(loader, jar, classLoaderExceptions, transformerExceptions, transformName,
                    runTransformers, skip, renamed, counts, log);
            // Class names in classpath order, then supers-first: coremod transformers (CodeChickenCore's hierarchy
            // manager, for one) learn each class's parents as the classes pass through, and look a parent up from the
            // class path when they have not seen it yet -- which fails for deobfuscated names. The JVM loads a
            // superclass before its subclasses, so this is the order they see on a desktop.
            java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
            for (URL source : sources) {
                if (!"file".equals(source.getProtocol()) || !source.getPath().endsWith(".jar")) {
                    log.println("skipping source " + source);
                    continue;
                }
                log.println("source " + source);
                try (JarFile jf = new JarFile(new File(source.toURI()))) {
                    Enumeration<JarEntry> entries = jf.entries();
                    while (entries.hasMoreElements()) {
                        String path = entries.nextElement().getName();
                        if (path.endsWith(".class") && !path.startsWith("META-INF/")) {
                            names.add(path.substring(0, path.length() - 6).replace('/', '.'));
                        }
                    }
                }
            }
            Map<String, List<String>> parents = new java.util.HashMap<>();
            for (String name : names) {
                try {
                    byte[] raw = loader.getClassBytes(name);
                    if (raw != null) {
                        org.objectweb.asm.ClassReader r = new org.objectweb.asm.ClassReader(raw);
                        List<String> ps = new ArrayList<>();
                        if (r.getSuperName() != null) {
                            ps.add(r.getSuperName().replace('/', '.'));
                        }
                        for (String i : r.getInterfaces()) {
                            ps.add(i.replace('/', '.'));
                        }
                        parents.put(name, ps);
                    }
                } catch (Throwable ignored) {
                    // not parseable by this ASM (newer class file version): no ordering information
                }
            }
            for (String name : names) {
                emitSupersFirst(name, names, parents, done, emit, new HashSet<String>());
            }
            // Classes that exist only as the output of Forge's binary patches (they add new classes, such as the
            // integrated server's thread class): they are in no jar, so the sweep above cannot see them.
            try {
                Class<?> manager = Class.forName("cpw.mods.fml.common.patcher.ClassPatchManager", true, loader);
                Object instance = manager.getField("INSTANCE").get(null);
                Field patchesField = manager.getDeclaredField("patches");
                patchesField.setAccessible(true);
                Object patches = patchesField.get(instance);
                Set<?> keys = (Set<?>) patches.getClass().getMethod("keySet").invoke(patches);
                int generated = 0;
                for (Object key : new java.util.TreeSet<Object>(keys)) {
                    String name = (String) key;
                    if (done.add(name)) {
                        emit.emit(name);
                        generated++;
                    }
                }
                log.println("classes only produced by binary patches: " + generated);
            } catch (Throwable t) {
                log.println("binary patch classes FAILED: " + t);
                t.printStackTrace(log);
            }
        }
        int written = counts[0];
        int failed = counts[1];
        try (PrintWriter map = new PrintWriter(new File(out, "renamed.txt"), "UTF-8")) {
            renamed.forEach((k, v) -> map.println(k + " " + v));
        }
        log.println("classes written: " + written + ", failed: " + failed);
        log.close();
        System.out.println("AOT dump: " + written + " classes (" + failed + " failed) -> " + out);
        // FML's security manager traps System.exit from outside its own packages (see RetroExit)
        try {
            Class.forName("cpw.mods.fml.relauncher.RetroExit").getMethod("exit").invoke(null);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void emitSupersFirst(String name, Set<String> all, Map<String, List<String>> parents,
            Set<String> done, Emitter emit, Set<String> visiting) {
        if (done.contains(name) || !visiting.add(name)) {
            return;
        }
        List<String> ps = parents.get(name);
        if (ps != null) {
            for (String p : ps) {
                if (all.contains(p)) {
                    emitSupersFirst(p, all, parents, done, emit, visiting);
                }
            }
        }
        if (done.add(name)) {
            emit.emit(name);
        }
    }

    private static Throwable rootOf(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /** Runs one class through the transformer chain (as LaunchClassLoader.findClass does) and writes the result. */
    private static final class Emitter {
        private final LaunchClassLoader loader;
        private final JarOutputStream jar;
        private final Set<String> classLoaderExceptions;
        private final Set<String> transformerExceptions;
        private final Method transformName;
        private final Method runTransformers;
        private final List<String> skip;
        private final TreeMap<String, String> renamed;
        private final int[] counts;
        private final PrintWriter log;

        Emitter(LaunchClassLoader loader, JarOutputStream jar, Set<String> classLoaderExceptions,
                Set<String> transformerExceptions, Method transformName, Method runTransformers, List<String> skip,
                TreeMap<String, String> renamed, int[] counts, PrintWriter log) {
            this.loader = loader;
            this.jar = jar;
            this.classLoaderExceptions = classLoaderExceptions;
            this.transformerExceptions = transformerExceptions;
            this.transformName = transformName;
            this.runTransformers = runTransformers;
            this.skip = skip;
            this.renamed = renamed;
            this.counts = counts;
            this.log = log;
        }

        void emit(String name) {
            if (startsWithAny(name, skip)) {
                return;
            }
            try {
                byte[] bytes = loader.getClassBytes(name); // null for classes only the patches produce
                String transformed = name;
                if (!startsWithAny(name, classLoaderExceptions) && !startsWithAny(name, transformerExceptions)) {
                    transformed = (String) transformName.invoke(loader, name);
                    byte[] original = bytes;
                    try {
                        bytes = (byte[]) runTransformers.invoke(loader, name, transformed, bytes);
                    } catch (InvocationTargetException chainFailure) {
                        // One coremod patch that cannot be applied (it could not find the code it patches) should cost
                        // that mod its hook, not the whole class: run the chain again, skipping transformers that throw.
                        bytes = original;
                        for (Object t : loader.getTransformers()) {
                            try {
                                byte[] next = ((net.minecraft.launchwrapper.IClassTransformer) t).transform(name, transformed,
                                        bytes);
                                bytes = next;
                            } catch (Throwable tf) {
                                if (String.valueOf(rootOf(tf)).contains("invalid side")) {
                                    bytes = null; // SideTransformer: the class is for the other side, it is removed
                                    break;
                                }
                                log.println("  transformer " + t.getClass().getName() + " skipped for " + name + " ("
                                        + transformed + "): " + rootOf(tf));
                            }
                        }
                    }
                }
                if (bytes == null || bytes.length == 0) {
                    return;
                }
                if (!transformed.equals(name)) {
                    renamed.put(name, transformed);
                }
                jar.putNextEntry(new ZipEntry(transformed.replace('.', '/') + ".class"));
                jar.write(bytes);
                jar.closeEntry();
                counts[0]++;
            } catch (InvocationTargetException ex) {
                counts[1]++;
                log.println("FAILED " + name + ": " + ex.getCause());
                Throwable root = ex.getCause();
                while (root != null && root.getCause() != null) {
                    root = root.getCause();
                }
                if (!(root instanceof ClassNotFoundException) && !String.valueOf(root).contains("invalid side")) {
                    log.println("  root cause: " + root);
                    for (StackTraceElement e : root.getStackTrace()) {
                        log.println("    at " + e);
                    }
                }
            } catch (Throwable ex) {
                counts[1]++;
                log.println("FAILED " + name + ": " + ex);
            }
        }
    }

    /**
     * Brings Forge up the way {@code FMLClientHandler.beginMinecraftLoading} does, minus the parts that need a window:
     * mod discovery and construction. That is what feeds ModAPITransformer and the mods' own access transformers.
     */
    private static void initializeFml(PrintWriter log) {
        try {
            Class<?> handler = Class.forName("cpw.mods.fml.client.FMLClientHandler", true, Launch.classLoader);
            Object client = handler.getMethod("instance").invoke(null);
            // beginMinecraftLoading would set these; without them registering mods as resource packs throws
            // (which ends LoadController.buildModList early and leaves half the mods inactive)
            for (String f : new String[] {"resourcePackList", "resourcePackMap"}) {
                Field field = handler.getDeclaredField(f);
                field.setAccessible(true);
                field.set(client, f.endsWith("List") ? new ArrayList<Object>() : new java.util.HashMap<Object, Object>());
            }
            Class<?> common = Class.forName("cpw.mods.fml.common.FMLCommonHandler", true, Launch.classLoader);
            Object fml = common.getMethod("instance").invoke(null);
            Class<?> sided = Class.forName("cpw.mods.fml.common.IFMLSidedHandler", true, Launch.classLoader);
            common.getMethod("beginLoading", sided).invoke(fml, client);
            Class<?> loaderClass = Class.forName("cpw.mods.fml.common.Loader", true, Launch.classLoader);
            Object loader = loaderClass.getMethod("instance").invoke(null);
            loaderClass.getMethod("loadMods").invoke(loader);
            log.println("Loader.loadMods completed");
        } catch (Throwable t) {
            Throwable c = t instanceof InvocationTargetException ? t.getCause() : t;
            log.println("Loader.loadMods FAILED (continuing): " + c);
            try {
                explainSortFailure(log);
            } catch (Throwable t2) {
                log.println("(could not explain: " + t2 + ")");
            }
            c.printStackTrace(log);
            c.printStackTrace();
        }
        log.flush();
    }

    /** Lists the mod dependencies that name something that is not a loaded mod (what breaks FML's mod sorter). */
    private static void explainSortFailure(PrintWriter log) throws Exception {
        Class<?> loaderClass = Class.forName("cpw.mods.fml.common.Loader", true, Launch.classLoader);
        Class<?> mc = Class.forName("cpw.mods.fml.common.ModContainer", true, Launch.classLoader);
        Object loader = loaderClass.getMethod("instance").invoke(null);
        List<?> active = (List<?>) loaderClass.getMethod("getActiveModList").invoke(loader);
        Map<?, ?> indexed = (Map<?, ?>) loaderClass.getMethod("getIndexedModList").invoke(loader);
        Set<Object> activeSet = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
        activeSet.addAll(active);
        log.println("sorter input (active mods): " + active.size() + ", indexed: " + indexed.size());
        int shown = 0;
        for (Object mod : active) {
            for (String kind : new String[] {"getDependencies", "getDependants"}) {
                for (Object dep : (java.util.Collection<?>) mc.getMethod(kind).invoke(mod)) {
                    String label = (String) dep.getClass().getMethod("getLabel").invoke(dep);
                    Object target = indexed.get(label);
                    if (target != null && !activeSet.contains(target) && shown++ < 30) {
                        log.println("  " + mc.getMethod("getModId").invoke(mod) + " " + kind + " " + label
                                + ": " + target.getClass().getName() + " is indexed but not active");
                    }
                }
            }
        }
        for (Object mod : indexed.values()) {
            if (!activeSet.contains(mod) && shown++ < 60) {
                log.println("  indexed but not active: " + mc.getMethod("getModId").invoke(mod) + " ("
                        + mod.getClass().getName() + ")");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> stringSet(LaunchClassLoader loader, String field) throws Exception {
        Field f = LaunchClassLoader.class.getDeclaredField(field);
        f.setAccessible(true);
        return new HashSet<>((Set<String>) f.get(loader));
    }

    private static boolean startsWithAny(String name, Iterable<String> prefixes) {
        for (String p : prefixes) {
            if (name.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

}
