package io.gwynt.core.oio;

import io.gwynt.core.ChannelOutboundBuffer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.List;

public class OioDatagramChannel extends AbstractOioChannel {

    @SuppressWarnings("unused")
    public OioDatagramChannel() throws IOException {
        super(new DatagramSocket());
    }

    @Override
    protected Unsafe newUnsafe() {
        return new OioDatagramChannelUnsafe();
    }

    private class OioDatagramChannelUnsafe extends AbstractOioUnsafe<DatagramSocket> {

        @Override
        protected boolean isActive() {
            return isOpen() && (javaChannel().isBound() || javaChannel().isConnected());
        }

        @Override
        protected boolean isOpen() {
            return !javaChannel().isClosed();
        }

        @Override
        protected int doReadMessages(List<Object> messages) {
            return 0;
        }

        @Override
        protected void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
        }

        @Override
        protected void closeJavaChannel() {
            javaChannel().close();
        }
    }
}
