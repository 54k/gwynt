package io.gwynt.core.nio;

import io.gwynt.core.EventLoop;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.SingleThreadEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NioEventLoop extends SingleThreadEventLoop implements EventLoop {

    private static final Logger logger = LoggerFactory.getLogger(io.gwynt.core.nio.NioEventLoop.class);

    private final AtomicBoolean awakened = new AtomicBoolean(true);
    Selector selector;
    private int ioRatio = 50;
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
            AbstractNioChannel channel = (AbstractNioChannel) key.attachment();

            if (key.isValid()) {
                processSelectedKey(channel, key);
            } else {
                channel.unsafe().close(channel.voidPromise());
            }
        }
    }

    private static void processSelectedKey(AbstractNioChannel channel, SelectionKey key) {
        try {
            if (key.isReadable()) {
                channel.unsafe().doRead();
            }
            if (key.isWritable()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                channel.unsafe().doWrite();
            }
            if (key.isAcceptable()) {
                channel.unsafe().doRead();
            }
            if (key.isConnectable()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
                channel.unsafe().doConnect();
            }
        } catch (CancelledKeyException e) {
            channel.unsafe().close(channel.voidPromise());
        }
    }

    private void processSelectedKeys() {
        processSelectedKeys(selector.selectedKeys().iterator());
    }

    public int getIoRatio() {
        return ioRatio;
    }

    public void setIoRatio(int ioRatio) {
        if (ioRatio < 1 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio must be in range [1...100]");
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
        if (!inExecutorThread() && awakened.compareAndSet(false, true)) {
            selector.wakeup();
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        key.attach(null);
    }

    @Override
    protected void run() {
        for (; ; ) {
            try {
                if (hasTasks()) {
                    selectNow();
                } else {
                    select();
                }
                awakened.set(true);

                if (ioRatio == 100) {
                    processSelectedKeys();
                    runAllTasks();
                } else {
                    long s = System.nanoTime();
                    processSelectedKeys();
                    long ioTime = System.nanoTime() - s;
                    runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                }

                if (isShuttingDown()) {
                    closeAll();
                    if (confirmShutdown()) {
                        break;
                    }
                }
            } catch (Throwable e) {
                throw new RuntimeException(getClass().getSimpleName() + " throws unexpected exception", e);
            }
        }
    }

    private void selectNow() throws IOException {
        selector.selectNow();
    }

    private void select() throws IOException {
        for (; ; ) {
            long nanos = System.nanoTime();
            long deadline = closestDeadlineNanos(nanos);
            long timeout = deadline >= 0 ? TimeUnit.NANOSECONDS.toMillis(deadline) : deadline;

            if (timeout <= 0) {
                selectNow();
                break;
            }

            awakened.set(false);
            int keyCount = selector.select(timeout);

            if (keyCount > 0 || awakened.get() || hasTasks()) {
                break;
            }

            if (Thread.interrupted()) {
                logger.warn("Thread.currentThread().interrupt() was called. Use NioEventLoop.shutdownGracefully() to shutdown NioEventLoop.");
                break;
            }
        }
    }

    private void closeAll() {
        try {
            selectNow();
        } catch (IOException ignore) {
        }

        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractNioChannel> channels = new ArrayList<>(keys.size());

        for (SelectionKey k : keys) {
            AbstractNioChannel a = (AbstractNioChannel) k.attachment();
            channels.add(a);
        }

        for (AbstractNioChannel ch : channels) {
            ch.unsafe().close(ch.voidPromise());
        }

        try {
            selectNow();
        } catch (IOException ignore) {
        }
        processSelectedKeys();
    }

    @Override
    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException ignore) {
        }
    }
}
