package io.gwynt.core.pipeline;

import io.gwynt.core.IoHandler;

public interface Pipeline {

    void addHandler(IoHandler ioHandler);

    void removeHandler(IoHandler ioHandler);
}
