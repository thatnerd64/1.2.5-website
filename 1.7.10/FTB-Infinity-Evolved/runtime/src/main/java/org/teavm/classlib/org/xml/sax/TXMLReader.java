package org.teavm.classlib.org.xml.sax;

import java.io.IOException;

/** org.xml.sax.XMLReader (SAXParser.getXMLReader()). */
public interface TXMLReader {
    boolean getFeature(String name);

    void setFeature(String name, boolean value);

    Object getProperty(String name);

    void setProperty(String name, Object value);

    void setContentHandler(TContentHandler handler);

    TContentHandler getContentHandler();

    void setErrorHandler(TErrorHandler handler);

    TErrorHandler getErrorHandler();

    void parse(TInputSource input) throws IOException, TSAXException;

    void parse(String systemId) throws IOException, TSAXException;
}
