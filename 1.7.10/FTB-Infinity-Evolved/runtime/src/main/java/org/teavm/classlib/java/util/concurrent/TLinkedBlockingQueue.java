package org.teavm.classlib.java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** java.util.concurrent.LinkedBlockingQueue for TeaVM's green threads (blocking operations poll and sleep). */
public class TLinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final ArrayDeque<E> items = new ArrayDeque<>();
    private final int capacity;

    public TLinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public TLinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
    }

    public TLinkedBlockingQueue(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    @Override
    public Iterator<E> iterator() {
        return items.iterator();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        if (items.size() >= capacity) {
            return false;
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
    public void put(E e) throws InterruptedException {
        while (!offer(e)) {
            Thread.sleep(1);
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (!offer(e)) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(1);
        }
        return true;
    }

    @Override
    public E take() throws InterruptedException {
        E e;
        while ((e = poll()) == null) {
            Thread.sleep(1);
        }
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        E e;
        while ((e = poll()) == null) {
            if (System.currentTimeMillis() >= deadline) {
                return null;
            }
            Thread.sleep(1);
        }
        return e;
    }

    @Override
    public int remainingCapacity() {
        return capacity - items.size();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        int n = 0;
        E e;
        while (n < maxElements && (e = poll()) != null) {
            c.add(e);
            n++;
        }
        return n;
    }
}
