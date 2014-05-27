package io.gwynt.core.nio;

import io.gwynt.core.EventLoop;
import io.gwynt.core.SingleThreadedEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class NioEventLoop extends SingleThreadedEventLoop implements EventLoop {

    private static final Logger logger = LoggerFactory.getLogger(io.gwynt.core.nio.NioEventLoop.class);

    private final AtomicBoolean selectorAwakened = new AtomicBoolean(true);
    Selector selector;
    private EventLoop parent;
    private int ioRatio = 100;
    private SelectorProvider selectorProvider;

    public NioEventLoop() {
        this(null);
    }

    public NioEventLoop(EventLoop parent) {
        this(parent, SelectorProvider.provider());
    }

    public NioEventLoop(EventLoop parent, SelectorProvider selectorProvider) {
        if (selectorProvider == null) {
            throw new IllegalArgumentException("selectorProvider");
        }
        this.parent = parent == null ? this : parent;
        this.selectorProvider = selectorProvider;
        openSelector();
    }

    private static void processSelectedKeys(Iterator<SelectionKey> keys) {
        while (keys != null && keys.hasNext()) {
            SelectionKey key = keys.next();
            keys.remove();

            if (key.isValid()) {
                processSelectedKey((AbstractNioChannel) key.attachment(), key);
            }
        }
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

    public int getIoRatio() {
        return ioRatio;
    }

    public void setIoRatio(int ioRatio) {
        if (ioRatio < 1 || ioRatio > 100) {
            throw new IllegalArgumentException("Must be in range[1...100]");
        }
        this.ioRatio = ioRatio;
    }

    private void openSelector() {
        try {
            this.selector = selectorProvider.openSelector();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EventLoop parent() {
        return parent;
    }

    @Override
    public EventLoop next() {
        return this;
    }

    @Override
    protected void addTask(Runnable task) {
        super.addTask(task);
        wakeUpSelector();
    }

    void wakeUpSelector() {
        if (!inExecutorThread() && !selectorAwakened.getAndSet(true)) {
            selector.wakeup();
        }
    }

    @Override
    protected void run() {
        try {
            Selector sel = selector;
            while (!isShutdown()) {
                int keyCount = 0;
                try {
                    selectorAwakened.set(false);
                    if (hasTasks()) {
                        keyCount = selector.selectNow();
                    } else {
                        keyCount = selector.select();
                    }
                    selectorAwakened.set(true);
                } catch (ClosedSelectorException e) {
                    logger.error(e.getMessage(), e);
                    break;
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
                Iterator<SelectionKey> keys = keyCount > 0 ? sel.selectedKeys().iterator() : null;

                if (ioRatio == 100) {
                    processSelectedKeys(keys);
                    runTasks();
                } else {
                    long start = System.currentTimeMillis();
                    processSelectedKeys(keys);
                    long ioTime = System.currentTimeMillis() - start;
                    runTasks(ioTime * (100 - ioRatio) / ioRatio);
                }
            }

            for (SelectionKey selectionKey : selector.keys()) {
                unregister((AbstractNioChannel) selectionKey.attachment());
                selectionKey.channel().close();
            }
            runTasks();
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}
