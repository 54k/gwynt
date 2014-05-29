package io.gwynt.core;

import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.pipeline.HandlerContextInvoker;

public interface EventLoop extends EventExecutor, EventLoopGroup {

    HandlerContextInvoker asInvoker();

    @Override
    EventLoopGroup parent();
}
