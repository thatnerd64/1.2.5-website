/**
 * Front cache for ChunkProvider.provideChunk/chunkExists. Minecraft keys loaded chunks by a 64-bit long, and
 * longs are BigInts in JavaScript: every block lookup across a chunk boundary allocated several of them. This
 * direct-mapped cache is keyed by the int coordinates and validated against the chunk itself (its position and
 * its loaded flag, which ChunkProvider sets on load and clears on unload), so it can never return a stale chunk.
 */
public final class RetroChunkCache {
    private static final int BITS = 5;
    private static final int MASK = (1 << BITS) - 1;
    private static Object owner;
    private static final ack[] cache = new ack[1 << (2 * BITS)];

    private RetroChunkCache() {
    }

    private static ack cached(Object provider, int x, int z) {
        if (provider != owner) {
            return null;
        }
        ack c = cache[(x & MASK) << BITS | (z & MASK)];
        return c != null && c.d && c.g == x && c.h == z ? c : null;
    }

    public static ack provideChunk(RetroChunkSource provider, int x, int z) {
        ack c = cached(provider, x, z);
        if (c != null) {
            return c;
        }
        c = provider.retro$provideChunk(x, z);
        if (c != null && c.d && c.g == x && c.h == z) {
            if (provider != owner) {
                owner = provider;
                java.util.Arrays.fill(cache, null);
            }
            cache[(x & MASK) << BITS | (z & MASK)] = c;
        }
        return c;
    }

    public static boolean chunkExists(RetroChunkSource provider, int x, int z) {
        return cached(provider, x, z) != null || provider.retro$chunkExists(x, z);
    }
}
