package retro.fs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystem;
import retro.rt.Pak;

/**
 * The browser's file system: an in-memory tree seeded from {@code fs.pak} (the modpack's .minecraft files), with
 * everything the game writes persisted to IndexedDB (see {@link Persistence}).
 */
public final class RetroFileSystem implements VirtualFileSystem {
    static class Node {
        String name;
        Dir parent;
        long modified = System.currentTimeMillis();

        String path() {
            if (parent == null) {
                return "";
            }
            String p = parent.path();
            return p.isEmpty() ? "/" + name : p + "/" + name;
        }
    }

    static final class Dir extends Node {
        final Map<String, Node> children = new LinkedHashMap<>();
    }

    static final class FileNode extends Node {
        byte[] data = new byte[0];
        int size;
        /** Set while the content still has to be downloaded. */
        Pak lazyPak;
        Pak.Entry lazyEntry;

        void ensureLoaded() {
            if (lazyPak != null) {
                byte[] bytes = lazyPak.read(lazyEntry);
                lazyPak = null;
                lazyEntry = null;
                if (bytes != null) {
                    data = bytes;
                    size = bytes.length;
                }
            }
        }

        int length() {
            return lazyPak != null ? lazyEntry.length : size;
        }
    }

    final Dir root = new Dir();
    private final String userDir;
    Persistence persistence;

    public RetroFileSystem(String userDir) {
        this.userDir = userDir;
        root.name = "";
    }

    // ---- Tree operations ----

    Node find(String path) {
        Node node = root;
        for (String part : split(path)) {
            if (!(node instanceof Dir)) {
                return null;
            }
            if (part.equals("..")) {
                node = node.parent != null ? node.parent : node;
                continue;
            }
            node = ((Dir) node).children.get(part);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    static List<String> split(String path) {
        List<String> parts = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isEmpty() && !part.equals(".")) {
                parts.add(part);
            }
        }
        return parts;
    }

    Dir mkdirs(String path) {
        Dir dir = root;
        for (String part : split(path)) {
            Node child = dir.children.get(part);
            if (child == null) {
                Dir created = new Dir();
                created.name = part;
                created.parent = dir;
                dir.children.put(part, created);
                child = created;
                changed(created);
            }
            if (!(child instanceof Dir)) {
                return null;
            }
            dir = (Dir) child;
        }
        return dir;
    }

    /** Adds a file from the build's file image (not persisted unless the game changes it). */
    public void addBaseFile(String path, Pak pak, Pak.Entry entry) {
        int slash = path.lastIndexOf('/');
        Dir dir = slash <= 0 ? root : mkdirsQuietly(path.substring(0, slash));
        FileNode file = new FileNode();
        file.name = path.substring(slash + 1);
        file.parent = dir;
        file.lazyPak = pak;
        file.lazyEntry = entry;
        dir.children.put(file.name, file);
    }

    public Dir mkdirsQuietly(String path) {
        Persistence saved = persistence;
        persistence = null;
        try {
            return mkdirs(path);
        } finally {
            persistence = saved;
        }
    }

    void changed(Node node) {
        node.modified = System.currentTimeMillis();
        if (persistence != null) {
            persistence.markChanged(node);
        }
    }

    void removed(Node node) {
        if (persistence != null) {
            persistence.markRemoved(node);
        }
    }

    // ---- VirtualFileSystem ----

    @Override
    public String getUserDir() {
        return userDir;
    }

    @Override
    public VirtualFile getFile(String path) {
        return new Handle(path);
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        return path;
    }

    @Override
    public String[] getRoots() {
        return new String[] { "/" };
    }

    final class Handle implements VirtualFile {
        private final String path;

        Handle(String path) {
            this.path = path;
        }

        @Override
        public String getName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        @Override
        public boolean isDirectory() {
            return find(path) instanceof Dir;
        }

        @Override
        public boolean isFile() {
            return find(path) instanceof FileNode;
        }

        @Override
        public String[] listFiles() {
            Node node = find(path);
            if (!(node instanceof Dir)) {
                return null;
            }
            return ((Dir) node).children.keySet().toArray(new String[0]);
        }

        @Override
        public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
            Node node = find(path);
            if (!(node instanceof FileNode)) {
                return null;
            }
            FileNode file = (FileNode) node;
            file.ensureLoaded();
            Accessor accessor = new Accessor(file, writable, append);
            if (writable && !readable && !append) {
                // FileOutputStream(file) (write-only, not appending) starts the file over; RandomAccessFile opens
                // read-write and keeps it. Without this a shorter rewrite leaves the old tail behind (Forge configs).
                accessor.resize(0);
            }
            return accessor;
        }

        @Override
        public boolean createFile(String fileName) throws IOException {
            Node node = find(path);
            if (!(node instanceof Dir)) {
                throw new IOException("Directory does not exist: " + path);
            }
            Dir dir = (Dir) node;
            if (dir.children.containsKey(fileName)) {
                return false;
            }
            FileNode file = new FileNode();
            file.name = fileName;
            file.parent = dir;
            dir.children.put(fileName, file);
            changed(file);
            return true;
        }

        @Override
        public boolean createDirectory(String fileName) {
            Node node = find(path);
            if (!(node instanceof Dir) || ((Dir) node).children.containsKey(fileName)) {
                return false;
            }
            Dir dir = new Dir();
            dir.name = fileName;
            dir.parent = (Dir) node;
            ((Dir) node).children.put(fileName, dir);
            changed(dir);
            return true;
        }

        @Override
        public boolean delete() {
            Node node = find(path);
            if (node == null || node.parent == null) {
                return false;
            }
            if (node instanceof Dir && !((Dir) node).children.isEmpty()) {
                return false;
            }
            node.parent.children.remove(node.name);
            removed(node);
            return true;
        }

        @Override
        public boolean adopt(VirtualFile file, String fileName) {
            Node target = find(path);
            Node source = find(((Handle) file).path);
            if (!(target instanceof Dir) || source == null || source.parent == null) {
                return false;
            }
            Dir dir = (Dir) target;
            if (dir.children.containsKey(fileName)) {
                return false;
            }
            for (Node p = dir; p != null; p = p.parent) {
                if (p == source) {
                    return false;
                }
            }
            removeTree(source);
            source.parent.children.remove(source.name);
            source.name = fileName;
            source.parent = dir;
            dir.children.put(fileName, source);
            addTree(source);
            return true;
        }

        private void removeTree(Node node) {
            if (node instanceof Dir) {
                for (Node child : ((Dir) node).children.values()) {
                    removeTree(child);
                }
            }
            removed(node);
        }

        private void addTree(Node node) {
            if (node instanceof FileNode) {
                ((FileNode) node).ensureLoaded();
            }
            changed(node);
            if (node instanceof Dir) {
                for (Node child : ((Dir) node).children.values()) {
                    addTree(child);
                }
            }
        }

        @Override
        public boolean canRead() {
            return find(path) != null;
        }

        @Override
        public boolean canWrite() {
            return find(path) != null;
        }

        @Override
        public long lastModified() {
            Node node = find(path);
            return node != null ? node.modified : 0;
        }

        @Override
        public boolean setLastModified(long lastModified) {
            Node node = find(path);
            if (node == null) {
                return false;
            }
            node.modified = lastModified;
            return true;
        }

        @Override
        public boolean setReadOnly(boolean readOnly) {
            return find(path) != null;
        }

        @Override
        public int length() {
            Node node = find(path);
            return node instanceof FileNode ? ((FileNode) node).length() : 0;
        }
    }

    final class Accessor implements VirtualFileAccessor {
        private final FileNode file;
        private final boolean writable;
        private int pos;

        Accessor(FileNode file, boolean writable, boolean append) {
            this.file = file;
            this.writable = writable;
            if (append) {
                pos = file.size;
            }
        }

        @Override
        public int read(byte[] buffer, int offset, int limit) {
            int n = Math.min(limit, file.size - pos);
            if (n <= 0) {
                return limit == 0 ? 0 : -1;
            }
            System.arraycopy(file.data, pos, buffer, offset, n);
            pos += n;
            return n;
        }

        @Override
        public void write(byte[] buffer, int offset, int limit) throws IOException {
            if (!writable) {
                throw new IOException("File is read-only");
            }
            ensureCapacity(pos + limit);
            if (pos > file.size) {
                java.util.Arrays.fill(file.data, file.size, pos, (byte) 0);
            }
            System.arraycopy(buffer, offset, file.data, pos, limit);
            pos += limit;
            if (pos > file.size) {
                file.size = pos;
            }
            changed(file);
        }

        private void ensureCapacity(int size) {
            if (file.data.length < size) {
                int capacity = Math.max(size, Math.min(file.data.length * 2, file.data.length + (8 << 20)));
                byte[] data = new byte[Math.max(capacity, 256)];
                System.arraycopy(file.data, 0, data, 0, file.size);
                file.data = data;
            }
        }

        @Override
        public int tell() {
            return pos;
        }

        @Override
        public void seek(int target) {
            pos = target;
        }

        @Override
        public void skip(int amount) {
            pos += amount;
        }

        @Override
        public int size() {
            return file.size;
        }

        @Override
        public void resize(int size) {
            ensureCapacity(size);
            if (size > file.size) {
                java.util.Arrays.fill(file.data, file.size, size, (byte) 0);
            }
            file.size = size;
            if (pos > size) {
                pos = size;
            }
            changed(file);
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }
    }
}
