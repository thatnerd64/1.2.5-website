package org.teavm.classlib.java.util.concurrent;

import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Timed tasks wait in a priority queue served by one scheduler thread, which hands due tasks to the pool. */
public class TScheduledThreadPoolExecutor extends TThreadPoolExecutor implements ScheduledExecutorService {
    private final PriorityQueue<Task<?>> timed = new PriorityQueue<>();
    private boolean schedulerRunning;
    private long sequence;

    public TScheduledThreadPoolExecutor(int corePoolSize) {
        this(corePoolSize, TExecutors.defaultThreadFactory());
    }

    public TScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory factory) {
        super(corePoolSize, Integer.MAX_VALUE, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory);
    }

    private final class Task<V> extends FutureTask<V> implements ScheduledFuture<V> {
        long time;
        final long period;
        final long seq = sequence++;

        Task(Callable<V> c, long time, long period) {
            super(c);
            this.time = time;
            this.period = period;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }
            Task<?> t = (Task<?>) other;
            int c = Long.compare(time, t.time);
            return c != 0 ? c : Long.compare(seq, t.seq);
        }

        @Override
        public void run() {
            if (period == 0) {
                super.run();
            } else if (!isCancelled() && runAndReset()) {
                time = period > 0 ? time + period : System.currentTimeMillis() - period;
                enqueue(this);
            }
        }
    }

    private void enqueue(Task<?> task) {
        if (isShutdown()) {
            throw new RejectedExecutionException();
        }
        timed.add(task);
        if (!schedulerRunning) {
            schedulerRunning = true;
            Thread t = getThreadFactory().newThread(this::schedule);
            t.start();
        }
    }

    private void schedule() {
        try {
            while (!isShutdown()) {
                Task<?> next = timed.peek();
                if (next == null || next.getDelay(TimeUnit.MILLISECONDS) > 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        return;
                    }
                    continue;
                }
                timed.poll();
                if (!next.isCancelled()) {
                    super.execute(next);
                }
            }
        } finally {
            schedulerRunning = false;
        }
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Task<Void> t = new Task<>(() -> {
            command.run();
            return null;
        }, System.currentTimeMillis() + unit.toMillis(delay), 0);
        enqueue(t);
        return t;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        Task<V> t = new Task<>(callable, System.currentTimeMillis() + unit.toMillis(delay), 0);
        enqueue(t);
        return t;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Task<Void> t = new Task<>(() -> {
            command.run();
            return null;
        }, System.currentTimeMillis() + unit.toMillis(initialDelay), unit.toMillis(period));
        enqueue(t);
        return t;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Task<Void> t = new Task<>(() -> {
            command.run();
            return null;
        }, System.currentTimeMillis() + unit.toMillis(initialDelay), -unit.toMillis(delay));
        enqueue(t);
        return t;
    }

    @Override
    public void execute(Runnable command) {
        schedule(command, 0, TimeUnit.MILLISECONDS);
    }
}
