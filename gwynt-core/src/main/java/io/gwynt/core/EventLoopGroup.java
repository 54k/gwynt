package io.gwynt.core;

public interface EventLoopGroup extends EventLoop {

    EventLoop next();
}
