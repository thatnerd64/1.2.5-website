package org.teavm.classlib.java.io;

import java.io.IOException;

/**
 * java.io.LineNumberReader, which TeaVM 0.15 lacks (IC2 and OpenPeripheral read their configuration through it). Lines
 * end at '\n', '\r' or "\r\n", as on the JVM.
 */
public class TLineNumberReader extends TBufferedReader {
    private int lineNumber;
    private int markedLineNumber;
    private boolean skipLf;
    private boolean markedSkipLf;

    public TLineNumberReader(TReader in) {
        super(in);
    }

    public TLineNumberReader(TReader in, int size) {
        super(in, size);
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        if (skipLf) {
            skipLf = false;
            if (c == '\n') {
                c = super.read();
            }
        }
        if (c == '\r') {
            skipLf = true;
            lineNumber++;
            return '\n';
        }
        if (c == '\n') {
            lineNumber++;
        }
        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int n = super.read(cbuf, off, len);
        for (int i = off; i < off + n; i++) {
            char c = cbuf[i];
            if (skipLf) {
                skipLf = false;
                if (c == '\n') {
                    continue;
                }
            }
            if (c == '\r') {
                skipLf = true;
                lineNumber++;
            } else if (c == '\n') {
                lineNumber++;
            }
        }
        return n;
    }

    @Override
    public String readLine() throws IOException {
        StringBuilder sb = null;
        while (true) {
            int c = read();
            if (c < 0) {
                return sb == null ? null : sb.toString();
            }
            if (sb == null) {
                sb = new StringBuilder();
            }
            if (c == '\n') {
                return sb.toString();
            }
            sb.append((char) c);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = 0;
        while (skipped < n && read() >= 0) {
            skipped++;
        }
        return skipped;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        super.mark(readAheadLimit);
        markedLineNumber = lineNumber;
        markedSkipLf = skipLf;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        lineNumber = markedLineNumber;
        skipLf = markedSkipLf;
    }
}
