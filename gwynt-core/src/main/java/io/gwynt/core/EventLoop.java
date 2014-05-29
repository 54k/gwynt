package io.gwynt.core;

import io.gwynt.core.pipeline.HandlerContextInvoker;

public interface EventLoop extends EventLoopGroup {

    HandlerContextInvoker asInvoker();

    @Override
    EventLoopGroup parent();
}
