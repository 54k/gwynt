package io.gwynt.core.oio;

import io.gwynt.core.EventLoop;
import io.gwynt.core.MultiThreadEventLoopGroup;

import java.util.concurrent.Executor;

public class OioEventLoopGroup extends MultiThreadEventLoopGroup {

    public OioEventLoopGroup(int nThreads) {
        super(nThreads, (Executor) null);
    }

    @Override
    protected EventLoop newEventExecutor(Executor executor, Object... args) {
        return new OioEventLoop(this, executor);
    }
}
