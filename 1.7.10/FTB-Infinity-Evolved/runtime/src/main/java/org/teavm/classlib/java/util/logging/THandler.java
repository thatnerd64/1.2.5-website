package org.teavm.classlib.java.util.logging;

public abstract class THandler {
    private TFormatter formatter;
    private TLevel level = TLevel.ALL;
    private TFilter filter;
    private String encoding;
    private TErrorManager errorManager = new TErrorManager();

    protected THandler() {
    }

    public abstract void publish(TLogRecord record);

    public abstract void flush();

    public abstract void close() throws SecurityException;

    public synchronized void setFormatter(TFormatter formatter) {
        this.formatter = formatter;
    }

    public TFormatter getFormatter() {
        return formatter;
    }

    public synchronized void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public synchronized void setFilter(TFilter filter) {
        this.filter = filter;
    }

    public TFilter getFilter() {
        return filter;
    }

    public synchronized void setErrorManager(TErrorManager em) {
        this.errorManager = em;
    }

    public TErrorManager getErrorManager() {
        return errorManager;
    }

    protected void reportError(String msg, Exception ex, int code) {
        errorManager.error(msg, ex, code);
    }

    public synchronized void setLevel(TLevel level) {
        this.level = level;
    }

    public TLevel getLevel() {
        return level;
    }

    public boolean isLoggable(TLogRecord record) {
        if (record == null || record.getLevel().intValue() < level.intValue() || level == TLevel.OFF) {
            return false;
        }
        return filter == null || filter.isLoggable(record);
    }
}
