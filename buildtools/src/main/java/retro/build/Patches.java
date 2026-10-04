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
    /**
     * Static {@code boolean m(File)} methods that add a jar to the running class path (via URLClassLoader.addURL
     * reflection). Every class is linked at build time, so these just report whether the jar exists.
     */
    static final Set<String> CLASSPATH_ADDERS = Set.of(
            "PlayerHelper.addToClasspath(Ljava/io/File;)Z");

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

    /**
     * Constructors TeaVM lacks, expressed through one it has: {@code owner.<init>(desc)} becomes a call to the
     * static {@code helper} (same arguments, returns the single argument of {@code newDesc}) followed by
     * {@code owner.<init>(newDesc)}. Works for {@code new X(...)} and {@code super(...)} alike.
     */
    record CtorAdapter(String owner, String desc, String helper, String helperName, String newDesc) {
    }

    static final List<CtorAdapter> CTOR_ADAPTERS = List.of(
            new CtorAdapter("java/io/PrintWriter", "(Ljava/io/File;)V", "retro/compat/IoCompat", "writer",
                    "(Ljava/io/Writer;)V"),
            new CtorAdapter("java/io/PrintWriter", "(Ljava/lang/String;)V", "retro/compat/IoCompat", "writer",
                    "(Ljava/io/Writer;)V"),
            new CtorAdapter("java/io/PrintWriter", "(Ljava/io/File;Ljava/lang/String;)V", "retro/compat/IoCompat",
                    "writer", "(Ljava/io/Writer;)V"),
            new CtorAdapter("java/io/PrintWriter", "(Ljava/lang/String;Ljava/lang/String;)V",
                    "retro/compat/IoCompat", "writer", "(Ljava/io/Writer;)V"),
            new CtorAdapter("java/io/PrintStream", "(Ljava/io/File;)V", "retro/compat/IoCompat", "output",
                    "(Ljava/io/OutputStream;)V"),
            new CtorAdapter("java/io/PrintStream", "(Ljava/lang/String;)V", "retro/compat/IoCompat", "output",
                    "(Ljava/io/OutputStream;)V"));

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
        virtual("java/lang/Package", false, "getImplementationVersion", "()Ljava/lang/String;", sys,
                "packageImplementationVersion");
        virtual("java/lang/Package", false, "getSpecificationVersion", "()Ljava/lang/String;", sys,
                "packageSpecificationVersion");
        virtual(CLASS, false, "getPackage", "()Ljava/lang/Package;", sys, "classGetPackage");
        virtual(CLASS, false, "isAnonymousClass", "()Z", sys, "isAnonymousClass");
        virtual(CLASS, false, "getGenericSuperclass", "()Ljava/lang/reflect/Type;", sys, "getGenericSuperclass");
        virtual("java/lang/reflect/Constructor", false, "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;",
                REFLECT, "constructorNewInstance");
        virtual(CLASS, false, "newInstance", "()Ljava/lang/Object;", REFLECT, "classNewInstance");
        virtual("java/net/URL", false, "getContent", "()Ljava/lang/Object;", "retro/compat/IoCompat",
                "urlGetContent");
        virtual("java/net/URL", false, "openStream", "()Ljava/io/InputStream;", "retro/compat/IoCompat",
                "urlOpenStream");
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

    /**
     * Redirects that apply inside one class only (Minecraft 1.2.5 client names):
     * <ul>
     *   <li>GuiMultiplayer (acp) splits server addresses on ':'; {@code wss://} URLs stay whole
     *       (retro.net.ServerAddress);</li>
     *   <li>the network reader and writer threads (rl, rm) poll with {@code sleep(2)}; retro.rt.Wakeup keeps that
     *       pace in a hidden tab, where browsers throttle timers and the server would drop the client.</li>
     * </ul>
     */
    static final java.util.Map<String, List<Redirect>> SCOPED_REDIRECTS = java.util.Map.of(
            "acp", List.of(new Redirect("java/lang/String", false, "split", "(Ljava/lang/String;)[Ljava/lang/String;",
                    false, "retro/net/ServerAddress", "split")),
            "rl", List.of(new Redirect("rl", false, "sleep", "(J)V", true, "retro/rt/Wakeup", "sleep")),
            "rm", List.of(new Redirect("rm", false, "sleep", "(J)V", true, "retro/rt/Wakeup", "sleep")));

    /** A method argument replaced on entry by {@code helper(argument)}: {@code slot} is its local variable. */
    record ArgFilter(int slot, String helper, String helperName, String helperDesc) {
    }

    static final java.util.Map<String, ArgFilter> ARG_FILTERS = java.util.Map.of(
            // Packet2Handshake(user, host, port) sends "user;host:port", which servers cut off at 64 characters:
            // a wss:// URL host goes in as its host name
            "jf.<init>(Ljava/lang/String;Ljava/lang/String;I)V", new ArgFilter(2, "retro/net/ServerAddress",
                    "handshakeHost", "(Ljava/lang/String;)Ljava/lang/String;"));

    /**
     * RegionFile (lz) compresses every saved chunk with {@code new DeflaterOutputStream(out)}, i.e. zlib level 6.
     * Chunks are written on the game thread (no worker threads here), so a save was a visible stall, and deflate
     * was the largest single cost of a settled world's profile. Level 1 is several times faster for a
     * slightly larger file; the reader inflates both alike.
     */
    private static final String REGION_FILE = "lz";
    private static final int REGION_FILE_DEFLATE_LEVEL = 1;

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
                if (CLASSPATH_ADDERS.contains(className + "." + name + desc)
                        && (access & Opcodes.ACC_STATIC) != 0) {
                    changed[0] = true;
                    MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File", "exists", "()Z", false);
                    mv.visitInsn(Opcodes.IRETURN);
                    mv.visitMaxs(1, 1);
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
                ArgFilter filter = ARG_FILTERS.get(className + "." + name + desc);
                List<Redirect> scoped = SCOPED_REDIRECTS.getOrDefault(className, List.of());
                int[] extraStack = {0};
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(maxStack + extraStack[0], maxLocals);
                    }

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if (filter != null) {
                            super.visitVarInsn(Opcodes.ALOAD, filter.slot());
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, filter.helper(), filter.helperName(),
                                    filter.helperDesc(), false);
                            super.visitVarInsn(Opcodes.ASTORE, filter.slot());
                            changed[0] = true;
                        }
                    }

                    @Override
                    public void visitMethodInsn(int op, String owner, String mname, String mdesc, boolean itf) {
                        for (Redirect r : scoped) {
                            if (r.owner.equals(owner) && r.name.equals(mname) && r.desc.equals(mdesc)
                                    && r.isStatic == (op == Opcodes.INVOKESTATIC)) {
                                String newDesc = r.isStatic ? mdesc : "(L" + r.owner + ";" + mdesc.substring(1);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, r.target, r.targetName, newDesc, false);
                                changed[0] = true;
                                return;
                            }
                        }
                        if (op == Opcodes.INVOKESPECIAL && className.equals(REGION_FILE)
                                && owner.equals("java/util/zip/DeflaterOutputStream")
                                && mname.equals("<init>") && mdesc.equals("(Ljava/io/OutputStream;)V")) {
                            // stack: ... stream -> ... stream, new Deflater(level)
                            super.visitTypeInsn(Opcodes.NEW, "java/util/zip/Deflater");
                            super.visitInsn(Opcodes.DUP);
                            super.visitIntInsn(Opcodes.BIPUSH, REGION_FILE_DEFLATE_LEVEL);
                            super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/zip/Deflater", "<init>", "(I)V",
                                    false);
                            super.visitMethodInsn(op, owner, mname, "(Ljava/io/OutputStream;Ljava/util/zip/Deflater;)V",
                                    false);
                            extraStack[0] = 3;
                            changed[0] = true;
                            return;
                        }
                        if (op == Opcodes.INVOKESPECIAL && mname.equals("<init>")) {
                            for (CtorAdapter a : CTOR_ADAPTERS) {
                                if (a.owner.equals(owner) && a.desc.equals(mdesc)) {
                                    String ret = a.newDesc.substring(1, a.newDesc.indexOf(')'));
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, a.helper, a.helperName,
                                            mdesc.substring(0, mdesc.indexOf(')') + 1) + ret, false);
                                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", a.newDesc, false);
                                    changed[0] = true;
                                    return;
                                }
                            }
                        }
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
