package io.gwynt.core;

import java.util.concurrent.Executor;

public interface EventExecutor extends Executor {

    boolean inExecutorThread();
}
