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
        this(new DefaultThreadFactory("gwynt-nio-eventloop"));
    }

    public NioEventLoopGroup(Executor executor) {
        super(executor);
    }

    public NioEventLoopGroup(int nThreads, Executor executor) {
        super(nThreads, executor);
    }

    public NioEventLoopGroup(ThreadFactory threadFactory) {
        super(threadFactory);
    }

    public NioEventLoopGroup(int nThreads) {
        this(nThreads, new DefaultThreadFactory("gwynt-nio-eventloop"));
    }

    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    @Override
    protected EventLoop newEventExecutor(Executor executor) {
        return new NioEventLoop(this, SelectorProvider.provider(), executor);
    }

    public void setIoRatio(int ioRatio) {
        for (EventExecutor e : children()) {
            ((NioEventLoop) e).setIoRatio(ioRatio);
        }
    }
}
