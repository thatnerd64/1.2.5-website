package org.teavm.classlib.java.util.concurrent.atomic;

/** java.util.concurrent.atomic.AtomicLongArray (single-threaded browser: plain array). */
public class TAtomicLongArray implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final long[] array;

    public TAtomicLongArray(int length) {
        array = new long[length];
    }

    public TAtomicLongArray(long[] array) {
        this.array = array.clone();
    }

    public final int length() {
        return array.length;
    }

    public final long get(int i) {
        return array[i];
    }

    public final void set(int i, long value) {
        array[i] = value;
    }

    public final void lazySet(int i, long value) {
        array[i] = value;
    }

    public final long getAndSet(int i, long value) {
        long old = array[i];
        array[i] = value;
        return old;
    }

    public final boolean compareAndSet(int i, long expect, long update) {
        if (array[i] != expect) {
            return false;
        }
        array[i] = update;
        return true;
    }

    public final long getAndAdd(int i, long delta) {
        long old = array[i];
        array[i] += delta;
        return old;
    }

    public final long addAndGet(int i, long delta) {
        return array[i] += delta;
    }

    public final long incrementAndGet(int i) {
        return ++array[i];
    }

    public final long decrementAndGet(int i) {
        return --array[i];
    }

    public final long getAndIncrement(int i) {
        return array[i]++;
    }

    public final long getAndDecrement(int i) {
        return array[i]--;
    }

    @Override
    public String toString() {
        return java.util.Arrays.toString(array);
    }
}
