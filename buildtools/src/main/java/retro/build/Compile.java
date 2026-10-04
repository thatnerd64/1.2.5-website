package retro.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.vm.TeaVMOptimizationLevel;

/** Runs TeaVM over the runtime shims + prepared game classes, producing {@code classes.js}. */
public final class Compile {
    private Compile() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            opts.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        }
        boolean dev = Boolean.parseBoolean(opts.getOrDefault("dev", "false"));
        List<File> classPath = new ArrayList<>();
        for (String entry : opts.get("classpath").split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                classPath.add(new File(entry));
            }
        }

        TeaVMTool tool = new TeaVMTool();
        tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
        tool.setMainClass(opts.getOrDefault("main", "retro.glue.Main"));
        tool.setTargetDirectory(new File(opts.get("out")));
        tool.setTargetFileName("classes.js");
        tool.setClassPath(classPath);
        tool.setObfuscated(!dev);
        String level = opts.getOrDefault("opt", "");
        tool.setOptimizationLevel(!level.isBlank() ? TeaVMOptimizationLevel.valueOf(level.toUpperCase())
                : dev ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.ADVANCED);
        tool.setDebugInformationGenerated(false);
        tool.setSourceMapsFileGenerated(dev);
        if (!dev) {
            // Don't ship a source map left over from an earlier development build.
            new File(opts.get("out"), "classes.js.map").delete();
        }
        tool.setStrict(false);
        if (opts.containsKey("cache")) {
            tool.setIncremental(dev);
            tool.setCacheDirectory(new File(opts.get("cache")));
        }
        tool.setLog(new ConsoleTeaVMToolLog(false));
        tool.generate();

        String graphFile = System.getProperty("retro.dumpCallGraph");
        if (graphFile != null && !graphFile.isBlank()) {
            dumpCallGraph(tool.getDependencyInfo(), new File(graphFile));
        }

        var problems = tool.getProblemProvider();
        if (!problems.getSevereProblems().isEmpty()) {
            TeaVMProblemRenderer.describeProblems(tool.getDependencyInfo().getCallGraph(), problems, tool.getLog());
            System.err.println(problems.getSevereProblems().size() + " TeaVM error(s)");
            System.exit(1);
        }
    }

    /**
     * Writes the compiled program's call graph for offline analysis of why methods end up in TeaVM's slow
     * resumable form: {@code M id flags signature} lines (flag A: annotated @Async, S: synchronized or takes a
     * monitor) followed by {@code E caller callee} lines (ids of the M lines).
     */
    private static void dumpCallGraph(org.teavm.dependency.DependencyInfo info, File out) throws Exception {
        var source = info.getClassSource();
        var graph = info.getCallGraph();
        java.util.Map<org.teavm.model.MethodReference, Integer> ids = new java.util.HashMap<>();
        java.util.List<org.teavm.model.MethodReference> methods = new ArrayList<>(info.getReachableMethods());
        for (org.teavm.model.MethodReference m : methods) {
            ids.put(m, ids.size());
        }
        try (java.io.PrintWriter w = new java.io.PrintWriter(new java.io.BufferedWriter(
                new java.io.FileWriter(out), 1 << 20))) {
            for (org.teavm.model.MethodReference m : methods) {
                StringBuilder flags = new StringBuilder();
                var cls = source.get(m.getClassName());
                var method = cls != null ? cls.getMethod(m.getDescriptor()) : null;
                if (method != null) {
                    if (method.getAnnotations().get(org.teavm.interop.Async.class.getName()) != null) {
                        flags.append('A');
                    }
                    if (method.hasModifier(org.teavm.model.ElementModifier.SYNCHRONIZED)) {
                        flags.append('S');
                    } else if (method.getProgram() != null && hasMonitor(method.getProgram())) {
                        flags.append('S');
                    }
                }
                w.println("M " + ids.get(m) + " " + (flags.length() == 0 ? "-" : flags) + " " + m);
            }
            for (org.teavm.model.MethodReference m : methods) {
                var node = graph.getNode(m);
                if (node == null) {
                    continue;
                }
                java.util.Set<Integer> seen = new java.util.HashSet<>();
                for (var site : node.getCallSites()) {
                    for (var callee : site.getCalledMethods()) {
                        Integer to = ids.get(callee.getMethod());
                        if (to != null && seen.add(to)) {
                            w.println("E " + ids.get(m) + " " + to);
                        }
                    }
                }
            }
        }
    }

    private static boolean hasMonitor(org.teavm.model.ProgramReader program) {
        boolean[] found = {false};
        var reader = new org.teavm.model.instructions.AbstractInstructionReader() {
            @Override
            public void monitorEnter(org.teavm.model.VariableReader objectRef) {
                found[0] = true;
            }
        };
        for (int i = 0; i < program.basicBlockCount() && !found[0]; i++) {
            program.basicBlockAt(i).readAllInstructions(reader);
        }
        return found[0];
    }
}
