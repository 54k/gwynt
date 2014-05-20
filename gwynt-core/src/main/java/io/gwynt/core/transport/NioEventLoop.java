package io.gwynt.core.transport;

import io.gwynt.core.exception.DispatcherStartupException;
import io.gwynt.core.scheduler.AbstractEventScheduler;
import io.gwynt.core.scheduler.EventScheduler;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class NioEventLoop extends AbstractEventScheduler {

    private final AtomicBoolean selectorAwaken = new AtomicBoolean(true);
    Selector selector;
    private NioEventLoop parent;

    private SelectorProvider selectorProvider;

    public NioEventLoop() {
        this(null);
    }

    public NioEventLoop(NioEventLoop parent) {
        this(parent, SelectorProvider.provider());
    }

    public NioEventLoop(NioEventLoop parent, SelectorProvider selectorProvider) {
        if (selectorProvider == null) {
            throw new IllegalArgumentException("selectorProvider");
        }
        this.parent = parent == null ? this : parent;
        this.selectorProvider = selectorProvider;
        openSelector();
    }

    private static void processSelectedKey(AbstractNioChannel channel, SelectionKey key) {
        try {
            if (key.isReadable()) {
                channel.unsafe().doRead();
            } else if (key.isWritable()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                channel.unsafe().doWrite();
            } else if (key.isAcceptable()) {
                channel.unsafe().doRead();
            } else if (key.isConnectable()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
                channel.unsafe().doConnect();
            }
        } catch (Throwable e) {
            channel.unsafe().exceptionCaught(e);
        }
    }

    private void openSelector() {
        try {
            this.selector = selectorProvider.openSelector();
        } catch (IOException e) {
            throw new DispatcherStartupException(e);
        }
    }

    @Override
    public EventScheduler parent() {
        return parent;
    }

    @Override
    public EventScheduler next() {
        return this;
    }

    protected void addTask(Runnable task) {
        super.addTask(task);
        wakeUpSelector();
    }

    private void wakeUpSelector() {
        if (!selectorAwaken.getAndSet(true)) {
            selector.wakeup();
        }
    }

    @Override
    public void run() {
        try (Selector sel = selector) {
            while (isRunning()) {
                long start = System.currentTimeMillis();
                int keyCount = 0;
                try {
                    selectorAwaken.set(false);
                    if (hasTasks()) {
                        keyCount = selector.selectNow();
                    } else {
                        keyCount = selector.select();
                    }
                } catch (ClosedSelectorException e) {
                    logger.error(e.getMessage(), e);
                    break;
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }

                Iterator<SelectionKey> keys = keyCount > 0 ? sel.selectedKeys().iterator() : null;

                while (keys != null && keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isValid()) {
                        processSelectedKey((AbstractNioChannel) key.attachment(), key);
                    }
                }

                runTasks(System.currentTimeMillis() - start);
            }

            for (SelectionKey selectionKey : selector.keys()) {
                unregister((AbstractNioChannel) selectionKey.attachment());
                selectionKey.channel().close();
            }
            runTasks(1000);
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}
