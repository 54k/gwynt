package io.gwynt.core.transport.udp;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.pipeline.IoHandlerContext;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

final class DatagramIoHandler extends AbstractIoHandler<Datagram, byte[]> {

    SocketAddress address;

    @Override
    public void onMessageReceived(IoHandlerContext context, Datagram message) {
        address = message.getAddress();
        byte[] messageBytes = new byte[message.getMessage().limit()];
        message.getMessage().get(messageBytes);
        context.fireMessageReceived(messageBytes);
    }

    @Override
    public void onMessageSent(IoHandlerContext context, byte[] message) {
        context.fireMessageSent(new Datagram(address, ByteBuffer.wrap(message)));
    }
}
