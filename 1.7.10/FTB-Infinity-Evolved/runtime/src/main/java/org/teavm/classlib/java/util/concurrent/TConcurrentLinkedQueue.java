package org.teavm.classlib.java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public class TConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    private final ArrayDeque<E> items = new ArrayDeque<>();

    public TConcurrentLinkedQueue() {
    }

    public TConcurrentLinkedQueue(Collection<? extends E> c) {
        for (E e : c) {
            offer(e);
        }
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        items.addLast(e);
        return true;
    }

    @Override
    public E poll() {
        return items.pollFirst();
    }

    @Override
    public E peek() {
        return items.peekFirst();
    }

    @Override
    public Iterator<E> iterator() {
        return new ArrayDeque<>(items).iterator();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean remove(Object o) {
        return items.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return items.contains(o);
    }
}
