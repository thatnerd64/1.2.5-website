package org.teavm.classlib.javax.imageio.spi;

/** javax.imageio.spi.IIORegistry: an empty registry (CraftStudio's image loader fetches the default one). */
public class TIIORegistry {
    private static final TIIORegistry DEFAULT = new TIIORegistry();

    protected TIIORegistry() {
    }

    public static TIIORegistry getDefaultInstance() {
        return DEFAULT;
    }
}
