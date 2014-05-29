package io.gwynt.core.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SingleThreadEventExecutor extends AbstractEventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);

    protected void runTasks() {
        fetchFromDelayedQueue();
        Runnable task;
        while ((task = pollTask()) != null) {
            try {
                task.run();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected void runTasks(long timeout) {
        fetchFromDelayedQueue();
        long elapsedTime = 0;
        Runnable task;
        while ((task = pollTask()) != null) {
            long startTime = System.currentTimeMillis();
            try {
                task.run();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
            elapsedTime += System.currentTimeMillis() - startTime;
            if (elapsedTime >= timeout) {
                break;
            }
        }
    }

    @Override
    protected void run() {
        while (!isShutdown()) {
            runTasks();
        }
    }
}
