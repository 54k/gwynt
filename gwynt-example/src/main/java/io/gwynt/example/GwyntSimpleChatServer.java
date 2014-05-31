package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.concurrent.ScheduledFuture;
import io.gwynt.core.group.ChannelGroup;
import io.gwynt.core.group.DefaultChannelGroup;
import io.gwynt.core.nio.NioEventLoop;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.concurrent.TimeUnit;

public class GwyntSimpleChatServer implements Runnable {

    private ChannelGroup channels = new DefaultChannelGroup();

    @Override
    public void run() {
        Endpoint endpoint = new EndpointBootstrap().setEventLoop(new NioEventLoop()).setChannelClass(NioServerSocketChannel.class).addHandler(new UtfStringConverter())
                .addHandler(new ChantHandler());

        try {
            endpoint.bind(1337).sync();
        } catch (InterruptedException ignore) {
        }
    }

    private static class ActivityListener implements Runnable {

        private Channel channel;
        private ScheduledFuture task;

        private ActivityListener(Channel channel) {
            this.channel = channel;
            this.channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void onComplete(ChannelFuture future) {
                    if (task != null) {
                        task.cancel();
                    }
                }
            });
        }

        public void refresh() {
            if (task != null) {
                task.cancel();
            }

            task = channel.eventLoop().schedule(this, 15, TimeUnit.SECONDS);
        }

        @Override
        public void run() {
            channel.close();
        }
    }

    private class ChantHandler extends AbstractHandler<String, Object> {

        @Override
        public void onOpen(final HandlerContext context) {
            channels.add(context.channel());
            channels.write(context.channel() + " entered in chat\r\n");
            ActivityListener activityListener = new ActivityListener(context.channel());
            context.channel().attach(activityListener);
            activityListener.refresh();
        }

        @Override
        public void onClose(HandlerContext context) {
            channels.remove(context.channel());
            channels.write(context.channel() + " leaved the chat\r\n");
        }

        @Override
        public void onMessageReceived(HandlerContext context, String message) {
            if ("list\r\n".equalsIgnoreCase(message)) {
                context.write(channels.toString() + "\r\n");
            } else if ("exit\r\n".equalsIgnoreCase(message)) {
                context.close();
            } else {
                channels.write(context.channel() + " wrote: " + message);
            }
            ((ActivityListener) context.channel().attachment()).refresh();
        }
    }
}
