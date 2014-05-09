package io.gwynt.core.transport;

import io.gwynt.core.IoSessionFactory;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public class NioDispatcher extends AbstractDispatcher {

    private IoSessionFactory<SelectableChannel, AbstractNioSession> ioSessionFactory;

    public NioDispatcher(IoSessionFactory<SelectableChannel, AbstractNioSession> ioSessionFactory) {
        this.ioSessionFactory = ioSessionFactory;
        daemon = true;
    }

    @Override
    protected int getChannelRegisterOps() {
        return SelectionKey.OP_READ;
    }

    @Override
    protected SelectorEventListener getChannelRegisterAttachment(SelectableChannel channel) {
        return ioSessionFactory.createConnection(channel);
    }

    @Override
    protected void onChannelRegistered(final SelectionKey key) {
        SelectorEventListener attachment = (SelectorEventListener) key.attachment();
        attachment.onSessionRegistered(this);
    }

    @Override
    protected void onChannelUnregistered(SelectionKey key) {
        SelectorEventListener attachment = (SelectorEventListener) key.attachment();
        attachment.onSessionUnregistered(this);
    }

    @Override
    public void processSelectedKey(SelectionKey key) throws IOException {
        SelectorEventListener attachment = (SelectorEventListener) key.attachment();
        try {
            if (key.isReadable()) {
                attachment.onSelectedForRead();
            } else if (key.isWritable()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                attachment.onSelectedForWrite();
            }
        } catch (IOException e) {
            attachment.onExceptionCaught(e);
        }
    }
}
