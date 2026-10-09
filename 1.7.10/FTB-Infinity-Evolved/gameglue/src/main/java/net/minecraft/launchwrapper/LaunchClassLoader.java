package net.minecraft.launchwrapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Replacement for LaunchWrapper's class loader. Class transformation already happened at build time (tools/aot.sh);
 * loading a class is a {@code Class.forName} lookup among the compiled-in classes.
 */
public class LaunchClassLoader extends ClassLoader {
    public static final int BUFFER_SIZE = 1 << 12;

    private final List<URL> sources = new ArrayList<>();
    // (named like LaunchWrapper's own fields: coremods such as CodeChickenCore and LogisticsPipes reach them
    // reflectively; they stay empty)
    private final List<IClassTransformer> transformers = new ArrayList<>();
    private final java.util.Map<String, Class<?>> cachedClasses = new java.util.HashMap<>();
    private final Set<String> invalidClasses = new java.util.HashSet<>();
    private final Set<String> classLoaderExceptions = new java.util.HashSet<>();
    private final Set<String> transformerExceptions = new java.util.HashSet<>();
    private final java.util.Map<String, byte[]> resourceCache = new java.util.HashMap<>();
    private final Set<String> negativeResourceCache = new java.util.HashSet<>();

    public LaunchClassLoader(URL[] sources) {
    }

    public void registerTransformer(String transformerClassName) {
    }

    public List<IClassTransformer> getTransformers() {
        return java.util.Collections.unmodifiableList(transformers);
    }

    public void addClassLoaderExclusion(String toExclude) {
    }

    public void addTransformerExclusion(String toExclude) {
    }

    public void addURL(URL url) {
    }

    public List<URL> getSources() {
        return sources;
    }

    // The bytecode of the classes mods read at run time (ForgeMultipart mixes trait classes into new ones), packed by
    // the build from what a capture run of the real game read (tools/capture.sh); null for any other class, as for a
    // class the real loader cannot find
    public byte[] getClassBytes(String name) {
        return retro.rt.Resources.read("retro/classbytes/" + name.replace('.', '/') + ".class");
    }

    // ForgeMultipart runs the transformer chain over the classes it generates, reflectively; the chain ran at build
    // time (the captured classes went through it), so the bytes pass unchanged
    private byte[] runTransformers(String name, String transformedName, byte[] basicClass) {
        return basicClass;
    }

    public void clearNegativeEntries(Set<String> entriesToClear) {
    }

    @Override
    public java.io.InputStream getResourceAsStream(String name) {
        return retro.rt.Resources.loaderGetResourceAsStream(this, name);
    }

    @Override
    public java.net.URL getResource(String name) {
        return retro.rt.Resources.loaderGetResource(this, name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
