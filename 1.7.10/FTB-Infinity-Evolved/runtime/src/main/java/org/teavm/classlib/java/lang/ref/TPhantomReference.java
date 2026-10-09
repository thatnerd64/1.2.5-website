package org.teavm.classlib.java.lang.ref;

/** Phantom references never yield their referent and are never enqueued here (nothing is finalized). */
public class TPhantomReference<T> extends TReference<T> {
    public TPhantomReference(T referent, TReferenceQueue<? super T> queue) {
    }

    @Override
    public T get() {
        return null;
    }
}
