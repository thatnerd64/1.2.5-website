package retro.compat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;

/** Redirect targets for JDK methods missing from TeaVM, here hooked to standard JVM APIs. */
public final class SystemCompat {
    private SystemCompat() {
    }

    public static void exit(int status) {
        System.exit(status);
    }

    public static long maxMemory(Runtime runtime) {
        return runtime.maxMemory();
    }

    public static void addShutdownHook(Runtime runtime, Thread hook) {
        runtime.addShutdownHook(hook);
    }

    public static boolean removeShutdownHook(Runtime runtime, Thread hook) {
        return runtime.removeShutdownHook(hook);
    }

    public static Process exec(Runtime runtime, String command) throws IOException {
        return runtime.exec(command);
    }

    public static Process exec(Runtime runtime, String[] command) throws IOException {
        return runtime.exec(command);
    }

    public static void dumpStack() {
        Thread.dumpStack();
    }

    public static void stop(Thread thread) {
        thread.interrupt();
    }

    public static void load(String name) {
        System.load(name);
    }

    public static void loadLibrary(String name) {
        System.loadLibrary(name);
    }

    public static String mapLibraryName(String name) {
        return System.mapLibraryName(name);
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> s) {
        return Collections.unmodifiableSortedSet(s);
    }

    public static void propertiesStore(Properties p, Writer w, String comments) throws IOException {
        p.store(w, comments);
    }

    public static void propertiesLoad(Properties p, InputStream in) throws IOException {
        p.load(in);
    }

    public static void propertiesStoreStream(Properties p, OutputStream out, String comments) throws IOException {
        p.store(out, comments);
    }

    public static String packageName(Package p) {
        return p != null ? p.getName() : null;
    }

    public static String packageImplementationVersion(Package p) {
        return p != null ? p.getImplementationVersion() : null;
    }

    public static String packageSpecificationVersion(Package p) {
        return p != null ? p.getSpecificationVersion() : null;
    }

    public static Package classGetPackage(Class<?> cls) {
        return cls.getPackage();
    }

    public static boolean isAnonymousClass(Class<?> cls) {
        return cls.isAnonymousClass();
    }

    public static java.lang.reflect.Type getGenericSuperclass(Class<?> cls) {
        return cls.getGenericSuperclass();
    }

    public static Enumeration<java.net.URL> getSystemResources(String name) throws IOException {
        return ClassLoader.getSystemResources(name);
    }
}
