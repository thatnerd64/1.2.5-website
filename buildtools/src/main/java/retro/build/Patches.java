package retro.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Bytecode patches applied to every game and mod class.
 *
 * <p>Most of the JDK is provided by TeaVM's class library plus our shims (see the runtime module), so game
 * bytecode is compiled mostly untouched. The patches here cover the few places where a JDK API cannot be
 * substituted by class name alone:
 * <ul>
 *   <li>classpath resource lookups ({@code Class.getResourceAsStream} etc.) are served from the asset pack</li>
 *   <li>JDK methods that TeaVM's classes lack are redirected to static helpers in {@code retro.compat}</li>
 * </ul>
 */
final class Patches {
    /** Classes replaced wholesale by versions in the gameglue module. */
    static final List<String> REPLACED_CLASSES = List.of(
            "cpw/mods/fml/common/ModClassLoader"
    );

    /**
     * Mods that swap Minecraft classes at runtime through ClassLoader.defineClass, mapped to the resource folder
     * holding their replacement classes. The build applies these replacements directly.
     */
    static final java.util.Map<String, String> RUNTIME_CLASS_PATCHES = java.util.Map.of(
            "LumySkinPatch", "class/1.2.5/"
    );

    /** Methods whose body is replaced by an empty one (owner.name+desc). */
    static final Set<String> EMPTIED = Set.of(
            // LumySkinPatch: its classes are applied at build time (RUNTIME_CLASS_PATCHES)
            "net/thecondemned/LumySkinPatch/mod_LumySkinPatch.patchClass(Ljava/lang/String;Ljava/lang/String;)V"
    );

    private static final String RES = "retro/rt/Resources";
    private static final String REFLECT = "retro/compat/ReflectCompat";
    private static final String CLASS = "java/lang/Class";
    private static final String LOADER = "java/lang/ClassLoader";
    private static final String FIELD = "java/lang/reflect/Field";

    /** A call to redirect: {@code owner.name desc} becomes {@code INVOKESTATIC target.targetName}. */
    record Redirect(String owner, boolean ownerOrSubclass, String name, String desc, boolean isStatic,
            String target, String targetName) {
    }

    static final List<Redirect> REDIRECTS = new ArrayList<>();

    static {
        // Classpath resources -> asset pack
        virtual(CLASS, false, "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;",
                RES, "classGetResourceAsStream");
        virtual(CLASS, false, "getResource", "(Ljava/lang/String;)Ljava/net/URL;", RES, "classGetResource");
        virtual(CLASS, false, "getProtectionDomain", "()Ljava/security/ProtectionDomain;",
                RES, "classGetProtectionDomain");
        virtual(LOADER, true, "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;",
                RES, "loaderGetResourceAsStream");
        virtual(LOADER, true, "getResource", "(Ljava/lang/String;)Ljava/net/URL;", RES, "loaderGetResource");
        virtual(LOADER, true, "getResources", "(Ljava/lang/String;)Ljava/util/Enumeration;",
                RES, "loaderGetResources");
        virtual(LOADER, true, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", RES, "loaderLoadClass");
        statik(LOADER, "getSystemResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;",
                RES, "getSystemResourceAsStream");
        statik(LOADER, "getSystemResource", "(Ljava/lang/String;)Ljava/net/URL;", RES, "getSystemResource");

        // JDK methods missing from TeaVM's class library
        String sys = "retro/compat/SystemCompat";
        statik("java/lang/System", "exit", "(I)V", sys, "exit");
        statik("java/lang/System", "load", "(Ljava/lang/String;)V", sys, "load");
        statik("java/lang/System", "loadLibrary", "(Ljava/lang/String;)V", sys, "loadLibrary");
        statik("java/lang/System", "mapLibraryName", "(Ljava/lang/String;)Ljava/lang/String;", sys, "mapLibraryName");
        virtual("java/lang/Runtime", false, "maxMemory", "()J", sys, "maxMemory");
        virtual("java/lang/Runtime", false, "addShutdownHook", "(Ljava/lang/Thread;)V", sys, "addShutdownHook");
        virtual("java/lang/Runtime", false, "removeShutdownHook", "(Ljava/lang/Thread;)Z", sys, "removeShutdownHook");
        virtual("java/lang/Runtime", false, "exec", "(Ljava/lang/String;)Ljava/lang/Process;", sys, "exec");
        virtual("java/lang/Runtime", false, "exec", "([Ljava/lang/String;)Ljava/lang/Process;", sys, "exec");
        statik("java/lang/Thread", "dumpStack", "()V", sys, "dumpStack");
        virtual("java/lang/Thread", true, "stop", "()V", sys, "stop");
        statik("java/util/Collections", "unmodifiableSortedSet", "(Ljava/util/SortedSet;)Ljava/util/SortedSet;", sys,
                "unmodifiableSortedSet");
        virtual("java/util/Properties", true, "store", "(Ljava/io/Writer;Ljava/lang/String;)V", sys,
                "propertiesStore");
        virtual("java/util/Properties", true, "load", "(Ljava/io/InputStream;)V", sys, "propertiesLoad");
        virtual("java/util/Properties", true, "store", "(Ljava/io/OutputStream;Ljava/lang/String;)V", sys,
                "propertiesStoreStream");
        virtual("java/lang/Package", false, "getName", "()Ljava/lang/String;", sys, "packageName");
        virtual(CLASS, false, "getPackage", "()Ljava/lang/Package;", sys, "classGetPackage");
        virtual(CLASS, false, "isAnonymousClass", "()Z", sys, "isAnonymousClass");
        virtual(CLASS, false, "getGenericSuperclass", "()Ljava/lang/reflect/Type;", sys, "getGenericSuperclass");
        statik(LOADER, "getSystemResources", "(Ljava/lang/String;)Ljava/util/Enumeration;", sys, "getSystemResources");

        // java.lang.reflect.Field primitive accessors (TeaVM only has get/set)
        String[][] prims = {
                {"Int", "I"}, {"Long", "J"}, {"Float", "F"}, {"Double", "D"},
                {"Boolean", "Z"}, {"Byte", "B"}, {"Short", "S"}, {"Char", "C"}
        };
        for (String[] p : prims) {
            virtual(FIELD, false, "get" + p[0], "(Ljava/lang/Object;)" + p[1], REFLECT, "get" + p[0]);
            virtual(FIELD, false, "set" + p[0], "(Ljava/lang/Object;" + p[1] + ")V", REFLECT, "set" + p[0]);
        }
    }

    private static void virtual(String owner, boolean subclasses, String name, String desc, String target,
            String targetName) {
        REDIRECTS.add(new Redirect(owner, subclasses, name, desc, false, target, targetName));
    }

    private static void statik(String owner, String name, String desc, String target, String targetName) {
        REDIRECTS.add(new Redirect(owner, false, name, desc, true, target, targetName));
    }

    private Patches() {
    }

    static byte[] transform(String className, byte[] bytes, Hierarchy hierarchy, Set<String> byName,
            Set<String> allClasses, Set<String> strings) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        boolean[] changed = {false};
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                if (EMPTIED.contains(className + "." + name + desc) && desc.endsWith(")V")) {
                    changed[0] = true;
                    MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);
                    mv.visitCode();
                    mv.visitInsn(Opcodes.RETURN);
                    int locals = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
                    for (Type t : Type.getArgumentTypes(desc)) {
                        locals += t.getSize();
                    }
                    mv.visitMaxs(0, locals);
                    mv.visitEnd();
                    return null;
                }
                if ((access & Opcodes.ACC_NATIVE) != 0) {
                    // JNI methods: behave like a JVM without the native library (UnsatisfiedLinkError on call).
                    changed[0] = true;
                    MethodVisitor mv = super.visitMethod(access & ~Opcodes.ACC_NATIVE, name, desc, sig, exc);
                    mv.visitCode();
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsatisfiedLinkError");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn(className.replace('/', '.') + "." + name + desc);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsatisfiedLinkError", "<init>",
                            "(Ljava/lang/String;)V", false);
                    mv.visitInsn(Opcodes.ATHROW);
                    int locals = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
                    for (Type t : Type.getArgumentTypes(desc)) {
                        locals += t.getSize();
                    }
                    mv.visitMaxs(3, locals);
                    mv.visitEnd();
                    return null;
                }
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(int op, String owner, String mname, String mdesc, boolean itf) {
                        Redirect r = find(op, owner, mname, mdesc, hierarchy);
                        if (r != null) {
                            String newDesc = r.isStatic ? mdesc : "(L" + r.owner + ";" + mdesc.substring(1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, r.target, r.targetName, newDesc, false);
                            changed[0] = true;
                            return;
                        }
                        super.visitMethodInsn(op, owner, mname, mdesc, itf);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String s && !s.isEmpty() && s.length() < 200) {
                            strings.add(s);
                            String internal = s.replace('.', '/');
                            if (allClasses.contains(internal)) {
                                byName.add(internal.replace('/', '.'));
                            }
                        }
                        super.visitLdcInsn(value);
                    }
                };
            }
        };
        reader.accept(cv, 0);
        return changed[0] ? writer.toByteArray() : bytes;
    }

    private static Redirect find(int op, String owner, String name, String desc, Hierarchy hierarchy) {
        for (Redirect r : REDIRECTS) {
            if (!r.name.equals(name) || !r.desc.equals(desc)) {
                continue;
            }
            if (r.isStatic != (op == Opcodes.INVOKESTATIC)) {
                continue;
            }
            if (r.owner.equals(owner) || (r.ownerOrSubclass && hierarchy.isSubclass(owner, r.owner))) {
                return r;
            }
        }
        return null;
    }

    /**
     * Routes ChunkProvider's chunk lookups through {@code RetroChunkCache} (gameglue): the original
     * {@code b(II)Lack;} (provideChunk) and {@code a(II)Z} (chunkExists) are renamed and replaced by
     * straight-line methods calling the cache, which falls back to the originals. Returns null when the class
     * does not have the expected shape (another Minecraft version), leaving it untouched.
     */
    static byte[] chunkProviderCache(byte[] bytes) {
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        new ClassReader(bytes).accept(node, 0);
        org.objectweb.asm.tree.MethodNode provide = null;
        org.objectweb.asm.tree.MethodNode exists = null;
        for (org.objectweb.asm.tree.MethodNode m : node.methods) {
            if (m.name.equals("b") && m.desc.equals("(II)Lack;") && (m.access & Opcodes.ACC_STATIC) == 0) {
                provide = m;
            } else if (m.name.equals("a") && m.desc.equals("(II)Z") && (m.access & Opcodes.ACC_STATIC) == 0) {
                exists = m;
            }
        }
        if (provide == null || exists == null) {
            return null;
        }
        provide.name = "retro$provideChunk";
        provide.access = (provide.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
        exists.name = "retro$chunkExists";
        exists.access = (exists.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
        node.interfaces.add("RetroChunkSource");
        String[][] wrappers = {
            { "b", "(II)Lack;", "provideChunk", "(LRetroChunkSource;II)Lack;" },
            { "a", "(II)Z", "chunkExists", "(LRetroChunkSource;II)Z" },
        };
        for (String[] w : wrappers) {
            org.objectweb.asm.tree.MethodNode m = new org.objectweb.asm.tree.MethodNode(Opcodes.ACC_PUBLIC, w[0],
                    w[1], null, null);
            m.visitCode();
            m.visitVarInsn(Opcodes.ALOAD, 0);
            m.visitVarInsn(Opcodes.ILOAD, 1);
            m.visitVarInsn(Opcodes.ILOAD, 2);
            m.visitMethodInsn(Opcodes.INVOKESTATIC, "RetroChunkCache", w[2], w[3], false);
            m.visitInsn(w[1].endsWith("Z") ? Opcodes.IRETURN : Opcodes.ARETURN);
            m.visitMaxs(3, 3);
            m.visitEnd();
            node.methods.add(m);
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
