package io.gwynt.core.scheduler;

public interface EventScheduler {

    boolean inSchedulerThread();

    void schedule(Runnable task);

    void runThread();

    void shutdownThread();
}
