package retro.build;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

/**
 * Finds classes a JVM's bytecode verifier would reject because checking a method's types needs a class that
 * cannot be loaded. Mods rely on this: IC2, for example, loads its NotEnoughItems integration and treats the
 * resulting NoClassDefFoundError (raised while verifying a helper class, before any code runs) as "NEI absent".
 * TeaVM links lazily, so without this the error would surface later, in the middle of a game tick.
 *
 * <p>Like HotSpot's verifier, an assignment to an interface type needs no loading (interfaces are treated as
 * Object), while an assignment to a class type loads the target and the source's superclasses.
 */
final class Verification {
    private static final class Missing extends RuntimeException {
        final String type;

        Missing(String type) {
            super(type, null, false, false);
            this.type = type;
        }
    }

    private static final class Info {
        final boolean isInterface;
        final String superName;

        Info(boolean isInterface, String superName) {
            this.isInterface = isInterface;
            this.superName = superName;
        }
    }

    private final Map<String, byte[]> classes;
    private final Loadability loadability;
    private final Set<String> rejected = new HashSet<>();
    private final Map<String, Info> infos = new HashMap<>();
    private final Map<String, String> reasons = new HashMap<>();

    Verification(Map<String, byte[]> classes, Loadability loadability) {
        this.classes = classes;
        this.loadability = loadability;
    }

    /**
     * Classes that fail verification (to a fixpoint: a rejected class can make its users fail in turn). Only
     * classes missing on a desktop JVM count: JDK classes TeaVM lacks exist there, and fail lazily here.
     */
    Set<String> run() {
        Set<String> unavailable = new HashSet<>();
        for (String name : classes.keySet()) {
            if (!desktopLoadable(name, new HashSet<>())) {
                unavailable.add(name);
            }
        }
        Set<String> result = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var e : classes.entrySet()) {
                String name = e.getKey();
                if (unavailable.contains(name) || !referencesAny(e.getValue(),
                        n -> unavailable.contains(n) || !desktopLoadable(n, new HashSet<>()))) {
                    continue;
                }
                String missing = verify(name, e.getValue(), unavailable);
                if (missing != null) {
                    unavailable.add(name);
                    result.add(name);
                    reasons.put(name, missing);
                    changed = true;
                }
            }
        }
        return result;
    }

    String reason(String name) {
        return reasons.get(name);
    }

    /** True if the constant pool names one of the classes (as a class entry or inside a descriptor). */
    private static boolean referencesAny(byte[] bytes, java.util.function.Predicate<String> names) {
        ClassReader reader = new ClassReader(bytes);
        char[] buf = new char[reader.getMaxStringLength()];
        for (int i = 1; i < reader.getItemCount(); i++) {
            int offset = reader.getItem(i);
            if (offset == 0) {
                continue;
            }
            int tag = reader.readByte(offset - 1);
            if (tag == 7) { // CONSTANT_Class
                String cls = reader.readUTF8(offset, buf);
                if (cls != null && !cls.startsWith("[") && names.test(cls)) {
                    return true;
                }
            } else if (tag == 12) { // CONSTANT_NameAndType: check the descriptor
                String desc = reader.readUTF8(offset + 2, buf);
                int start;
                int from = 0;
                while (desc != null && (start = desc.indexOf('L', from)) >= 0) {
                    int end = desc.indexOf(';', start);
                    if (end < 0) {
                        break;
                    }
                    if (names.test(desc.substring(start + 1, end))) {
                        return true;
                    }
                    from = end + 1;
                }
            }
        }
        return false;
    }

    /** Returns the missing type that makes verification fail, or null if the class verifies. */
    private String verify(String name, byte[] bytes, Set<String> unavailable) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG);
        Type self = Type.getObjectType(name);
        Type superType = node.superName == null ? null : Type.getObjectType(node.superName);
        List<Type> itfs = new java.util.ArrayList<>();
        for (String i : node.interfaces) {
            itfs.add(Type.getObjectType(i));
        }
        boolean selfInterface = (node.access & Opcodes.ACC_INTERFACE) != 0;
        for (MethodNode m : node.methods) {
            if (m.instructions.size() == 0) {
                continue;
            }
            Analyzer<BasicValue> analyzer = new Analyzer<>(new Checker(self, superType, itfs, selfInterface,
                    unavailable));
            try {
                analyzer.analyze(name, m);
            } catch (Missing e) {
                return e.type + " (in " + m.name + m.desc + ")";
            } catch (AnalyzerException e) {
                if (e.getCause() instanceof Missing) {
                    return ((Missing) e.getCause()).type + " (in " + m.name + m.desc + ")";
                }
                // Some other verifier disagreement (e.g. ASM's stricter checks); not a load failure on a JVM.
            } catch (RuntimeException e) {
                // Analyzer limitations: ignore.
            }
        }
        return null;
    }

    private final Map<String, Boolean> desktop = new HashMap<>();

    /** Whether a desktop JVM could load the class: its supertypes exist in the game, the mods or the JDK. */
    private boolean desktopLoadable(String name, Set<String> visiting) {
        Boolean known = desktop.get(name);
        if (known != null) {
            return known;
        }
        byte[] bytes = classes.get(name);
        boolean ok;
        if (bytes == null) {
            ok = loadability.isProvided(name) || Patches.REPLACED_CLASSES.contains(name) || isJdkClass(name);
        } else if (!visiting.add(name)) {
            ok = true;
        } else {
            ClassReader r = new ClassReader(bytes);
            ok = r.getSuperName() == null || desktopLoadable(r.getSuperName(), visiting);
            for (String itf : r.getInterfaces()) {
                ok &= desktopLoadable(itf, visiting);
            }
        }
        desktop.put(name, ok);
        return ok;
    }

    private static boolean isJdkClass(String name) {
        try {
            Class.forName(name.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private Info info(String name, Set<String> unavailable) {
        if (unavailable.contains(name)) {
            throw new Missing(name);
        }
        Info info = infos.get(name);
        if (info != null) {
            return info;
        }
        byte[] bytes = classes.get(name);
        if (bytes != null) {
            ClassReader r = new ClassReader(bytes);
            info = new Info((r.getAccess() & Opcodes.ACC_INTERFACE) != 0, r.getSuperName());
        } else if (loadability.isProvided(name) || Patches.REPLACED_CLASSES.contains(name) || isJdkClass(name)) {
            info = jdkInfo(name);
        } else {
            throw new Missing(name);
        }
        infos.put(name, info);
        return info;
    }

    /** Runtime-provided classes: the JDK's own hierarchy matches TeaVM's class library for what we need. */
    private static Info jdkInfo(String name) {
        try {
            Class<?> c = Class.forName(name.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
            Class<?> s = c.getSuperclass();
            return new Info(c.isInterface(), s == null ? null : Type.getInternalName(s));
        } catch (Throwable e) {
            // Not a JDK class (our shims, LWJGL facades): unknown hierarchy, treated as a direct Object subclass.
            return new Info(false, "java/lang/Object");
        }
    }

    private final class Checker extends SimpleVerifier {
        private final Set<String> unavailable;

        Checker(Type self, Type superType, List<Type> itfs, boolean isInterface, Set<String> unavailable) {
            super(Opcodes.ASM9, self, superType, itfs, isInterface);
            this.unavailable = unavailable;
        }

        @Override
        protected boolean isInterface(Type type) {
            return type.getSort() == Type.OBJECT && info(type.getInternalName(), unavailable).isInterface;
        }

        @Override
        protected Type getSuperClass(Type type) {
            if (type.getSort() != Type.OBJECT) {
                return Type.getObjectType("java/lang/Object");
            }
            String s = info(type.getInternalName(), unavailable).superName;
            return s == null ? null : Type.getObjectType(s);
        }

        @Override
        protected boolean isAssignableFrom(Type target, Type source) {
            if (target.equals(source)) {
                return true;
            }
            if (target.getSort() == Type.OBJECT && target.getInternalName().equals("java/lang/Object")) {
                return true;
            }
            if (target.getSort() == Type.ARRAY) {
                if (source.getSort() != Type.ARRAY) {
                    return false;
                }
                Type te = target.getElementType();
                Type se = source.getElementType();
                if (target.getDimensions() == source.getDimensions()) {
                    if (te.getSort() != Type.OBJECT || se.getSort() != Type.OBJECT) {
                        return te.equals(se);
                    }
                    return isAssignableFrom(te, se);
                }
                return target.getDimensions() < source.getDimensions() && te.getSort() == Type.OBJECT
                        && (te.getInternalName().equals("java/lang/Object") || isInterface(te));
            }
            if (target.getSort() != Type.OBJECT) {
                return false;
            }
            if (source.getSort() == Type.ARRAY) {
                String t = target.getInternalName();
                return t.equals("java/lang/Cloneable") || t.equals("java/io/Serializable");
            }
            if (source.getSort() != Type.OBJECT) {
                return false;
            }
            if (isInterface(target)) {
                return true;
            }
            String t = target.getInternalName();
            String n = source.getInternalName();
            Set<String> seen = new HashSet<>();
            while (n != null && seen.add(n)) {
                if (n.equals(t)) {
                    return true;
                }
                n = info(n, unavailable).superName;
            }
            return false;
        }

        @Override
        protected Class<?> getClass(Type type) {
            throw new UnsupportedOperationException("class loading during verification: " + type);
        }
    }
}
