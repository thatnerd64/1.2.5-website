package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.TimeUnit;

public class TCountDownLatch {
    private long count;

    public TCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = count;
    }

    public void await() throws InterruptedException {
        while (count > 0) {
            Thread.sleep(1);
        }
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (count > 0) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(1);
        }
        return true;
    }

    public void countDown() {
        if (count > 0) {
            count--;
        }
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return super.toString() + "[Count = " + count + "]";
    }
}
