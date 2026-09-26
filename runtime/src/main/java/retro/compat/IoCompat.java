package retro.compat;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Stand-ins for JDK java.io constructors that TeaVM lacks. The build rewrites {@code new PrintWriter(file)} into
 * {@code new PrintWriter(IoCompat.writer(file))} (and likewise for the other overloads), matching the JDK's
 * behaviour: a buffered writer over a new FileOutputStream, FileNotFoundException if it cannot be created.
 */
public final class IoCompat {
    private IoCompat() {
    }

    public static Writer writer(File file) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.defaultCharset()));
    }

    public static Writer writer(String fileName) throws FileNotFoundException {
        return writer(new File(fileName));
    }

    public static Writer writer(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        Charset cs = charset(csn);
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), cs));
    }

    public static Writer writer(String fileName, String csn) throws FileNotFoundException,
            UnsupportedEncodingException {
        return writer(new File(fileName), csn);
    }

    public static OutputStream output(File file) throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    public static OutputStream output(String fileName) throws FileNotFoundException {
        return new FileOutputStream(fileName);
    }

    /** URL.getContent(): for the stream-backed URLs mods use, the JDK returns an InputStream. */
    public static Object urlGetContent(URL url) throws IOException {
        return urlOpenStream(url);
    }

    /**
     * URL.openStream(). Joining an online-mode server makes the client ask Mojang's long-retired session server
     * ({@code joinserver.jsp}); a browser has no Minecraft session either way, so the answer is a clear reason,
     * which the game shows as "Failed to login: ...".
     */
    public static InputStream urlOpenStream(URL url) throws IOException {
        if ("session.minecraft.net".equalsIgnoreCase(url.getHost()) && url.getPath().endsWith("/joinserver.jsp")) {
            return new ByteArrayInputStream("the server must set online-mode=false\n".getBytes(StandardCharsets.UTF_8));
        }
        return url.openStream();
    }

    private static Charset charset(String csn) throws UnsupportedEncodingException {
        if (csn == null) {
            throw new NullPointerException("charsetName");
        }
        try {
            return Charset.forName(csn);
        } catch (RuntimeException e) {
            if (csn.equalsIgnoreCase("UTF8") || csn.equalsIgnoreCase("UTF-8")) {
                return StandardCharsets.UTF_8;
            }
            throw new UnsupportedEncodingException(csn);
        }
    }
}
