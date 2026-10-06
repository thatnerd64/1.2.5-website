package retro;

import org.teavm.jso.JSBody;

/**
 * Benchmark mode: the page was opened with {@code ?benchmark=SEED} (web/js/benchmark.js, tools/benchmark.js).
 * The game side ({@code RetroBenchmark} in gameglue) reports its state here and reads what the page asks for, through
 * {@code window.__retroBenchmark}. The scripts' local variable is called {@code bench}: the release build renames the
 * parameters to b, c, d... without looking at the script, so a local "b" would replace the first one.
 */
public final class Benchmark {
    private static int enabled = -1;

    private Benchmark() {
    }

    public static boolean enabled() {
        if (enabled < 0) {
            enabled = JS.config("benchmark") != null ? 1 : 0;
        }
        return enabled == 1;
    }

    /** The world seed as typed in Create New World: a number, or text that is hashed like the game does. */
    public static String seed() {
        String s = JS.config("benchmark");
        return s == null || s.isEmpty() ? "benchmark" : s;
    }

    /** A setting from the page URL (e.g. "render" for {@code &render=far}), or null. */
    public static String option(String name) {
        return JS.config("benchmark." + name);
    }

    /** "title", "ingame" or "menu" (a screen is open in game). */
    @JSBody(params = "state", script = "var bench = window.__retroBenchmark; if (bench) bench.state = state;")
    public static native void state(String state);

    @JSBody(params = { "entities", "chunks", "x", "y", "z", "settings" }, script = ""
            + "var bench = window.__retroBenchmark; if (!bench) return;"
            + "bench.entities = entities; bench.chunks = chunks; bench.x = x; bench.y = y; bench.z = z;"
            + "bench.settings = settings;")
    public static native void stats(int entities, String chunks, double x, double y, double z, String settings);

    /** Whether the page asks for the player to walk (the "walk" phase). */
    @JSBody(script = "var bench = window.__retroBenchmark; return !!(bench && bench.walk);")
    public static native boolean walking();
}
