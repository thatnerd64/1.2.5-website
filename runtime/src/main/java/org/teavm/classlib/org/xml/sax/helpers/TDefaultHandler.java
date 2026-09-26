package org.teavm.classlib.org.xml.sax.helpers;

import org.teavm.classlib.org.xml.sax.TAttributes;
import org.teavm.classlib.org.xml.sax.TSAXException;
import org.teavm.classlib.org.xml.sax.TSAXParseException;

/** org.xml.sax.helpers.DefaultHandler: every callback does nothing (fatal errors are rethrown). */
public class TDefaultHandler {
    public TDefaultHandler() {
    }

    public void startDocument() throws TSAXException {
    }

    public void endDocument() throws TSAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws TSAXException {
    }

    public void endPrefixMapping(String prefix) throws TSAXException {
    }

    public void startElement(String uri, String localName, String qName, TAttributes attributes)
            throws TSAXException {
    }

    public void endElement(String uri, String localName, String qName) throws TSAXException {
    }

    public void characters(char[] ch, int start, int length) throws TSAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws TSAXException {
    }

    public void processingInstruction(String target, String data) throws TSAXException {
    }

    public void skippedEntity(String name) throws TSAXException {
    }

    public void warning(TSAXParseException e) throws TSAXException {
    }

    public void error(TSAXParseException e) throws TSAXException {
    }

    public void fatalError(TSAXParseException e) throws TSAXException {
        throw e;
    }
}
