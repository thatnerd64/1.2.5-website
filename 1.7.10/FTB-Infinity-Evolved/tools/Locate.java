import java.io.FileInputStream;
import java.io.InputStream;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.GeneratedLocation;
import org.teavm.model.MethodReference;

/**
 * The reverse of Symbolize: where Java methods start in classes.js of a -Pdebuginfo=true build.
 *   java -cp teavm-core.jar:... tools/Locate.java classes.js.teavmdbg 'mods.x.Y.method()V' ...
 * Prints "line:column" (1-based line, 0-based column) per entrance, comma-separated, for run.cjs --breakat=.
 */
public class Locate {
    public static void main(String[] args) throws Exception {
        DebugInformation info;
        try (InputStream in = new FileInputStream(args[0])) {
            info = DebugInformation.read(in);
        }
        StringBuilder all = new StringBuilder();
        for (int a = 1; a < args.length; a++) {
            // "pkg.Class.method(descriptor)", e.g. 'mods.x.Y.preInit()V'
            MethodReference ref = MethodReference.parse(args[a]);
            for (GeneratedLocation loc : info.getMethodEntrances(ref)) {
                String pos = (loc.getLine() + 1) + ":" + loc.getColumn();
                System.err.println(ref + " -> " + pos);
                all.append(all.length() > 0 ? "," : "").append(pos);
            }
        }
        System.out.println(all);
    }
}
