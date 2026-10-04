package org.teavm.classlib.java.util.logging;

public abstract class TFormatter {
    protected TFormatter() {
    }

    public abstract String format(TLogRecord record);

    public String getHead(THandler h) {
        return "";
    }

    public String getTail(THandler h) {
        return "";
    }

    public String formatMessage(TLogRecord record) {
        String format = record.getMessage();
        Object[] parameters = record.getParameters();
        if (format == null || parameters == null || parameters.length == 0) {
            return format;
        }
        try {
            if (format.indexOf("{0") >= 0 || format.indexOf("{1") >= 0 || format.indexOf("{2") >= 0
                    || format.indexOf("{3") >= 0) {
                return java.text.MessageFormat.format(format, parameters);
            }
            return format;
        } catch (Exception ex) {
            return format;
        }
    }
}
