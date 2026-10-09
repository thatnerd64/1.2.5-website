package retro.glue;

/**
 * Logger that prints Minecraft-style lines to standard output/error (the browser console). The printing, level and
 * filters live in the replacement {@link org.apache.logging.log4j.core.Logger}, so mods that cast a logger to
 * log4j-core's class get one.
 */
public final class RetroLogger extends org.apache.logging.log4j.core.Logger {
    public RetroLogger(String name) {
        super(name);
    }
}
