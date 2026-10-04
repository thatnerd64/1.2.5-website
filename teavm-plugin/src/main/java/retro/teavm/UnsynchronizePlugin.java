package retro.teavm;

import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Removes locking from a few classes whose {@code synchronized} methods override widely used methods.
 *
 * <p>TeaVM compiles every method that takes a monitor into the slow, resumable form (a state machine with a
 * suspend check after each call), because a contended monitor suspends the calling green thread. It then does
 * the same for every caller of any method that one of these methods overrides. A single synchronized
 * {@code Hashtable.hashCode} or {@code BufferedInputStream.read} therefore turns every {@code hashCode},
 * {@code equals}, {@code toString} and {@code InputStream.read} call site in the game, and everything that
 * calls those, into resumable code. Without this plugin about half of all compiled methods were.
 *
 * <p>The threads are cooperative: they switch only where a method suspends (sleep, wait, I/O). None of the
 * methods listed here does that inside its critical sections, so dropping the lock changes nothing. The same
 * goes for a class initializer: one synchronized method it calls makes every class that touches it resumable.
 */
public class UnsynchronizePlugin implements TeaVMPlugin {
    private static final Set<String> CLASSES = Set.of(
            "java.util.Vector",
            "java.util.Hashtable",
            "java.util.Properties",
            "java.io.BufferedInputStream",
            "java.io.PushbackReader",
            "java.io.OutputStreamWriter",
            // a mod's synchronized wrapper around two HashMaps (overrides Map.put/remove/clear)
            "buildcraft.additionalpipes.util.BidiMap",
            // Minecraft's GLAllocation (static synchronized buffer allocators), used by many class initializers
            "ew",
            // library code that locks around plain computation or one-time setup, reached from class initializers
            // or overriding common methods (read, run, close)
            "org.newsclub.net.unix.NativeLibraryLoader",
            "com.gitlab.cdagaming.craftpresence.utils.ImageUtils",
            "external.org.slf4j.LoggerFactory",
            "external.org.slf4j.helpers.SubstituteLoggerFactory",
            "com.google.gson.DefaultDateTypeAdapter",
            "com.google.gson.internal.bind.DateTypeAdapter",
            "com.google.gson.internal.bind.SqlDateTypeAdapter",
            "com.google.gson.internal.bind.TimeTypeAdapter");

    @Override
    public void install(TeaVMHost host) {
        host.add((cls, context) -> {
            if (CLASSES.contains(cls.getName())) {
                unsynchronize(cls);
            }
        });
    }

    private static void unsynchronize(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            method.getModifiers().remove(ElementModifier.SYNCHRONIZED);
            if (!method.hasProgram()) {
                continue;
            }
            Program program = method.getProgram();
            for (BasicBlock block : program.getBasicBlocks()) {
                Instruction insn = block.getFirstInstruction();
                while (insn != null) {
                    Instruction next = insn.getNext();
                    if (insn instanceof MonitorEnterInstruction || insn instanceof MonitorExitInstruction) {
                        insn.delete();
                    }
                    insn = next;
                }
            }
        }
    }
}
