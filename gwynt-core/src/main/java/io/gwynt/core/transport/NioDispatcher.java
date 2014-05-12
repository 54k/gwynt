package io.gwynt.core.transport;

import io.gwynt.core.IoSessionFactory;
import io.gwynt.core.transport.tcp.NioTcpSession;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public class NioDispatcher extends AbstractDispatcher {

    protected IoSessionFactory<SelectableChannel, NioTcpSession> ioSessionFactory;

    public NioDispatcher(IoSessionFactory<SelectableChannel, NioTcpSession> ioSessionFactory) {
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
                attachment.onSelectedForRead(key);
            } else if (key.isWritable()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                attachment.onSelectedForWrite(key);
            }
        } catch (IOException e) {
            attachment.onExceptionCaught(e);
        }
    }
}
