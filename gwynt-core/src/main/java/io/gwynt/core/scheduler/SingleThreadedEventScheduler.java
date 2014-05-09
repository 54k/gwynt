package io.gwynt.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.State;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SingleThreadedEventScheduler implements EventScheduler, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(EventScheduler.class);

    private Thread schedulerThread;

    private AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch shutdownLock = new CountDownLatch(1);

    private BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

    @Override
    public boolean inSchedulerThread() {
        if (!running.get()) {
            throw new IllegalStateException("Scheduler is not running");
        }
        return schedulerThread == Thread.currentThread();
    }

    @Override
    public void schedule(Runnable task) {
        if (!running.get()) {
            throw new IllegalStateException("Scheduler is not running");
        }
        taskQueue.add(task);
    }

    @Override
    public void run() {
        shutdownLock.countDown();
        while (running.get()) {
            Runnable task;
            try {
                task = taskQueue.take();
            } catch (InterruptedException e) {
                break;
            }

            try {
                task.run();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
        shutdownLock.countDown();
    }

    @Override
    public void start() {
        running.set(true);
        Thread thread = new Thread(this);
        thread.setName("gwynt-scheduler");
        thread.start();
        schedulerThread = thread;
        try {
            shutdownLock.await();
        } catch (InterruptedException e) {
            // ignore
        }
        shutdownLock = new CountDownLatch(1);
    }

    @Override
    public void stop() {
        running.set(false);
        if (schedulerThread.getState() == State.WAITING) {
            schedulerThread.interrupt();
        }
        try {
            shutdownLock.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
