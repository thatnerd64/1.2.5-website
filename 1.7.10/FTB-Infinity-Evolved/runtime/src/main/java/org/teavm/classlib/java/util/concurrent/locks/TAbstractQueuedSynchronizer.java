package org.teavm.classlib.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * AbstractQueuedSynchronizer for green threads: a thread that cannot acquire yields (sleeps a millisecond) and tries
 * again, so there is no wait queue.
 */
public abstract class TAbstractQueuedSynchronizer extends TAbstractOwnableSynchronizer {
    private int state;

    protected TAbstractQueuedSynchronizer() {
    }

    protected final int getState() {
        return state;
    }

    protected final void setState(int newState) {
        state = newState;
    }

    protected final boolean compareAndSetState(int expect, int update) {
        if (state == expect) {
            state = update;
            return true;
        }
        return false;
    }

    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public final void acquire(int arg) {
        while (!tryAcquire(arg)) {
            pause();
        }
    }

    public final void acquireInterruptibly(int arg) throws InterruptedException {
        while (!tryAcquire(arg)) {
            Thread.sleep(1);
        }
    }

    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        long deadline = System.nanoTime() + nanosTimeout;
        while (!tryAcquire(arg)) {
            if (System.nanoTime() >= deadline) {
                return false;
            }
            Thread.sleep(1);
        }
        return true;
    }

    public final boolean release(int arg) {
        return tryRelease(arg);
    }

    public final void acquireShared(int arg) {
        while (tryAcquireShared(arg) < 0) {
            pause();
        }
    }

    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        while (tryAcquireShared(arg) < 0) {
            Thread.sleep(1);
        }
    }

    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        long deadline = System.nanoTime() + nanosTimeout;
        while (tryAcquireShared(arg) < 0) {
            if (System.nanoTime() >= deadline) {
                return false;
            }
            Thread.sleep(1);
        }
        return true;
    }

    public final boolean releaseShared(int arg) {
        return tryReleaseShared(arg);
    }

    public final boolean hasQueuedThreads() {
        return false;
    }

    public final boolean hasContended() {
        return false;
    }

    public final int getQueueLength() {
        return 0;
    }

    private static void pause() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
