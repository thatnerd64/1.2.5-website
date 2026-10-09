package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** FutureTask for green threads: waiting callers poll (and so yield to the worker threads). */
public class TFutureTask<V> implements RunnableFuture<V> {
    private static final int NEW = 0;
    private static final int RUNNING = 1;
    private static final int DONE = 2;
    private static final int CANCELLED = 3;

    private Callable<V> callable;
    private int state = NEW;
    private V result;
    private Throwable failure;

    public TFutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
    }

    public TFutureTask(Runnable runnable, V result) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        this.callable = () -> {
            runnable.run();
            return result;
        };
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state != NEW && !(state == RUNNING && mayInterruptIfRunning)) {
            return false;
        }
        state = CANCELLED;
        done();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return state == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state >= DONE;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        while (state < DONE) {
            Thread.sleep(1);
        }
        return report();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (state < DONE) {
            if (System.currentTimeMillis() >= deadline) {
                throw new TimeoutException();
            }
            Thread.sleep(1);
        }
        return report();
    }

    private V report() throws ExecutionException {
        if (state == CANCELLED) {
            throw new CancellationException();
        }
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return result;
    }

    protected void done() {
    }

    protected void set(V v) {
        if (state < DONE) {
            result = v;
            state = DONE;
            done();
        }
    }

    protected void setException(Throwable t) {
        if (state < DONE) {
            failure = t;
            state = DONE;
            done();
        }
    }

    @Override
    public void run() {
        if (state != NEW) {
            return;
        }
        state = RUNNING;
        try {
            V v = callable.call();
            if (state == RUNNING) {
                set(v);
            }
        } catch (Throwable t) {
            if (state == RUNNING) {
                setException(t);
            }
        }
    }

    protected boolean runAndReset() {
        if (state != NEW) {
            return false;
        }
        try {
            callable.call();
            return state == NEW;
        } catch (Throwable t) {
            setException(t);
            return false;
        }
    }
}
