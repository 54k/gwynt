package io.gwynt.core.transport;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface SelectorEventListener {

    void onSessionRegistered(Dispatcher dispatcher);

    void onSessionUnregistered(Dispatcher dispatcher);

    void onSelectedForRead(SelectionKey key) throws IOException;

    void onSelectedForWrite(SelectionKey key) throws IOException;

    void onExceptionCaught(Throwable e);
}
