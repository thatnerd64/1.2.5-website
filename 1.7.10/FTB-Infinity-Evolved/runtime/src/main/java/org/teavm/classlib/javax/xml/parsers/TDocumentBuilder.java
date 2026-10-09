package org.teavm.classlib.javax.xml.parsers;

import java.io.InputStream;
import org.teavm.classlib.org.w3c.dom.TDocument;
import org.teavm.classlib.org.xml.sax.TSAXException;

public abstract class TDocumentBuilder {
    protected TDocumentBuilder() {
    }

    public abstract TDocument parse(InputStream is) throws TSAXException, java.io.IOException;

    public TDocument parse(String uri) throws TSAXException, java.io.IOException {
        throw new java.io.IOException("XML parsing is not available");
    }

    public TDocument parse(java.io.File f) throws TSAXException, java.io.IOException {
        throw new java.io.IOException("XML parsing is not available");
    }

    public abstract TDocument newDocument();
}
