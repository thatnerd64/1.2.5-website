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
            "cpw/mods/fml/common/ModClassLoader",
            "net/minecraft/launchwrapper/Launch",
            "net/minecraft/launchwrapper/LaunchClassLoader",
            "cpw/mods/fml/common/eventhandler/ASMEventHandler",
            "cpw/mods/fml/common/discovery/JarDiscoverer",
            "cpw/mods/fml/common/functions/ArtifactVersionNameFunction",
            "net/minecraftforge/common/util/EnumHelper",
            "com/google/gson/internal/UnsafeAllocator",
            "cpw/mods/fml/common/TracingPrintStream",
            "io/netty/channel/nio/NioEventLoopGroup",
            "cpw/mods/fml/common/registry/ObjectHolderRef",
            "cpw/mods/fml/client/FMLFileResourcePack",
            "cpw/mods/fml/client/FMLFolderResourcePack",
            "org/apache/logging/log4j/LogManager",
            "org/apache/logging/log4j/core/Logger",
            "org/apache/logging/log4j/ThreadContext",
            "org/apache/logging/log4j/ThreadContext$ContextStack",
            "com/google/common/reflect/ClassPath",
            "com/google/common/reflect/ClassPath$ResourceInfo",
            "com/google/common/reflect/ClassPath$ClassInfo",
            "io/netty/channel/socket/nio/NioSocketChannel"
    );

    static final Set<String> EMPTIED = Set.of();

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
        statik("java/lang/System", "getenv", "()Ljava/util/Map;", sys, "getenv");
        statik("java/lang/System", "getenv", "(Ljava/lang/String;)Ljava/lang/String;", sys, "getenv");
        statik("java/lang/System", "load", "(Ljava/lang/String;)V", sys, "load");
        statik("java/lang/System", "loadLibrary", "(Ljava/lang/String;)V", sys, "loadLibrary");
        statik("java/lang/System", "mapLibraryName", "(Ljava/lang/String;)Ljava/lang/String;", sys, "mapLibraryName");
        virtual("java/lang/Runtime", false, "maxMemory", "()J", sys, "maxMemory");
        virtual("java/lang/Runtime", false, "addShutdownHook", "(Ljava/lang/Thread;)V", sys, "addShutdownHook");
        virtual("java/lang/Runtime", false, "removeShutdownHook", "(Ljava/lang/Thread;)Z", sys, "removeShutdownHook");
        virtual("java/lang/Runtime", false, "exec", "(Ljava/lang/String;)Ljava/lang/Process;", sys, "exec");
        virtual("java/lang/Runtime", false, "exec", "([Ljava/lang/String;)Ljava/lang/Process;", sys, "exec");
        statik("java/lang/Thread", "dumpStack", "()V", sys, "dumpStack");
        virtual("java/lang/Throwable", true, "getStackTrace", "()[Ljava/lang/StackTraceElement;", sys, "stackTrace");
        virtual("java/lang/Thread", true, "getStackTrace", "()[Ljava/lang/StackTraceElement;", sys,
                "threadStackTrace");
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
        virtual(CLASS, false, "isLocalClass", "()Z", sys, "isLocalClass");
        virtual(CLASS, false, "getGenericSuperclass", "()Ljava/lang/reflect/Type;", sys, "getGenericSuperclass");
        virtual(CLASS, false, "getTypeParameters", "()[Ljava/lang/reflect/TypeVariable;", sys, "getTypeParameters");
        virtual(CLASS, false, "getGenericInterfaces", "()[Ljava/lang/reflect/Type;", sys, "getGenericInterfaces");
        String rx = "retro/compat/RegexCompat";
        virtual("java/util/regex/Matcher", false, "replaceAll", "(Ljava/lang/String;)Ljava/lang/String;", rx, "replaceAll");
        virtual("java/util/regex/Matcher", false, "replaceFirst", "(Ljava/lang/String;)Ljava/lang/String;", rx,
                "replaceFirst");
        virtual("java/util/regex/Matcher", false, "appendReplacement",
                "(Ljava/lang/StringBuffer;Ljava/lang/String;)Ljava/util/regex/Matcher;", rx, "appendReplacement");
        virtual("java/lang/String", false, "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", rx,
                "stringReplaceAll");
        virtual("java/lang/String", false, "replaceFirst", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                rx, "stringReplaceFirst");
        virtual("java/lang/String", false, "matches", "(Ljava/lang/String;)Z", rx, "stringMatches");
        statik("java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", rx, "compile");
        statik("java/util/regex/Pattern", "compile", "(Ljava/lang/String;I)Ljava/util/regex/Pattern;", rx, "compile");
        virtual("java/lang/Throwable", true, "setStackTrace", "([Ljava/lang/StackTraceElement;)V", sys,
                "setStackTrace");
        virtual("java/lang/Throwable", true, "printStackTrace", "()V", sys, "printStackTrace");
        virtual("java/lang/Throwable", true, "printStackTrace", "(Ljava/io/PrintStream;)V", sys, "printStackTrace");
        virtual("java/lang/Throwable", true, "printStackTrace", "(Ljava/io/PrintWriter;)V", sys, "printStackTrace");
        virtual("java/lang/annotation/Annotation", true, "annotationType", "()Ljava/lang/Class;", sys,
                "annotationType");
        virtual(CLASS, false, "getSigners", "()[Ljava/lang/Object;", sys, "getSigners");
        virtual(CLASS, false, "getSimpleName", "()Ljava/lang/String;", sys, "getSimpleName");
        virtual(CLASS, false, "desiredAssertionStatus", "()Z", sys, "desiredAssertionStatus");
        virtual(CLASS, false, "getClassLoader", "()Ljava/lang/ClassLoader;", sys, "getClassLoader");
        virtual("java/lang/Thread", true, "getContextClassLoader", "()Ljava/lang/ClassLoader;", sys,
                "getContextClassLoader");
        virtual(CLASS, false, "getEnclosingMethod", "()Ljava/lang/reflect/Method;", sys, "getEnclosingMethod");
        virtual(CLASS, false, "getEnclosingConstructor", "()Ljava/lang/reflect/Constructor;", sys,
                "getEnclosingConstructor");
        virtual("java/lang/reflect/Method", false, "getGenericExceptionTypes", "()[Ljava/lang/reflect/Type;", sys,
                "methodGenericExceptionTypes");
        virtual("java/lang/reflect/Constructor", false, "getGenericExceptionTypes", "()[Ljava/lang/reflect/Type;", sys,
                "constructorGenericExceptionTypes");
        virtual("java/lang/reflect/Constructor", false, "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;",
                REFLECT, "constructorNewInstance");
        virtual(CLASS, false, "newInstance", "()Ljava/lang/Object;", REFLECT, "classNewInstance");
        virtual(FIELD, false, "getType", "()Ljava/lang/Class;", REFLECT, "fieldType");
        virtual(CLASS, false, "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", REFLECT,
                "getDeclaredField");
        // FML's call-stack inspection (SecurityManager.getClassContext): no stack in the browser
        virtual("cpw/mods/fml/common/LoadController$FMLSecurityManager", false, "getStackClasses",
                "()[Ljava/lang/Class;", "cpw/mods/fml/common/RetroCallStack", "getStackClasses");
        virtual(CLASS, false, "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", REFLECT,
                "getMethod");
        statik(CLASS, "forName", "(Ljava/lang/String;)Ljava/lang/Class;", REFLECT, "forName");
        statik(CLASS, "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", REFLECT, "forName");
        virtual("java/lang/reflect/Method", false, "invoke",
                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", REFLECT, "methodInvoke");
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

    /** Redirects for one class only. */
    static final java.util.Map<String, List<Redirect>> SCOPED_REDIRECTS = java.util.Map.of(
            // server addresses may be ws:// or wss:// URLs (multiplayer through a WebSocket relay): ServerAddress
            // splits "host:port" on ':', which would cut a URL apart
            "net/minecraft/client/multiplayer/ServerAddress", List.of(new Redirect("java/lang/String", false, "split",
                    "(Ljava/lang/String;)[Ljava/lang/String;", false, "retro/net/ServerAddress", "split")));

    /** A method argument replaced on entry by {@code helper(argument)}: {@code slot} is its local variable. */
    record ArgFilter(int slot, String helper, String helperName, String helperDesc) {
    }

    static final java.util.Map<String, ArgFilter> ARG_FILTERS = java.util.Map.of();

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
            public void visit(int version, int access, String name, String sig, String superName, String[] itfs) {
                String clean = validMemberSignature(sig, false);
                changed[0] |= !java.util.Objects.equals(clean, sig);
                super.visit(version, access, name, clean, superName, itfs);
            }

            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc, String sig,
                    Object value) {
                String clean = validFieldSignature(sig);
                changed[0] |= !java.util.Objects.equals(clean, sig);
                return super.visitField(access, name, desc, clean, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                String clean = validMemberSignature(sig, true);
                changed[0] |= !java.util.Objects.equals(clean, sig);
                sig = clean;
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
                ArgFilter filter = ARG_FILTERS.get(className + "." + name + desc);
                List<Redirect> scoped = SCOPED_REDIRECTS.getOrDefault(className, List.of());
                return new MethodVisitor(Opcodes.ASM9, mv) {
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

    /**
     * Generic signatures are only read by reflection, so a JVM never notices a malformed one (some obfuscated or
     * hand-edited mod classes carry them); TeaVM refuses to parse them. Malformed ones are dropped.
     */
    static String validMemberSignature(String sig, boolean method) {
        if (sig == null) {
            return null;
        }
        try {
            new org.objectweb.asm.signature.SignatureReader(sig).accept(new org.objectweb.asm.signature.SignatureVisitor(
                    Opcodes.ASM9) {
            });
            return sig;
        } catch (RuntimeException e) {
            return null;
        }
    }

    static String validFieldSignature(String sig) {
        if (sig == null) {
            return null;
        }
        try {
            org.objectweb.asm.signature.SignatureReader r = new org.objectweb.asm.signature.SignatureReader(sig);
            r.acceptType(new org.objectweb.asm.signature.SignatureVisitor(Opcodes.ASM9) {
            });
            // the whole string must be one type
            org.objectweb.asm.signature.SignatureWriter w = new org.objectweb.asm.signature.SignatureWriter();
            r.acceptType(w);
            return w.toString().equals(sig) ? sig : null;
        } catch (RuntimeException e) {
            return null;
        }
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
}
