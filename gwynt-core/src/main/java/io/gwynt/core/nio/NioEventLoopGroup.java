package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.EventLoop;
import io.gwynt.core.MultiThreadEventLoopGroup;
import io.gwynt.core.concurrent.EventExecutor;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

public class NioEventLoopGroup extends MultiThreadEventLoopGroup {

    public NioEventLoopGroup() {
    }

    public NioEventLoopGroup(int nThreads) {
        super(nThreads);
    }

    @Override
    protected EventLoop newEventExecutor(Executor executor) {
        return new NioEventLoop(this, SelectorProvider.provider(), executor);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise channelPromise) {
        return next().register(channel, channelPromise);
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    public void setIoRatio(int ioRatio) {
        for (EventExecutor e : children()) {
            ((NioEventLoop) e).setIoRatio(ioRatio);
        }
    }
}
