package org.teavm.classlib.java.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TSimpleFormatter extends TFormatter {
    @Override
    public synchronized String format(TLogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getLevel().getName()).append(": ").append(formatMessage(record)).append('\n');
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }
        return sb.toString();
    }
}
