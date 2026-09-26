package retro.rt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Classpath resources for the game and mods.
 *
 * <p>The build redirects {@code Class.getResourceAsStream}, {@code ClassLoader.getResource} and friends here
 * (see {@code retro.build.Patches}). Resources come from {@code assets.pak}, which holds every non-class file
 * of the Minecraft jar and the mods, in classpath order.
 */
public final class Resources {
    private static Pak pak;
    private static final Map<String, byte[]> overrides = new HashMap<>();

    private Resources() {
    }

    public static void init(Pak assets) {
        pak = assets;
    }

    /** Adds or replaces a resource at runtime. */
    public static void put(String path, byte[] data) {
        overrides.put(strip(path), data);
    }

    public static boolean exists(String path) {
        path = strip(path);
        return overrides.containsKey(path) || (pak != null && pak.get(path) != null);
    }

    public static byte[] read(String path) {
        path = strip(path);
        byte[] data = overrides.get(path);
        if (data != null) {
            return data;
        }
        if (pak == null) {
            return null;
        }
        Pak.Entry entry = pak.get(path);
        return entry == null ? null : pak.read(entry);
    }

    public static InputStream open(String path) {
        byte[] data = read(path);
        return data == null ? null : new ByteArrayInputStream(data);
    }

    private static String strip(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        // Normalise "a/./b" and "a/../b", which some mods produce.
        if (path.contains("./")) {
            java.util.ArrayList<String> parts = new java.util.ArrayList<>();
            for (String part : path.split("/")) {
                if (part.equals("..")) {
                    if (!parts.isEmpty()) {
                        parts.remove(parts.size() - 1);
                    }
                } else if (!part.equals(".") && !part.isEmpty()) {
                    parts.add(part);
                }
            }
            path = String.join("/", parts);
        }
        return path;
    }

    private static String resolve(Class<?> cls, String name) {
        if (name.startsWith("/")) {
            return name.substring(1);
        }
        String className = cls.getName();
        int dot = className.lastIndexOf('.');
        return dot < 0 ? name : className.substring(0, dot).replace('.', '/') + "/" + name;
    }

    // ---- Redirect targets (signatures must match retro.build.Patches) ----

    public static InputStream classGetResourceAsStream(Class<?> cls, String name) {
        return open(resolve(cls, name));
    }

    public static URL classGetResource(Class<?> cls, String name) {
        return url(resolve(cls, name));
    }

    public static java.security.ProtectionDomain classGetProtectionDomain(Class<?> cls) {
        return new java.security.ProtectionDomain(new java.security.CodeSource(Origins.jarUrl(cls), (java.security.cert.Certificate[]) null), null);
    }

    public static InputStream loaderGetResourceAsStream(ClassLoader loader, String name) {
        return open(name);
    }

    public static URL loaderGetResource(ClassLoader loader, String name) {
        return url(name);
    }

    public static Enumeration<URL> loaderGetResources(ClassLoader loader, String name) {
        URL url = url(name);
        return url == null ? Collections.emptyEnumeration() : Collections.enumeration(Collections.singletonList(url));
    }

    public static Class<?> loaderLoadClass(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    public static InputStream getSystemResourceAsStream(String name) {
        return open(name);
    }

    public static URL getSystemResource(String name) {
        return url(name);
    }

    /** URLs for resources use a private scheme that reads back from the pak. */
    static URL url(String path) {
        path = strip(path);
        if (!exists(path)) {
            return path.endsWith(".class") ? classUrl(path) : null;
        }
        try {
            return new URL("res", "", -1, "/" + path, ResourceHandler.INSTANCE);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * A class file's URL, as a JVM gives it: {@code jar:file:/.../bin/minecraft.jar!/X.class} (or the mod's jar,
     * or {@code file:} inside a mod folder). The jar is the virtual one listing the game's entries, so mods can
     * enumerate it (Single Player Commands finds its plugins this way). The class bytes themselves are not
     * available.
     */
    private static URL classUrl(String path) {
        java.io.File jar = Origins.jarFile(path.substring(0, path.length() - 6).replace('/', '.'));
        if (jar == null) {
            return null;
        }
        try {
            if (jar.isDirectory()) {
                return new URL("file", "", -1, new java.io.File(jar, path).getAbsolutePath(), ClassFileHandler.INSTANCE);
            }
            return new URL("jar", "", -1, "file:" + jar.getAbsolutePath() + "!/" + path, ClassFileHandler.INSTANCE);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    static final class ClassFileHandler extends java.net.URLStreamHandler {
        static final ClassFileHandler INSTANCE = new ClassFileHandler();

        @Override
        protected java.net.URLConnection openConnection(URL u) throws IOException {
            throw new IOException("Class files are not available at run time: " + u);
        }
    }

    public static final class ResourceHandler extends java.net.URLStreamHandler {
        static final ResourceHandler INSTANCE = new ResourceHandler();

        @Override
        protected java.net.URLConnection openConnection(URL u) throws IOException {
            String path = u.getPath();
            return new java.net.URLConnection(u) {
                @Override
                public void connect() {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    InputStream in = open(path);
                    if (in == null) {
                        throw new java.io.FileNotFoundException(path);
                    }
                    return in;
                }
            };
        }
    }
}
