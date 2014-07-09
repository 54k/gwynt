package io.gwynt.core.rudp.nio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.EventLoop;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RudpNioChannel extends AbstractChannel {

    public RudpNioChannel(Channel parent, Object ch) {
        super(parent, ch);
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return true;
    }

    @Override
    protected Unsafe newUnsafe() {
        return null;
    }

    private class RudpNioChannelUnsafe extends AbstractUnsafe<Void> {

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
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {

        }

        @Override
        public void closeForcibly() {

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
