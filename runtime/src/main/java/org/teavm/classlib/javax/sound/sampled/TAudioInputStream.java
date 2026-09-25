package org.teavm.classlib.javax.sound.sampled;

import java.io.IOException;
import java.io.InputStream;

/** javax.sound.sampled.AudioInputStream: PCM frames of a known format. */
public class TAudioInputStream extends InputStream {
    protected TAudioFormat format;
    protected long frameLength;
    protected int frameSize;
    protected long framePos;
    private final InputStream stream;

    public TAudioInputStream(InputStream stream, TAudioFormat format, long length) {
        this.stream = stream;
        this.format = format;
        this.frameLength = length;
        this.frameSize = Math.max(1, format.getFrameSize());
    }

    public TAudioFormat getFormat() {
        return format;
    }

    public long getFrameLength() {
        return frameLength;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        return read(b, 0, 1) <= 0 ? -1 : b[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        len -= len % frameSize;
        if (frameLength >= 0) {
            long remaining = (frameLength - framePos) * frameSize;
            if (remaining <= 0) {
                return -1;
            }
            len = (int) Math.min(len, remaining);
        }
        if (len == 0) {
            return 0;
        }
        int total = 0;
        while (total < len) {
            int n = stream.read(b, off + total, len - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        total -= total % frameSize;
        framePos += total / frameSize;
        return total == 0 ? -1 : total;
    }

    @Override
    public long skip(long n) throws IOException {
        n -= n % frameSize;
        long skipped = stream.skip(n);
        framePos += skipped / frameSize;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        int available = stream.available();
        if (frameLength >= 0) {
            available = (int) Math.min(available, (frameLength - framePos) * frameSize);
        }
        return available;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
