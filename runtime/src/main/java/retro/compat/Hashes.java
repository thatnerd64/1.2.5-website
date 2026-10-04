package retro.compat;

/**
 * Hash functions for game classes whose own {@code hashCode} spreads badly.
 *
 * <p>TeaVM's {@code HashMap} picks a bucket with {@code hash & (capacity - 1)} and no bit mixing, unlike the Java 6/7
 * and 8+ implementations Minecraft was written against. Minecraft 1.2.5's {@code NextTickListEntry} hashes to
 * {@code x << 28 + z << 18 + y << 8 + blockId}: the low bits are only the block id and height, so every pending
 * water or lava update at one height and block type lands in one bucket. Each {@code contains}, {@code add} and
 * {@code remove} on the scheduled-tick set then walks that whole chain, and terrain generation (which schedules
 * thousands of fluid updates) spent about 40% of its time doing so.
 */
public final class Hashes {
    private Hashes() {
    }

    /** Hash of four ints that uses every input bit and mixes them into the low bits a hash table indexes by. */
    public static int mix4(int a, int b, int c, int d) {
        int h = 0x811C9DC5;
        h = (h ^ a) * 0x01000193;
        h = (h ^ b) * 0x01000193;
        h = (h ^ c) * 0x01000193;
        h = (h ^ d) * 0x01000193;
        // murmur3 finalizer: high bits reach the low bits
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h;
    }
}
