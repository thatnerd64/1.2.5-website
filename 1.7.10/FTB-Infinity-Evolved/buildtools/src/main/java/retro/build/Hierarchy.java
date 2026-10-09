package retro.build;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;

/** Superclass lookups across the merged classes plus the few JDK classes that matter to the patches. */
final class Hierarchy {
    private static final Map<String, String> JDK = Map.of(
            "java/net/URLClassLoader", "java/security/SecureClassLoader",
            "java/security/SecureClassLoader", "java/lang/ClassLoader",
            "java/lang/ClassLoader", "java/lang/Object",
            "java/lang/Thread", "java/lang/Object",
            "java/util/Properties", "java/util/Hashtable",
            "java/util/Hashtable", "java/lang/Object");

    private final Map<String, String> superNames = new HashMap<>();
    private final Map<String, String[]> interfaces = new HashMap<>();

    Hierarchy(Map<String, byte[]> classes) {
        for (var e : classes.entrySet()) {
            ClassReader reader = new ClassReader(e.getValue());
            superNames.put(e.getKey(), reader.getSuperName());
            interfaces.put(e.getKey(), reader.getInterfaces());
        }
    }

    /** The class itself, its superclasses and every interface it inherits (as far as the merged classes go). */
    java.util.Set<String> ancestors(String name) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String n = queue.poll();
            if (n == null || !seen.add(n)) {
                continue;
            }
            String sup = superNames.get(n);
            if (sup != null) {
                queue.add(sup);
            }
            for (String itf : interfaces.getOrDefault(n, new String[0])) {
                queue.add(itf);
            }
        }
        return seen;
    }

    /** True if ancestor is name itself, one of its superclasses or one of the interfaces it inherits. */
    boolean isAncestor(String name, String ancestor) {
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String n = queue.poll();
            if (n == null || !seen.add(n)) {
                continue;
            }
            if (n.equals(ancestor)) {
                return true;
            }
            String sup = superNames.get(n);
            if (sup != null) {
                queue.add(sup);
            }
            for (String itf : interfaces.getOrDefault(n, new String[0])) {
                queue.add(itf);
            }
        }
        return false;
    }

    boolean isSubclass(String name, String ancestor) {
        for (int guard = 0; name != null && guard < 64; guard++) {
            if (name.equals(ancestor)) {
                return true;
            }
            String next = superNames.get(name);
            if (next == null && JDK.get(name) == null && ancestor.equals("java/lang/Throwable")
                    && (name.startsWith("java/") || name.startsWith("javax/"))
                    && (name.endsWith("Exception") || name.endsWith("Error"))) {
                return true; // JDK exception classes are not loaded here; all of them are throwables
            }
            name = next != null ? next : JDK.get(name);
        }
        return false;
    }
}
