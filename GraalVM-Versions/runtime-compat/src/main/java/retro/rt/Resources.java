package retro.rt;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/** Classpath resources for Minecraft 1.2.5 and mods, redirected from Class.getResourceAsStream etc. */
public final class Resources {
    private static Pak pak;
    private static final Map<String, byte[]> overrides = new HashMap<>();

    static {
        ensureLoaded();
    }

    private Resources() {
    }

    public static void init(Pak assets) {
        pak = assets;
    }

    private static synchronized void ensureLoaded() {
        if (pak != null) {
            return;
        }
        Path[] candidates = new Path[] {
            Paths.get("assets.pak"),
            Paths.get("build/prepared/web/assets.pak"),
            Paths.get("../build/prepared/web/assets.pak"),
            Paths.get("../../build/prepared/web/assets.pak")
        };
        for (Path p : candidates) {
            if (Files.exists(p)) {
                try {
                    pak = new Pak(p.toAbsolutePath());
                    System.out.println("[Resources] Auto-loaded assets from: " + p.toAbsolutePath());
                    break;
                } catch (Exception e) {
                    System.err.println("[Resources] Failed to load " + p + ": " + e);
                }
            }
        }
    }

    public static void put(String path, byte[] data) {
        overrides.put(strip(path), data);
    }

    public static boolean exists(String path) {
        path = strip(path);
        ensureLoaded();
        return overrides.containsKey(path) || (pak != null && pak.get(path) != null);
    }

    public static byte[] read(String path) {
        path = strip(path);
        byte[] data = overrides.get(path);
        if (data != null) {
            return data;
        }
        ensureLoaded();
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
        if (path.contains("./")) {
            java.util.ArrayList<String> parts = new java.util.ArrayList<>();
            for (String part : path.split("/")) {
                if (part.equals("..")) {
                    if (!parts.isEmpty()) parts.remove(parts.size() - 1);
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

    public static InputStream classGetResourceAsStream(Class<?> cls, String name) {
        InputStream in = open(resolve(cls, name));
        if (in != null) return in;
        return cls.getResourceAsStream(name);
    }

    public static URL classGetResource(Class<?> cls, String name) {
        URL u = url(resolve(cls, name));
        if (u != null) return u;
        return cls.getResource(name);
    }

    public static java.security.ProtectionDomain classGetProtectionDomain(Class<?> cls) {
        return cls.getProtectionDomain();
    }

    public static InputStream loaderGetResourceAsStream(ClassLoader loader, String name) {
        InputStream in = open(name);
        if (in != null) return in;
        if (loader == null) loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader.getResourceAsStream(name) : ClassLoader.getSystemResourceAsStream(name);
    }

    public static URL loaderGetResource(ClassLoader loader, String name) {
        URL u = url(name);
        if (u != null) return u;
        if (loader == null) loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader.getResource(name) : ClassLoader.getSystemResource(name);
    }

    public static Enumeration<URL> loaderGetResources(ClassLoader loader, String name) throws IOException {
        URL u = url(name);
        if (u != null) return Collections.enumeration(Collections.singletonList(u));
        if (loader == null) loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader.getResources(name) : ClassLoader.getSystemResources(name);
    }

    public static Class<?> loaderLoadClass(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    public static InputStream getSystemResourceAsStream(String name) {
        InputStream in = open(name);
        if (in != null) return in;
        return ClassLoader.getSystemResourceAsStream(name);
    }

    public static URL getSystemResource(String name) {
        URL u = url(name);
        if (u != null) return u;
        return ClassLoader.getSystemResource(name);
    }

    static URL url(String path) {
        path = strip(path);
        if (!exists(path)) return null;
        try {
            return new URL("res", "", -1, "/" + path, ResourceHandler.INSTANCE);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static final class ResourceHandler extends URLStreamHandler {
        public static final ResourceHandler INSTANCE = new ResourceHandler();

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            String path = u.getPath();
            return new URLConnection(u) {
                @Override
                public void connect() {}

                @Override
                public InputStream getInputStream() throws IOException {
                    InputStream in = open(path);
                    if (in == null) throw new FileNotFoundException(path);
                    return in;
                }
            };
        }
    }
}
