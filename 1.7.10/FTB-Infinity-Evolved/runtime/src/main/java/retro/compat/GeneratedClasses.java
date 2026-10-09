package retro.compat;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassLoader.defineClass in the browser. Mods generate classes at run time (ForgeMultipart's trait mixins,
 * LogisticsPipes' proxy wrappers, MineTweaker's compiled scripts, ...); a browser cannot load bytecode, so the build
 * compiles in the classes a capture run of the real game generated (tools/capture.sh) and lists them in
 * retro/generated.txt by a hash of their bytes. defineClass hands back the compiled class with the same bytes, or
 * failing that the one with the same name (the bytes can differ in details: transformers, reflection order).
 */
public final class GeneratedClasses {
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static Map<Long, String> byHash;

    /** The captured class standing in for a generated class of this name, or null. */
    public static String alias(String name) {
        return ALIASES.get(name);
    }
    private static java.util.Set<String> names;

    private GeneratedClasses() {
    }

    public static Class<?> define(String name, byte[] b, int off, int len) {
        load();
        if (name == null) {
            // defineClass(byte[], int, int): the name is in the bytes (LogisticsPipes' proxy wrappers)
            name = classNameOf(b, off);
        }
        String found = byHash.get(hash(b, off, len));
        if (found == null && name != null) {
            String dotted = name.replace('/', '.');
            if (names.contains(dotted)) {
                found = dotted;
            } else {
                // cglib ends its names with a hash of the generator's key, which differs here
                // (X$$EnhancerByCGLIB$$99552f47): a captured class with the same stem stands in for it
                String stem = hashStem(dotted);
                if (stem != null) {
                    for (String n : names) {
                        if (stem.equals(hashStem(n))) {
                            if (found != null) {
                                found = null;
                                break;
                            }
                            found = n;
                        }
                    }
                }
            }
            if (found != null) {
                System.err.println("[retro] generated class " + dotted + " differs from the captured one; using "
                        + found);
                if (!found.equals(dotted)) {
                    // the generator looks its class up by its own name next (cglib: Class.forName)
                    ALIASES.put(dotted, found);
                }
            }
        }
        if (found == null) {
            // (ClassFormatError on a JVM; TeaVM has no such class)
            throw new NoClassDefFoundError("Class generated at run time was not captured at build time: "
                    + (name != null ? name : "(unnamed)") + " (see tools/capture.sh)");
        }
        try {
            return Class.forName(found);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(found);
        }
    }

    /** The name without a trailing "$$" + hex hash, or null if it has none. */
    private static String hashStem(String name) {
        int i = name.lastIndexOf("$$");
        if (i < 0 || i + 2 >= name.length()) {
            return null;
        }
        for (int k = i + 2; k < name.length(); k++) {
            if (Character.digit(name.charAt(k), 16) < 0) {
                return null;
            }
        }
        return name.substring(0, i);
    }

    /** FNV-1a, 64 bits; Prepare computes the same over the captured bytes. */
    public static long hash(byte[] b, int off, int len) {
        long h = 0xcbf29ce484222325L;
        for (int i = off; i < off + len; i++) {
            h ^= b[i] & 0xff;
            h *= 0x100000001b3L;
        }
        return h;
    }

    /** The this_class name of a class file (walks the constant pool), or null. */
    static String classNameOf(byte[] b, int off) {
        try {
            java.io.DataInputStream in = new java.io.DataInputStream(
                    new java.io.ByteArrayInputStream(b, off, b.length - off));
            if (in.readInt() != 0xCAFEBABE) {
                return null;
            }
            in.readUnsignedShort();
            in.readUnsignedShort();
            int count = in.readUnsignedShort();
            String[] utf8 = new String[count];
            int[] classIndex = new int[count];
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1: utf8[i] = in.readUTF(); break;
                    case 7: classIndex[i] = in.readUnsignedShort(); break;
                    case 8: case 16: in.readUnsignedShort(); break;
                    case 15: in.readUnsignedByte(); in.readUnsignedShort(); break;
                    case 3: case 4: case 9: case 10: case 11: case 12: case 18: in.readInt(); break;
                    case 5: case 6: in.readLong(); i++; break;
                    default: return null;
                }
            }
            in.readUnsignedShort();
            return utf8[classIndex[in.readUnsignedShort()]];
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    private static long parseHex(String s) {
        long v = 0;
        for (int i = 0; i < s.length(); i++) {
            v = (v << 4) | Character.digit(s.charAt(i), 16);
        }
        return v;
    }

    private static void load() {
        if (byHash != null) {
            return;
        }
        byHash = new HashMap<>();
        names = new java.util.HashSet<>();
        byte[] list = retro.rt.Resources.read("retro/generated.txt");
        if (list == null) {
            return;
        }
        for (String line : new String(list, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0) {
                String name = line.substring(tab + 1);
                byHash.put(parseHex(line.substring(0, tab)), name);
                names.add(name);
            }
        }
    }
}
