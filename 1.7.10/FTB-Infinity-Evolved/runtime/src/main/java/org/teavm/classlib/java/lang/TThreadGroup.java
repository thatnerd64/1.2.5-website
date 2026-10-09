package org.teavm.classlib.java.lang;

/** java.lang.ThreadGroup: threads here are green threads, so a group is only a name and a parent. */
public class TThreadGroup extends TObject {
    static final TThreadGroup MAIN = new TThreadGroup(null, "main");
    private final TThreadGroup parent;
    private final String name;
    private int maxPriority = 10;
    private boolean daemon;

    public TThreadGroup(String name) {
        this(MAIN, name);
    }

    public TThreadGroup(TThreadGroup parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public final TThreadGroup getParent() {
        return parent;
    }

    public final int getMaxPriority() {
        return maxPriority;
    }

    public final void setMaxPriority(int priority) {
        maxPriority = priority;
    }

    public final boolean isDaemon() {
        return daemon;
    }

    public final void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public int activeCount() {
        return 1;
    }

    public void uncaughtException(TThread thread, Throwable e) {
        if (parent != null) {
            parent.uncaughtException(thread, e);
        } else {
            System.err.print("Exception in thread \"" + thread.getName() + "\" ");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + name + ",maxpri=" + maxPriority + "]";
    }
}
