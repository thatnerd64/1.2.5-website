package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TExecutors {
    private static int poolNumber;

    private TExecutors() {
    }

    public static ThreadFactory defaultThreadFactory() {
        int pool = ++poolNumber;
        int[] threads = new int[1];
        return r -> new Thread(r, "pool-" + pool + "-thread-" + (++threads[0]));
    }

    public static ExecutorService newFixedThreadPool(int n) {
        return new ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public static ExecutorService newFixedThreadPool(int n, ThreadFactory factory) {
        return new ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), factory);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return newFixedThreadPool(1);
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory factory) {
        return newFixedThreadPool(1, factory);
    }

    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory factory) {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), factory);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int n) {
        return new java.util.concurrent.ScheduledThreadPoolExecutor(n);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int n, ThreadFactory factory) {
        return new java.util.concurrent.ScheduledThreadPoolExecutor(n, factory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return newScheduledThreadPool(1);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory factory) {
        return newScheduledThreadPool(1, factory);
    }

    public static ExecutorService unconfigurableExecutorService(ExecutorService e) {
        return e;
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        return () -> {
            task.run();
            return result;
        };
    }

    public static Callable<Object> callable(Runnable task) {
        return callable(task, null);
    }
}
