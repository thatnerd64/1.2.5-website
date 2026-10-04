package org.teavm.classlib.java.util.logging;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class TLogManager {
    public static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";
    private static final TLogManager INSTANCE = new TLogManager();
    final Map<String, TLogger> loggers = new HashMap<>();

    protected TLogManager() {
    }

    public static TLogManager getLogManager() {
        return INSTANCE;
    }

    public TLogger getLogger(String name) {
        return loggers.get(name);
    }

    public boolean addLogger(TLogger logger) {
        if (loggers.containsKey(logger.getName())) {
            return false;
        }
        loggers.put(logger.getName(), logger);
        return true;
    }

    public Enumeration<String> getLoggerNames() {
        return java.util.Collections.enumeration(new java.util.ArrayList<>(loggers.keySet()));
    }

    public void reset() {
        for (TLogger logger : loggers.values()) {
            for (THandler h : logger.getHandlers()) {
                logger.removeHandler(h);
                h.close();
            }
        }
    }

    public void readConfiguration() {
    }

    public void readConfiguration(java.io.InputStream in) {
    }

    public String getProperty(String name) {
        return null;
    }
}
