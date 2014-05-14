package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelListener;
import io.gwynt.core.exception.DispatcherStartupException;
import io.gwynt.core.exception.RegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class NioEventLoop implements Dispatcher {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);
    private static final ChannelListener voidCallback = new ChannelListener() {
        @Override
        public void onComplete(Channel channel) {
        }

        @Override
        public void onError(Channel channel, Throwable e) {
        }
    };

    private volatile boolean running;
    private CountDownLatch shutdownLock = new CountDownLatch(1);
    private Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();

    private Selector selector;
    private SelectorProvider selectorProvider;

    public NioEventLoop() {
        this(SelectorProvider.provider());
    }

    public NioEventLoop(SelectorProvider selectorProvider) {
        if (selectorProvider == null) {
            throw new IllegalArgumentException("selectorProvider");
        }

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
    public Dispatcher next() {
        return this;
    }

    @Override
    public ChannelFuture register(final Channel channel) {
        final ChannelFuture channelFuture = channel.newChannelFuture();
        addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.unsafe().javaChannel().register(selector, 0, channel);
                    channel.unsafe().doRegister(NioEventLoop.this);
                    channelFuture.complete();
                } catch (IOException e) {
                    channelFuture.complete(e);
                }
            }
        });
        return channelFuture;
    }

    @Override
    public ChannelFuture unregister(final Channel channel) {
        final SelectionKey key = channel.unsafe().javaChannel().keyFor(selector);
        if (key == null) {
            throw new RegistrationException("unregistered unsafe");
        }
        final ChannelFuture channelFuture = channel.newChannelFuture();

        addTask(new Runnable() {
            @Override
            public void run() {
                key.cancel();
                key.attach(null);
                channel.unsafe().doUnregister(NioEventLoop.this);
                channelFuture.complete();
            }
        });
        return channelFuture;
    }

    @Override
    public ChannelFuture modifyRegistration(final Channel channel, final int interestOps) {
        if ((interestOps & ~channel.unsafe().javaChannel().validOps()) != 0) {
            throw new IllegalArgumentException("interestOps are not valid");
        }
        final ChannelFuture channelFuture = channel.newChannelFuture();

        addTask(new Runnable() {
            @Override
            public void run() {
                SelectionKey key = channel.unsafe().javaChannel().keyFor(selector);
                if (key != null && key.isValid()) {
                    key.interestOps(key.interestOps() | interestOps);
                    channelFuture.complete();
                }
            }
        });
        return channelFuture;
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
            throw new IllegalStateException("thread already started");
        }

        running = true;
        Thread workerThread = new SelectorLoopThread();
        workerThread.start();
        try {
            shutdownLock.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void shutdownThread() {
        if (!running) {
            throw new IllegalStateException("thread already stopped");
        }

        running = false;
        selector.wakeup();
        try {
            shutdownLock.await();
        } catch (InterruptedException e) {
            // ignore
        }
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
