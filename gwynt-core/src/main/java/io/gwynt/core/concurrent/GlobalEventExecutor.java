package io.gwynt.core.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class GlobalEventExecutor extends SingleThreadEventExecutor {

    public static final EventExecutor INSTANCE = new GlobalEventExecutor(false);

    private static final Logger logger = LoggerFactory.getLogger(GlobalEventExecutor.class);

    private GlobalEventExecutor(boolean wakeUpForTask) {
        super(wakeUpForTask);
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Override
    protected void run() {
        for (; ; ) {
            Runnable task = takeTask();
            if (task != null) {
                try {
                    task.run();
                } catch (Throwable t) {
                    logger.warn("Unexpected exception from the global event executor: ", t);
                }

                if (!(task instanceof PurgeTask)) {
                    continue;
                }

                if (!hasTasks() && pendingTasks() == 1) {
                    break;
                }
            }
        }
    }
}
