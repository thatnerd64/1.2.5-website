package org.teavm.classlib.java.sql;

/** java.sql.Date: only what Gson's date adapters use. */
public class TDate extends java.util.Date {
    private static final long serialVersionUID = 1L;

    public TDate(long date) {
        super(date);
    }
}
