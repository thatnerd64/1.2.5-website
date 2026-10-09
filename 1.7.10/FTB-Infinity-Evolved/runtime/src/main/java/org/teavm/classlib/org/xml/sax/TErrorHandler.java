package org.teavm.classlib.org.xml.sax;

/** org.xml.sax.ErrorHandler. */
public interface TErrorHandler {
    void warning(TSAXParseException exception) throws TSAXException;

    void error(TSAXParseException exception) throws TSAXException;

    void fatalError(TSAXParseException exception) throws TSAXException;
}
