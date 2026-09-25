package org.teavm.classlib.java.util.concurrent.atomic;

/** java.util.concurrent.atomic.AtomicIntegerArray (single-threaded browser: plain array). */
public class TAtomicIntegerArray implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int[] array;

    public TAtomicIntegerArray(int length) {
        array = new int[length];
    }

    public TAtomicIntegerArray(int[] array) {
        this.array = array.clone();
    }

    public final int length() {
        return array.length;
    }

    public final int get(int i) {
        return array[i];
    }

    public final void set(int i, int value) {
        array[i] = value;
    }

    public final void lazySet(int i, int value) {
        array[i] = value;
    }

    public final int getAndSet(int i, int value) {
        int old = array[i];
        array[i] = value;
        return old;
    }

    public final boolean compareAndSet(int i, int expect, int update) {
        if (array[i] != expect) {
            return false;
        }
        array[i] = update;
        return true;
    }

    public final int getAndAdd(int i, int delta) {
        int old = array[i];
        array[i] += delta;
        return old;
    }

    public final int addAndGet(int i, int delta) {
        return array[i] += delta;
    }

    public final int incrementAndGet(int i) {
        return ++array[i];
    }

    public final int decrementAndGet(int i) {
        return --array[i];
    }

    public final int getAndIncrement(int i) {
        return array[i]++;
    }

    public final int getAndDecrement(int i) {
        return array[i]--;
    }

    @Override
    public String toString() {
        return java.util.Arrays.toString(array);
    }
}
