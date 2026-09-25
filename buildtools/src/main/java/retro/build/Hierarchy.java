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

    Hierarchy(Map<String, byte[]> classes) {
        for (var e : classes.entrySet()) {
            superNames.put(e.getKey(), new ClassReader(e.getValue()).getSuperName());
        }
    }

    boolean isSubclass(String name, String ancestor) {
        for (int guard = 0; name != null && guard < 64; guard++) {
            if (name.equals(ancestor)) {
                return true;
            }
            String next = superNames.get(name);
            name = next != null ? next : JDK.get(name);
        }
        return false;
    }
}
