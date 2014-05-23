package io.gwynt.core;

public interface EventLoopGroup {

    EventLoop parent();

    EventLoop next();
}
