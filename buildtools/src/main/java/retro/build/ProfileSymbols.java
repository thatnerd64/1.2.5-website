package retro.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.ExactMethodIterator;
import org.teavm.model.MethodReference;

/**
 * Writes {@code profile-symbols.json} for a profiling build: which Java method each stretch of {@code classes.js}
 * belongs to, with Minecraft's obfuscated names replaced by MCP names, so that {@code tools/profile-report.js}
 * can label the functions in a browser CPU profile.
 *
 * <pre>
 * { "format": 1, "mcp": true,
 *   "names": ["World.updateEntities", ...],
 *   "ranges": [line, column, nameIndex, ...] }   // sorted; nameIndex -1: no Java method from here on
 * </pre>
 * Lines and columns are 0-based, as in V8's profiles. The method ranges come from TeaVM's debug information
 * (outermost layer, i.e. the method a generated function was compiled from, not methods inlined into it). The MCP
 * names come from {@code client.srg} and {@code methods.csv} in Forge's source zip for 1.2.5 (MCP 6.2); without
 * it the obfuscated names are kept.
 */
public final class ProfileSymbols {
    private ProfileSymbols() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            opts.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        }
        DebugInformation info;
        try (InputStream in = new FileInputStream(opts.get("debug"))) {
            info = DebugInformation.read(in);
        }
        Mcp mcp = null;
        String zip = opts.getOrDefault("mcp", "");
        if (!zip.isEmpty() && new File(zip).isFile()) {
            mcp = Mcp.read(new File(zip));
        }
        if (mcp == null) {
            System.out.println("profile symbols: no MCP mappings (Forge 1.2.5 source zip), Minecraft methods keep their"
                    + " obfuscated names");
        }

        Map<String, Integer> nameIndex = new LinkedHashMap<>();
        List<int[]> ranges = new ArrayList<>();
        int last = Integer.MIN_VALUE;
        for (ExactMethodIterator it = info.iterateOverExactMethods(0); !it.isEndReached(); it.next()) {
            int index = -1;
            if (it.getExactMethodId() >= 0) {
                String name = displayName(it.getExactMethod(), mcp);
                index = nameIndex.computeIfAbsent(name, k -> nameIndex.size());
            }
            if (index != last) {
                ranges.add(new int[] { it.getLocation().getLine(), it.getLocation().getColumn(), index });
                last = index;
            }
        }

        File out = new File(opts.get("out"));
        out.getParentFile().mkdirs();
        try (Writer w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
            w.write("{\"format\":1,\"mcp\":" + (mcp != null) + ",\n\"names\":[");
            boolean first = true;
            for (String name : nameIndex.keySet()) {
                w.write((first ? "" : ",") + "\n\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
                first = false;
            }
            w.write("],\n\"ranges\":[");
            for (int i = 0; i < ranges.size(); i++) {
                int[] r = ranges.get(i);
                w.write((i == 0 ? "" : i % 8 == 0 ? ",\n" : ",") + r[0] + "," + r[1] + "," + r[2]);
            }
            w.write("]}\n");
        }
        System.out.println("profile symbols: " + nameIndex.size() + " methods, " + ranges.size() + " ranges -> " + out);
    }

    /** "World.updateEntities" for game classes, "java.util.HashMap.get" / "buildcraft.core.X.y" for the rest. */
    static String displayName(MethodReference method, Mcp mcp) {
        String cls = method.getClassName();
        String internal = cls.replace('.', '/');
        String name = method.getName();
        String mapped = mcp != null ? mcp.classes.get(internal) : null;
        String shown;
        if (mapped != null) {
            shown = mapped.substring(mapped.lastIndexOf('/') + 1);
        } else if (cls.startsWith("net.minecraft.")) {
            shown = cls.substring(cls.lastIndexOf('.') + 1);
        } else {
            shown = cls;
        }
        if (mcp != null && !name.startsWith("<")) {
            String searge = mcp.methods.get(internal + "/" + name + " " + method.getDescriptor().signatureToString());
            if (searge != null) {
                name = mcp.names.getOrDefault(searge, searge);
            }
        }
        return shown + "." + name;
    }

    /** MCP 6.2 mappings: obfuscated class -> MCP class, obfuscated method -> searge name -> MCP name. */
    static final class Mcp {
        final Map<String, String> classes = new HashMap<>();
        final Map<String, String> methods = new HashMap<>();
        final Map<String, String> names = new HashMap<>();

        static Mcp read(File zipFile) throws IOException {
            Mcp mcp = new Mcp();
            String srg = null;
            String csv = null;
            try (ZipFile zip = new ZipFile(zipFile)) {
                for (ZipEntry e : java.util.Collections.list(zip.entries())) {
                    if (e.getName().endsWith("conf/client.srg")) {
                        srg = read(zip, e);
                    } else if (e.getName().endsWith("conf/methods.csv")) {
                        csv = read(zip, e);
                    }
                }
            }
            if (srg == null || csv == null) {
                System.out.println("profile symbols: " + zipFile + " has no conf/client.srg + conf/methods.csv");
                return null;
            }
            for (String line : srg.split("\r?\n")) {
                String[] p = line.trim().split("\\s+");
                if (p[0].equals("CL:") && p.length >= 3) {
                    mcp.classes.put(p[1], p[2]);
                } else if (p[0].equals("MD:") && p.length >= 5) {
                    // MD: obfClass/name desc  mcpClass/func_N_x desc
                    mcp.methods.put(p[1] + " " + p[2], p[3].substring(p[3].lastIndexOf('/') + 1));
                }
            }
            List<List<String>> rows = csv(csv);
            for (List<String> row : rows.subList(1, rows.size())) {
                // searge,name,side,desc. Before 1.3 MCP numbered client (0) and server (1) methods separately,
                // so the same func_N can be two different methods.
                if (row.size() >= 3 && row.get(2).equals("0")) {
                    mcp.names.put(row.get(0), row.get(1));
                }
            }
            return mcp;
        }

        private static String read(ZipFile zip, ZipEntry e) throws IOException {
            try (InputStream in = zip.getInputStream(e)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                return out.toString(StandardCharsets.UTF_8);
            }
        }

        /** RFC 4180 rows: quoted fields may hold commas, quotes ("") and line breaks. */
        private static List<List<String>> csv(String text) {
            List<List<String>> rows = new ArrayList<>();
            List<String> row = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (quoted) {
                    if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else if (c == '"') {
                        quoted = false;
                    } else {
                        field.append(c);
                    }
                } else if (c == '"') {
                    quoted = true;
                } else if (c == ',') {
                    row.add(field.toString());
                    field.setLength(0);
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(row);
                    row = new ArrayList<>();
                } else {
                    field.append(c);
                }
            }
            if (field.length() > 0 || !row.isEmpty()) {
                row.add(field.toString());
                rows.add(row);
            }
            return rows;
        }
    }
}
