package org.teavm.classlib.java.util.concurrent.locks;

public class TLockSupport {
    private TLockSupport() {
    }

    public static void park() {
        parkNanos(1_000_000L);
    }

    public static void park(Object blocker) {
        parkNanos(1_000_000L);
    }

    public static void parkNanos(long nanos) {
        try {
            Thread.sleep(Math.max(1, nanos / 1_000_000L));
        } catch (InterruptedException e) {
            // park returns on interrupt
        }
    }

    public static void parkNanos(Object blocker, long nanos) {
        parkNanos(nanos);
    }

    public static void unpark(Thread thread) {
    }
}
