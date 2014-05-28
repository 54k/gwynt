package io.gwynt.core.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventExecutor extends AbstractExecutorService implements EventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEventExecutor.class);

    private static final Runnable WAKE_TASK = new WakeTask();

    private Thread thread;
    private Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private Queue<ScheduledFutureTask<?>> delayedTaskQueue = new PriorityQueue<>();
    private volatile boolean running;

    protected Runnable peekTask() {
        for (; ; ) {
            Runnable task = taskQueue.peek();
            if (task instanceof WakeTask) {
                continue;
            }
            return task;
        }
    }

    protected Runnable pollTask() {
        for (; ; ) {
            Runnable task = taskQueue.poll();
            if (task instanceof WakeTask) {
                continue;
            }
            return task;
        }
    }

    protected void fetchFromDelayedQueue() {
        long millisTime = 0L;
        for (; ; ) {
            ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();
            if (delayedTask == null) {
                break;
            }

            if (millisTime == 0L) {
                millisTime = ScheduledFutureTask.time();
            }

            if (delayedTask.deadline() <= millisTime) {
                delayedTaskQueue.remove();
                taskQueue.add(delayedTask);
            } else {
                break;
            }
        }
    }

    protected boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        if (taskQueue.add(task)) {
            if (!(task instanceof WakeTask)) {
                taskAdded(task);
            }
        }
    }

    protected void taskAdded(Runnable task) {
    }

    protected void removeTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        if (taskQueue.remove(task)) {
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
        addTask(WAKE_TASK);
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

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        //        new PromiseTask<>()
        return super.newTaskFor(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return super.newTaskFor(callable);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return null;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return null;
    }

    private <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        if (inExecutorThread()) {
            delayedTaskQueue.add(task);
        } else {
            execute(new Runnable() {
                @Override
                public void run() {
                    delayedTaskQueue.add(task);
                }
            });
        }

        return task;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return null;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return (Future<?>) super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return (Future<T>) super.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return (Future<T>) super.submit(task);
    }

    private static class WakeTask implements Runnable {
        @Override
        public void run() {
        }
    }
}
