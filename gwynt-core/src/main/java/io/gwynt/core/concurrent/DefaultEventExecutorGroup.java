package io.gwynt.core.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public final class DefaultEventExecutorGroup extends MultiThreadEventExecutorGroup {

    public DefaultEventExecutorGroup(int nThreads) {
        this(nThreads, null);
    }

    public DefaultEventExecutorGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    @Override
    protected EventExecutor newEventExecutor(Executor executor, Object... args) {
        return new DefaultEventExecutor(this, executor);
    }
}
