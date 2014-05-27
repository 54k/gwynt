package io.gwynt.core.concurrent;

import java.util.concurrent.ScheduledExecutorService;

public interface EventExecutor extends ScheduledExecutorService {

    boolean inExecutorThread();
}
