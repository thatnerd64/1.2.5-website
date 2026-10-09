package org.teavm.classlib.javax.imageio.stream;

import java.io.IOException;
import java.io.InputStream;

/** javax.imageio.stream.MemoryCacheImageInputStream: an image input stream over an InputStream. */
public class TMemoryCacheImageInputStream implements TImageInputStream {
    private final InputStream stream;

    public TMemoryCacheImageInputStream(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        this.stream = stream;
    }

    /** The underlying stream (for the image readers here). */
    public InputStream stream() {
        return stream;
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
    }
}
