package io.gwynt.core.transport;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NioAcceptor extends AbstractDispatcher {

    private DispatcherPool dispatcherPool;

    public NioAcceptor(DispatcherPool dispatcherPool) {
        this.dispatcherPool = dispatcherPool;
    }

    @Override
    protected int getChannelRegisterOps() {
        return SelectionKey.OP_ACCEPT;
    }

    @Override
    public void processSelectedKey(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            try {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                SocketChannel channel;
                while ((channel = serverSocketChannel.accept()) != null) {
                    channel.configureBlocking(false);
                    dispatcherPool.getDispatcher().register(channel);
                }
            } catch (SocketTimeoutException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
