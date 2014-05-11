package io.gwynt.core.transport.tcp;

import io.gwynt.core.exception.EofException;
import io.gwynt.core.transport.AbstractSelectableChannel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioSocketChannel extends AbstractSelectableChannel<SocketChannel> {

    public NioSocketChannel(SocketChannel channel) {
        super(channel);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int totalBytesRead = 0;
        int bytesRead;

        do {
            bytesRead = channel.read(dst);
            totalBytesRead += bytesRead;
        } while (dst.hasRemaining() && bytesRead > 0);

        if (bytesRead == -1) {
            throw new EofException();
        }

        return totalBytesRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int totalBytesWritten = 0;
        int bytesWritten;

        do {
            bytesWritten = channel.write(src);
            totalBytesWritten += bytesWritten;
        } while (src.hasRemaining() && bytesWritten > 0);

        if (bytesWritten == -1) {
            throw new EofException();
        }

        return totalBytesWritten;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public SocketAddress getLocalAddress() {
        try {
            return channel.getLocalAddress();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public SocketAddress getRemoteAddress() {
        try {
            return channel.getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }
}
