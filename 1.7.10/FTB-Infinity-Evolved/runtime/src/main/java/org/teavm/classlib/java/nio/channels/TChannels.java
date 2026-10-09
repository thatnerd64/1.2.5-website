package org.teavm.classlib.java.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * java.nio.channels.Channels: streams and channels adapted to each other (Decocraft's CraftStudio library reads its
 * model packs through Channels.newChannel(InputStream)).
 */
public final class TChannels {
    private TChannels() {
    }

    public static ReadableByteChannel newChannel(InputStream in) {
        return new ReadableByteChannel() {
            private boolean open = true;

            @Override
            public int read(ByteBuffer dst) throws IOException {
                if (!open) {
                    throw new ClosedChannelException();
                }
                // as the JDK does: keep reading while the stream has data available without blocking (CraftStudio
                // reads a whole pack file with one call into a buffer of its size)
                int len = dst.remaining();
                int total = 0;
                byte[] buf = new byte[Math.min(len, 8192)];
                while (total < len) {
                    int n = in.read(buf, 0, Math.min(len - total, buf.length));
                    if (n < 0) {
                        return total > 0 ? total : -1;
                    }
                    dst.put(buf, 0, n);
                    total += n;
                    if (total > 0 && in.available() <= 0) {
                        break;
                    }
                }
                return total;
            }

            @Override
            public boolean isOpen() {
                return open;
            }

            @Override
            public void close() throws IOException {
                open = false;
                in.close();
            }
        };
    }

    public static WritableByteChannel newChannel(OutputStream out) {
        return new WritableByteChannel() {
            private boolean open = true;

            @Override
            public int write(ByteBuffer src) throws IOException {
                if (!open) {
                    throw new ClosedChannelException();
                }
                int n = src.remaining();
                byte[] buf = new byte[n];
                src.get(buf);
                out.write(buf);
                return n;
            }

            @Override
            public boolean isOpen() {
                return open;
            }

            @Override
            public void close() throws IOException {
                open = false;
                out.close();
            }
        };
    }

    public static InputStream newInputStream(ReadableByteChannel ch) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                byte[] one = new byte[1];
                return read(one, 0, 1) <= 0 ? -1 : one[0] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                int n;
                ByteBuffer buf = ByteBuffer.wrap(b, off, len);
                do {
                    n = ch.read(buf);
                } while (n == 0);
                return n;
            }

            @Override
            public void close() throws IOException {
                ch.close();
            }
        };
    }

    public static OutputStream newOutputStream(WritableByteChannel ch) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                write(new byte[] {(byte) b}, 0, 1);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                ByteBuffer buf = ByteBuffer.wrap(b, off, len);
                while (buf.hasRemaining()) {
                    ch.write(buf);
                }
            }

            @Override
            public void close() throws IOException {
                ch.close();
            }
        };
    }
}
