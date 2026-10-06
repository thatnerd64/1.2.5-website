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
        // Profiling build: the release optimizations, but readable function names and TeaVM's debug information
        // (classes.js.teavmdbg), which ProfileSymbols turns into the method names tools/benchmark.js reports.
        boolean profile = Boolean.parseBoolean(opts.getOrDefault("profile", "false"));
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
        tool.setObfuscated(!dev && !profile);
        tool.setOptimizationLevel(dev ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.ADVANCED);
        tool.setDebugInformationGenerated(profile);
        tool.setSourceMapsFileGenerated(dev);
        if (!dev) {
            // Don't ship a source map left over from an earlier development build.
            new File(opts.get("out"), "classes.js.map").delete();
        }
        if (!profile) {
            new File(opts.get("out"), "classes.js.teavmdbg").delete();
        }
        tool.setStrict(false);
        if (opts.containsKey("cache")) {
            tool.setIncremental(dev);
            tool.setCacheDirectory(new File(opts.get("cache")));
        }
        tool.setLog(new ConsoleTeaVMToolLog(false));
        tool.generate();

        var problems = tool.getProblemProvider();
        if (!problems.getSevereProblems().isEmpty()) {
            TeaVMProblemRenderer.describeProblems(tool.getDependencyInfo().getCallGraph(), problems, tool.getLog());
            System.err.println(problems.getSevereProblems().size() + " TeaVM error(s)");
            System.exit(1);
        }
    }
}
