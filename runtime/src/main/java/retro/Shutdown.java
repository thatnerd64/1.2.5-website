package retro;

import java.util.ArrayList;
import java.util.List;

/** Runtime.addShutdownHook: hooks run when the page is being closed. */
public final class Shutdown {
    private static final List<Thread> hooks = new ArrayList<>();

    private Shutdown() {
    }

    public static synchronized void add(Thread hook) {
        hooks.add(hook);
    }

    public static synchronized boolean remove(Thread hook) {
        return hooks.remove(hook);
    }
}
