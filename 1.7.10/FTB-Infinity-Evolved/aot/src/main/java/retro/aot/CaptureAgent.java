package retro.aot;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Java agent for the capture run (tools/capture.sh): the real client, with the whole pack, on a JVM. Records every
 * class the game defines at run time from bytes it generated itself (ForgeMultipart's trait mixins, LogisticsPipes'
 * proxy wrappers, MineTweaker's compiled scripts, ...), which a browser cannot do. The build compiles them in, and the
 * browser's ClassLoader.defineClass hands back the compiled class.
 *
 * <p>Agent arguments: the output jar. With -Dretro.capture.play=SECONDS the agent also opens a singleplayer world
 * once the main menu is up, plays for that long and quits, so world-time generators run too.
 */
public final class CaptureAgent {
    private static final Map<String, byte[]> CAPTURED = new ConcurrentHashMap<>();

    private CaptureAgent() {
    }

    /** Classes whose bytecode the game read through LaunchClassLoader.getClassBytes (ForgeMultipart's traits). */
    private static final java.util.Set<String> BYTES_READ = ConcurrentHashMap.newKeySet();

    /** Records "name TAB caller" (the first frame outside LaunchWrapper; Prepare keeps run-time readers only). */
    public static void recordBytesRequest(String name) {
        if (name == null) {
            return;
        }
        // the direct caller of getClassBytes; LaunchClassLoader's own class loading (findClass) does not count
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 0; i + 1 < stack.length; i++) {
            if (stack[i].getMethodName().equals("getClassBytes")
                    && stack[i].getClassName().equals("net.minecraft.launchwrapper.LaunchClassLoader")) {
                String caller = stack[i + 1].getClassName();
                if (!caller.startsWith("net.minecraft.launchwrapper.")) {
                    BYTES_READ.add(name + "\t" + caller);
                }
                return;
            }
        }
    }

    /** LaunchClassLoader.getClassBytes with a call to recordBytesRequest(name) on entry. */
    private static byte[] hookGetClassBytes(byte[] bytes) {
        org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(bytes);
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(reader, 0);
        reader.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM5, writer) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String mname, String desc, String sig,
                    String[] exc) {
                org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                if (!mname.equals("getClassBytes") || !desc.equals("(Ljava/lang/String;)[B")) {
                    return mv;
                }
                return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM5, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
                        super.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "retro/aot/CaptureAgent",
                                "recordBytesRequest", "(Ljava/lang/String;)V", false);
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(Math.max(maxStack, 1), maxLocals);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    public static void premain(String args, Instrumentation inst) {
        final String out = args;
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> redefined, ProtectionDomain pd,
                    byte[] bytes) {
                if ("net/minecraft/launchwrapper/LaunchClassLoader".equals(name)) {
                    try {
                        return hookGetClassBytes(bytes);
                    } catch (Throwable t) {
                        System.err.println("[capture] could not hook getClassBytes: " + t);
                        return null;
                    }
                }
                // generated classes come without a code source location (a jar's classes, even transformed, carry
                // theirs; ClassLoader.defineClass without a domain gives the loader's default one, whose code source
                // has no location)
                if (loader != null && redefined == null && (pd == null || pd.getCodeSource() == null
                        || pd.getCodeSource().getLocation() == null || generatedByLibrary(loader, name))) {
                    // defineClass(byte[], int, int) passes no name (LogisticsPipes' proxy wrappers)
                    String n = name != null ? name : classNameOf(bytes);
                    if (n != null && keep(n)) {
                        CAPTURED.put(n, bytes.clone());
                    }
                }
                return null;
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> write(out), "capture-writer"));
        String play = System.getProperty("retro.capture.play");
        if (play != null) {
            Thread t = new Thread(() -> autoplay(Integer.parseInt(play)), "capture-autoplay");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * A class defined with a library's own protection domain (cglib passes the domain of its jar), recognised by
     * having no class file its loader could have read it from.
     */
    private static boolean generatedByLibrary(ClassLoader loader, String name) {
        if (name == null || name.startsWith("java/") || name.startsWith("sun/")) {
            return false;
        }
        try {
            return loader.getResource(name + ".class") == null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean keep(String name) {
        return !name.startsWith("java/") && !name.startsWith("sun/") && !name.startsWith("com/sun/")
                && !name.startsWith("jdk/") && !name.contains("$$Lambda$")
                // FML's per-subscriber event handler classes: the browser build replaces ASMEventHandler
                && !name.substring(name.lastIndexOf('/') + 1).startsWith("ASMEventHandler_");
    }

    /** The this_class name of a class file (walks the constant pool), or null. */
    static String classNameOf(byte[] b) {
        try {
            java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(b));
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
            int thisClass = in.readUnsignedShort();
            return utf8[classIndex[thisClass]];
        } catch (Exception e) {
            return null;
        }
    }

    private static synchronized void write(String out) {
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(out))) {
            for (Map.Entry<String, byte[]> e : new TreeMap<>(CAPTURED).entrySet()) {
                jar.putNextEntry(new JarEntry(e.getKey() + ".class"));
                jar.write(e.getValue());
                jar.closeEntry();
            }
            System.err.println("[capture] wrote " + CAPTURED.size() + " generated classes to " + out);
            java.nio.file.Files.write(java.nio.file.Paths.get(out).resolveSibling("classbytes.txt"),
                    new java.util.TreeSet<>(BYTES_READ));
            System.err.println("[capture] " + BYTES_READ.size() + " classes read through getClassBytes");
        } catch (Exception e) {
            System.err.println("[capture] could not write " + out + ": " + e);
        }
    }

    /** Waits for the main menu, opens a new singleplayer world, plays, quits. Uses Minecraft's SRG names. */
    private static void autoplay(int seconds) {
        try {
            ClassLoader cl = null;
            while (cl == null) {
                Thread.sleep(2000);
                try {
                    Field f = Class.forName("net.minecraft.launchwrapper.Launch").getField("classLoader");
                    cl = (ClassLoader) f.get(null);
                } catch (Throwable ignored) {
                    // LaunchWrapper not up yet
                }
            }
            Class<?> mcClass = null;
            Object mc = null;
            while (mc == null) {
                Thread.sleep(2000);
                try {
                    mcClass = Class.forName("net.minecraft.client.Minecraft", false, cl);
                    mc = mcClass.getMethod("func_71410_x").invoke(null);
                } catch (Throwable ignored) {
                    // not loaded yet
                }
            }
            Field screen = mcClass.getDeclaredField("field_71462_r");
            screen.setAccessible(true);
            while (true) {
                Object s = screen.get(mc);
                if (s != null && s.getClass().getName().contains("MainMenu")) {
                    break;
                }
                Thread.sleep(2000);
            }
            System.err.println("[capture] main menu up; opening a world");
            Thread.sleep(5000);
            final Object game = mc;
            final Class<?> gameClass = mcClass;
            final ClassLoader loader = cl;
            Runnable open = () -> {
                try {
                    Class<?> gameType = Class.forName("net.minecraft.world.WorldSettings$GameType", true, loader);
                    Class<?> worldType = Class.forName("net.minecraft.world.WorldType", true, loader);
                    Class<?> settingsClass = Class.forName("net.minecraft.world.WorldSettings", true, loader);
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object survival = Enum.valueOf((Class) gameType, "SURVIVAL");
                    Object defaultType = worldType.getMethod("func_77130_a", String.class).invoke(null, "default");
                    Object settings = settingsClass.getConstructor(long.class, gameType, boolean.class, boolean.class,
                            worldType).newInstance(1234L, survival, true, false, defaultType);
                    gameClass.getMethod("func_71371_a", String.class, String.class, settingsClass)
                            .invoke(game, "capture", "capture", settings);
                } catch (Throwable t) {
                    System.err.println("[capture] could not open a world: " + t);
                    t.printStackTrace();
                }
            };
            gameClass.getMethod("func_152344_a", Runnable.class).invoke(game, open);
            Field player = gameClass.getDeclaredField("field_71439_g");
            player.setAccessible(true);
            long deadline = System.currentTimeMillis() + 15 * 60_000L;
            while (player.get(game) == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(2000);
            }
            System.err.println("[capture] in the world: " + (player.get(game) != null) + "; playing " + seconds + " s");
            Thread.sleep(seconds * 1000L);
            Method shutdown = gameClass.getMethod("func_71400_g");
            shutdown.invoke(game);
            System.err.println("[capture] shutdown requested");
            // Minecraft exits through System.exit; if it does not within a minute, end the JVM here
            Thread.sleep(60_000);
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("[capture] autoplay failed: " + t);
            t.printStackTrace();
        }
    }
}
