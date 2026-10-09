package retro.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Lists the members of the browser runtime's shim classes that the game calls but the shims lack.
 *
 * <pre>java retro.build.ApiCheck game.jar runtime.jar org/lwjgl/ paulscode/ ...</pre>
 */
public final class ApiCheck {
    private ApiCheck() {
    }

    record Shim(String superName, Set<String> members) {
    }

    public static void main(String[] args) throws IOException {
        Path game = Path.of(args[0]);
        Path runtime = Path.of(args[1]);
        String[] prefixes = new String[args.length - 2];
        System.arraycopy(args, 2, prefixes, 0, prefixes.length);

        Map<String, Shim> shims = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(Files.newInputStream(runtime))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (!e.getName().endsWith(".class")) {
                    continue;
                }
                ClassReader r = new ClassReader(z.readAllBytes());
                Set<String> members = new TreeSet<>();
                r.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public FieldVisitor visitField(int access, String name, String desc, String sig, Object v) {
                        members.add("." + name);
                        return null;
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] x) {
                        members.add(name + desc);
                        return null;
                    }
                }, ClassReader.SKIP_CODE);
                String[] itfs = r.getInterfaces();
                shims.put(r.getClassName(), new Shim(r.getSuperName(), members));
                for (String i : itfs) {
                    shims.get(r.getClassName()).members().add("itf:" + i);
                }
            }
        }

        Map<String, Set<String>> missing = new TreeSet<String>().stream().collect(java.util.stream.Collectors.toMap(a -> a, a -> new TreeSet<String>()));
        Map<String, Set<String>> result = new java.util.TreeMap<>();
        try (ZipInputStream z = new ZipInputStream(Files.newInputStream(game))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (!e.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes = z.readAllBytes();
                String user = e.getName().substring(0, e.getName().length() - 6);
                new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] x) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                                check(owner, n + d, user);
                            }

                            @Override
                            public void visitFieldInsn(int op, String owner, String n, String d) {
                                check(owner, "." + n, user);
                            }
                        };
                    }

                    void check(String owner, String member, String user) {
                        for (String p : prefixes) {
                            if (owner.startsWith(p)) {
                                if (!has(shims, owner, member)) {
                                    result.computeIfAbsent(owner, k -> new TreeSet<>()).add(member + "   <- " + user);
                                }
                                return;
                            }
                        }
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        // Collapse the callers: print each missing member once with up to two callers.
        for (var e : result.entrySet()) {
            Map<String, java.util.List<String>> byMember = new java.util.TreeMap<>();
            for (String s : e.getValue()) {
                int i = s.indexOf("   <- ");
                byMember.computeIfAbsent(s.substring(0, i), k -> new java.util.ArrayList<>()).add(s.substring(i + 6));
            }
            if (!shims.containsKey(e.getKey())) {
                System.out.println("MISSING CLASS " + e.getKey() + " (" + byMember.size() + " members used)");
                continue;
            }
            System.out.println(e.getKey());
            for (var m : byMember.entrySet()) {
                System.out.println("    " + m.getKey() + "   <- " + m.getValue().get(0)
                        + (m.getValue().size() > 1 ? " (+" + (m.getValue().size() - 1) + ")" : ""));
            }
        }
    }

    private static boolean has(Map<String, Shim> shims, String owner, String member) {
        Shim s = shims.get(owner);
        while (s != null) {
            if (s.members().contains(member)) {
                return true;
            }
            for (String m : s.members()) {
                if (m.startsWith("itf:") && has(shims, m.substring(4), member)) {
                    return true;
                }
            }
            s = shims.get(s.superName());
        }
        return false;
    }
}
