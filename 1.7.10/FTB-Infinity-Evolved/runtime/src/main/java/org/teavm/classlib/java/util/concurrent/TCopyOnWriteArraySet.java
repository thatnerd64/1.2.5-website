package org.teavm.classlib.java.util.concurrent;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class TCopyOnWriteArraySet<E> extends AbstractSet<E> implements java.io.Serializable {
    private Set<E> items = new LinkedHashSet<>();

    public TCopyOnWriteArraySet() {
    }

    public TCopyOnWriteArraySet(Collection<? extends E> c) {
        items.addAll(c);
    }

    @Override
    public Iterator<E> iterator() {
        Iterator<E> it = new LinkedHashSet<>(items).iterator();
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return it.next();
            }
        };
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean add(E e) {
        Set<E> copy = new LinkedHashSet<>(items);
        boolean changed = copy.add(e);
        items = copy;
        return changed;
    }

    @Override
    public boolean remove(Object o) {
        Set<E> copy = new LinkedHashSet<>(items);
        boolean changed = copy.remove(o);
        items = copy;
        return changed;
    }

    @Override
    public boolean contains(Object o) {
        return items.contains(o);
    }

    @Override
    public void clear() {
        items = new LinkedHashSet<>();
    }
}
