package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelInitializer;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.codec.ByteToMessageCodec;
import io.gwynt.core.concurrent.GlobalEventExecutor;
import io.gwynt.core.concurrent.ScheduledFuture;
import io.gwynt.core.group.ChannelGroup;
import io.gwynt.core.group.DefaultChannelGroup;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.nio.NioSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

//import io.gwynt.core.DatagramChannel;
//import io.gwynt.core.Datagram;
//import io.gwynt.core.nio.NioDatagramChannel;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.net.NetworkInterface;
//import java.nio.ByteBuffer;

public class GwyntSimpleChatServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GwyntSimpleChatServer.class);

    private ChannelGroup channels;
    private int port = 1337;
    private EventLoopGroup eventLoop = new NioEventLoopGroup(2);

    @Override
    public void run() {
        final ChatHandler chatHandler = new ChatHandler();
        channels = new DefaultChannelGroup();

        IOReactor endpoint =
                new IOReactor().group(eventLoop).channelClass(NioServerSocketChannel.class)/*.addHandler(new UtfStringConverter())*/.addHandler(new ChannelInitializer() {
                    @Override
                    protected void initialize(Channel channel) {
                        channel.pipeline().addFirst(new MessageDecoder());
                        channel.pipeline().addLast(chatHandler);
                    }
                });

        try {
            endpoint.bind(port).sync();
            //            runDiscoveryServer(3000).sync();
            //            runDiscoveryClient(3000).sync();
            logger.info("Server listening port {}", 1337);
            createBots(port);
        } catch (InterruptedException ignore) {
        }

        GlobalEventExecutor.INSTANCE.schedule(new Runnable() {
            @Override
            public void run() {
                logger.info("tick");
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void createBots(int port) {
        IOReactor client = new IOReactor().group(eventLoop).channelClass(NioSocketChannel.class).addHandler(new UtfStringConverter()).addHandler(new AbstractHandler() {
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

        for (int i = 0; i < 1000; i++) {
            client.connect("localhost", port);
        }
    }

    //    private ChannelFuture runDiscoveryServer(final int port) {
    //        Endpoint endpoint = new EndpointBootstrap().channelClass(NioDatagramChannel.class).group(primaryGroup);
    //
    //        return endpoint.bind(port).addListener(new ChannelFutureListener() {
    //            @Override
    //            public void onComplete(final ChannelFuture future) {
    //                try {
    //                    final InetAddress multicastAddress = InetAddress.getByName("FF01:0:0:0:0:0:0:1");
    //                    future.channel().primaryGroup().scheduleWithFixedDelay(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            ByteBuffer bb = ByteBuffer.allocate(4);
    //                            bb.putInt(GwyntSimpleChatServer.this.port);
    //                            bb.flip();
    //                            future.channel().write(new Datagram(new InetSocketAddress(multicastAddress, port), bb));
    //                        }
    //                    }, 5, 5, TimeUnit.SECONDS);
    //                } catch (Throwable t) {
    //                    throw new RuntimeException(t);
    //                }
    //            }
    //        });
    //    }
    //
    //    private ChannelFuture runDiscoveryClient(int port) {
    //        Endpoint endpoint = new EndpointBootstrap().channelClass(NioDatagramChannel.class).group(primaryGroup);
    //        final InetAddress multicastAddress;
    //        final NetworkInterface networkInterface;
    //        try {
    //            multicastAddress = InetAddress.getByName("FF01:0:0:0:0:0:0:1");
    //            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
    //        } catch (Throwable t) {
    //            throw new RuntimeException(t);
    //        }
    //        endpoint.addHandler(new AbstractHandler<Datagram, Object>() {
    //            @Override
    //            public void onMessageReceived(HandlerContext context, Datagram message) {
    //                int port = message.content().getInt();
    //                logger.info("Discovered port {}, creating clients", port);
    //                createBots(port);
    //                ((DatagramChannel) context.channel()).leaveGroup(multicastAddress, networkInterface);
    //                context.close();
    //            }
    //        });
    //
    //        return endpoint.bind(port).addListener(new ChannelFutureListener() {
    //            @Override
    //            public void onComplete(ChannelFuture future) {
    //                try {
    //                    ((DatagramChannel) future.channel()).joinGroup(multicastAddress, networkInterface).sync();
    //                } catch (Throwable t) {
    //                    throw new RuntimeException(t);
    //                }
    //            }
    //        });
    //    }

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

        @Override
        public void onExceptionCaught(HandlerContext context, Throwable e) {
            GwyntSimpleChatServer.logger.error(e.getMessage(), e);
        }
    }

    private class MessageDecoder extends ByteToMessageCodec<String> {

        private StringBuilder buffer = new StringBuilder();

        @Override
        protected void encode(HandlerContext context, String message, ByteBuffer out) {
            out.put(message.getBytes());
        }

        @Override
        protected void decode(HandlerContext context, ByteBuffer message, List<Object> out) {
            while (message.hasRemaining()) {
                char c = (char) message.get();
                buffer.append(c);
                if (c == '\n') {
                    buffer.trimToSize();
                    out.add(buffer.toString());
                    buffer.delete(0, buffer.length());
                }
            }
        }
    }
}
