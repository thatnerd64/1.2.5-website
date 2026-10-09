package org.teavm.classlib.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * java.util.concurrent.locks.ReentrantLock for TeaVM's green threads: threads only switch at blocking calls,
 * so waiting is a short sleep-and-retry loop, and conditions count signals.
 */
public class TReentrantLock implements TLock, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Thread owner;
    private int holds;

    public TReentrantLock() {
    }

    public TReentrantLock(boolean fair) {
    }

    @Override
    public void lock() {
        Thread me = Thread.currentThread();
        while (owner != null && owner != me) {
            pause();
        }
        owner = me;
        holds++;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        Thread me = Thread.currentThread();
        while (owner != null && owner != me) {
            Thread.sleep(1);
        }
        owner = me;
        holds++;
    }

    @Override
    public boolean tryLock() {
        Thread me = Thread.currentThread();
        if (owner != null && owner != me) {
            return false;
        }
        owner = me;
        holds++;
        return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(time);
        while (!tryLock()) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(1);
        }
        return true;
    }

    @Override
    public void unlock() {
        if (owner != Thread.currentThread()) {
            throw new IllegalMonitorStateException();
        }
        if (--holds == 0) {
            owner = null;
        }
    }

    public boolean isLocked() {
        return owner != null;
    }

    public final boolean hasQueuedThreads() {
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        return false;
    }

    public final int getQueueLength() {
        return 0;
    }

    public boolean isHeldByCurrentThread() {
        return owner == Thread.currentThread();
    }

    public int getHoldCount() {
        return owner == Thread.currentThread() ? holds : 0;
    }

    @Override
    public TCondition newCondition() {
        return new ConditionImpl();
    }

    static void pause() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private final class ConditionImpl implements TCondition {
        private long signals;
        private long waiters;

        private boolean awaitUntil(long deadline, boolean interruptible) throws InterruptedException {
            if (owner != Thread.currentThread()) {
                throw new IllegalMonitorStateException();
            }
            long ticket = ++waiters;
            int saved = holds;
            owner = null;
            holds = 0;
            boolean signalled;
            try {
                while (signals < ticket && (deadline == 0 || System.currentTimeMillis() < deadline)) {
                    if (interruptible) {
                        Thread.sleep(1);
                    } else {
                        pause();
                    }
                }
                signalled = signals >= ticket;
            } finally {
                Thread me = Thread.currentThread();
                while (owner != null && owner != me) {
                    pause();
                }
                owner = me;
                holds = saved;
            }
            return signalled;
        }

        @Override
        public void await() throws InterruptedException {
            awaitUntil(0, true);
        }

        @Override
        public void awaitUninterruptibly() {
            try {
                awaitUntil(0, false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            long start = System.nanoTime();
            awaitUntil(System.currentTimeMillis() + Math.max(1, nanosTimeout / 1000000), true);
            return nanosTimeout - (System.nanoTime() - start);
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return awaitUntil(System.currentTimeMillis() + Math.max(1, unit.toMillis(time)), true);
        }

        @Override
        public void signal() {
            if (signals < waiters) {
                signals++;
            }
        }

        @Override
        public void signalAll() {
            signals = waiters;
        }
    }
}
