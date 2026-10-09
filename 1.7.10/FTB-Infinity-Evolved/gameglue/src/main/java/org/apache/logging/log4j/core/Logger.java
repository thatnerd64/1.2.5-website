package org.apache.logging.log4j.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;

/**
 * Replacement for log4j-core's Logger. Mods cast their loggers to it to add filters (INpureCore watches FML's
 * messages that way), to raise the level (FTBLib) or to wrap one (EnderCore subclasses it); the real class needs a
 * LoggerContext whose default configuration scans for plugins, which cannot work here. This one keeps the level,
 * the filters and the appenders itself and prints Minecraft-style lines to the browser console.
 */
public class Logger extends AbstractLogger {
    // FML replaces System.out/err with streams that log through us: keep the originals to print to
    private static final java.io.PrintStream OUT = System.out;
    private static final java.io.PrintStream ERR = System.err;

    private final LoggerContext context;
    private volatile Level level = Level.INFO;
    private final List<Filter> filters = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<String, Appender> appenders = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean additive = true;

    protected Logger(LoggerContext context, String name, MessageFactory messageFactory) {
        super(name, messageFactory);
        this.context = context;
        if (debugLoggers().contains(name)) {
            level = Level.ALL;
        }
    }

    private static java.util.Set<String> debugLoggers;

    /** Loggers named in the page's ?debuglog= list log everything. */
    private static java.util.Set<String> debugLoggers() {
        if (debugLoggers == null) {
            debugLoggers = new java.util.HashSet<>();
            String list = retro.JS.config("debugLoggers");
            if (list != null) {
                for (String n : list.split(",")) {
                    if (!n.trim().isEmpty()) {
                        debugLoggers.add(n.trim());
                    }
                }
            }
        }
        return debugLoggers;
    }

    protected Logger(String name) {
        this(null, name, null);
    }

    public Logger getParent() {
        return null;
    }

    public LoggerContext getContext() {
        return context;
    }

    public synchronized void setLevel(Level level) {
        if (level != null) {
            this.level = level;
        }
    }

    public Level getLevel() {
        return level;
    }

    @Override
    public void log(Marker marker, String fqcn, Level level, Message message, Throwable t) {
        if (!level.isAtLeastAsSpecificAs(this.level) || denied(marker, fqcn, level, message, t)) {
            return;
        }
        StringBuilder line = new StringBuilder();
        line.append('[').append(Thread.currentThread().getName()).append('/').append(level).append("]: ");
        if (!getName().isEmpty() && !getName().startsWith("net.minecraft")) {
            line.append('[').append(getName()).append("] ");
        }
        line.append(message.getFormattedMessage());
        if (level.isAtLeastAsSpecificAs(Level.WARN)) {
            ERR.println(line);
        } else {
            OUT.println(line);
        }
        if (t != null) {
            retro.compat.SystemCompat.printStackTrace(t, ERR);
        }
    }

    private boolean denied(Marker marker, String fqcn, Level level, Message message, Throwable t) {
        if (filters.isEmpty()) {
            return false;
        }
        LogEvent event = null;
        for (Filter filter : filters) {
            try {
                Filter.Result r = filter.filter(this, level, marker, message, t);
                if (r == Filter.Result.NEUTRAL) {
                    if (event == null) {
                        // (without the throwable: the event would wrap it in a ThrowableProxy, which resolves the
                        // code source of every stack frame's class)
                        event = new org.apache.logging.log4j.core.impl.Log4jLogEvent(getName(), marker, fqcn, level,
                                message, (Throwable) null);
                    }
                    r = filter.filter(event);
                }
                if (r == Filter.Result.DENY) {
                    return true;
                }
                if (r == Filter.Result.ACCEPT) {
                    return false;
                }
            } catch (RuntimeException e) {
                // a broken filter must not take logging down with it
            }
        }
        return false;
    }

    private boolean enabled(Level level) {
        return level.isAtLeastAsSpecificAs(this.level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message) {
        return enabled(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Throwable t) {
        return enabled(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object... params) {
        return enabled(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Object message, Throwable t) {
        return enabled(level);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Message message, Throwable t) {
        return enabled(level);
    }

    public void addAppender(Appender appender) {
        appenders.put(appender.getName(), appender);
    }

    public void removeAppender(Appender appender) {
        appenders.remove(appender.getName());
    }

    public Map<String, Appender> getAppenders() {
        return appenders;
    }

    public Iterator<Filter> getFilters() {
        return new ArrayList<>(filters).iterator();
    }

    public int filterCount() {
        return filters.size();
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public boolean isAdditive() {
        return additive;
    }

    public void setAdditive(boolean additive) {
        this.additive = additive;
    }

    @Override
    public String toString() {
        String name = getName();
        return name.isEmpty() ? "root" : name;
    }
}
