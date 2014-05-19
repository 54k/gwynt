package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.exception.DispatcherStartupException;
import io.gwynt.core.exception.RegistrationException;
import io.gwynt.core.scheduler.EventScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class NioEventLoop implements Dispatcher, EventScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);

    Selector selector;

    private NioEventLoop parent;

    private Thread thread;
    private volatile boolean running;
    private CountDownLatch shutdownLock = new CountDownLatch(1);

    private Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();
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
                channel.unsafe().doAccept();
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
    public Dispatcher parent() {
        return parent;
    }

    @Override
    public Dispatcher next() {
        return this;
    }

    @Override
    public ChannelFuture register(final Channel channel) {
        return register(channel, channel.newChannelPromise());
    }

    @Override
    public ChannelFuture unregister(Channel channel) {
        return unregister(channel, channel.newChannelPromise());
    }

    @Override
    public ChannelFuture register(final Channel channel, final ChannelPromise channelPromise) {
        addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.unsafe().register(NioEventLoop.this);
                    channelPromise.complete();
                } catch (ChannelException e) {
                    channelPromise.complete(e);
                }
            }
        });
        return channelPromise;
    }

    @Override
    public ChannelFuture unregister(final Channel channel, final ChannelPromise channelPromise) {
        final SelectionKey selectionKey = ((SelectableChannel) channel.unsafe().javaChannel()).keyFor(selector);
        if (selectionKey == null) {
            throw new RegistrationException("unregistered unsafe");
        }

        addTask(new Runnable() {
            @Override
            public void run() {
                selectionKey.cancel();
                selectionKey.attach(null);
                channel.unsafe().unregister();
                channelPromise.complete();
            }
        });
        return channelPromise;
    }

    protected void addTask(Runnable task) {
        pendingTasks.add(task);
        selector.wakeup();
    }

    private void performTasks() {
        while (pendingTasks.peek() != null) {
            try {
                pendingTasks.poll().run();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void runThread() {
        if (running) {
            return;
        }

        running = true;
        Thread workerThread = new SelectorLoopThread();
        workerThread.start();
        thread = workerThread;
        try {
            shutdownLock.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void shutdownThread() {
        if (!running) {
            return;
        }

        running = false;
        selector.wakeup();
        try {
            shutdownLock.await();
        } catch (InterruptedException e) {
            // ignore
        }
        thread = null;
    }

    @Override
    public boolean inSchedulerThread() {
        return parent.thread == Thread.currentThread();
    }

    @Override
    public void schedule(Runnable task) {
        parent.addTask(task);
    }

    private class SelectorLoopThread extends Thread {

        @Override
        public void run() {
            shutdownLock.countDown();

            try (Selector sel = selector) {
                while (running) {
                    performTasks();

                    int keyCount = 0;
                    try {
                        keyCount = selector.select();
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
                }

                pendingTasks.clear();
                for (SelectionKey selectionKey : selector.keys()) {
                    unregister((AbstractNioChannel) selectionKey.attachment());
                    selectionKey.channel().close();
                }
                performTasks();
                shutdownLock.countDown();
            } catch (Throwable e) {
                throw new RuntimeException("Unexpected exception", e);
            }
        }
    }
}
