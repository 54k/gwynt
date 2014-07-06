package io.gwynt.core.rudp;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.EventLoop;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;

public class RudpNioServerChannel extends AbstractRudpChannel {

    public RudpNioServerChannel(Channel parent, Object ch) {
        super(parent, ch);
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return false;
    }

    @Override
    protected Unsafe newUnsafe() {
        return null;
    }

    @Override
    public ChannelFuture writeReliable(Object message) {
        return null;
    }

    @Override
    public ChannelFuture writeReliable(Object message, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress) {
        return null;
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return null;
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return null;
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress) {
        return null;
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return null;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return null;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source) {
        return null;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return null;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source) {
        return null;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return null;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return null;
    }

    private class RudpNioServerUnsafe extends AbstractRudpUnsafe<Void> {
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
        protected void doClose() {

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
