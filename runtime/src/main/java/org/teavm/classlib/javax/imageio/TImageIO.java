package org.teavm.classlib.javax.imageio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.teavm.classlib.java.awt.image.TBufferedImage;
import org.teavm.classlib.java.awt.image.TRenderedImage;
import retro.img.Png;

/** javax.imageio.ImageIO: PNG and BMP (all Minecraft and mod textures are one of these). */
public final class TImageIO {
    private TImageIO() {
    }

    public static void setUseCache(boolean useCache) {
    }

    public static TBufferedImage read(InputStream in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("input == null!");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }
        return decode(bytes.toByteArray());
    }

    public static TBufferedImage read(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("input == null!");
        }
        if (!file.exists()) {
            throw new IOException("Can't read input file!");
        }
        try (InputStream in = new FileInputStream(file)) {
            return read(in);
        }
    }

    public static TBufferedImage read(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("input == null!");
        }
        if ("res".equals(url.getProtocol())) {
            byte[] data = retro.rt.Resources.read(url.getPath());
            if (data == null) {
                throw new IOException("Can't get input stream from URL!");
            }
            return decode(data);
        }
        try (InputStream in = url.openStream()) {
            return read(in);
        }
    }

    public static TBufferedImage decode(byte[] data) throws IOException {
        Png.Image image;
        if (Png.isPng(data)) {
            image = Png.decode(data);
        } else if (retro.img.Bmp.isBmp(data)) {
            image = retro.img.Bmp.decode(data);
        } else {
            retro.JS.log("ImageIO: unsupported image data (" + data.length + " bytes, not PNG or BMP)");
            return null;
        }
        return new TBufferedImage(image.width, image.height,
                image.hasAlpha ? TBufferedImage.TYPE_INT_ARGB : TBufferedImage.TYPE_INT_ARGB, image.argb);
    }

    public static boolean write(TRenderedImage image, String format, OutputStream out) throws IOException {
        if (!(image instanceof TBufferedImage)) {
            return false;
        }
        TBufferedImage img = (TBufferedImage) image;
        int type = img.getType();
        boolean alpha = type != TBufferedImage.TYPE_INT_RGB && type != TBufferedImage.TYPE_3BYTE_BGR
                && type != TBufferedImage.TYPE_INT_BGR;
        int[] argb = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        out.write(Png.encode(img.getWidth(), img.getHeight(), argb, alpha));
        return true;
    }

    public static boolean write(TRenderedImage image, String format, File file) throws IOException {
        boolean ok;
        try (OutputStream out = new FileOutputStream(file)) {
            ok = write(image, format, out);
        }
        if (ok && file.getPath().contains("/screenshots/")) {
            retro.Screenshots.offerDownload(file);
        }
        return ok;
    }

    public static java.util.Iterator<Object> getImageReadersByFormatName(String format) {
        return java.util.Collections.emptyIterator();
    }

    public static java.util.Iterator<Object> getImageReadersBySuffix(String suffix) {
        return java.util.Collections.emptyIterator();
    }

    public static String[] getReaderFormatNames() {
        return new String[] { "png", "PNG", "bmp", "BMP" };
    }

    public static String[] getWriterFormatNames() {
        return new String[] { "png", "PNG" };
    }
}
