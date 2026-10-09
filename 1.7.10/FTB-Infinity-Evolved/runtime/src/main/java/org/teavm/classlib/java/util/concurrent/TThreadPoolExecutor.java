package org.teavm.classlib.java.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolExecutor on TeaVM's green threads. Same policy as the JDK's (core threads first, then the queue, then up
 * to the maximum), worker threads poll their queue.
 */
public class TThreadPoolExecutor extends TAbstractExecutorService {
    private final BlockingQueue<Runnable> workQueue;
    private final ThreadFactory factory;
    private int corePoolSize;
    private int maximumPoolSize;
    private long keepAliveMillis;
    private int workers;
    private int idle;
    private boolean shutdown;
    private boolean allowCoreTimeout;

    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, TExecutors.defaultThreadFactory());
    }

    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory factory) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0) {
            throw new IllegalArgumentException();
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveMillis = unit.toMillis(keepAliveTime);
        this.workQueue = workQueue;
        this.factory = factory;
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        if (shutdown) {
            throw new RejectedExecutionException("Task " + command + " rejected from " + this);
        }
        if (workers < corePoolSize) {
            addWorker(command);
        } else if (workQueue.offer(command)) {
            if (workers == 0) {
                addWorker(null);
            }
        } else if (workers < maximumPoolSize) {
            addWorker(command);
        } else {
            throw new RejectedExecutionException("Task " + command + " rejected from " + this);
        }
    }

    private void addWorker(Runnable first) {
        workers++;
        Thread t = factory.newThread(() -> runWorker(first));
        t.start();
    }

    private void runWorker(Runnable first) {
        Runnable task = first;
        try {
            while (true) {
                if (task == null) {
                    task = nextTask();
                    if (task == null) {
                        return;
                    }
                }
                try {
                    task.run();
                } catch (Throwable t) {
                    Thread th = Thread.currentThread();
                    System.err.println("Exception in thread \"" + th.getName() + "\": " + t);
                }
                task = null;
            }
        } finally {
            workers--;
        }
    }

    private Runnable nextTask() {
        long idleSince = System.currentTimeMillis();
        idle++;
        try {
            while (true) {
                Runnable r = workQueue.poll();
                if (r != null) {
                    return r;
                }
                if (shutdown) {
                    return null;
                }
                boolean timed = allowCoreTimeout || workers > corePoolSize;
                if (timed && System.currentTimeMillis() - idleSince >= keepAliveMillis) {
                    return null;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        } finally {
            idle--;
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> pending = new ArrayList<>();
        workQueue.drainTo(pending);
        return pending;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown && workers == 0;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (!isTerminated()) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(1);
        }
        return true;
    }

    public void allowCoreThreadTimeOut(boolean value) {
        allowCoreTimeout = value;
    }

    public int getPoolSize() {
        return workers;
    }

    public int getActiveCount() {
        return workers - idle;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        this.keepAliveMillis = unit.toMillis(time);
    }

    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    public ThreadFactory getThreadFactory() {
        return factory;
    }

    public boolean prestartCoreThread() {
        if (workers < corePoolSize) {
            addWorker(null);
            return true;
        }
        return false;
    }

    public int prestartAllCoreThreads() {
        int n = 0;
        while (prestartCoreThread()) {
            n++;
        }
        return n;
    }
}
