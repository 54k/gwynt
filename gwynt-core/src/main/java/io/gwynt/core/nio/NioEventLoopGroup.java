package io.gwynt.core.nio;

import io.gwynt.core.EventLoop;

public class NioEventLoopGroup extends NioEventLoop {

    private NioEventLoop[] workers;
    private volatile int currentWorker = 0;

    public NioEventLoopGroup() {
        this(Math.max(1, Runtime.getRuntime().availableProcessors() * 2 - 1));
    }

    public NioEventLoopGroup(int workersCount) {
        if (workersCount < 0) {
            throw new IllegalArgumentException("workersCount must be positive");
        }
        workers = new NioEventLoop[workersCount];
        spawnWorkers(workersCount);
    }

    @Override
    public EventLoop next() {
        if (workers.length > 0) {
            currentWorker = currentWorker % workers.length;
            EventLoop scheduler = workers[currentWorker];
            currentWorker++;
            return scheduler;
        }
        return this;
    }

    private void spawnWorkers(int count) {
        for (int i = 0; i < count; i++) {
            NioEventLoop worker = new NioEventLoop(this);
            workers[i] = worker;
        }
    }

    @Override
    public void shutdown() {
        for (NioEventLoop worker : workers) {
            worker.shutdown();
        }
        super.shutdown();
    }
}
