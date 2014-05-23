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
    public void runThread() {
    }

    @Override
    public void shutdownThread() {
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void shutdown() {
    }

    @Deprecated
    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Deprecated
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {

    }
}
