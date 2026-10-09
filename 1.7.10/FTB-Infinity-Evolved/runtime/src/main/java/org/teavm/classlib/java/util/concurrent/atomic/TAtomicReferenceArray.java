package org.teavm.classlib.java.util.concurrent.atomic;

public class TAtomicReferenceArray<E> implements java.io.Serializable {
    private final Object[] array;

    public TAtomicReferenceArray(int length) {
        array = new Object[length];
    }

    public TAtomicReferenceArray(E[] source) {
        array = source.clone();
    }

    public final int length() {
        return array.length;
    }

    @SuppressWarnings("unchecked")
    public final E get(int i) {
        return (E) array[i];
    }

    public final void set(int i, E value) {
        array[i] = value;
    }

    public final void lazySet(int i, E value) {
        array[i] = value;
    }

    @SuppressWarnings("unchecked")
    public final E getAndSet(int i, E value) {
        E old = (E) array[i];
        array[i] = value;
        return old;
    }

    public final boolean compareAndSet(int i, E expect, E update) {
        if (array[i] == expect) {
            array[i] = update;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int i, E expect, E update) {
        return compareAndSet(i, expect, update);
    }

    @Override
    public String toString() {
        return java.util.Arrays.toString(array);
    }
}
