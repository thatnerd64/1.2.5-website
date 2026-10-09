package retro.aot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import net.minecraft.launchwrapper.Launch;

/**
 * Records what FML's mod discovery found, so the browser can replay it instead of scanning jars: for every candidate
 * file, its class list, mcmod.info, whether it is a coremod, the {@code @Mod} containers (class + annotation values)
 * and the annotation data FML collected. Read by cpw.mods.fml.common.discovery.JarDiscoverer (gameglue).
 */
final class Discovery {
    private Discovery() {
    }

    private static Class<?> cls(String name) throws Exception {
        return Class.forName(name, true, Launch.classLoader);
    }

    private static Object field(Object o, Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void write(File file, PrintWriter log) throws Exception {
        Class<?> loaderClass = cls("cpw.mods.fml.common.Loader");
        Object loader = loaderClass.getMethod("instance").invoke(null);
        Object discoverer = field(loader, loaderClass, "discoverer");
        Class<?> discoverClass = cls("cpw.mods.fml.common.discovery.ModDiscoverer");
        List<?> candidates = (List<?>) field(discoverer, discoverClass, "candidates");
        Object table = discoverClass.getMethod("getASMTable").invoke(discoverer);
        Class<?> tableClass = cls("cpw.mods.fml.common.discovery.ASMDataTable");
        Collection<?> all = ((com.google.common.collect.Multimap<?, ?>) field(table, tableClass, "globalAnnotationData")).values();
        Class<?> dataClass = cls("cpw.mods.fml.common.discovery.ASMDataTable$ASMData");
        Class<?> candClass = cls("cpw.mods.fml.common.discovery.ModCandidate");
        Class<?> fmlContainer = cls("cpw.mods.fml.common.FMLModContainer");

        JsonArray out = new JsonArray();
        java.util.TreeSet<String> modClasses = new java.util.TreeSet<>();
        for (Object cand : candidates) {
            File jar = (File) candClass.getMethod("getModContainer").invoke(cand);
            JsonObject c = new JsonObject();
            c.addProperty("file", jar.getName());
            c.addProperty("classpath", (Boolean) candClass.getMethod("isClasspath").invoke(cand));
            if (jar.isFile()) {
                try (JarFile jf = new JarFile(jar)) {
                    Manifest mf = jf.getManifest();
                    boolean core = mf != null && (mf.getMainAttributes().getValue("FMLCorePlugin") != null
                            || mf.getMainAttributes().getValue("TweakClass") != null);
                    c.addProperty("coremod", core);
                    ZipEntry info = jf.getEntry("mcmod.info");
                    if (info != null) {
                        c.addProperty("mcmod", new String(readAll(jf.getInputStream(info)), StandardCharsets.UTF_8));
                    }
                }
            }
            if (jar.isFile()) {
                // asset domains (assets/<domain>/...) the mod's resource pack answers for
                java.util.Set<String> domains = new java.util.TreeSet<>();
                try (JarFile jf = new JarFile(jar)) {
                    for (java.util.Enumeration<java.util.jar.JarEntry> en = jf.entries(); en.hasMoreElements(); ) {
                        String n = en.nextElement().getName();
                        if (n.startsWith("assets/")) {
                            int slash = n.indexOf('/', 7);
                            if (slash > 7) {
                                domains.add(n.substring(7, slash));
                            }
                        }
                    }
                }
                JsonArray d = new JsonArray();
                for (String x : domains) {
                    d.add(new JsonPrimitive(x));
                }
                c.add("domains", d);
            }
            boolean classpath = (Boolean) candClass.getMethod("isClasspath").invoke(cand);
            List<?> containedMods = (List<?>) candClass.getMethod("getContainedMods").invoke(cand);
            if (classpath && (containedMods == null || containedMods.isEmpty())) {
                out.add(c); // a library on the class path: only its asset domains matter
                continue;
            }
            JsonArray classes = new JsonArray();
            for (Object name : (Collection<?>) candClass.getMethod("getClassList").invoke(cand)) {
                classes.add(new JsonPrimitive(String.valueOf(name)));
                modClasses.add(String.valueOf(name).replace('/', '.'));
            }
            c.add("classes", classes);

            JsonArray mods = new JsonArray();
            List<?> contained = (List<?>) candClass.getMethod("getContainedMods").invoke(cand);
            if (contained != null) {
                for (Object mod : contained) {
                    if (!fmlContainer.isInstance(mod)) {
                        log.println("discovery: unsupported container " + mod.getClass() + " in " + jar.getName());
                        continue;
                    }
                    JsonObject m = new JsonObject();
                    m.addProperty("className", (String) field(mod, fmlContainer, "className"));
                    m.add("descriptor", encode(field(mod, fmlContainer, "descriptor")));
                    mods.add(m);
                }
            }
            c.add("mods", mods);

            JsonArray asm = new JsonArray();
            for (Object d : all) {
                if (field(d, dataClass, "candidate") != cand) {
                    continue;
                }
                String annotation = (String) field(d, dataClass, "annotationName");
                if (annotation.startsWith("cpw.mods.fml.relauncher.")) {
                    continue; // @SideOnly and friends: every client class carries them and nothing reads them
                }
                JsonObject a = new JsonObject();
                a.addProperty("a", annotation);
                a.addProperty("c", (String) field(d, dataClass, "className"));
                String member = (String) field(d, dataClass, "objectName");
                if (member != null) {
                    a.addProperty("o", member);
                }
                a.add("i", encode(field(d, dataClass, "annotationInfo")));
                asm.add(a);
            }
            c.add("asm", asm);
            out.add(c);
            log.println("discovery: " + jar.getName() + " classes=" + classes.size() + " mods=" + mods.size()
                    + " asm=" + asm.size());
        }
        try (FileWriter w = new FileWriter(file)) {
            new Gson().toJson(out, w);
        }
        // the mod containers the coremods contribute (IFMLLoadingPlugin.getModContainerClass()): the Loader instantiates
        // them from FMLInjectionData.containers, which the browser start-up replays
        try (PrintWriter w = new PrintWriter(new File(file.getParentFile(), "containers.txt"), "UTF-8")) {
            for (String c : cpw.mods.fml.relauncher.FMLInjectionData.containers) {
                w.println(c);
            }
        }
        // the coremod plugins (class, jar, setup class): their injectData/setup state (e.g. the jar location a mod
        // container reports as its source) lives in statics that the browser start-up has to rebuild
        try (PrintWriter w = new PrintWriter(new File(file.getParentFile(), "plugins.txt"), "UTF-8")) {
            java.lang.reflect.Field lp = Class.forName("cpw.mods.fml.relauncher.CoreModManager")
                    .getDeclaredField("loadPlugins");
            lp.setAccessible(true);
            for (Object wrapper : (java.util.List<?>) lp.get(null)) {
                java.lang.reflect.Field fi = wrapper.getClass().getDeclaredField("coreModInstance");
                java.lang.reflect.Field fl = wrapper.getClass().getDeclaredField("location");
                fi.setAccessible(true);
                fl.setAccessible(true);
                cpw.mods.fml.relauncher.IFMLLoadingPlugin plugin = (cpw.mods.fml.relauncher.IFMLLoadingPlugin) fi.get(wrapper);
                File loc = (File) fl.get(wrapper);
                String setup = plugin.getSetupClass();
                String[] transformers = plugin.getASMTransformerClass();
                w.println(plugin.getClass().getName() + "\t" + (loc == null ? "" : loc.getName()) + "\t"
                        + (setup == null ? "" : setup) + "\t"
                        + (transformers == null ? "" : String.join(",", transformers)));
            }
        } catch (Throwable t) {
            log.println("plugins.txt FAILED: " + t);
        }
        // every class of every mod jar: they can all be loaded by name (the build makes them reachable)
        try (PrintWriter w = new PrintWriter(new File(file.getParentFile(), "modclasses.txt"), "UTF-8")) {
            for (String c : modClasses) {
                w.println(c);
            }
        }
    }

    /** Annotation values: String/boxed primitives, ASM Types, enum holders, lists and (nested annotation) maps. */
    private static JsonElement encode(Object v) throws Exception {
        JsonObject o = new JsonObject();
        if (v == null) {
            o.addProperty("t", "n");
        } else if (v instanceof String) {
            o.addProperty("t", "s");
            o.addProperty("v", (String) v);
        } else if (v instanceof Integer) {
            o.addProperty("t", "i");
            o.addProperty("v", (Integer) v);
        } else if (v instanceof Long) {
            o.addProperty("t", "l");
            o.addProperty("v", String.valueOf(v));
        } else if (v instanceof Boolean) {
            o.addProperty("t", "z");
            o.addProperty("v", (Boolean) v);
        } else if (v instanceof Double) {
            o.addProperty("t", "d");
            o.addProperty("v", String.valueOf(v));
        } else if (v instanceof Float) {
            o.addProperty("t", "f");
            o.addProperty("v", String.valueOf(v));
        } else if (v instanceof Short) {
            o.addProperty("t", "h");
            o.addProperty("v", (Short) v);
        } else if (v instanceof Byte) {
            o.addProperty("t", "y");
            o.addProperty("v", (Byte) v);
        } else if (v instanceof Character) {
            o.addProperty("t", "c");
            o.addProperty("v", (int) (Character) v);
        } else if (v.getClass().getName().equals("org.objectweb.asm.Type")) {
            // ASM comes from LaunchWrapper's class loader here, so it is not our Type class: ask by reflection
            o.addProperty("t", "T");
            o.addProperty("v", (String) v.getClass().getMethod("getDescriptor").invoke(v));
        } else if (v.getClass().isArray() && v.getClass().getComponentType().isPrimitive()) {
            o.addProperty("t", "A");
            o.addProperty("c", String.valueOf(primitiveCode(v.getClass().getComponentType())));
            JsonArray a = new JsonArray();
            for (int i = 0; i < java.lang.reflect.Array.getLength(v); i++) {
                a.add(new JsonPrimitive(String.valueOf(java.lang.reflect.Array.get(v, i))));
            }
            o.add("v", a);
        } else if (v instanceof List) {
            o.addProperty("t", "L");
            JsonArray a = new JsonArray();
            for (Object e : (List<?>) v) {
                a.add(encode(e));
            }
            o.add("v", a);
        } else if (v instanceof Map) {
            o.addProperty("t", "M");
            JsonObject m = new JsonObject();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                m.add(String.valueOf(e.getKey()), encode(e.getValue()));
            }
            o.add("v", m);
        } else if (v.getClass().getName().equals("cpw.mods.fml.common.discovery.asm.ModAnnotation$EnumHolder")) {
            o.addProperty("t", "E");
            o.addProperty("d", (String) field(v, v.getClass(), "desc"));
            o.addProperty("v", (String) field(v, v.getClass(), "value"));
        } else {
            throw new IllegalStateException("unsupported annotation value " + v.getClass());
        }
        return o;
    }

    private static char primitiveCode(Class<?> c) {
        if (c == boolean.class) {
            return 'Z';
        } else if (c == byte.class) {
            return 'B';
        } else if (c == char.class) {
            return 'C';
        } else if (c == short.class) {
            return 'S';
        } else if (c == int.class) {
            return 'I';
        } else if (c == long.class) {
            return 'J';
        } else if (c == float.class) {
            return 'F';
        }
        return 'D';
    }

    private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n; (n = in.read(buf)) > 0; ) {
            b.write(buf, 0, n);
        }
        return b.toByteArray();
    }

}
