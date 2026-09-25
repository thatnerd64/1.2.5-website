/**
 * Added to Minecraft's ChunkProvider ({@code ko}) at build time; its original chunk lookups, renamed. See
 * {@link RetroChunkCache}. In the default package because the game's obfuscated classes live there.
 */
public interface RetroChunkSource {
    ack retro$provideChunk(int x, int z);

    boolean retro$chunkExists(int x, int z);
}
