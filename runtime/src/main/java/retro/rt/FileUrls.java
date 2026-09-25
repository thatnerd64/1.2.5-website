package retro.rt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;

/**
 * {@code file:} URLs over the browser file system. paulscode SoundSystem opens every sound as
 * {@code file.toURI().toURL()}, and mods read configs and textures the same way.
 */
public final class FileUrls extends URLStreamHandler {
    static final FileUrls INSTANCE = new FileUrls();

    private FileUrls() {
    }

    /** Registers the file: and res: protocols with java.net.URL. */
    public static void install() {
        URL.setURLStreamHandlerFactory(protocol -> {
            switch (protocol) {
                case "file":
                    return INSTANCE;
                case "res":
                    return Resources.ResourceHandler.INSTANCE;
                default:
                    return null;
            }
        });
    }

    static File toFile(URL url) {
        String path = url.getPath();
        try {
            path = URLDecoder.decode(path.replace("+", "%2B"), "UTF-8");
        } catch (IllegalArgumentException | java.io.UnsupportedEncodingException e) {
            // keep undecoded
        }
        return new File(path);
    }

    @Override
    protected URLConnection openConnection(URL url) {
        File file = toFile(url);
        return new URLConnection(url) {
            @Override
            public void connect() throws IOException {
                if (!file.exists()) {
                    throw new FileNotFoundException(file.getPath());
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(file);
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return new FileOutputStream(file);
            }

            @Override
            public int getContentLength() {
                return (int) file.length();
            }

            @Override
            public long getLastModified() {
                return file.lastModified();
            }
        };
    }
}
