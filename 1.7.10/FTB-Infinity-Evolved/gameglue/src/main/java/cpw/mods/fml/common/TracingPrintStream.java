package cpw.mods.fml.common;

import java.io.PrintStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Replacement for FML's stdout/stderr tracer, which prefixes each line with the calling class by walking the stack
 * (not available here). Lines go to the logger unchanged.
 */
public class TracingPrintStream extends PrintStream {
    private final Logger logger;
    private final Level level;

    public TracingPrintStream(Logger logger, PrintStream original) {
        super(original);
        this.logger = logger;
        this.level = "STDERR".equals(logger.getName()) ? Level.ERROR : Level.INFO;
    }

    @Override
    public void println(String line) {
        logger.log(level, line);
    }

    @Override
    public void println(Object value) {
        logger.log(level, String.valueOf(value));
    }
}
