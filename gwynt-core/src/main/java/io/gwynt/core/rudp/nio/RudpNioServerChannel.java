package io.gwynt.core.rudp.nio;

import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.EventLoop;
import io.gwynt.core.nio.NioDatagramChannel;
import io.gwynt.core.nio.NioEventLoop;
import io.gwynt.core.rudp.AbstractRudpChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RudpNioServerChannel extends AbstractRudpChannel<NioDatagramChannel> {

    @SuppressWarnings("unused")
    public RudpNioServerChannel() throws IOException {
        super(new NioDatagramChannel());
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return eventLoop instanceof NioEventLoop;
    }

    @Override
    protected Unsafe newUnsafe() {
        return new RudpNioServerUnsafe(parent().unsafe());
    }

    @Override
    public ChannelFuture writeReliable(Object message) {
        return null;
    }

    @Override
    public ChannelFuture writeReliable(Object message, ChannelPromise channelPromise) {
        return null;
    }

    private class RudpNioServerUnsafe extends AbstractRudpUnsafe<Void> {

        private final Unsafe unsafe;

        private RudpNioServerUnsafe(Unsafe unsafe) {
            this.unsafe = unsafe;
        }

        @Override
        protected void readRequested() {
        }

        @Override
        protected void writeRequested() {
        }

        @Override
        protected boolean isActive() {
            return false;
        }

        @Override
        protected boolean isOpen() {
            return false;
        }

        @Override
        protected void afterRegister() {

        }

        @Override
        protected void afterUnregister() {

        }

        @Override
        protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {

        }

        @Override
        public void closeForcibly() {

        }

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {

        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return null;
        }
    }
}
