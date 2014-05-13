package io.gwynt.core.pipeline;

import io.gwynt.core.Handler;

public interface Pipeline {

    void addFirst(Handler handler);

    void addFirst(String name, Handler handler);

    void addLast(Handler handler);

    void addLast(String name, Handler handler);

    void addBefore(Handler handler, Handler before);

    void addBefore(Handler handler, String beforeName);

    void addBefore(String name, Handler handler, Handler before);

    void addBefore(String name, Handler handler, String beforeName);

    void addAfter(Handler handler, Handler after);

    void addAfter(Handler handler, String afterName);

    void addAfter(String name, Handler handler, Handler after);

    void addAfter(String name, Handler handler, String afterName);

    void remove(Handler handler);

    void remove(String name);
}
