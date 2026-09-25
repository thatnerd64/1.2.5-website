package retro.compat;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;

/** Redirect targets for JDK methods TeaVM does not provide (see retro.build.Patches). */
public final class SystemCompat {
    private SystemCompat() {
    }

    public static void exit(int status) {
        retro.JS.log("Minecraft exited with status " + status);
        retro.fs.Persistence.flushAll();
        exitPage();
        throw new ThreadDeath();
    }

    @org.teavm.jso.JSBody(script = "if (window.retroLoader && window.retroLoader.exited) window.retroLoader.exited();")
    private static native void exitPage();

    public static long maxMemory(Runtime runtime) {
        return 2048L * 1024 * 1024;
    }

    public static void addShutdownHook(Runtime runtime, Thread hook) {
        retro.Shutdown.add(hook);
    }

    public static boolean removeShutdownHook(Runtime runtime, Thread hook) {
        return retro.Shutdown.remove(hook);
    }

    public static Process exec(Runtime runtime, String command) throws IOException {
        throw new IOException("Cannot run programs in a browser: " + command);
    }

    public static Process exec(Runtime runtime, String[] command) throws IOException {
        throw new IOException("Cannot run programs in a browser");
    }

    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    public static void stop(Thread thread) {
        thread.interrupt();
    }

    public static void load(String name) {
        throw new UnsatisfiedLinkError("Native libraries are not available in a browser: " + name);
    }

    public static void loadLibrary(String name) {
        throw new UnsatisfiedLinkError("Native libraries are not available in a browser: " + name);
    }

    public static String mapLibraryName(String name) {
        return "lib" + name + ".so";
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> set) {
        return new UnmodifiableSortedSet<>(set);
    }

    static final class UnmodifiableSortedSet<T> extends java.util.AbstractSet<T> implements SortedSet<T> {
        private final SortedSet<T> set;

        UnmodifiableSortedSet(SortedSet<T> set) {
            this.set = set;
        }

        @Override
        public java.util.Iterator<T> iterator() {
            java.util.Iterator<T> it = set.iterator();
            return new java.util.Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public T next() {
                    return it.next();
                }
            };
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean contains(Object o) {
            return set.contains(o);
        }

        @Override
        public java.util.Comparator<? super T> comparator() {
            return set.comparator();
        }

        @Override
        public SortedSet<T> subSet(T from, T to) {
            return new UnmodifiableSortedSet<>(set.subSet(from, to));
        }

        @Override
        public SortedSet<T> headSet(T to) {
            return new UnmodifiableSortedSet<>(set.headSet(to));
        }

        @Override
        public SortedSet<T> tailSet(T from) {
            return new UnmodifiableSortedSet<>(set.tailSet(from));
        }

        @Override
        public T first() {
            return set.first();
        }

        @Override
        public T last() {
            return set.last();
        }
    }

    public static void propertiesStore(Properties props, Writer writer, String comments) throws IOException {
        if (comments != null) {
            writer.write("#" + comments.replace("\n", "\n#") + "\n");
        }
        writer.write("#" + new java.util.Date() + "\n");
        for (Map.Entry<Object, Object> e : new TreeMap<>(props).entrySet()) {
            writer.write(escape(String.valueOf(e.getKey()), true) + "=" + escape(String.valueOf(e.getValue()), false)
                    + "\n");
        }
        writer.flush();
    }

    /** Properties.load(InputStream): ISO-8859-1 with \\uXXXX escapes (TeaVM lacks the "8859_1" charset alias). */
    public static void propertiesLoad(Properties props, java.io.InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }
        byte[] data = bytes.toByteArray();
        char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) (data[i] & 0xFF);
        }
        props.load(new java.io.StringReader(new String(chars)));
    }

    /** Properties.store(OutputStream): ISO-8859-1, non-Latin-1 characters written as \\uXXXX. */
    public static void propertiesStoreStream(Properties props, java.io.OutputStream out, String comments)
            throws IOException {
        java.io.StringWriter writer = new java.io.StringWriter();
        propertiesStore(props, writer, comments);
        String text = writer.toString();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 0xFF) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        byte[] data = new byte[sb.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) sb.charAt(i);
        }
        out.write(data);
        out.flush();
    }

    private static String escape(String s, boolean key) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '=':
                case ':':
                case '#':
                case '!':
                    sb.append('\\').append(c);
                    break;
                case ' ':
                    if (key || i == 0) {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /** TeaVM's package names keep a trailing '.' ("a.b." instead of "a.b"). */
    public static String packageName(Package pkg) {
        String name = pkg.getName();
        return name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
    }

    /** Like the JDK, classes in the default package have no Package. */
    public static Package classGetPackage(Class<?> cls) {
        return cls.getName().indexOf('.') < 0 ? null : cls.getPackage();
    }

    public static boolean isAnonymousClass(Class<?> cls) {
        String name = cls.getName();
        int dollar = name.lastIndexOf('$');
        return dollar >= 0 && dollar + 1 < name.length() && Character.isDigit(name.charAt(dollar + 1));
    }

    public static java.lang.reflect.Type getGenericSuperclass(Class<?> cls) {
        return GenericSignatures.genericSuperclass(cls);
    }

    public static Enumeration<java.net.URL> getSystemResources(String name) {
        return retro.rt.Resources.loaderGetResources(null, name);
    }
}
