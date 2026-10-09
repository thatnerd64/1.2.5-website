package org.teavm.classlib.java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** SynchronousQueue: an offer only succeeds while a consumer is waiting (so a pool starts a thread otherwise). */
public class TSynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final ArrayDeque<E> handoff = new ArrayDeque<>();
    private int waiting;

    public TSynchronousQueue() {
    }

    public TSynchronousQueue(boolean fair) {
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        if (waiting > handoff.size()) {
            handoff.addLast(e);
            return true;
        }
        return false;
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
        waiting++;
        try {
            E e;
            while ((e = handoff.pollFirst()) == null) {
                Thread.sleep(1);
            }
            return e;
        } finally {
            waiting--;
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        waiting++;
        try {
            E e;
            while ((e = handoff.pollFirst()) == null) {
                if (System.currentTimeMillis() >= deadline) {
                    return null;
                }
                Thread.sleep(1);
            }
            return e;
        } finally {
            waiting--;
        }
    }

    @Override
    public E poll() {
        return handoff.pollFirst();
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int max) {
        int n = 0;
        E e;
        while (n < max && (e = handoff.pollFirst()) != null) {
            c.add(e);
            n++;
        }
        return n;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public int size() {
        return 0;
    }
}
