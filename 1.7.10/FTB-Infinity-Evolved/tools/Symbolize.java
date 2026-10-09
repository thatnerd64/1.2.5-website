import java.io.FileInputStream;
import java.io.InputStream;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.SourceLocation;

/**
 * Turns JavaScript stack positions of a -Pdebuginfo=true build into Java methods:
 *   java -cp teavm-core.jar:teavm-relocated-libs-hppc.jar:... tools/Symbolize.java classes.js.teavmdbg < log
 * Reads lines containing "(classes.js:LINE:COL)" from stdin and prints each with the Java method and source line.
 */
public class Symbolize {
    public static void main(String[] args) throws Exception {
        DebugInformation info;
        try (InputStream in = new FileInputStream(args[0])) {
            info = DebugInformation.read(in);
        }
        var pattern = java.util.regex.Pattern.compile("classes\\.js:(\\d+):(\\d+)");
        var reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        for (String line; (line = reader.readLine()) != null; ) {
            var m = pattern.matcher(line);
            if (!m.find()) {
                System.out.println(line);
                continue;
            }
            int l = Integer.parseInt(m.group(1)) - 1;
            int c = Integer.parseInt(m.group(2)) - 1;
            var method = info.getMethodAt(l, c);
            SourceLocation src = info.getSourceLocation(l, c);
            System.out.println(line.trim() + "   =>   " + (method == null ? "?" : method.getClassName() + "." + method.getName())
                    + (src == null ? "" : " (" + src.getFileName() + ":" + src.getLine() + ")"));
        }
    }
}
