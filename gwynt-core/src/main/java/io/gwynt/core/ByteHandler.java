package io.gwynt.core;

import io.gwynt.core.pipeline.HandlerContext;

public abstract class ByteHandler extends AbstractHandler<byte[], byte[]> {

    @Override
    public void onMessageReceived(HandlerContext context, byte[] message) {
        context.fireMessageReceived(message);
    }

    @Override
    public void onMessageSent(HandlerContext context, byte[] message, ChannelPromise channelPromise) {
        context.write(message, channelPromise);
    }
}
