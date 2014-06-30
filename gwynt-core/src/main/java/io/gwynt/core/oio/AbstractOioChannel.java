package io.gwynt.core.oio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.EventLoop;
import io.gwynt.core.ThreadPerChannelEventLoop;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOioChannel extends AbstractChannel {

    protected static final int SO_TIMEOUT = 1;

    protected AbstractOioChannel(Object ch) {
        this(null, ch);
    }

    protected AbstractOioChannel(Channel parent, Object ch) {
        super(parent, ch);
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return eventLoop instanceof ThreadPerChannelEventLoop;
    }

    protected abstract class AbstractOioUnsafe<T> extends AbstractUnsafe<T> {

        private final Runnable READ_TASK = new Runnable() {
            @Override
            public void run() {
                doRead();
            }
        };
        private final Runnable WRITE_TASK = new Runnable() {
            @Override
            public void run() {
                doWrite();
            }
        };
        private final List<Object> messages = new ArrayList<>(1);

        @Override
        protected void readRequested() {
            invokeLater(READ_TASK);
        }

        @Override
        protected void writeRequested() {
            invokeLater(WRITE_TASK);
        }

        @Override
        protected void beforeClose() {
        }

        @Override
        protected void afterRegister() {
        }

        @Override
        protected void afterUnregister() {
        }

        @Override
        public void doRead() {
            assert eventLoop().inExecutorThread();

            int messagesRead = doReadMessages(messages);
            for (int i = 0; i < messagesRead; i++) {
                pipeline().fireMessageReceived(messages.get(i));
            }

            if (messagesRead > 0) {
                messages.clear();
            }
        }

        @Override
        protected void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            boolean done = false;
            Object message = channelOutboundBuffer.current();
            if (message != null) {
                done = doWriteMessage(message);
            }

            if (done) {
                channelOutboundBuffer.remove();
            } else {
                writeRequested();
            }
        }

        protected boolean doWriteMessage(Object message) throws Exception {
            throw new UnsupportedOperationException();
        }
    }
}
