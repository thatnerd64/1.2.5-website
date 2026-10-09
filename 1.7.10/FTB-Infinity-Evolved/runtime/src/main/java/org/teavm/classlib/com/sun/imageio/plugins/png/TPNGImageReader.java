package org.teavm.classlib.com.sun.imageio.plugins.png;

import java.io.IOException;
import java.io.InputStream;
import org.teavm.classlib.java.awt.image.TBufferedImage;
import org.teavm.classlib.javax.imageio.TImageIO;
import org.teavm.classlib.javax.imageio.TImageReadParam;
import org.teavm.classlib.javax.imageio.TImageReader;
import org.teavm.classlib.javax.imageio.spi.TImageReaderSpi;
import org.teavm.classlib.javax.imageio.stream.TImageInputStream;

/**
 * The JDK's internal PNG reader, which CraftStudio (Decocraft's model library) constructs directly; decodes with
 * ImageIO.read (PNG and BMP).
 */
public class TPNGImageReader extends TImageReader {
    public TPNGImageReader(TImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public TBufferedImage read(int imageIndex, TImageReadParam param) throws IOException {
        Object in = getInput();
        if (in == null) {
            throw new IllegalStateException("Input not set!");
        }
        if (in instanceof InputStream) {
            return TImageIO.read((InputStream) in);
        }
        TImageInputStream stream = (TImageInputStream) in;
        return TImageIO.read(new InputStream() {
            @Override
            public int read() throws IOException {
                return stream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return stream.read(b, off, len);
            }
        });
    }
}
