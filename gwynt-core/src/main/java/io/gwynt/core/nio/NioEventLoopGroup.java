package io.gwynt.core.nio;

import io.gwynt.core.EventLoop;
import io.gwynt.core.MultiThreadEventLoopGroup;
import io.gwynt.core.concurrent.DefaultThreadFactory;
import io.gwynt.core.concurrent.EventExecutor;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class NioEventLoopGroup extends MultiThreadEventLoopGroup {

    public NioEventLoopGroup() {
        this(0, new DefaultThreadFactory("gwynt-nio-eventloop", Thread.MAX_PRIORITY));
    }

    public NioEventLoopGroup(int nThreads) {
        this(nThreads, new DefaultThreadFactory("gwynt-nio-eventloop", Thread.MAX_PRIORITY));
    }

    public NioEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }

    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SelectorProvider.provider());
    }

    public NioEventLoopGroup(int nThreads, Executor executor, SelectorProvider selectorProvider) {
        super(nThreads, executor, selectorProvider);
    }

    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectorProvider selectorProvider) {
        super(nThreads, threadFactory, selectorProvider);
    }

    @Override
    protected EventLoop newEventExecutor(Executor executor, Object... args) {
        return new NioEventLoop(this, (SelectorProvider) args[0], executor);
    }

    public void setIoRatio(int ioRatio) {
        for (EventExecutor e : children()) {
            ((NioEventLoop) e).setIoRatio(ioRatio);
        }
    }
}
