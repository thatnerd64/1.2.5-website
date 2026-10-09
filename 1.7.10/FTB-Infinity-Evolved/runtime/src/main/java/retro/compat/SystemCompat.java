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

    private static final int MIN_STACK_DEPTH = 24;

    /**
     * Throwable.getStackTrace: TeaVM records no frames in JavaScript, and its arrays have no bounds checks, so code
     * that inspects its caller ({@code getStackTrace()[1].getClassName()}, ExtraUtilities' QED name) would read an
     * undefined element. The result is padded with placeholder frames that match no game class.
     */
    public static StackTraceElement[] stackTrace(Throwable t) {
        return padStack(t.getStackTrace());
    }

    public static StackTraceElement[] threadStackTrace(Thread thread) {
        return padStack(thread.getStackTrace());
    }

    private static StackTraceElement[] padStack(StackTraceElement[] frames) {
        if (frames == null) {
            frames = new StackTraceElement[0];
        }
        if (frames.length >= MIN_STACK_DEPTH) {
            return frames;
        }
        StackTraceElement[] padded = java.util.Arrays.copyOf(frames, MIN_STACK_DEPTH);
        for (int i = frames.length; i < padded.length; i++) {
            padded[i] = new StackTraceElement("java.lang.Object", "<unknown>", null, -1);
        }
        return padded;
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

    /** javac names anonymous classes Outer$1 and local classes Outer$1Local: digits only vs digits then a name. */
    private static int localPartKind(Class<?> cls) {
        String name = cls.getName();
        int dollar = name.lastIndexOf('$');
        if (dollar < 0 || dollar + 1 >= name.length() || !Character.isDigit(name.charAt(dollar + 1))) {
            return 0;
        }
        for (int i = dollar + 1; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return 2;
            }
        }
        return 1;
    }

    public static boolean isAnonymousClass(Class<?> cls) {
        return localPartKind(cls) == 1;
    }

    /** Class.isLocalClass (Guava's Types probes it to learn how this JVM reports owner types). */
    public static boolean isLocalClass(Class<?> cls) {
        return localPartKind(cls) == 2;
    }

    public static java.lang.reflect.Type getGenericSuperclass(Class<?> cls) {
        return GenericSignatures.genericSuperclass(cls);
    }

    @org.teavm.jso.JSBody(params = "t", script = "var e = t.$jsException; return e && e.stack ? String(e.stack) : '';")
    private static native String jsStack(Object t);

    /**
     * Throwable.printStackTrace: TeaVM keeps no Java stack frames, but the JavaScript stack of the throw is available
     * (names are the compiler's, readable in a development build); print that, with the causes.
     */
    public static void printStackTrace(Throwable t, java.io.PrintStream out) {
        out.println(stackText(t));
    }

    public static void printStackTrace(Throwable t, java.io.PrintWriter out) {
        out.println(stackText(t));
    }

    public static void printStackTrace(Throwable t) {
        System.err.println(stackText(t));
    }

    /** Throwable.setStackTrace: frames are not kept, so there is nothing to replace (Netty clears them on shared exceptions). */
    public static void setStackTrace(Throwable t, StackTraceElement[] trace) {
    }

    private static String stackText(Throwable t) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (Throwable c = t; c != null && depth < 8; c = c.getCause() == c ? null : c.getCause(), depth++) {
            sb.append(depth == 0 ? "" : "Caused by: ").append(c.toString());
            String js = jsStack(c);
            int nl = js.indexOf('\n');
            if (nl >= 0) {
                // first line repeats the message; keep the frames, without the runtime's own helpers
                for (String line : js.substring(nl + 1).split("\n")) {
                    if (!line.contains("$rt_fill") && !line.contains("jl_Throwable_")
                            && !line.matches(".*(Exception|Error|Throwable)__init_.*") && !line.isEmpty()) {
                        sb.append("\n").append(line.replaceAll("http://[^/]*/", ""));
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Class.getSimpleName(): the class library derives it from the text after the last '$', which fails for a top-level
     * class whose name ends in '$' (a Scala object).
     */
    private static ClassLoader gameLoader;

    /** The loader the game's classes report (LaunchWrapper's LaunchClassLoader); set once at start-up. */
    public static void setGameClassLoader(ClassLoader loader) {
        gameLoader = loader;
    }

    /**
     * Class.getClassLoader: on desktop every game class comes from LaunchClassLoader, and mods cast it to that to
     * read class bytes or define classes (ForgeMultipart, FunkyLocomotion, OpenMods); TeaVM reports its system loader.
     */
    public static ClassLoader getClassLoader(Class<?> cls) {
        if (gameLoader == null || cls.isPrimitive() || cls.getName().startsWith("java.")) {
            return cls.getClassLoader();
        }
        return gameLoader;
    }

    /** Thread.getContextClassLoader: LaunchWrapper makes LaunchClassLoader every thread's context loader. */
    public static ClassLoader getContextClassLoader(Thread thread) {
        return gameLoader != null ? gameLoader : thread.getContextClassLoader();
    }

    private static int nullItemReports;

    /**
     * Called on entry to ItemStack(Item, int, int) (see retro.build.Prepare.reportNullItems): a stack of a null item
     * means some mod's item was never created; the first few are reported with where they come from.
     */
    public static void checkItem(Object item) {
        if (item == null && nullItemReports < 8) {
            nullItemReports++;
            System.err.println("[retro] ItemStack created with a null item (some item was never registered):");
            printStackTrace(new Throwable("null item"), System.err);
            if (nullItemReports == 1) {
                // vanilla's own item fields: all null means net.minecraft.init.Items was initialized before the
                // items were registered (something touched it too early)
                try {
                    int nulls = 0;
                    int total = 0;
                    StringBuilder names = new StringBuilder();
                    for (java.lang.reflect.Field f : Class.forName("net.minecraft.init.Items").getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                            total++;
                            if (f.get(null) == null) {
                                nulls++;
                                if (nulls <= 12) {
                                    names.append(' ').append(f.getName());
                                }
                            }
                        }
                    }
                    System.err.println("[retro] net.minecraft.init.Items: " + nulls + " of " + total
                            + " fields null:" + names);
                } catch (Throwable t) {
                    System.err.println("[retro] could not inspect Items: " + t);
                }
            }
        }
    }

    /** System.getenv(): a browser has no process environment. */
    public static java.util.Map<String, String> getenv() {
        return java.util.Collections.emptyMap();
    }

    public static String getenv(String name) {
        return null;
    }

    /** Assertions are off, as on a JVM started without -ea (TeaVM's Class reports them on). */
    public static boolean desiredAssertionStatus(Class<?> cls) {
        return false;
    }

    public static String getSimpleName(Class<?> cls) {
        try {
            return cls.getSimpleName();
        } catch (RuntimeException e) {
            String name = cls.getName();
            return name.substring(name.lastIndexOf('.') + 1);
        }
    }

    /** Generic interface signatures are not kept: the raw interfaces stand in (enough for Gson/Guava lookups). */
    public static java.lang.reflect.Type[] getGenericInterfaces(Class<?> cls) {
        return GenericSignatures.genericInterfaces(cls);
    }

    public static java.lang.reflect.TypeVariable<?>[] getTypeParameters(Class<?> cls) {
        return GenericSignatures.typeParameters(cls);
    }

    /** Annotation.annotationType(): the annotation instance implements exactly its annotation interface. */
    @SuppressWarnings("unchecked")
    public static Class<? extends java.lang.annotation.Annotation> annotationType(java.lang.annotation.Annotation a) {
        for (Class<?> c : a.getClass().getInterfaces()) {
            if (c != java.lang.annotation.Annotation.class
                    && java.lang.annotation.Annotation.class.isAssignableFrom(c)) {
                return (Class<? extends java.lang.annotation.Annotation>) c;
            }
        }
        return java.lang.annotation.Annotation.class;
    }

    public static Object[] getSigners(Class<?> cls) {
        return null;
    }

    public static java.lang.reflect.Method getEnclosingMethod(Class<?> cls) {
        return null;
    }

    public static java.lang.reflect.Constructor<?> getEnclosingConstructor(Class<?> cls) {
        return null;
    }

    public static java.lang.reflect.Type[] methodGenericExceptionTypes(java.lang.reflect.Method m) {
        return m.getExceptionTypes();
    }

    public static java.lang.reflect.Type[] constructorGenericExceptionTypes(java.lang.reflect.Constructor<?> c) {
        return c.getExceptionTypes();
    }

    public static Enumeration<java.net.URL> getSystemResources(String name) {
        return retro.rt.Resources.loaderGetResources(null, name);
    }

    /** Package.getImplementationVersion: jar manifests are not kept, so the version is unknown (null). */
    public static String packageImplementationVersion(Package p) {
        return null;
    }

    /** Package.getSpecificationVersion: see above. */
    public static String packageSpecificationVersion(Package p) {
        return null;
    }
}
