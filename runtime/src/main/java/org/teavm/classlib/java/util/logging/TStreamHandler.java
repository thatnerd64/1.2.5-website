package org.teavm.classlib.java.util.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class TStreamHandler extends THandler {
    private Writer writer;
    private boolean headWritten;

    public TStreamHandler() {
        setFormatter(new TSimpleFormatter());
    }

    public TStreamHandler(OutputStream out, TFormatter formatter) {
        setFormatter(formatter);
        setOutputStream(out);
    }

    protected synchronized void setOutputStream(OutputStream out) {
        flushAndClose();
        writer = out == null ? null : new OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8);
        headWritten = false;
    }

    @Override
    public synchronized void publish(TLogRecord record) {
        if (writer == null || !isLoggable(record)) {
            return;
        }
        try {
            if (!headWritten) {
                writer.write(getFormatter().getHead(this));
                headWritten = true;
            }
            writer.write(getFormatter().format(record));
        } catch (IOException e) {
            reportError(null, e, TErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public synchronized void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                reportError(null, e, TErrorManager.FLUSH_FAILURE);
            }
        }
    }

    private void flushAndClose() {
        if (writer != null) {
            try {
                if (headWritten) {
                    writer.write(getFormatter().getTail(this));
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                reportError(null, e, TErrorManager.CLOSE_FAILURE);
            }
            writer = null;
        }
    }

    @Override
    public synchronized void close() {
        flushAndClose();
    }
}
