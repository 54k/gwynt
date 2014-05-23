package io.gwynt.core;

import java.util.concurrent.ExecutorService;

public interface EventExecutor extends ExecutorService {

    boolean inExecutorThread();
}
