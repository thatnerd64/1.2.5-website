package retro.teavm;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.diagnostics.Problem;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * JVM-style lazy linking. Mods reference optional classes from other mods (RedPower, newer BuildCraft APIs...)
 * and desktop-only JDK features behind runtime checks. TeaVM already compiles such references into code that
 * throws NoClassDefFoundError / NoSuchMethodError / NoSuchFieldError when reached, exactly as a JVM would, but
 * refuses to write output because it reports them as errors. This plugin turns those particular problems into
 * warnings (listed in build/teavm/unresolved.txt) so the build proceeds.
 */
public class LazyLinkingPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        host.add(new AbstractDependencyListener() {
            private Diagnostics diagnostics;

            @Override
            public void started(DependencyAgent agent) {
                diagnostics = agent.getDiagnostics();
            }

            @Override
            public void complete() {
                try {
                    downgrade(diagnostics);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Cannot adjust TeaVM diagnostics", e);
                }
            }
        });
    }

    static boolean isLinkageProblem(Problem p) {
        String text = p.getText();
        return text.endsWith(" was not found") || text.contains("which is missing in the classpath")
                || text.endsWith("is not supported on current target");
    }

    @SuppressWarnings("unchecked")
    private static void downgrade(Diagnostics diagnostics) throws ReflectiveOperationException {
        Field field = diagnostics.getClass().getDeclaredField("severeProblems");
        field.setAccessible(true);
        List<Problem> severe = (List<Problem>) field.get(diagnostics);
        List<Problem> linkage = new ArrayList<>();
        severe.removeIf(p -> {
            if (isLinkageProblem(p)) {
                linkage.add(p);
                return true;
            }
            return false;
        });
        TreeSet<String> lines = new TreeSet<>();
        for (Problem p : linkage) {
            StringBuilder sb = new StringBuilder(p.getText());
            for (Object param : p.getParams()) {
                int i = sb.indexOf("{{");
                int j = sb.indexOf("}}", i);
                if (i >= 0 && j > i) {
                    sb.replace(i, j + 2, String.valueOf(param));
                }
            }
            if (p.getLocation() != null && p.getLocation().getMethod() != null) {
                sb.append("    <- ").append(p.getLocation().getMethod());
            }
            lines.add(sb.toString());
        }
        String out = System.getProperty("retro.unresolved");
        if (out != null) {
            try {
                Files.write(Path.of(out), lines);
            } catch (IOException e) {
                // report only
            }
        }
        System.out.println("Lazy-linked " + linkage.size() + " unresolved references (see unresolved.txt)");
    }
}
