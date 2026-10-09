package org.apache.logging.log4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.message.MessageFactory;
import retro.glue.RetroLogger;

/**
 * Replacement for log4j's LogManager: the real one discovers a logger implementation through class path
 * scanning and reflection. Loggers here print to the browser console.
 */
public class LogManager {
    public static final String ROOT_LOGGER_NAME = "";

    private static final Map<String, Logger> LOGGERS = new ConcurrentHashMap<>();

    protected LogManager() {
    }

    public static Logger getLogger() {
        return getLogger(callerName());
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(Class<?> clazz, MessageFactory factory) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(MessageFactory factory) {
        return getLogger(callerName());
    }

    public static Logger getLogger(Object value) {
        return getLogger(value.getClass().getName());
    }

    public static Logger getLogger(Object value, MessageFactory factory) {
        return getLogger(value.getClass().getName());
    }

    public static Logger getLogger(String name, MessageFactory factory) {
        return getLogger(name);
    }

    public static Logger getLogger(String name) {
        Logger logger = LOGGERS.get(name);
        if (logger == null) {
            logger = new RetroLogger(name);
            LOGGERS.put(name, logger);
        }
        return logger;
    }

    public static Logger getFormatterLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getFormatterLogger(Object value) {
        return getLogger(value.getClass().getName());
    }

    public static Logger getFormatterLogger(String name) {
        return getLogger(name);
    }

    public static Logger getRootLogger() {
        return getLogger(ROOT_LOGGER_NAME);
    }

    private static String callerName() {
        return "Minecraft";
    }
}
