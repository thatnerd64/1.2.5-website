package org.teavm.classlib.org.xml.sax;

import java.io.InputStream;
import java.io.Reader;

/** org.xml.sax.InputSource: where an XML document comes from (a byte stream, a character stream or a URI). */
public class TInputSource {
    private String systemId;
    private String publicId;
    private InputStream byteStream;
    private Reader characterStream;
    private String encoding;

    public TInputSource() {
    }

    public TInputSource(String systemId) {
        this.systemId = systemId;
    }

    public TInputSource(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    public TInputSource(Reader characterStream) {
        this.characterStream = characterStream;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public InputStream getByteStream() {
        return byteStream;
    }

    public void setByteStream(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    public Reader getCharacterStream() {
        return characterStream;
    }

    public void setCharacterStream(Reader characterStream) {
        this.characterStream = characterStream;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
