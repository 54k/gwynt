package io.gwynt.core.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class DefaultThreadFactory implements ThreadFactory {

    private static final String DEFAULT_POOL_NAME = "gwynt-pool";

    private static final AtomicInteger poolNumber = new AtomicInteger(0);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final String poolName;

    public DefaultThreadFactory() {
        this(DEFAULT_POOL_NAME);
    }

    public DefaultThreadFactory(String poolName) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.poolName = poolName + "-" + poolNumber.incrementAndGet();
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, poolName(), 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

    private String poolName() {
        return poolName + "-thread-" + threadNumber.incrementAndGet();
    }
}
