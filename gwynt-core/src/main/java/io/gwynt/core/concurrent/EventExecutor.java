package io.gwynt.core.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public interface EventExecutor extends ExecutorService, ScheduledExecutorService {

    boolean inExecutorThread();
}
