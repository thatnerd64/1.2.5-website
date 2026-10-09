package org.teavm.classlib.javax.imageio;

import java.io.IOException;
import org.teavm.classlib.java.awt.image.TBufferedImage;
import org.teavm.classlib.javax.imageio.spi.TImageReaderSpi;

/**
 * javax.imageio.ImageReader: the input and read(imageIndex) part of the API, for code that drives a reader itself
 * (CraftStudio's PNG reader, FML's splash textures). Subclasses decode from the stream.
 */
public abstract class TImageReader {
    protected TImageReaderSpi originatingProvider;
    protected Object input;

    protected TImageReader(TImageReaderSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    public TImageReaderSpi getOriginatingProvider() {
        return originatingProvider;
    }

    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        this.input = input;
    }

    public void setInput(Object input, boolean seekForwardOnly) {
        this.input = input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public TImageReadParam getDefaultReadParam() {
        return new TImageReadParam();
    }

    public int getNumImages(boolean allowSearch) throws IOException {
        return 1;
    }

    public abstract TBufferedImage read(int imageIndex, TImageReadParam param) throws IOException;

    public TBufferedImage read(int imageIndex) throws IOException {
        return read(imageIndex, null);
    }

    public int getWidth(int imageIndex) throws IOException {
        return read(imageIndex).getWidth();
    }

    public int getHeight(int imageIndex) throws IOException {
        return read(imageIndex).getHeight();
    }

    public void reset() {
        input = null;
    }

    public void dispose() {
    }
}
