package org.teavm.classlib.java.util.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Log file in the virtual file system (pattern supports %h, %t, %g, %u and %%). */
public class TFileHandler extends TStreamHandler {
    public TFileHandler() throws IOException {
        this("%h/java%u.log", 0, 1, false);
    }

    public TFileHandler(String pattern) throws IOException {
        this(pattern, 0, 1, false);
    }

    public TFileHandler(String pattern, boolean append) throws IOException {
        this(pattern, 0, 1, append);
    }

    public TFileHandler(String pattern, int limit, int count) throws IOException {
        this(pattern, limit, count, false);
    }

    public TFileHandler(String pattern, int limit, int count, boolean append) throws IOException {
        setLevel(TLevel.ALL);
        setFormatter(new TSimpleFormatter());
        String path = pattern.replace("%h", System.getProperty("user.home", "/"))
                .replace("%t", System.getProperty("java.io.tmpdir", "/tmp"))
                .replace("%g", "0").replace("%u", "0").replace("%%", "%");
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        setOutputStream(new FileOutputStream(file, append));
    }
}
