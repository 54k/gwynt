package io.gwynt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventExecutor extends AbstractExecutorService implements EventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEventExecutor.class);

    private Thread thread;
    private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private volatile boolean running;

    protected Runnable peekTask() {
        return tasks.peek();
    }

    protected Runnable pollTask() {
        return tasks.poll();
    }

    protected boolean hasTasks() {
        return !tasks.isEmpty();
    }

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        if (tasks.add(task)) {
            taskAdded(task);
        }
    }

    protected void taskAdded(Runnable task) {
    }

    protected void removeTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        if (tasks.remove(task)) {
            taskRemoved(task);
        }
    }

    protected void taskRemoved(Runnable task) {
    }

    @Override
    public boolean inExecutorThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        thread = null;
    }

    @Deprecated
    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return !running;
    }

    @Override
    public boolean isTerminated() {
        return !running && !hasTasks();
    }

    @Deprecated
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
        addTask(command);
        if (!running) {
            runThread();
        }
    }

    private void runThread() {
        if (running) {
            return;
        }
        running = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AbstractEventExecutor.this.run();
            }
        });
        thread.start();
        this.thread = thread;
    }

    protected abstract void run();
}
