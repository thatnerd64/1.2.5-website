package org.teavm.classlib.java.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

/**
 * java.util.Scanner: line reading and whitespace-separated tokens (next, nextInt, nextLong, nextDouble,
 * nextBoolean). Enough for mods that read small config files.
 */
public final class TScanner implements Closeable {
    private final BufferedReader reader;
    private String line;
    private boolean lineLoaded;
    private int col;
    private boolean closed;

    public TScanner(InputStream source) {
        this(new InputStreamReader(source, Charset.defaultCharset()));
    }

    public TScanner(InputStream source, String charsetName) {
        this(new InputStreamReader(source, Charset.forName(charsetName)));
    }

    public TScanner(File source) throws FileNotFoundException {
        this(new FileInputStream(source));
    }

    public TScanner(File source, String charsetName) throws FileNotFoundException {
        this(new FileInputStream(source), charsetName);
    }

    public TScanner(String source) {
        this(new StringReader(source));
    }

    public TScanner(Reader source) {
        reader = source instanceof BufferedReader ? (BufferedReader) source : new BufferedReader(source);
    }

    private boolean loadLine() {
        if (closed) {
            throw new IllegalStateException("Scanner closed");
        }
        if (!lineLoaded) {
            try {
                line = reader.readLine();
            } catch (IOException e) {
                line = null;
            }
            lineLoaded = true;
            col = 0;
        }
        return line != null;
    }

    public boolean hasNextLine() {
        return loadLine();
    }

    public String nextLine() {
        if (!loadLine()) {
            throw new NoSuchElementException("No line found");
        }
        String result = line.substring(Math.min(col, line.length()));
        lineLoaded = false;
        return result;
    }

    private boolean skipToToken() {
        while (loadLine()) {
            while (col < line.length() && Character.isWhitespace(line.charAt(col))) {
                col++;
            }
            if (col < line.length()) {
                return true;
            }
            lineLoaded = false;
        }
        return false;
    }

    public boolean hasNext() {
        return skipToToken();
    }

    public String next() {
        if (!skipToToken()) {
            throw new NoSuchElementException();
        }
        int start = col;
        while (col < line.length() && !Character.isWhitespace(line.charAt(col))) {
            col++;
        }
        return line.substring(start, col);
    }

    private String peek() {
        if (!skipToToken()) {
            return null;
        }
        int end = col;
        while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
            end++;
        }
        return line.substring(col, end);
    }

    public boolean hasNextInt() {
        String t = peek();
        if (t == null) {
            return false;
        }
        try {
            Integer.parseInt(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int nextInt() {
        String t = next();
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new java.util.InputMismatchException(t);
        }
    }

    public long nextLong() {
        String t = next();
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw new java.util.InputMismatchException(t);
        }
    }

    public double nextDouble() {
        String t = next();
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            throw new java.util.InputMismatchException(t);
        }
    }

    public float nextFloat() {
        return (float) nextDouble();
    }

    public boolean nextBoolean() {
        String t = next();
        if (t.equalsIgnoreCase("true")) {
            return true;
        }
        if (t.equalsIgnoreCase("false")) {
            return false;
        }
        throw new java.util.InputMismatchException(t);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                reader.close();
            } catch (IOException e) {
                // ignore, as the JDK does
            }
        }
    }
}
