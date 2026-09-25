package org.teavm.classlib.java.sql;

/** java.sql.Timestamp: only what Gson's date adapters use. */
public class TTimestamp extends java.util.Date {
    private static final long serialVersionUID = 1L;

    public TTimestamp(long time) {
        super(time);
    }
}
