package org.teavm.classlib.java.util.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** java.util.logging.Logger with handlers, parents and levels (TeaVM's own only prints to the console). */
public class TLogger {
    public static final String GLOBAL_LOGGER_NAME = "global";
    private static TLogger root;
    private final String name;
    private TLogger parent;
    private TLevel level;
    private final List<THandler> handlers = new ArrayList<>();
    private boolean useParentHandlers = true;
    private TFilter filter;

    protected TLogger(String name, String resourceBundleName) {
        this.name = name;
    }

    private static synchronized TLogger root() {
        if (root == null) {
            root = new TLogger("", null);
            root.level = TLevel.INFO;
            root.handlers.add(new TConsoleHandler());
            TLogManager.getLogManager().loggers.put("", root);
        }
        return root;
    }

    public static synchronized TLogger getLogger(String name) {
        TLogManager manager = TLogManager.getLogManager();
        TLogger logger = manager.loggers.get(name);
        if (logger == null) {
            if (name.isEmpty()) {
                return root();
            }
            logger = new TLogger(name, null);
            logger.parent = root();
            manager.loggers.put(name, logger);
        }
        return logger;
    }

    public static TLogger getLogger(String name, String resourceBundleName) {
        return getLogger(name);
    }

    public static TLogger getGlobal() {
        return getLogger(GLOBAL_LOGGER_NAME);
    }

    public static TLogger getAnonymousLogger() {
        TLogger logger = new TLogger(null, null);
        logger.parent = root();
        return logger;
    }

    public static TLogger getAnonymousLogger(String resourceBundleName) {
        return getAnonymousLogger();
    }

    public String getName() {
        return name;
    }

    public TLogger getParent() {
        return parent;
    }

    public void setParent(TLogger parent) {
        this.parent = parent;
    }

    public void setLevel(TLevel level) {
        this.level = level;
    }

    public TLevel getLevel() {
        return level;
    }

    private int effectiveLevel() {
        for (TLogger l = this; l != null; l = l.parent) {
            if (l.level != null) {
                return l.level.intValue();
            }
        }
        return TLevel.INFO.intValue();
    }

    public boolean isLoggable(TLevel level) {
        int min = effectiveLevel();
        return level.intValue() >= min && min != TLevel.OFF.intValue();
    }

    public synchronized void addHandler(THandler handler) {
        handlers.add(handler);
    }

    public synchronized void removeHandler(THandler handler) {
        handlers.remove(handler);
    }

    public synchronized THandler[] getHandlers() {
        return handlers.toArray(new THandler[0]);
    }

    public void setUseParentHandlers(boolean use) {
        useParentHandlers = use;
    }

    public boolean getUseParentHandlers() {
        return useParentHandlers;
    }

    public void setFilter(TFilter filter) {
        this.filter = filter;
    }

    public TFilter getFilter() {
        return filter;
    }

    public String getResourceBundleName() {
        return null;
    }

    public void log(TLogRecord record) {
        if (!isLoggable(record.getLevel())) {
            return;
        }
        if (filter != null && !filter.isLoggable(record)) {
            return;
        }
        for (TLogger l = this; l != null; l = l.parent) {
            for (THandler h : l.getHandlers()) {
                h.publish(record);
            }
            if (!l.useParentHandlers) {
                break;
            }
        }
    }

    private void doLog(TLevel level, String msg, Throwable thrown, Object[] params, String cls, String method) {
        if (!isLoggable(level)) {
            return;
        }
        TLogRecord record = new TLogRecord(level, msg);
        record.setLoggerName(name);
        record.setThrown(thrown);
        record.setParameters(params);
        record.setSourceClassName(cls);
        record.setSourceMethodName(method);
        log(record);
    }

    public void log(TLevel level, String msg) {
        doLog(level, msg, null, null, null, null);
    }

    public void log(TLevel level, Supplier<String> msg) {
        if (isLoggable(level)) {
            doLog(level, msg.get(), null, null, null, null);
        }
    }

    public void log(TLevel level, String msg, Object param) {
        doLog(level, msg, null, new Object[] { param }, null, null);
    }

    public void log(TLevel level, String msg, Object[] params) {
        doLog(level, msg, null, params, null, null);
    }

    public void log(TLevel level, String msg, Throwable thrown) {
        doLog(level, msg, thrown, null, null, null);
    }

    public void logp(TLevel level, String cls, String method, String msg) {
        doLog(level, msg, null, null, cls, method);
    }

    public void logp(TLevel level, String cls, String method, String msg, Object param) {
        doLog(level, msg, null, new Object[] { param }, cls, method);
    }

    public void logp(TLevel level, String cls, String method, String msg, Object[] params) {
        doLog(level, msg, null, params, cls, method);
    }

    public void logp(TLevel level, String cls, String method, String msg, Throwable thrown) {
        doLog(level, msg, thrown, null, cls, method);
    }

    public void entering(String cls, String method) {
        doLog(TLevel.FINER, "ENTRY", null, null, cls, method);
    }

    public void entering(String cls, String method, Object param) {
        doLog(TLevel.FINER, "ENTRY {0}", null, new Object[] { param }, cls, method);
    }

    public void entering(String cls, String method, Object[] params) {
        doLog(TLevel.FINER, "ENTRY", null, params, cls, method);
    }

    public void exiting(String cls, String method) {
        doLog(TLevel.FINER, "RETURN", null, null, cls, method);
    }

    public void exiting(String cls, String method, Object result) {
        doLog(TLevel.FINER, "RETURN {0}", null, new Object[] { result }, cls, method);
    }

    public void throwing(String cls, String method, Throwable thrown) {
        doLog(TLevel.FINER, "THROW", thrown, null, cls, method);
    }

    public void severe(String msg) {
        log(TLevel.SEVERE, msg);
    }

    public void warning(String msg) {
        log(TLevel.WARNING, msg);
    }

    public void info(String msg) {
        log(TLevel.INFO, msg);
    }

    public void config(String msg) {
        log(TLevel.CONFIG, msg);
    }

    public void fine(String msg) {
        log(TLevel.FINE, msg);
    }

    public void finer(String msg) {
        log(TLevel.FINER, msg);
    }

    public void finest(String msg) {
        log(TLevel.FINEST, msg);
    }

    public void severe(Supplier<String> msg) {
        log(TLevel.SEVERE, msg);
    }

    public void warning(Supplier<String> msg) {
        log(TLevel.WARNING, msg);
    }

    public void info(Supplier<String> msg) {
        log(TLevel.INFO, msg);
    }

    public void fine(Supplier<String> msg) {
        log(TLevel.FINE, msg);
    }
}
