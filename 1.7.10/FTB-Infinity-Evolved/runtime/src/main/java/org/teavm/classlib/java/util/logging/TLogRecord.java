package org.teavm.classlib.java.util.logging;

import java.io.Serializable;

public class TLogRecord implements Serializable {
    private static long sequence;
    private TLevel level;
    private String message;
    private String loggerName;
    private long millis;
    private long sequenceNumber;
    private String sourceClassName;
    private String sourceMethodName;
    private Object[] parameters;
    private Throwable thrown;
    private int threadId;
    private String resourceBundleName;

    public TLogRecord(TLevel level, String msg) {
        this.level = level;
        this.message = msg;
        this.millis = System.currentTimeMillis();
        this.sequenceNumber = sequence++;
        this.threadId = (int) Thread.currentThread().getId();
    }

    public TLevel getLevel() {
        return level;
    }

    public void setLevel(TLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String name) {
        this.loggerName = name;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long seq) {
        this.sequenceNumber = seq;
    }

    public String getSourceClassName() {
        return sourceClassName;
    }

    public void setSourceClassName(String name) {
        this.sourceClassName = name;
    }

    public String getSourceMethodName() {
        return sourceMethodName;
    }

    public void setSourceMethodName(String name) {
        this.sourceMethodName = name;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public Throwable getThrown() {
        return thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    public int getThreadID() {
        return threadId;
    }

    public void setThreadID(int id) {
        this.threadId = id;
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    public void setResourceBundleName(String name) {
        this.resourceBundleName = name;
    }

    public java.util.ResourceBundle getResourceBundle() {
        return null;
    }
}
