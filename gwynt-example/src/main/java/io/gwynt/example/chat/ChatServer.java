package io.gwynt.example.chat;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelInitializer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.MulticastChannel;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.group.ChannelGroup;
import io.gwynt.core.group.DefaultChannelGroup;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.example.UtfStringConverter;

import java.net.SocketAddress;

public class ChatServer implements Runnable {

    private EventLoopGroup group;
    private Class<? extends ServerChannel> serverChannel;

    public ChatServer(EventLoopGroup group, Class<? extends ServerChannel> serverChannel) {
        this.group = group;
        this.serverChannel = serverChannel;
    }

    @Override
    public void run() {
        IOReactor reactor = new IOReactor();
        reactor.channelClass(serverChannel).group(group).addChildHandler(new ChannelInitializer() {
            @Override
            protected void initialize(Channel channel) {
                channel.pipeline().addFirst(new UtfStringConverter());
                if (channel instanceof MulticastChannel) {
                    channel.pipeline().addFirst(new DatagramHandler());
                }
            }
        }).addChildHandler(new ChatHandler()).bind(1337);
    }

    private static final class ChatHandler extends AbstractHandler<String, String> {

        private ChannelGroup channels = new DefaultChannelGroup("chat-clients");

        @Override
        public void onOpen(HandlerContext context) {
            channels.add(context.channel());
            channels.write(context.channel() + " entered in chat\r\n");
        }

        @Override
        public void onMessageReceived(HandlerContext context, String message) {
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
        public void onClose(HandlerContext context) {
            channels.remove(context.channel());
            channels.write(context.channel() + " left the chat\r\n");
        }
    }

    private static final class DatagramHandler extends AbstractHandler<Datagram, byte[]> {

        private SocketAddress sender;

        @Override
        public void onMessageReceived(HandlerContext context, Datagram message) {
            if (sender == null) {
                context.fireOpen();
            }
            sender = message.sender();
            context.fireMessageReceived(message.content());
        }

        @Override
        public void onClosing(HandlerContext context, ChannelPromise channelPromise) {
            super.onClosing(context, channelPromise);
        }

        @Override
        public void onMessageSent(HandlerContext context, byte[] message, ChannelPromise channelPromise) {
            context.write(new Datagram(message, sender, null), channelPromise);
        }
    }
}
