package io.gwynt.core.transport;

public class NioEventLoopGroup extends NioEventLoop {

    private NioEventLoop[] workers;
    private volatile int currentWorker = 0;

    public NioEventLoopGroup() {
        this(Math.max(1, (Runtime.getRuntime().availableProcessors() - 1) * 2));
    }

    public NioEventLoopGroup(int workersCount) {
        if (workersCount < 0) {
            throw new IllegalArgumentException("workersCount must be positive");
        }
        workers = new NioEventLoop[workersCount];
        spawnWorkers(workersCount);
    }

    @Override
    public Dispatcher next() {
        if (workers.length > 0) {
            currentWorker = currentWorker % workers.length;
            Dispatcher dispatcher = workers[currentWorker];
            currentWorker++;
            return dispatcher;
        }
        return this;
    }

    private void spawnWorkers(int count) {
        for (int i = 0; i < count; i++) {
            NioEventLoop worker = new NioEventLoop();
            workers[i] = worker;
        }
    }

    @Override
    public void runThread() {
        for (NioEventLoop worker : workers) {
            worker.runThread();
        }
        super.runThread();
    }

    @Override
    public void shutdownThread() {
        for (NioEventLoop worker : workers) {
            worker.shutdownThread();
        }
        super.shutdownThread();
    }
}