package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.TimeUnit;

public class TSemaphore {
    private int permits;

    public TSemaphore(int permits) {
        this.permits = permits;
    }

    public TSemaphore(int permits, boolean fair) {
        this.permits = permits;
    }

    public void acquire() throws InterruptedException {
        acquire(1);
    }

    public void acquireUninterruptibly() {
        try {
            acquire(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void acquire(int n) throws InterruptedException {
        while (permits < n) {
            Thread.sleep(1);
        }
        permits -= n;
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int n) {
        if (permits >= n) {
            permits -= n;
            return true;
        }
        return false;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int n, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (permits < n) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(1);
        }
        permits -= n;
        return true;
    }

    public void release() {
        permits++;
    }

    public void release(int n) {
        permits += n;
    }

    public int availablePermits() {
        return permits;
    }
}
