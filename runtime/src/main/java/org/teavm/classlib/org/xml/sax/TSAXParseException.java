package org.teavm.classlib.org.xml.sax;

public class TSAXParseException extends TSAXException {
    private final int lineNumber;

    public TSAXParseException(String message, int lineNumber) {
        super(message + " (line " + lineNumber + ")");
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return -1;
    }

    public String getSystemId() {
        return null;
    }

    public String getPublicId() {
        return null;
    }
}
