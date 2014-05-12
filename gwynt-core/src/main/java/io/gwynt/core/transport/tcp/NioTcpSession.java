package io.gwynt.core.transport.tcp;

import io.gwynt.core.Endpoint;
import io.gwynt.core.IoSessionStatus;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.transport.AbstractIoSession;
import io.gwynt.core.transport.Channel;
import io.gwynt.core.transport.Dispatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioTcpSession extends AbstractIoSession<SocketChannel> {

    public NioTcpSession(Channel<SocketChannel> channel, Endpoint endpoint) {
        super(channel, endpoint);
    }

    @Override
    public void write(Object data) {
        if (!(data instanceof byte[])) {
            throw new IllegalArgumentException("Data is not instanceof byte[]");
        }
        if (status.get() != IoSessionStatus.PENDING_CLOSE && status.get() != IoSessionStatus.CLOSED) {
            writeQueue.add(ByteBuffer.wrap((byte[]) data));
            synchronized (registrationLock) {
                if (registered.get()) {
                    dispatcher.get().modifyRegistration(javaChannel(), SelectionKey.OP_WRITE);
                }
            }
        }
    }

    @Override
    public void close() {
        if (status.get() != IoSessionStatus.PENDING_CLOSE && status.get() != IoSessionStatus.CLOSED) {
            status.set(IoSessionStatus.PENDING_CLOSE);
            synchronized (registrationLock) {
                if (registered.get()) {
                    dispatcher.get().modifyRegistration(javaChannel(), SelectionKey.OP_WRITE);
                }
            }
        }
    }

    @Override
    public void onSessionRegistered(Dispatcher dispatcher) {
        synchronized (registrationLock) {
            registered.set(true);
            this.dispatcher.set(dispatcher);
            pipeline.fireRegistered();
        }
        if (status.get() != IoSessionStatus.PENDING_CLOSE) {
            boolean wasActive = status.getAndSet(IoSessionStatus.OPENED) == IoSessionStatus.OPENED;
            if (!wasActive) {
                pipeline.fireOpen();
            }
            if (!writeQueue.isEmpty()) {
                this.dispatcher.get().modifyRegistration(javaChannel(), SelectionKey.OP_WRITE);
            }
        }
    }

    @Override
    public void onSessionUnregistered(Dispatcher dispatcher) {
        synchronized (registrationLock) {
            registered.set(false);
            this.dispatcher.set(null);
            pipeline.fireUnregistered();
        }
        if (status.get() == IoSessionStatus.PENDING_CLOSE) {
            try {
                channel.close();
            } catch (IOException e) {
                // ignore
            }
            boolean wasClosed = status.getAndSet(IoSessionStatus.CLOSED) == IoSessionStatus.CLOSED;
            if (!wasClosed) {
                pipeline.fireClose();
            }
        }
    }

    @Override
    public void onSelectedForRead(SelectionKey key) throws IOException {
        int totalBytesRead;
        boolean eof = false;

        try {
            totalBytesRead = channel.read(readBuffer);
        } catch (EofException e) {
            eof = true;
            totalBytesRead = readBuffer.position();
        }
        readBuffer.flip();

        if (totalBytesRead > 0) {
            byte[] message = new byte[readBuffer.limit()];
            readBuffer.get(message);
            readBuffer.clear();
            pipeline.fireMessageReceived(message);
        }

        if (eof) {
            closeConnection();
        }
    }

    @Override
    public void onSelectedForWrite(SelectionKey key) throws IOException {
        ByteBuffer data = (ByteBuffer) writeQueue.peek();

        if (data != null) {
            try {
                channel.write(data);
            } catch (EofException e) {
                closeConnection();
                return;
            }

            if (!data.hasRemaining()) {
                writeQueue.poll();
            }
        }

        if (!writeQueue.isEmpty()) {
            dispatcher.get().modifyRegistration(javaChannel(), SelectionKey.OP_WRITE);
        } else if (status.get() == IoSessionStatus.PENDING_CLOSE) {
            closeConnection();
        }
    }

    @Override
    public void onExceptionCaught(Throwable e) {
        pipeline.fireExceptionCaught(e);
        closeConnection();
    }

    private void closeConnection() {
        status.set(IoSessionStatus.PENDING_CLOSE);
        if (!writeQueue.isEmpty()) {
            writeQueue.clear();
        }
        dispatcher.get().unregister(javaChannel());
    }
}
