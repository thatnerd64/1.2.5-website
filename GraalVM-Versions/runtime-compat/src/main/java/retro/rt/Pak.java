package retro.rt;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native / Desktop archive reader for the RPK2 format produced by retro.build.Pak. */
public final class Pak {
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

    private final Path path;
    private final Path lazyDir;
    private final RandomAccessFile raf;
    private final FileChannel channel;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public Pak(Path path) throws IOException {
        this.path = path;
        this.lazyDir = path.resolveSibling(path.getFileName().toString() + ".d");
        this.raf = new RandomAccessFile(path.toFile(), "r");
        this.channel = raf.getChannel();

        ByteBuffer header = ByteBuffer.allocate(8);
        channel.read(header);
        header.flip();
        byte[] magic = new byte[4];
        header.get(magic);
        if (magic[0] != 'R' || magic[1] != 'P' || magic[2] != 'K' || magic[3] != '2') {
            throw new IllegalStateException(path + " is not an RPK2 pak file");
        }
        int count = header.getInt();

        for (int i = 0; i < count; i++) {
            ByteBuffer lenBuf = ByteBuffer.allocate(4);
            channel.read(lenBuf);
            lenBuf.flip();
            int nameLen = lenBuf.getInt();

            ByteBuffer nameBuf = ByteBuffer.allocate(nameLen);
            channel.read(nameBuf);
            nameBuf.flip();
            String name = new String(nameBuf.array(), StandardCharsets.UTF_8);

            ByteBuffer meta = ByteBuffer.allocate(8);
            channel.read(meta);
            meta.flip();
            int offset = meta.getInt();
            int length = meta.getInt();

            entries.put(name, new Entry(name, offset, length));
        }
    }

    public Map<String, Entry> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public Entry get(String path) {
        return entries.get(path);
    }

    public byte[] read(Entry entry) {
        try {
            if (entry.isLazy()) {
                Path f = lazyDir.resolve(entry.path);
                if (Files.exists(f)) {
                    return Files.readAllBytes(f);
                }
                return null;
            }
            ByteBuffer buf = ByteBuffer.allocate(entry.length);
            channel.read(buf, entry.offset);
            return buf.array();
        } catch (IOException e) {
            System.err.println("[Pak] Error reading entry " + entry.path + ": " + e);
            return null;
        }
    }
}
