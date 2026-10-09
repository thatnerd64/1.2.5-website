package org.teavm.classlib.java.util.logging;

public interface TFilter {
    boolean isLoggable(TLogRecord record);
}
