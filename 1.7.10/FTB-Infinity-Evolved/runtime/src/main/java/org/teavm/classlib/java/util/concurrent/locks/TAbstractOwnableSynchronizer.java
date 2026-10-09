package org.teavm.classlib.java.util.concurrent.locks;

public abstract class TAbstractOwnableSynchronizer implements java.io.Serializable {
    private Thread exclusiveOwnerThread;

    protected TAbstractOwnableSynchronizer() {
    }

    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
