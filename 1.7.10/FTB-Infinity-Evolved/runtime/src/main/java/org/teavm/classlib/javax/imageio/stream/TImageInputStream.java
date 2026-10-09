package org.teavm.classlib.javax.imageio.stream;

import java.io.IOException;

/** javax.imageio.stream.ImageInputStream: the byte-reading part (image readers here take the bytes as a stream). */
public interface TImageInputStream extends java.io.Closeable {
    int read() throws IOException;

    int read(byte[] b, int off, int len) throws IOException;

    @Override
    void close() throws IOException;
}
