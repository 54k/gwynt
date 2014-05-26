package io.gwynt.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public interface EventExecutor extends ExecutorService, ScheduledExecutorService {

    boolean inExecutorThread();
}
