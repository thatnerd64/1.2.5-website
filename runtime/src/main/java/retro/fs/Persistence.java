package retro.fs;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.typedarrays.ArrayBuffer;
import retro.JS;

/**
 * Saves the game's file changes (worlds, options, configs edited in game) to IndexedDB.
 *
 * <p>Only files the game creates or modifies are stored; unmodified modpack files come from {@code fs.pak}. Each
 * record is {@code {path, type, data, mtime}} where type is {@code f} (file), {@code d} (directory) or
 * {@code x} (a deleted modpack file). Writes are batched into one transaction shortly after the last change.
 */
public final class Persistence {
    interface Record extends JSObject {
        @org.teavm.jso.JSProperty
        String getPath();

        @org.teavm.jso.JSProperty
        String getType();

        @org.teavm.jso.JSProperty
        ArrayBuffer getData();

        @org.teavm.jso.JSProperty
        double getMtime();
    }

    @JSFunctor
    interface DbCallback extends JSObject {
        void accept(JSObject db, JSArray<Record> records, String error);
    }

    @JSFunctor
    interface Flusher extends JSObject {
        void flush();
    }

    private final RetroFileSystem fs;
    private final JSObject db;
    private final Set<String> basePaths = new HashSet<>();
    /** Pending writes by path: a node to store, or null to delete. */
    private final Map<String, RetroFileSystem.Node> pending = new LinkedHashMap<>();
    private boolean scheduled;
    private static Persistence instance;

    /** Writes pending changes of the active file system, if any. */
    public static void flushAll() {
        if (instance != null) {
            instance.flush();
        }
    }

    private Persistence(RetroFileSystem fs, JSObject db) {
        this.fs = fs;
        this.db = db;
    }

    /** Opens the database and applies the saved state on top of the base files. */
    public static Persistence attach(RetroFileSystem fs, String dbName) {
        OpenResult result = open(dbName);
        if (result.error != null) {
            JS.error("Saving is disabled: IndexedDB unavailable (" + result.error + ")");
            return null;
        }
        Persistence p = new Persistence(fs, result.db);
        collectPaths(fs.root, p.basePaths);
        int applied = 0;
        for (int i = 0; i < result.records.getLength(); i++) {
            Record r = result.records.get(i);
            p.apply(r);
            applied++;
        }
        JS.log("Restored " + applied + " saved files");
        fs.persistence = p;
        instance = p;
        p.installUnloadHook();
        return p;
    }

    private static Set<String> collectPaths(RetroFileSystem.Node node, Set<String> into) {
        if (node.parent != null) {
            into.add(node.path());
        }
        if (node instanceof RetroFileSystem.Dir) {
            for (RetroFileSystem.Node child : ((RetroFileSystem.Dir) node).children.values()) {
                collectPaths(child, into);
            }
        }
        return into;
    }

    private void apply(Record r) {
        String path = r.getPath();
        switch (r.getType()) {
            case "d":
                fs.mkdirsQuietly(path);
                break;
            case "f": {
                int slash = path.lastIndexOf('/');
                RetroFileSystem.Dir dir = fs.mkdirsQuietly(slash <= 0 ? "/" : path.substring(0, slash));
                if (dir == null) {
                    return;
                }
                String name = path.substring(slash + 1);
                RetroFileSystem.Node existing = dir.children.get(name);
                RetroFileSystem.FileNode file;
                if (existing instanceof RetroFileSystem.FileNode) {
                    file = (RetroFileSystem.FileNode) existing;
                } else {
                    file = new RetroFileSystem.FileNode();
                    file.name = name;
                    file.parent = dir;
                    dir.children.put(name, file);
                }
                file.lazyPak = null;
                file.lazyEntry = null;
                file.data = JS.toBytes(r.getData());
                file.size = file.data.length;
                file.modified = (long) r.getMtime();
                break;
            }
            case "x": {
                RetroFileSystem.Node node = fs.find(path);
                if (node != null && node.parent != null) {
                    node.parent.children.remove(node.name);
                }
                break;
            }
            default:
                break;
        }
    }

    void markChanged(RetroFileSystem.Node node) {
        pending.put(node.path(), node);
        schedule();
    }

    void markRemoved(RetroFileSystem.Node node) {
        pending.put(node.path(), null);
        schedule();
    }

    private void schedule() {
        if (!scheduled) {
            scheduled = true;
            setTimeout(this::flush, 1500);
        }
    }

    /** Writes all pending changes now. */
    public void flush() {
        scheduled = false;
        if (pending.isEmpty()) {
            return;
        }
        JSObject tx = beginTransaction(db);
        for (var e : pending.entrySet()) {
            String path = e.getKey();
            RetroFileSystem.Node node = e.getValue();
            if (node == null || !path.equals(node.path()) || fs.find(path) != node) {
                if (basePaths.contains(path)) {
                    put(tx, path, "x", null, System.currentTimeMillis());
                } else {
                    delete(tx, path);
                }
            } else if (node instanceof RetroFileSystem.Dir) {
                put(tx, path, "d", null, node.modified);
            } else {
                RetroFileSystem.FileNode file = (RetroFileSystem.FileNode) node;
                put(tx, path, "f", JS.copyPrefix(file.data, file.size), node.modified);
            }
        }
        pending.clear();
    }

    private void installUnloadHook() {
        onPageHide(this::flush);
    }

    // ---- IndexedDB plumbing ----

    private static final class OpenResult {
        JSObject db;
        JSArray<Record> records;
        String error;
    }

    @Async
    private static native OpenResult open(String name);

    private static void open(String name, AsyncCallback<OpenResult> callback) {
        openImpl(name, (db, records, error) -> {
            OpenResult r = new OpenResult();
            r.db = db;
            r.records = records;
            r.error = error;
            callback.complete(r);
        });
    }

    @JSBody(params = { "name", "cb" }, script = ""
            + "if (!window.indexedDB) { cb(null, null, 'no indexedDB'); return; }"
            + "var req;"
            + "try { req = indexedDB.open(name, 1); } catch (e) { cb(null, null, String(e)); return; }"
            + "req.onupgradeneeded = function() {"
            + "  var db = req.result;"
            + "  if (!db.objectStoreNames.contains('files')) db.createObjectStore('files', { keyPath: 'path' });"
            + "};"
            + "req.onerror = function() { cb(null, null, String(req.error)); };"
            + "req.onsuccess = function() {"
            + "  var db = req.result;"
            + "  var all = db.transaction('files', 'readonly').objectStore('files').getAll();"
            + "  all.onsuccess = function() {"
            + "    var list = all.result;"
            + "    list.sort(function(a, b) { return a.path.length - b.path.length; });"
            + "    cb(db, list, null);"
            + "  };"
            + "  all.onerror = function() { cb(null, null, String(all.error)); };"
            + "};")
    private static native void openImpl(String name, DbCallback cb);

    @JSBody(params = "db", script = ""
            + "var tx = db.transaction('files', 'readwrite');"
            + "tx.onerror = function() { console.error('Saving failed', tx.error); };"
            + "return tx;")
    private static native JSObject beginTransaction(JSObject db);

    @JSBody(params = { "tx", "path", "type", "data", "mtime" }, script = ""
            + "tx.objectStore('files').put({ path: path, type: type, data: data, mtime: mtime });")
    private static native void put(JSObject tx, String path, String type, ArrayBuffer data, double mtime);

    @JSBody(params = { "tx", "path" }, script = "tx.objectStore('files').delete(path);")
    private static native void delete(JSObject tx, String path);

    @JSBody(params = { "f", "ms" }, script = "setTimeout(function() { f(); }, ms);")
    private static native void setTimeout(Flusher f, int ms);

    @JSBody(params = "f", script = ""
            + "window.addEventListener('pagehide', function() { f(); });"
            + "document.addEventListener('visibilitychange', function() {"
            + "  if (document.visibilityState === 'hidden') f();"
            + "});")
    private static native void onPageHide(Flusher f);

    /** Deletes all saved data (used by the launcher's reset button). */
    @JSBody(params = "name", script = "indexedDB.deleteDatabase(name);")
    public static native void wipe(String name);
}
