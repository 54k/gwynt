package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelInitializer;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.EventLoop;
import io.gwynt.core.concurrent.ScheduledFuture;
import io.gwynt.core.group.ChannelGroup;
import io.gwynt.core.group.DefaultChannelGroup;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.nio.NioSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GwyntSimpleChatServer implements Runnable {

    private ChannelGroup channels = new DefaultChannelGroup();

    @Override
    public void run() {
        final ChatHandler chatHandler = new ChatHandler();
        EventLoop eventLoop = new NioEventLoopGroup();

        Endpoint endpoint = new EndpointBootstrap().setEventLoop(eventLoop).setChannelClass(NioServerSocketChannel.class).addHandler(new UtfStringConverter())
                .addHandler(new ChannelInitializer() {
                    @Override
                    protected void initialize(Channel channel) {
                        channel.pipeline().addLast(new AbstractHandler<String, Object>() {
                            private StringBuilder sb = new StringBuilder();

                            @Override
                            public void onMessageReceived(HandlerContext context, String message) {
                                sb.append(message);
                                if (message.lastIndexOf("\n") > -1) {
                                    sb.trimToSize();
                                    context.fireMessageReceived(sb.toString());
                                    sb.delete(0, sb.length() - 1);
                                }
                            }
                        });
                        channel.pipeline().addLast(chatHandler);
                    }
                }).addHandler(new ChatHandler());

        try {
            endpoint.bind(1337).sync();
        } catch (InterruptedException ignore) {
        }

        Endpoint client =
                new EndpointBootstrap().setEventLoop(eventLoop).setChannelClass(NioSocketChannel.class).addHandler(new UtfStringConverter()).addHandler(new AbstractHandler() {
                    @Override
                    public void onOpen(final HandlerContext context) {
                        context.write("hello\r\n").addListener(new ChannelFutureListener() {
                            @Override
                            public void onComplete(final ChannelFuture future) {
                                future.channel().eventLoop().scheduleAtFixedRate(new Runnable() {
                                    @Override
                                    public void run() {
                                        future.channel().write(new Date().toString() + "\r\n");
                                    }
                                }, new Random().nextInt(120) + 15, new Random().nextInt(300) + 15, TimeUnit.SECONDS);
                            }
                        });
                    }
                });

        for (int i = 0; i < 4000; i++) {
            try {
                client.connect("localhost", 1337).sync();
            } catch (InterruptedException ignore) {
            }
        }
    }

    private static class ActivityListener implements Runnable {

        private Channel channel;
        private ScheduledFuture task;
        private boolean removed;

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
            if (removed) {
                return;
            }
            if (task != null) {
                task.cancel();
            }
            task = channel.eventLoop().schedule(this, 15, TimeUnit.SECONDS);
        }

        public void remove() {
            if (removed) {
                return;
            }
            if (task != null) {
                task.cancel();
                removed = true;
            }
        }

        @Override
        public void run() {
            channel.close();
        }
    }

    private class ChatHandler extends AbstractHandler<String, Object> {

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
            channels.write(context.channel() + " left the chat\r\n");
        }

        @Override
        public void onMessageReceived(HandlerContext context, String message) {
            ((ActivityListener) context.channel().attachment()).remove();
            if ("list\r\n".equalsIgnoreCase(message)) {
                context.write(channels.toString() + "\r\n");
            } else if ("exit\r\n".equalsIgnoreCase(message)) {
                context.close();
            } else if ("!\r\n".equalsIgnoreCase(message)) {
                if (channels.contains(context.channel())) {
                    channels.remove(context.channel());
                } else {
                    channels.add(context.channel());
                }
            } else {
                channels.write(context.channel() + " wrote: " + message);
            }
        }
    }
}
