package retro.rt;

/** Stand-in for browser tab sleep timers, backed directly by Thread.sleep. */
public final class Wakeup {
    private Wakeup() {
    }

    public static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
