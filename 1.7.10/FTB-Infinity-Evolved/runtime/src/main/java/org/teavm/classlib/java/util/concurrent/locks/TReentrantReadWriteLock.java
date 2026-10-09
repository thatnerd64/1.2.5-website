package org.teavm.classlib.java.util.concurrent.locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

/** One exclusive lock serves as both the read and the write lock: threads only switch when they block. */
public class TReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {
    private final ReentrantLock lock = new ReentrantLock();
    private final ReadLock readLock = new ReadLock(lock);
    private final WriteLock writeLock = new WriteLock(lock);

    public TReentrantReadWriteLock() {
    }

    public TReentrantReadWriteLock(boolean fair) {
    }

    // (covariant, as in the JDK: callers compiled against it ask for ReadLock/WriteLock; javac adds the Lock bridges)
    @Override
    public ReadLock readLock() {
        return readLock;
    }

    @Override
    public WriteLock writeLock() {
        return writeLock;
    }

    public boolean isWriteLocked() {
        return lock.isLocked();
    }

    public int getReadLockCount() {
        return 0;
    }

    public static class ReadLock extends Delegate {
        protected ReadLock(ReentrantLock l) {
            super(l);
        }
    }

    public static class WriteLock extends Delegate {
        protected WriteLock(ReentrantLock l) {
            super(l);
        }

        public boolean isHeldByCurrentThread() {
            return lock.isHeldByCurrentThread();
        }

        public int getHoldCount() {
            return lock.getHoldCount();
        }
    }

    abstract static class Delegate implements Lock {
        final ReentrantLock lock;

        Delegate(ReentrantLock lock) {
            this.lock = lock;
        }

        @Override
        public void lock() {
            lock.lock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
        }

        @Override
        public boolean tryLock() {
            return lock.tryLock();
        }

        @Override
        public boolean tryLock(long time, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            return lock.tryLock(time, unit);
        }

        @Override
        public void unlock() {
            lock.unlock();
        }

        @Override
        public java.util.concurrent.locks.Condition newCondition() {
            return lock.newCondition();
        }
    }
}
