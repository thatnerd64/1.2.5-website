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
        // TeaVM walks the call graph recursively (AsyncMethodFinder); the full modpack's call chains overflow the default stack.
        Throwable[] failure = new Throwable[1];
        Thread worker = new Thread(null, () -> {
            try {
                run(args);
            } catch (Throwable e) {
                failure[0] = e;
            }
        }, "teavm", 8L << 30);
        worker.start();
        worker.join();
        if (failure[0] != null) {
            failure[0].printStackTrace();
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
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
        // -Pobfuscate=false keeps readable names in an optimised (small) build, for stack traces
        tool.setObfuscated(Boolean.parseBoolean(opts.getOrDefault("obf", String.valueOf(!dev))));
        tool.setOptimizationLevel(dev ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.ADVANCED);
        // -Pdebuginfo=true writes classes.js.teavmdbg (tools/Symbolize.java turns a JS position in a stack trace into a Java method)
        tool.setDebugInformationGenerated(Boolean.parseBoolean(opts.getOrDefault("debuginfo", "false")));
        tool.setSourceMapsFileGenerated(dev);
        if (!dev) {
            // Don't ship a source map left over from an earlier development build.
            new File(opts.get("out"), "classes.js.map").delete();
        }
        tool.setStrict(false);
        tool.setFastDependencyAnalysis(Boolean.parseBoolean(opts.getOrDefault("fast", "false")));
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
