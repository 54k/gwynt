package io.gwynt.core.transport;

import io.gwynt.core.exception.DispatcherStartupException;
import io.gwynt.core.exception.RegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDispatcher implements Dispatcher {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractDispatcher.class);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    protected Selector selector;
    protected boolean daemon;
    private CountDownLatch lock = new CountDownLatch(1);
    private String name;

    protected AbstractDispatcher() {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new DispatcherStartupException(e);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void register(final SelectableChannel channel) {
        if (!running.get()) {
            throw new IllegalStateException("Not running");
        }

        addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    //noinspection MagicConstant
                    SelectionKey key = channel.register(selector, getChannelRegisterOps(), getChannelRegisterAttachment(channel));
                    onChannelRegistered(key);
                } catch (IOException e) {
                    throw new RegistrationException(e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void unregister(final SelectableChannel channel) {
        addTask(new Runnable() {
            @Override
            public void run() {
                SelectionKey key = channel.keyFor(selector);
                if (key == null) {
                    throw new RegistrationException("Unregistered channel");
                }
                key.cancel();
                onChannelUnregistered(key);
                key.attach(null);
            }
        });
    }

    @Override
    public void modifyRegistration(final SelectableChannel channel, final int interestOps) {
        addTask(new Runnable() {
            @Override
            public void run() {
                SelectionKey key = channel.keyFor(selector);
                if (key != null && key.isValid()) {
                    key.interestOps(key.interestOps() | interestOps);
                }
            }
        });
    }

    protected abstract int getChannelRegisterOps();

    protected SelectorEventListener getChannelRegisterAttachment(SelectableChannel channel) {
        return null;
    }

    protected void onChannelRegistered(SelectionKey key) {
    }

    protected void onChannelUnregistered(SelectionKey key) {
    }

    protected void addTask(Runnable task) {
        tasks.add(task);
        selector.wakeup();
    }

    private void performTasks() {
        while (tasks.peek() != null) {
            try {
                tasks.poll().run();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected abstract void processSelectedKey(SelectionKey key) throws IOException;

    @Override
    public void start() {
        if (running.get()) {
            throw new IllegalStateException("Already started");
        }

        running.set(true);
        Thread workerThread = new SelectorLoopWorker();
        workerThread.setName(name);
        workerThread.setDaemon(daemon);
        workerThread.start();
        try {
            lock.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    public void stop() {
        if (!running.get()) {
            throw new IllegalStateException("Already stopped");
        }

        running.set(false);
        selector.wakeup();
        try {
            lock.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private class SelectorLoopWorker extends Thread {

        @Override
        public void run() {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new DispatcherStartupException(e);
            }
            lock.countDown();

            try (Selector sel = selector) {
                while (running.get()) {
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
                            try {
                                processSelectedKey(key);
                            } catch (IOException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }

                tasks.clear();
                for (SelectionKey selectionKey : selector.keys()) {
                    unregister(selectionKey.channel());
                    selectionKey.channel().close();
                }
                performTasks();
                lock.countDown();
            } catch (Throwable e) {
                throw new RuntimeException("Unexpected exception", e);
            }
        }
    }
}
