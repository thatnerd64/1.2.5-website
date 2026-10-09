package cpw.mods.fml.common.discovery;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.FMLModContainer;
import cpw.mods.fml.common.MetadataCollection;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.discovery.asm.ModAnnotation;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Type;

/**
 * Replacement for FML's jar scanner. Reading and ASM-parsing 140 jars at start-up is neither possible (no zip files
 * here) nor necessary: what discovery finds in each jar was recorded at build time (aot/.../Discovery.java) and is
 * replayed from retro/discovery.json, keyed by the jar's file name.
 */
public class JarDiscoverer implements ITypeDiscoverer {
    private static Map<String, JsonObject> entries;

    private static synchronized Map<String, JsonObject> entries() {
        if (entries == null) {
            entries = new HashMap<>();
            // (not Class.getResourceAsStream: only the patched game classes have that redirected to the asset pack)
            try {
                byte[] data = retro.rt.Resources.read("retro/discovery.json");
                if (data == null) {
                    FMLLog.severe("retro/discovery.json is missing: no mods will be found");
                } else {
                    JsonArray all = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(data),
                            StandardCharsets.UTF_8)).getAsJsonArray();
                    for (JsonElement e : all) {
                        JsonObject o = e.getAsJsonObject();
                        entries.put(o.get("file").getAsString(), o);
                    }
                    FMLLog.info("Recorded mod discovery data for %d files", entries.size());
                }
            } catch (Exception e) {
                FMLLog.severe("Cannot read the recorded mod discovery data: %s", e);
            }
        }
        return entries;
    }

    /** The asset domains (assets/&lt;domain&gt;) inside the named mod file, as recorded at build time. */
    public static java.util.Set<String> domainsFor(String fileName) {
        java.util.Set<String> result = new java.util.HashSet<>();
        JsonObject entry = entries().get(fileName);
        if (entry != null && entry.has("domains")) {
            for (JsonElement d : entry.getAsJsonArray("domains")) {
                result.add(d.getAsString());
            }
        }
        return result;
    }

    @Override
    public List<ModContainer> discover(ModCandidate candidate, ASMDataTable table) {
        List<ModContainer> found = Lists.newArrayList();
        String name = candidate.getModContainer().getName();
        FMLLog.fine("Examining file %s for potential mods", name);
        JsonObject entry = entries().get(name);
        if (entry == null) {
            return found;
        }
        if (!entry.has("classes")) {
            return found; // a class path library recorded only for its asset domains
        }
        MetadataCollection mc;
        if (entry.has("mcmod")) {
            mc = MetadataCollection.from(new ByteArrayInputStream(entry.get("mcmod").getAsString()
                    .getBytes(StandardCharsets.UTF_8)), name);
        } else {
            mc = MetadataCollection.from(null, "");
        }
        for (JsonElement c : entry.getAsJsonArray("classes")) {
            candidate.addClassEntry(c.getAsString() + ".class");
        }
        for (JsonElement a : entry.getAsJsonArray("asm")) {
            JsonObject o = a.getAsJsonObject();
            table.addASMData(candidate, o.get("a").getAsString(), o.get("c").getAsString(),
                    o.has("o") ? o.get("o").getAsString() : null, decodeMap(o.get("i")));
        }
        for (JsonElement m : entry.getAsJsonArray("mods")) {
            JsonObject o = m.getAsJsonObject();
            Map<String, Object> descriptor = decodeMap(o.get("descriptor"));
            // jar signatures are not carried over (the mods are not jars here): nothing to verify a fingerprint against
            descriptor.remove("certificateFingerprint");
            FMLModContainer container = new FMLModContainer(o.get("className").getAsString(), candidate,
                    descriptor);
            table.addContainer(container);
            found.add(container);
            container.bindMetadata(mc);
        }
        return found;
    }

    private static ModAnnotation dummyOwner() {
        return new ModAnnotation(null, null, (String) null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> decodeMap(JsonElement e) {
        return (Map<String, Object>) decode(e);
    }

    private static Object decode(JsonElement e) {
        JsonObject o = e.getAsJsonObject();
        String t = o.get("t").getAsString();
        switch (t) {
            case "n":
                return null;
            case "s":
                return o.get("v").getAsString();
            case "i":
                return o.get("v").getAsInt();
            case "l":
                return Long.valueOf(o.get("v").getAsString());
            case "z":
                return o.get("v").getAsBoolean();
            case "d":
                return Double.valueOf(o.get("v").getAsString());
            case "f":
                return Float.valueOf(o.get("v").getAsString());
            case "h":
                return o.get("v").getAsShort();
            case "y":
                return o.get("v").getAsByte();
            case "c":
                return (char) o.get("v").getAsInt();
            case "T":
                return Type.getType(o.get("v").getAsString());
            case "A": {
                JsonArray items = o.getAsJsonArray("v");
                char code = o.get("c").getAsString().charAt(0);
                int n = items.size();
                switch (code) {
                    case 'Z': {
                        boolean[] a = new boolean[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = Boolean.parseBoolean(items.get(i).getAsString());
                        }
                        return a;
                    }
                    case 'B': {
                        byte[] a = new byte[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = Byte.parseByte(items.get(i).getAsString());
                        }
                        return a;
                    }
                    case 'C': {
                        char[] a = new char[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = items.get(i).getAsString().charAt(0);
                        }
                        return a;
                    }
                    case 'S': {
                        short[] a = new short[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = Short.parseShort(items.get(i).getAsString());
                        }
                        return a;
                    }
                    case 'I': {
                        int[] a = new int[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = Integer.parseInt(items.get(i).getAsString());
                        }
                        return a;
                    }
                    case 'J': {
                        long[] a = new long[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = Long.parseLong(items.get(i).getAsString());
                        }
                        return a;
                    }
                    case 'F': {
                        float[] a = new float[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = Float.parseFloat(items.get(i).getAsString());
                        }
                        return a;
                    }
                    default: {
                        double[] a = new double[n];
                        for (int i = 0; i < n; i++) {
                            a[i] = Double.parseDouble(items.get(i).getAsString());
                        }
                        return a;
                    }
                }
            }
            case "L": {
                List<Object> list = new ArrayList<>();
                for (JsonElement x : o.getAsJsonArray("v")) {
                    list.add(decode(x));
                }
                return list;
            }
            case "M": {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> x : o.getAsJsonObject("v").entrySet()) {
                    map.put(x.getKey(), decode(x.getValue()));
                }
                return map;
            }
            case "E":
                return dummyOwner().new EnumHolder(o.get("d").getAsString(), o.get("v").getAsString());
            default:
                throw new IllegalStateException("unknown recorded value type " + t);
        }
    }
}
