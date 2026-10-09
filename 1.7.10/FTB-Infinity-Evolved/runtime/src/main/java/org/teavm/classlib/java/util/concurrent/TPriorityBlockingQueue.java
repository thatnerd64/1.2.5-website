package org.teavm.classlib.java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TPriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final PriorityQueue<E> items;

    public TPriorityBlockingQueue() {
        items = new PriorityQueue<>();
    }

    public TPriorityBlockingQueue(int capacity) {
        items = new PriorityQueue<>(capacity);
    }

    public TPriorityBlockingQueue(int capacity, Comparator<? super E> comparator) {
        items = new PriorityQueue<>(capacity, comparator);
    }

    @Override
    public boolean offer(E e) {
        return items.offer(e);
    }

    @Override
    public void put(E e) {
        items.offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return items.offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        E e;
        while ((e = items.poll()) == null) {
            Thread.sleep(1);
        }
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        E e;
        while ((e = items.poll()) == null) {
            if (System.currentTimeMillis() >= deadline) {
                return null;
            }
            Thread.sleep(1);
        }
        return e;
    }

    @Override
    public E poll() {
        return items.poll();
    }

    @Override
    public E peek() {
        return items.peek();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int max) {
        int n = 0;
        E e;
        while (n < max && (e = items.poll()) != null) {
            c.add(e);
            n++;
        }
        return n;
    }

    @Override
    public Iterator<E> iterator() {
        return items.iterator();
    }

    @Override
    public int size() {
        return items.size();
    }
}
