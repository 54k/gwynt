package io.gwynt.core.transport;

import java.io.IOException;

public interface SelectorEventListener {

    void onSessionRegistered(Dispatcher dispatcher);

    void onSessionUnregistered(Dispatcher dispatcher);

    void onSelectedForRead() throws IOException;

    void onSelectedForWrite() throws IOException;

    void onExceptionCaught(Throwable e);
}
