package org.teavm.classlib.javax.xml.parsers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.org.xml.sax.TAttributes;
import org.teavm.classlib.org.xml.sax.TSAXException;
import org.teavm.classlib.org.xml.sax.TSAXParseException;
import org.teavm.classlib.org.xml.sax.helpers.TDefaultHandler;

/**
 * javax.xml.parsers.SAXParser: a small non-validating, non-namespace-aware XML parser (as the JDK's default
 * factory is). Handles elements, attributes, text, CDATA, comments, processing instructions, a skipped DOCTYPE,
 * and the predefined and numeric character entities. Used by Inventory Tweaks for its item tree.
 */
public class TSAXParser {
    protected TSAXParser() {
    }

    public boolean isNamespaceAware() {
        return false;
    }

    public boolean isValidating() {
        return false;
    }

    public void parse(File f, TDefaultHandler handler) throws TSAXException, IOException {
        try (InputStream in = new FileInputStream(f)) {
            parse(in, handler);
        }
    }

    public void parse(String uri, TDefaultHandler handler) throws TSAXException, IOException {
        try (InputStream in = new java.net.URL(uri).openStream()) {
            parse(in, handler);
        }
    }

    /** Reads a whole document from a stream (UTF-8, with or without a byte order mark). */
    static String readDocument(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }
        byte[] data = bytes.toByteArray();
        int start = data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB
                && (data[2] & 0xFF) == 0xBF ? 3 : 0;
        return new String(data, start, data.length - start, StandardCharsets.UTF_8);
    }

    static String readDocument(org.teavm.classlib.org.xml.sax.TInputSource source) throws IOException {
        if (source.getCharacterStream() != null) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = source.getCharacterStream().read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            String text = sb.toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        }
        if (source.getByteStream() != null) {
            return readDocument(source.getByteStream());
        }
        if (source.getSystemId() != null) {
            try (InputStream in = new java.net.URL(source.getSystemId()).openStream()) {
                return readDocument(in);
            }
        }
        throw new IOException("InputSource with nothing to read");
    }

    static void parseDocument(String text, org.teavm.classlib.org.xml.sax.TContentHandler handler)
            throws TSAXException {
        new Parser(text, handler).run();
    }

    public void parse(org.teavm.classlib.org.xml.sax.TInputSource source, TDefaultHandler handler)
            throws TSAXException, IOException {
        parseDocument(readDocument(source), handler);
    }

    /** SAXParser.getXMLReader: the same parser behind the XMLReader interface (EnderIO's recipe files). */
    public org.teavm.classlib.org.xml.sax.TXMLReader getXMLReader() {
        return new Reader();
    }

    private static final class Reader implements org.teavm.classlib.org.xml.sax.TXMLReader {
        private org.teavm.classlib.org.xml.sax.TContentHandler content = new TDefaultHandler();
        private org.teavm.classlib.org.xml.sax.TErrorHandler errors;
        private final java.util.Map<String, Boolean> features = new java.util.HashMap<>();
        private final java.util.Map<String, Object> properties = new java.util.HashMap<>();

        @Override
        public boolean getFeature(String name) {
            return Boolean.TRUE.equals(features.get(name));
        }

        @Override
        public void setFeature(String name, boolean value) {
            features.put(name, value);
        }

        @Override
        public Object getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public void setProperty(String name, Object value) {
            properties.put(name, value);
        }

        @Override
        public void setContentHandler(org.teavm.classlib.org.xml.sax.TContentHandler handler) {
            content = handler;
        }

        @Override
        public org.teavm.classlib.org.xml.sax.TContentHandler getContentHandler() {
            return content;
        }

        @Override
        public void setErrorHandler(org.teavm.classlib.org.xml.sax.TErrorHandler handler) {
            errors = handler;
        }

        @Override
        public org.teavm.classlib.org.xml.sax.TErrorHandler getErrorHandler() {
            return errors;
        }

        @Override
        public void parse(org.teavm.classlib.org.xml.sax.TInputSource input) throws IOException, TSAXException {
            String text = readDocument(input);
            try {
                parseDocument(text, content);
            } catch (TSAXParseException e) {
                if (errors == null) {
                    throw e;
                }
                errors.fatalError(e);
            }
        }

        @Override
        public void parse(String systemId) throws IOException, TSAXException {
            parse(new org.teavm.classlib.org.xml.sax.TInputSource(systemId));
        }
    }

    public void parse(InputStream in, TDefaultHandler handler) throws TSAXException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }
        byte[] data = bytes.toByteArray();
        int start = data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB
                && (data[2] & 0xFF) == 0xBF ? 3 : 0;
        new Parser(new String(data, start, data.length - start, StandardCharsets.UTF_8), handler).run();
    }

    private static final class Attrs implements TAttributes {
        final List<String> names = new ArrayList<>();
        final List<String> values = new ArrayList<>();

        @Override
        public int getLength() {
            return names.size();
        }

        @Override
        public String getURI(int index) {
            return index >= 0 && index < names.size() ? "" : null;
        }

        @Override
        public String getLocalName(int index) {
            return index >= 0 && index < names.size() ? "" : null;
        }

        @Override
        public String getQName(int index) {
            return index >= 0 && index < names.size() ? names.get(index) : null;
        }

        @Override
        public String getType(int index) {
            return index >= 0 && index < names.size() ? "CDATA" : null;
        }

        @Override
        public String getValue(int index) {
            return index >= 0 && index < values.size() ? values.get(index) : null;
        }

        @Override
        public int getIndex(String uri, String localName) {
            return -1;
        }

        @Override
        public int getIndex(String qName) {
            return names.indexOf(qName);
        }

        @Override
        public String getType(String uri, String localName) {
            return null;
        }

        @Override
        public String getType(String qName) {
            return getIndex(qName) >= 0 ? "CDATA" : null;
        }

        @Override
        public String getValue(String uri, String localName) {
            return null;
        }

        @Override
        public String getValue(String qName) {
            int i = getIndex(qName);
            return i >= 0 ? values.get(i) : null;
        }
    }

    private static final class Parser {
        private final String s;
        private final org.teavm.classlib.org.xml.sax.TContentHandler h;
        private int pos;
        private final List<String> open = new ArrayList<>();

        Parser(String s, org.teavm.classlib.org.xml.sax.TContentHandler h) {
            this.s = s;
            this.h = h;
        }

        private TSAXParseException error(String message) {
            int line = 1;
            for (int i = 0; i < pos && i < s.length(); i++) {
                if (s.charAt(i) == '\n') {
                    line++;
                }
            }
            return new TSAXParseException(message, line);
        }

        void run() throws TSAXException {
            h.startDocument();
            boolean seenRoot = false;
            StringBuilder text = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c != '<') {
                    int end = s.indexOf('<', pos);
                    if (end < 0) {
                        end = s.length();
                    }
                    decode(s.substring(pos, end), text);
                    pos = end;
                    continue;
                }
                if (s.startsWith("<!--", pos)) {
                    int end = s.indexOf("-->", pos + 4);
                    if (end < 0) {
                        throw error("Unterminated comment");
                    }
                    pos = end + 3;
                } else if (s.startsWith("<![CDATA[", pos)) {
                    int end = s.indexOf("]]>", pos + 9);
                    if (end < 0) {
                        throw error("Unterminated CDATA section");
                    }
                    text.append(s, pos + 9, end);
                    pos = end + 3;
                } else if (s.startsWith("<?", pos)) {
                    int end = s.indexOf("?>", pos + 2);
                    if (end < 0) {
                        throw error("Unterminated processing instruction");
                    }
                    String body = s.substring(pos + 2, end);
                    int sp = 0;
                    while (sp < body.length() && !Character.isWhitespace(body.charAt(sp))) {
                        sp++;
                    }
                    String target = body.substring(0, sp);
                    if (!target.equalsIgnoreCase("xml")) {
                        flush(text);
                        h.processingInstruction(target, body.substring(sp).trim());
                    }
                    pos = end + 2;
                } else if (s.startsWith("<!", pos)) {
                    skipDeclaration();
                } else if (s.startsWith("</", pos)) {
                    flush(text);
                    pos += 2;
                    String name = name();
                    skipSpace();
                    expect('>');
                    if (open.isEmpty() || !open.get(open.size() - 1).equals(name)) {
                        throw error("Unexpected end tag </" + name + ">");
                    }
                    open.remove(open.size() - 1);
                    h.endElement("", "", name);
                } else {
                    flush(text);
                    if (seenRoot && open.isEmpty()) {
                        throw error("Content is not allowed after the root element");
                    }
                    seenRoot = true;
                    pos++;
                    String name = name();
                    Attrs attrs = new Attrs();
                    while (true) {
                        skipSpace();
                        if (pos >= s.length()) {
                            throw error("Unterminated start tag <" + name + ">");
                        }
                        char d = s.charAt(pos);
                        if (d == '/' || d == '>') {
                            break;
                        }
                        String attr = name();
                        skipSpace();
                        expect('=');
                        skipSpace();
                        char q = pos < s.length() ? s.charAt(pos) : 0;
                        if (q != '"' && q != '\'') {
                            throw error("Attribute value must be quoted");
                        }
                        int end = s.indexOf(q, pos + 1);
                        if (end < 0) {
                            throw error("Unterminated attribute value");
                        }
                        StringBuilder value = new StringBuilder();
                        decode(s.substring(pos + 1, end), value);
                        for (int i = 0; i < value.length(); i++) {
                            char v = value.charAt(i);
                            if (v == '\t' || v == '\n' || v == '\r') {
                                value.setCharAt(i, ' ');
                            }
                        }
                        attrs.names.add(attr);
                        attrs.values.add(value.toString());
                        pos = end + 1;
                    }
                    boolean empty = s.charAt(pos) == '/';
                    if (empty) {
                        pos++;
                    }
                    expect('>');
                    h.startElement("", "", name, attrs);
                    if (empty) {
                        h.endElement("", "", name);
                    } else {
                        open.add(name);
                    }
                }
            }
            flush(text);
            if (!open.isEmpty()) {
                throw error("Element <" + open.get(open.size() - 1) + "> is not closed");
            }
            if (!seenRoot) {
                throw error("Premature end of file");
            }
            h.endDocument();
        }

        private void flush(StringBuilder text) throws TSAXException {
            if (text.length() == 0) {
                return;
            }
            if (!open.isEmpty()) {
                char[] chars = text.toString().toCharArray();
                h.characters(chars, 0, chars.length);
            }
            text.setLength(0);
        }

        private void skipDeclaration() throws TSAXParseException {
            // <!DOCTYPE ...> possibly with an internal subset in [...]
            int depth = 0;
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                } else if (c == '>' && depth <= 0) {
                    return;
                }
            }
            throw error("Unterminated declaration");
        }

        private String name() throws TSAXParseException {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (Character.isWhitespace(c) || c == '>' || c == '/' || c == '=') {
                    break;
                }
                pos++;
            }
            if (pos == start) {
                throw error("Name expected");
            }
            return s.substring(start, pos);
        }

        private void skipSpace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        private void expect(char c) throws TSAXParseException {
            if (pos >= s.length() || s.charAt(pos) != c) {
                throw error("'" + c + "' expected");
            }
            pos++;
        }

        private void decode(String raw, StringBuilder out) throws TSAXParseException {
            int i = 0;
            while (i < raw.length()) {
                char c = raw.charAt(i);
                if (c != '&') {
                    out.append(c);
                    i++;
                    continue;
                }
                int semi = raw.indexOf(';', i);
                if (semi < 0) {
                    throw error("Unterminated entity reference");
                }
                String ent = raw.substring(i + 1, semi);
                switch (ent) {
                    case "lt":
                        out.append('<');
                        break;
                    case "gt":
                        out.append('>');
                        break;
                    case "amp":
                        out.append('&');
                        break;
                    case "quot":
                        out.append('"');
                        break;
                    case "apos":
                        out.append('\'');
                        break;
                    default:
                        if (ent.startsWith("#x") || ent.startsWith("#X")) {
                            out.appendCodePoint(Integer.parseInt(ent.substring(2), 16));
                        } else if (ent.startsWith("#")) {
                            out.appendCodePoint(Integer.parseInt(ent.substring(1)));
                        } else {
                            throw error("Undeclared entity &" + ent + ";");
                        }
                        break;
                }
                i = semi + 1;
            }
        }
    }
}
