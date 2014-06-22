package io.gwynt.core.nio;

import io.gwynt.core.EventLoop;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.SingleThreadEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NioEventLoop extends SingleThreadEventLoop implements EventLoop {

    private static final Logger logger = LoggerFactory.getLogger(io.gwynt.core.nio.NioEventLoop.class);

    private final AtomicBoolean selectorAwakened = new AtomicBoolean(true);
    Selector selector;
    private int ioRatio = 100;
    private SelectorProvider selectorProvider;

    public NioEventLoop() {
        this(null);
    }

    public NioEventLoop(EventLoopGroup parent) {
        this(parent, SelectorProvider.provider());
    }

    public NioEventLoop(EventLoopGroup parent, SelectorProvider selectorProvider) {
        super(parent, true);
        if (selectorProvider == null) {
            throw new IllegalArgumentException("selectorProvider");
        }
        this.selectorProvider = selectorProvider;
        openSelector();
    }

    public NioEventLoop(EventLoopGroup parent, SelectorProvider selectorProvider, ThreadFactory threadFactory) {
        super(parent, true, threadFactory);
        if (selectorProvider == null) {
            throw new IllegalArgumentException("selectorProvider");
        }
        this.selectorProvider = selectorProvider;
        openSelector();
    }

    public NioEventLoop(EventLoopGroup parent, SelectorProvider selectorProvider, Executor executor) {
        super(parent, true, executor);
        if (selectorProvider == null) {
            throw new IllegalArgumentException("selectorProvider");
        }
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
    public EventLoop next() {
        return this;
    }

    @Override
    protected void wakeup(boolean inExecutorThread) {
        wakeUpSelector();
    }

    void wakeUpSelector() {
        if (!inExecutorThread() && !selectorAwakened.getAndSet(true)) {
            selector.wakeup();
        }
    }

    void cancel(final SelectionKey key) {
        execute(new Runnable() {
            @Override
            public void run() {
                key.cancel();
                key.attach(null);
            }
        });
    }

    @Override
    protected void run() {
        try {
            Selector sel = selector;
            for (; ; ) {
                int keyCount = 0;

                long nanos = System.nanoTime();
                long deadline = closestDeadlineNanos(nanos);
                long timeout = deadline > -1 ? TimeUnit.NANOSECONDS.toMillis(deadline) : deadline;

                try {
                    selectorAwakened.set(false);
                    if (hasTasks() || timeout == 0) {
                        keyCount = selector.selectNow();
                    } else if (timeout == -1) {
                        keyCount = selector.select();
                    } else {
                        keyCount = selector.select(timeout);
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
                    runAllTasks();
                } else {
                    long s = System.nanoTime();
                    processSelectedKeys(keys);
                    long ioTime = System.nanoTime() - s;
                    runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                }

                if (confirmShutdown()) {
                    for (SelectionKey selectionKey : selector.keys()) {
                        unregister((AbstractNioChannel) selectionKey.attachment());
                        selectionKey.channel().close();
                    }
                    runAllTasks();
                    break;
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}
