package org.teavm.classlib.org.xml.sax;

/** org.xml.sax.ContentHandler (what the SAX parser reports to; DefaultHandler implements it). */
public interface TContentHandler {
    void startDocument() throws TSAXException;

    void endDocument() throws TSAXException;

    void startPrefixMapping(String prefix, String uri) throws TSAXException;

    void endPrefixMapping(String prefix) throws TSAXException;

    void startElement(String uri, String localName, String qName, TAttributes atts) throws TSAXException;

    void endElement(String uri, String localName, String qName) throws TSAXException;

    void characters(char[] ch, int start, int length) throws TSAXException;

    void ignorableWhitespace(char[] ch, int start, int length) throws TSAXException;

    void processingInstruction(String target, String data) throws TSAXException;

    void skippedEntity(String name) throws TSAXException;
}
