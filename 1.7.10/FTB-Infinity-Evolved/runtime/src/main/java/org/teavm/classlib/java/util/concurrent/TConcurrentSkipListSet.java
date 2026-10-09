package org.teavm.classlib.java.util.concurrent;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/** java.util.concurrent.ConcurrentSkipListSet as a sorted tree set (see TConcurrentSkipListMap). */
public class TConcurrentSkipListSet<E> extends TreeSet<E> {
    public TConcurrentSkipListSet() {
    }

    public TConcurrentSkipListSet(Comparator<? super E> comparator) {
        super(comparator);
    }

    public TConcurrentSkipListSet(Collection<? extends E> c) {
        super(c);
    }

    public TConcurrentSkipListSet(SortedSet<E> s) {
        super(s);
    }
}
