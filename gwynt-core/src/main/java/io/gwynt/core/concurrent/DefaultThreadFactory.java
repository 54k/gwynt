package io.gwynt.core.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class DefaultThreadFactory implements ThreadFactory {

    private static final String DEFAULT_POOL_NAME = "gwynt-pool";

    private static final AtomicInteger poolNumber = new AtomicInteger(0);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final String poolName;
    private final int threadPriority;
    private final boolean daemon;

    public DefaultThreadFactory() {
        this(DEFAULT_POOL_NAME, Thread.NORM_PRIORITY, false);
    }

    public DefaultThreadFactory(String poolName) {
        this(poolName, Thread.NORM_PRIORITY, false);
    }

    public DefaultThreadFactory(int threadPriority) {
        this(DEFAULT_POOL_NAME, threadPriority);
    }

    public DefaultThreadFactory(boolean daemon) {
        this(DEFAULT_POOL_NAME, daemon);
    }

    public DefaultThreadFactory(String poolName, int threadPriority) {
        this(poolName, threadPriority, false);
    }

    public DefaultThreadFactory(String poolName, boolean daemon) {
        this(poolName, Thread.NORM_PRIORITY, daemon);
    }

    public DefaultThreadFactory(int threadPriority, boolean daemon) {
        this(DEFAULT_POOL_NAME, threadPriority, daemon);
    }

    public DefaultThreadFactory(String poolName, int threadPriority, boolean daemon) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.poolName = poolName + "-" + poolNumber.incrementAndGet();
        this.threadPriority = threadPriority;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, poolName(), 0);
        if (t.isDaemon()) {
            if (!daemon) {
                t.setDaemon(false);
            }
        } else {
            t.setDaemon(daemon);
        }

        if (t.getPriority() != threadPriority) {
            t.setPriority(threadPriority);
        }
        return t;
    }

    private String poolName() {
        return poolName + "-thread-" + threadNumber.incrementAndGet();
    }
}
