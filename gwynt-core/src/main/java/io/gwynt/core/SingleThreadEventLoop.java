package io.gwynt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SingleThreadEventLoop extends AbstractEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

    protected void runTasks() {
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
