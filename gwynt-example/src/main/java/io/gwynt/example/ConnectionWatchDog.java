package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.IOReactor;
import io.gwynt.core.pipeline.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ConnectionWatchDog extends AbstractHandler implements ChannelFutureListener {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionWatchDog.class);

    private boolean connected;
    private boolean shouldReconnect = true;
    private InetSocketAddress address;
    private IOReactor reactor;

    public ConnectionWatchDog(IOReactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void onRegistered(HandlerContext context) {
        if (!connected) {
            context.fireRegistered();
        }
    }

    @Override
    public void onUnregistered(HandlerContext context) {
        if (!shouldReconnect) {
            context.fireUnregistered();
        }
    }

    @Override
    public void onOpen(HandlerContext context) {
        //        if (!connected) {
        connected = true;
        address = ((InetSocketAddress) context.channel().getRemoteAddress());
        context.fireOpen();
        //        }
    }

    @Override
    public void onClose(HandlerContext context) {
        if (!shouldReconnect) {
            context.fireClose();
            return;
        }
        logger.info("Connection lost to {}", address);
        reconnect();
    }

    @Override
    public void onClosing(HandlerContext context, ChannelPromise channelPromise) {
        logger.info("Connection to {} closed by application", address);
        shouldReconnect = false;
        connected = false;
        context.close(channelPromise);
    }

    private void reconnect() {
        logger.info("Reconnecting to {}", address);
        reactor.connect(address.getHostName(), address.getPort()).addListener(this);
    }

    @Override
    public void onComplete(ChannelFuture channelFuture) {
        if (channelFuture.isFailed()) {
            channelFuture.channel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    reconnect();
                }
            });
        } else {
            logger.info("Reconnected to {}", address);
        }
    }
}
