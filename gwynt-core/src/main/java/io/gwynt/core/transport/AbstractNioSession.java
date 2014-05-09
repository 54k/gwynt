package io.gwynt.core.transport;

import io.gwynt.core.AbstractIoSession;
import io.gwynt.core.Channel;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.EofException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractNioSession extends AbstractIoSession<SelectableChannel> implements SelectorEventListener {

    private final Object registrationLock = new Object();

    private AtomicBoolean registered = new AtomicBoolean(false);
    private AtomicReference<Dispatcher> dispatcher = new AtomicReference<>();

    public AbstractNioSession(Channel<SelectableChannel> channel, Endpoint endpoint) {
        super(channel, endpoint);
    }

    @Override
    public void write(byte[] data) {
        if (!pendingClose.get() && !closed.get()) {
            writeQueue.add(ByteBuffer.wrap(data));
            synchronized (registrationLock) {
                if (registered.get()) {
                    dispatcher.get().modifyRegistration(channel.unwrap(), SelectionKey.OP_WRITE);
                }
            }
        }
    }

    @Override
    public void close() {
        if (!pendingClose.get() && !closed.get()) {
            pendingClose.set(true);
            synchronized (registrationLock) {
                if (registered.get()) {
                    dispatcher.get().modifyRegistration(channel.unwrap(), SelectionKey.OP_WRITE);
                }
            }
        }
    }

    @Override
    public void onSessionRegistered(Dispatcher dispatcher) {
        synchronized (registrationLock) {
            registered.set(true);
            this.dispatcher.set(dispatcher);
        }
        if (!pendingClose.get()) {
            pipeline.fireOpen();
            if (!writeQueue.isEmpty()) {
                this.dispatcher.get().modifyRegistration(channel.unwrap(), SelectionKey.OP_WRITE);
            }
        }
    }

    @Override
    public void onSessionUnregistered(Dispatcher dispatcher) {
        synchronized (registrationLock) {
            registered.set(false);
            this.dispatcher.set(null);
        }
        if (pendingClose.get()) {
            try {
                channel.close();
            } catch (IOException e) {
                // ignore
            }
            boolean wasClosed = closed.getAndSet(true);
            if (!wasClosed) {
                pipeline.fireClose();
            }
        }
    }

    @Override
    public void onSelectedForRead() throws IOException {
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
    public void onSelectedForWrite() throws IOException {
        ByteBuffer data = writeQueue.peek();

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
            dispatcher.get().modifyRegistration(channel.unwrap(), SelectionKey.OP_WRITE);
        } else if (pendingClose.get()) {
            closeConnection();
        }
    }

    @Override
    public void onExceptionCaught(Throwable e) {
        pipeline.fireExceptionCaught(e);
        closeConnection();
    }

    private void closeConnection() {
        pendingClose.set(true);
        if (!writeQueue.isEmpty()) {
            writeQueue.clear();
        }
        dispatcher.get().unregister(channel.unwrap());
    }
}
