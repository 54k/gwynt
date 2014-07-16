package io.gwynt.core;

import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.buffer.RecvByteBufferAllocator;
import io.gwynt.core.util.AbstractConstant;
import io.gwynt.core.util.ConstantPool;

import java.net.NetworkInterface;

public final class ChannelOption<T> extends AbstractConstant<ChannelOption<T>> {

    public static final ChannelOption<Boolean> SO_REUSEADDR = valueOf("SO_REUSEADDR");
    public static final ChannelOption<Boolean> TCP_NODELAY = valueOf("TCP_NODELAY");
    public static final ChannelOption<Integer> TRAFFIC_CLASS = valueOf("TRAFFIC_CLASS");
    public static final ChannelOption<NetworkInterface> IP_MULTICAST_IF = valueOf("IP_MULTICAST_IF");
    public static final ChannelOption<Integer> IP_MULTICAST_TTL = valueOf("IP_MULTICAST_TTL");
    public static final ChannelOption<Boolean> IP_MULTICAST_LOOP = valueOf("IP_MULTICAST_LOOP");
    public static final ChannelOption<Integer> IP_TOS = valueOf("IP_TOS");
    public static final ChannelOption<Integer> SO_LINGER = valueOf("SO_LINGER");
    public static final ChannelOption<Integer> SO_RCVBUF = valueOf("SO_RCVBUF");
    public static final ChannelOption<Integer> SO_SNDBUF = valueOf("SO_SNDBUF");
    public static final ChannelOption<Boolean> SO_KEEPALIVE = valueOf("SO_KEEPALIVE");
    public static final ChannelOption<Boolean> SO_BROADCAST = valueOf("SO_BROADCAST");
    public static final ChannelOption<Boolean> SO_TIMEOUT = valueOf("SO_TIMEOUT");
    public static final ChannelOption<Boolean> SO_BACKLOG = valueOf("SO_BACKLOG");

    public static final ChannelOption<Boolean> AUTO_READ = valueOf("AUTO_READ");
    public static final ChannelOption<RecvByteBufferAllocator> RECV_BYTE_BUFFER_ALLOCATOR = valueOf("RECV_BYTE_BUFFER_ALLOCATOR");
    public static final ChannelOption<ByteBufferPool> BYTE_BUFFER_POOL = valueOf("BYTE_BUFFER_POOL");
    public static final ChannelOption<Integer> WRITE_SPIN_COUNT = valueOf("WRITE_SPIN_COUNT");
    public static final ChannelOption<Integer> READ_SPIN_COUNT = valueOf("READ_SPIN_COUNT");
    public static final ChannelOption<Integer> CONNECT_TIMEOUT_MILLIS = valueOf("CONNECT_TIMEOUT_MILLIS");

    public static final ChannelOption<Integer> RUDP_PROTOCOL_MAGIC = valueOf("RUDP_PROTOCOL_MAGIC");
    public static final ChannelOption<Integer> RUDP_KEEP_ALIVE_PERIOD_MILLIS = valueOf("RUDP_KEEP_ALIVE_PERIOD_MILLIS");

    private static final ConstantPool<ChannelOption<Object>> pool = new ConstantPool<ChannelOption<Object>>() {
        @Override
        protected ChannelOption<Object> newConstant(int id, String name) {
            return new ChannelOption<>(id, name);
        }
    };

    private ChannelOption(int id, String name) {
        super(id, name);
    }

    @SuppressWarnings("unchecked")
    public static <T> ChannelOption<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (ChannelOption<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }

    @SuppressWarnings("unchecked")
    public static <T> ChannelOption<T> valueOf(String name) {
        return (ChannelOption<T>) pool.valueOf(name);
    }
}
