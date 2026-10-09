package retro.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Reports JDK (java.*, javax.*) classes and members that the game calls but TeaVM's class library plus our shims
 * lack. Mirrors the build's naming rule: {@code java.util.Foo} is provided by {@code org.teavm.classlib.java.util.TFoo}.
 *
 * <pre>java retro.build.JdkCheck game.jar userPrefixes(comma separated, e.g. net/minecraft/,cpw/) teavm-lib.jar runtime.jar ...</pre>
 */
public final class JdkCheck {
    private JdkCheck() {
    }

    record Cls(String superName, List<String> interfaces, Set<String> members) {
    }

    static final Map<String, Cls> provided = new HashMap<>();

    static String plain(String name) {
        String prefix = "org/teavm/classlib/";
        if (!name.startsWith(prefix)) {
            return name;
        }
        String rest = name.substring(prefix.length());
        int slash = rest.lastIndexOf('/');
        String pkg = rest.substring(0, slash + 1);
        String simple = rest.substring(slash + 1);
        // nested: TFoo$Bar
        if (simple.length() > 1 && simple.charAt(0) == 'T' && Character.isUpperCase(simple.charAt(1))) {
            return pkg + simple.substring(1);
        }
        return name;
    }

    public static void main(String[] args) throws IOException {
        Path game = Path.of(args[0]);
        String[] users = args[1].split(",");
        for (int i = 2; i < args.length; i++) {
            load(Path.of(args[i]));
        }
        Map<String, Set<String>> missingClasses = new TreeMap<>();
        Map<String, Map<String, Set<String>>> missingMembers = new TreeMap<>();
        try (ZipInputStream z = new ZipInputStream(Files.newInputStream(game))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (!e.getName().endsWith(".class")) {
                    continue;
                }
                String user = e.getName().substring(0, e.getName().length() - 6);
                boolean wanted = false;
                for (String u : users) {
                    wanted |= user.startsWith(u);
                }
                if (!wanted) {
                    continue;
                }
                byte[] bytes = z.readAllBytes();
                ClassReader r = new ClassReader(bytes);
                check(r.getSuperName(), null, user, missingClasses, missingMembers);
                for (String itf : r.getInterfaces()) {
                    check(itf, null, user, missingClasses, missingMembers);
                }
                r.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] x) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                                check(owner, n + d, user, missingClasses, missingMembers);
                            }

                            @Override
                            public void visitFieldInsn(int op, String owner, String n, String d) {
                                check(owner, "." + n, user, missingClasses, missingMembers);
                            }

                            @Override
                            public void visitTypeInsn(int op, String type) {
                                check(type, null, user, missingClasses, missingMembers);
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        System.out.println("=== missing classes (referencing classes) ===");
        missingClasses.entrySet().stream().sorted((a, b) -> b.getValue().size() - a.getValue().size()).forEach(en ->
                System.out.println(en.getKey() + "   (" + en.getValue().size() + ")   e.g. " + en.getValue().iterator().next()));
        System.out.println("=== missing members ===");
        for (var en : missingMembers.entrySet()) {
            System.out.println(en.getKey());
            for (var m : en.getValue().entrySet()) {
                System.out.println("    " + m.getKey() + "   <- " + m.getValue().iterator().next()
                        + (m.getValue().size() > 1 ? " (+" + (m.getValue().size() - 1) + ")" : ""));
            }
        }
    }

    static void load(Path jar) throws IOException {
        try (ZipInputStream z = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (!e.getName().endsWith(".class")) {
                    continue;
                }
                ClassReader r = new ClassReader(z.readAllBytes());
                Set<String> members = new HashSet<>();
                r.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public FieldVisitor visitField(int access, String name, String desc, String sig, Object v) {
                        members.add("." + name);
                        return null;
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] x) {
                        members.add(name + plainDesc(desc));
                        return null;
                    }
                }, ClassReader.SKIP_CODE);
                List<String> itfs = new ArrayList<>();
                for (String i : r.getInterfaces()) {
                    itfs.add(plain(i));
                }
                provided.put(plain(r.getClassName()),
                        new Cls(r.getSuperName() == null ? null : plain(r.getSuperName()), itfs, members));
            }
        }
    }

    /** Descriptors in shim classes name shim types (TFoo); the game's descriptors name the JDK types. */
    static String plainDesc(String desc) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                sb.append('L').append(plain(desc.substring(i + 1, end))).append(';');
                i = end + 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    static boolean isJdk(String owner) {
        return owner.startsWith("java/") || owner.startsWith("javax/");
    }

    static void check(String owner, String member, String user, Map<String, Set<String>> missingClasses,
            Map<String, Map<String, Set<String>>> missingMembers) {
        if (owner == null || owner.startsWith("[") || !isJdk(owner)) {
            return;
        }
        if (!provided.containsKey(owner)) {
            missingClasses.computeIfAbsent(owner, k -> new TreeSet<>()).add(user);
            return;
        }
        if (member != null && !has(owner, member, new HashSet<>())) {
            missingMembers.computeIfAbsent(owner, k -> new TreeMap<>()).computeIfAbsent(member, k -> new TreeSet<>()).add(user);
        }
    }

    static boolean has(String owner, String member, Set<String> seen) {
        if (owner == null || !seen.add(owner)) {
            return false;
        }
        Cls c = provided.get(owner);
        if (c == null) {
            // a JDK supertype outside our knowledge (e.g. java/lang/Object): trust it
            return owner.equals("java/lang/Object") || !isJdk(owner);
        }
        if (c.members().contains(member)) {
            return true;
        }
        if (has(c.superName(), member, seen)) {
            return true;
        }
        for (String i : c.interfaces()) {
            if (has(i, member, seen)) {
                return true;
            }
        }
        return false;
    }
}
