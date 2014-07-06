package io.gwynt.core.rudp;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.MulticastChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

public abstract class AbstractRudpChannel<C extends MulticastChannel> extends AbstractChannel implements RudpChannel {

    protected AbstractRudpChannel(C parent, Object ch) {
        super(parent, ch);
    }

    @SuppressWarnings("unchecked")
    @Override
    public C parent() {
        return (C) super.parent();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return parent().getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return parent().getLocalAddress();
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return parent().unblock(multicastAddress, networkInterface, source, channelPromise);
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return parent().unblock(multicastAddress, networkInterface, source);
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        return parent().unblock(multicastAddress, source, channelPromise);
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source) {
        return parent().unblock(multicastAddress, source);
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return parent().block(multicastAddress, networkInterface, source, channelPromise);
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return parent().block(multicastAddress, networkInterface, source);
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        return parent().block(multicastAddress, source, channelPromise);
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source) {
        return parent().block(multicastAddress, source);
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return parent().leaveGroup(multicastAddress, networkInterface, source, channelPromise);
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return parent().leaveGroup(multicastAddress, networkInterface, source);
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        return parent().leaveGroup(multicastAddress, networkInterface, channelPromise);
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise channelPromise) {
        return parent().leaveGroup(multicastAddress, channelPromise);
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return parent().leaveGroup(multicastAddress, networkInterface);
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress) {
        return parent().leaveGroup(multicastAddress);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        return parent().joinGroup(multicastAddress, networkInterface, source, channelPromise);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return parent().joinGroup(multicastAddress, networkInterface, source);
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        return parent().joinGroup(multicastAddress, networkInterface, channelPromise);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise channelPromise) {
        return parent().joinGroup(multicastAddress, channelPromise);
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return parent().joinGroup(multicastAddress, networkInterface);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress) {
        return parent().joinGroup(multicastAddress);
    }

    protected abstract class AbstractRudpUnsafe<T> extends AbstractUnsafe<T> {

    }
}
