package io.gwynt.core.transport;

import io.gwynt.core.scheduler.AbstractEventScheduler;

public abstract class AbstractNioEventScheduler extends AbstractEventScheduler {

    private Thread thread;
    private volatile boolean running;

    @Override
    public boolean inSchedulerThread() {
        return thread == Thread.currentThread();
    }

    public void runThread() {
        if (running) {
            return;
        }
        running = true;
        Thread workerThread = new Thread(this);
        workerThread.start();
        thread = workerThread;
    }

    public void shutdownThread() {
        if (!running) {
            return;
        }
        running = false;
        thread = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
