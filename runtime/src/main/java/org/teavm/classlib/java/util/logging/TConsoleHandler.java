package org.teavm.classlib.java.util.logging;

/** Writes log records to the browser console (warnings and errors with console.warn/console.error). */
public class TConsoleHandler extends THandler {
    public TConsoleHandler() {
        setLevel(TLevel.INFO);
        setFormatter(new TSimpleFormatter());
    }

    @Override
    public void publish(TLogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String text = getFormatter().format(record);
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        int level = record.getLevel().intValue();
        if (level >= TLevel.SEVERE.intValue()) {
            retro.JS.error(text);
        } else if (level >= TLevel.WARNING.intValue()) {
            warn(text);
        } else {
            retro.JS.log(text);
        }
    }

    @org.teavm.jso.JSBody(params = "s", script = "console.warn(s);")
    private static native void warn(String s);

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
