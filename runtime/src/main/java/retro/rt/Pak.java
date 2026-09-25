package retro.rt;

import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.Int8Array;
import retro.JS;

/** Reader for the archives written by the build ({@code retro.build.Pak}). */
public final class Pak {
    /** An entry: either a slice of the pak, or a lazy file next to it ({@code <pak>.d/<path>}). */
    public static final class Entry {
        public final String path;
        final int offset;
        public final int length;

        Entry(String path, int offset, int length) {
            this.path = path;
            this.offset = offset;
            this.length = length;
        }

        public boolean isLazy() {
            return offset < 0;
        }
    }

    private final String url;
    private final ArrayBuffer buffer;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    private Pak(String url, ArrayBuffer buffer) {
        this.url = url;
        this.buffer = buffer;
        DataView view = DataView.create(buffer);
        if (view.getUint8(0) != 'R' || view.getUint8(1) != 'P' || view.getUint8(2) != 'K' || view.getUint8(3) != '2') {
            throw new IllegalStateException(url + " is not a pak file");
        }
        int count = view.getInt32(4);
        int pos = 8;
        Int8Array bytes = new Int8Array(buffer);
        for (int i = 0; i < count; i++) {
            int nameLength = view.getInt32(pos);
            pos += 4;
            String name = Utf8.decode(bytes, pos, nameLength);
            pos += nameLength;
            int offset = view.getInt32(pos);
            int length = view.getInt32(pos + 4);
            pos += 8;
            entries.put(name, new Entry(name, offset, length));
        }
    }

    public static Pak load(String url, JS.ProgressCallback progress) {
        ArrayBuffer buffer = JS.fetch(url, progress);
        if (buffer == null) {
            throw new IllegalStateException("Could not download " + url);
        }
        return new Pak(url, buffer);
    }

    public Map<String, Entry> entries() {
        return entries;
    }

    public Entry get(String path) {
        return entries.get(path);
    }

    /** Returns the entry's data, downloading it first if it is lazy. Null if the download fails. */
    public byte[] read(Entry entry) {
        if (!entry.isLazy()) {
            return JS.toBytes(buffer, entry.offset, entry.length);
        }
        ArrayBuffer data = JS.fetch(url + ".d/" + encodePath(entry.path));
        return data == null ? null : JS.toBytes(data);
    }

    static String encodePath(String path) {
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("/")) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(encodeComponent(part));
        }
        return sb.toString();
    }

    @org.teavm.jso.JSBody(params = "s", script = "return encodeURIComponent(s);")
    private static native String encodeComponent(String s);
}
