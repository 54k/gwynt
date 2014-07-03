package io.gwynt.core.oio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.EventLoop;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.ThreadPerChannelEventLoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOioChannel extends AbstractChannel {

    protected static final int SO_TIMEOUT = 16;

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
                flush();
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
        protected void afterRegister() {
            // NO OP
        }

        @Override
        protected void afterUnregister() {
            // NO OP
        }

        public void doRead() {
            Throwable error = null;
            boolean closed = false;
            try {
                int messagesRead = 0;
                try {
                    messagesRead = doReadMessages(messages);
                    if (messagesRead < 0) {
                        closed = true;
                    }
                } catch (Throwable e) {
                    error = e;
                }

                for (int i = 0; i < messagesRead; i++) {
                    pipeline().fireMessageReceived(messages.get(i));
                }

                if (error != null) {
                    if (error instanceof IOException) {
                        closed = !(AbstractOioChannel.this instanceof ServerChannel);
                    }
                    pipeline().fireExceptionCaught(error);
                }

                if (closed && isOpen()) {
                    doClose();
                }
                messages.clear();
            } finally {
                if (isActive() && config().isAutoRead()) {
                    readRequested();
                }
            }
        }

        /**
         * @return number of messages read or -1 if end of stream occurred
         */
        protected abstract int doReadMessages(List<Object> messages) throws Exception;
    }
}
