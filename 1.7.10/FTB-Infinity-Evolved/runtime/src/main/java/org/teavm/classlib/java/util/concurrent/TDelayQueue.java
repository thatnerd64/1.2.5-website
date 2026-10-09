package org.teavm.classlib.java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * java.util.concurrent.DelayQueue: elements become available once their delay has expired (Logistics Pipes' crafter
 * module times out lost items with one). Waiting polls, as in the other queues here.
 */
public class TDelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final PriorityQueue<E> items = new PriorityQueue<>();

    public TDelayQueue() {
    }

    public TDelayQueue(Collection<? extends E> c) {
        addAll(c);
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
    public E poll() {
        E first = items.peek();
        return first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0 ? null : items.poll();
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
        while (n < max && (e = poll()) != null) {
            c.add(e);
            n++;
        }
        return n;
    }

    @Override
    public boolean remove(Object o) {
        return items.remove(o);
    }

    @Override
    public void clear() {
        items.clear();
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
