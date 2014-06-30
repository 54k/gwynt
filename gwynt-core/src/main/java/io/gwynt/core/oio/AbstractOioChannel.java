package io.gwynt.core.oio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.EventLoop;
import io.gwynt.core.ThreadPerChannelEventLoop;

public abstract class AbstractOioChannel extends AbstractChannel {

    protected static final int SO_TIMEOUT = 1000;

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

        @Override
        protected void readRequested() {
            invokeLater(READ_TASK);
        }

        @Override
        protected void writeRequested() {
            invokeLater(WRITE_TASK);
        }

        @Override
        protected void closeRequested() {
        }

        @Override
        protected void afterRegister() {
        }

        @Override
        protected void afterUnregister() {
        }
    }
}
